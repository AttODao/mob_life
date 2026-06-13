package cc.attodao.mob_life.server;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.gameplay.jump.ChargedJumpingPlayer;
import cc.attodao.mob_life.gameplay.jump.MobChargedJump;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.world.WorldMorphData;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ServerMorphManager {
    private static final Map<UUID, Long> LAST_CHARGED_JUMP_TICK =
        new HashMap<>();

    private static MorphType activeMorph;

    private ServerMorphManager() {}

    public static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            WorldMorphData data = server
                .getDataStorage()
                .computeIfAbsent(WorldMorphData.TYPE);
            data.setDirty();
            activeMorph = data.morph();
            MobLife.LOGGER.info("World morph locked to {}", activeMorph.id());
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(
            server -> {
                activeMorph = null;
                LAST_CHARGED_JUMP_TICK.clear();
            }
        );

        ServerPlayerEvents.JOIN.register(ServerMorphManager::initializePlayer);
        ServerPlayerEvents.AFTER_RESPAWN.register(
            (oldPlayer, newPlayer, alive) -> initializePlayer(newPlayer)
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!hasMobForm()) {
                return;
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                addMovementExhaustion(player);
                if (activeMorph == MorphType.CHICKEN) {
                    slowChickenFall(player);
                }
            }
        });
    }

    public static MorphType activeMorph() {
        return activeMorph;
    }

    public static boolean hasMobForm() {
        return activeMorph != null && !activeMorph.isPlayer();
    }

    public static void performChargedJump(
        ServerPlayer player,
        int chargeAmount
    ) {
        if (
            !hasMobForm() ||
            !player.onGround() ||
            player.getAbilities().flying
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

    public static void changeMorph(MinecraftServer server, MorphType morph) {
        WorldMorphData data = server
            .getDataStorage()
            .computeIfAbsent(WorldMorphData.TYPE);
        data.setMorph(morph);
        activeMorph = morph;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayerMorphApplier.apply(player, morph, true);
        }

        MobLife.LOGGER.info("World morph changed to {}", morph.id());
    }

    private static void initializePlayer(ServerPlayer player) {
        MorphType morph = activeMorph;
        if (morph == null) {
            return;
        }

        ServerPlayerMorphApplier.apply(player, morph, false);
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
}
