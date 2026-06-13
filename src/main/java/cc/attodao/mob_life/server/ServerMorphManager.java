package cc.attodao.mob_life.server;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.jump.ChargedJumpingPlayer;
import cc.attodao.mob_life.gameplay.jump.MobChargedJump;
import cc.attodao.mob_life.gameplay.targeting.MorphPredation;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import cc.attodao.mob_life.world.WorldMorphData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

public final class ServerMorphManager {

    private static final Map<UUID, Long> LAST_CHARGED_JUMP_TICK =
        new HashMap<>();
    private static final Map<UUID, Integer> SPRINT_TICKS = new HashMap<>();
    private static final Map<UUID, Float> LAST_SYNCED_AWKWARDNESS =
        new HashMap<>();

    private static final float PASSIVE_DECAY_PER_SECOND = 0.2F;
    private static final float SAME_MOB_DECAY_PER_SECOND = 1.0F;
    private static final float NON_FORWARD_MOVEMENT_GAIN = 0.04F;
    private static final float LONG_SPRINT_GAIN = 0.08F;
    private static final int LONG_SPRINT_START_TICKS = 60;

    private static MorphDefinition activeDefinition;
    private static EntityDimensions activeDimensions;
    private static float activeEyeHeight;

    private ServerMorphManager() {}

    public static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            WorldMorphData data = server
                .getDataStorage()
                .computeIfAbsent(WorldMorphData.TYPE);
            data.setDirty();
            setActiveDefinition(server, data.definition());
            MobLife.LOGGER.info(
                "World morph locked to {} with NBT {}",
                activeDefinition.type().id(),
                activeDefinition.nbt()
            );
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            activeDefinition = null;
            activeDimensions = null;
            activeEyeHeight = 0.0F;
            LAST_CHARGED_JUMP_TICK.clear();
            SPRINT_TICKS.clear();
            LAST_SYNCED_AWKWARDNESS.clear();
        });

        ServerPlayerEvents.JOIN.register(ServerMorphManager::initializePlayer);
        ServerPlayerEvents.AFTER_RESPAWN.register(
            (oldPlayer, newPlayer, alive) -> {
                MorphAwkwardness.set(
                    newPlayer,
                    MorphAwkwardness.get(oldPlayer)
                );
                initializePlayer(newPlayer);
            }
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!hasMobForm()) {
                return;
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                addMovementExhaustion(player);
                tickAwkwardness(player);
                if (activeMorph() == MorphType.CHICKEN) {
                    slowChickenFall(player);
                }
                if (player.tickCount % 10 == 0) {
                    MorphPredation.acquirePredators(player, activeMorph());
                }
            }
        });
    }

    public static MorphType activeMorph() {
        return activeDefinition != null ? activeDefinition.type() : null;
    }

    public static EntityDimensions activeDimensions() {
        return activeDimensions;
    }

    public static float activeEyeHeight() {
        return activeEyeHeight;
    }

    public static boolean hasMobForm() {
        return activeDefinition != null && activeDefinition.hasMobForm();
    }

    public static void performChargedJump(
        ServerPlayer player,
        int chargeAmount
    ) {
        if (
            !hasMobForm() || !player.onGround() || player.getAbilities().flying
        ) {
            return;
        }

        long gameTime = player.level().getGameTime();
        long lastJumpTick = LAST_CHARGED_JUMP_TICK.getOrDefault(
            player.getUUID(),
            Long.MIN_VALUE
        );
        if (gameTime - lastJumpTick < MobChargedJump.COOLDOWN_TICKS) {
            return;
        }

        float jumpScale = MobChargedJump.jumpScale(chargeAmount);
        ((ChargedJumpingPlayer) player).mobLife$performChargedJump(jumpScale);
        LAST_CHARGED_JUMP_TICK.put(player.getUUID(), gameTime);
        player.awardStat(Stats.JUMP);
        player.causeFoodExhaustion(0.4F);
    }

    public static void adjustAwkwardness(ServerPlayer player, float amount) {
        if (!hasMobForm()) {
            return;
        }

        float oldValue = MorphAwkwardness.get(player);
        float newValue = MorphAwkwardness.add(player, amount);
        if (newValue != oldValue) {
            syncAwkwardness(player, true);
        }
    }

    public static void setAwkwardness(ServerPlayer player, float value) {
        MorphAwkwardness.set(player, value);
        syncAwkwardness(player, true);
    }

    public static void changeMorph(
        MinecraftServer server,
        MorphDefinition definition
    ) {
        WorldMorphData data = server
            .getDataStorage()
            .computeIfAbsent(WorldMorphData.TYPE);
        data.setDefinition(definition);
        setActiveDefinition(server, definition);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayerMorphApplier.apply(player, definition, true);
        }

        MobLife.LOGGER.info(
            "World morph changed to {} with NBT {}",
            definition.type().id(),
            definition.nbt()
        );
    }

    private static void initializePlayer(ServerPlayer player) {
        MorphDefinition definition = activeDefinition;
        if (definition == null) {
            return;
        }

        ServerPlayerMorphApplier.apply(player, definition, false);
        syncAwkwardness(player, true);
    }

    private static void setActiveDefinition(
        MinecraftServer server,
        MorphDefinition definition
    ) {
        activeDefinition = definition;
        activeDimensions = null;
        activeEyeHeight = 0.0F;
        if (!definition.hasMobForm()) {
            return;
        }

        Entity entity = MorphEntityFactory.create(
            definition,
            server.overworld()
        );
        if (entity != null) {
            activeDimensions = entity.getDimensions(Pose.STANDING);
            activeEyeHeight = entity.getEyeHeight();
        }
    }

    private static void slowChickenFall(ServerPlayer player) {
        if (player.onGround() || player.getAbilities().flying) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < 0.0) {
            player.setDeltaMovement(velocity.x, velocity.y * 0.6, velocity.z);
        }
    }

    private static void addMovementExhaustion(ServerPlayer player) {
        if (player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4) {
            return;
        }

        if (player.isSprinting()) {
            player.causeFoodExhaustion(0.02F);
        } else if (player.isShiftKeyDown()) {
            player.causeFoodExhaustion(0.01F);
        }
    }

    private static void tickAwkwardness(ServerPlayer player) {
        float delta = 0.0F;
        Input input = player.getLastClientInput();
        boolean moving =
            input.forward() ||
            input.backward() ||
            input.left() ||
            input.right();
        if (moving && (input.backward() || input.left() || input.right())) {
            delta += NON_FORWARD_MOVEMENT_GAIN;
        }

        UUID uuid = player.getUUID();
        if (moving && player.isSprinting()) {
            int sprintTicks = SPRINT_TICKS.merge(uuid, 1, Integer::sum);
            if (sprintTicks > LONG_SPRINT_START_TICKS) {
                delta += LONG_SPRINT_GAIN;
            }
        } else {
            SPRINT_TICKS.remove(uuid);
        }

        if (player.tickCount % 20 == 0) {
            delta -= PASSIVE_DECAY_PER_SECOND;
            if (hasNearbySameMob(player)) {
                delta -= SAME_MOB_DECAY_PER_SECOND;
            }
        }

        if (delta != 0.0F) {
            MorphAwkwardness.add(player, delta);
        }
        if (player.tickCount % 5 == 0) {
            syncAwkwardness(player, false);
        }
    }

    private static boolean hasNearbySameMob(ServerPlayer player) {
        MorphType morph = activeMorph();
        if (morph == null || morph.isPlayer()) {
            return false;
        }

        return !player
            .level()
            .getEntities(
                player,
                player.getBoundingBox().inflate(6.0),
                entity ->
                    entity.getType() == morph.entityType() ||
                    entity instanceof ServerPlayer
            )
            .isEmpty();
    }

    private static void syncAwkwardness(ServerPlayer player, boolean force) {
        float value = MorphAwkwardness.get(player);
        Float previous = LAST_SYNCED_AWKWARDNESS.get(player.getUUID());
        if (!force && previous != null && Math.abs(previous - value) < 0.05F) {
            return;
        }

        LAST_SYNCED_AWKWARDNESS.put(player.getUUID(), value);
        ServerPlayNetworking.send(
            player,
            new MobLifeNetworking.AwkwardnessPayload(value)
        );
    }
}
