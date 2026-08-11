package cc.attodao.mob_life.server;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.config.MobLifeConfig;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.ability.MorphAbility;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.combat.MorphAttackDamage;
import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.gameplay.jump.ChargedJumpingPlayer;
import cc.attodao.mob_life.gameplay.jump.MobChargedJump;
import cc.attodao.mob_life.gameplay.movement.MorphMovementSpeed;
import cc.attodao.mob_life.gameplay.movement.RabbitHopMovement;
import cc.attodao.mob_life.gameplay.targeting.MorphOutlineManager;
import cc.attodao.mob_life.gameplay.targeting.MorphPredation;
import cc.attodao.mob_life.mixin.sound.LivingEntitySoundAccessor;
import cc.attodao.mob_life.mixin.sound.MobSoundAccessor;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import cc.attodao.mob_life.network.MobLifeNetworking.MorphConfigEntry;
import cc.attodao.mob_life.network.MobLifeNetworking.WorldMorphSelectionPromptPayload;
import cc.attodao.mob_life.world.MorphInitialSpawn;
import cc.attodao.mob_life.world.PendingWorldSelection;
import cc.attodao.mob_life.world.WorldMorphData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class ServerMorphManager {

  private static final Map<UUID, Long> JUMP_COOLDOWN_UNTIL_TICKS = new HashMap<>();
  private static final Map<UUID, Boolean> LAST_JUMP_GROUNDED_STATES = new HashMap<>();
  private static final Map<UUID, Integer> RABBIT_HOP_COOLDOWNS = new HashMap<>();
  private static final Map<UUID, Integer> AMBIENT_SOUND_TIMES = new HashMap<>();
  private static final Map<UUID, Integer> GRASS_EATING_TICKS = new HashMap<>();
  private static final Map<UUID, Float> LAST_SYNCED_AWKWARDNESS = new HashMap<>();

  private static final float PASSIVE_DECAY_PER_SECOND = 0.2F;
  private static final float EMPTY_INVENTORY_DECAY_MULTIPLIER = 5.0F;
  private static final float ITEM_DECAY_SCALE = 16.0F;
  private static final float SAME_MOB_DECAY_PER_SECOND = 1.0F;
  private static final float NON_FORWARD_MOVEMENT_GAIN = 0.04F;
  private static final float NORMAL_AWKWARDNESS_GAIN_MULTIPLIER = 2.0F;
  private static final float MAX_SINGLE_NORMAL_AWKWARDNESS_GAIN = 20.0F;
  private static final int GRASS_EATING_DURATION_TICKS = 40;
  private static final int GRASS_FOOD_RESTORE = 2;

  private static MorphDefinition activeDefinition;
  private static MorphConfig activeConfig;
  private static EntityDimensions activeDimensions;
  private static float activeEyeHeight;
  private static float activeWaterMovementInputScale = 1.0F;
  private static boolean activeFallDamageImmune;
  private static boolean activeHasAttackAi;
  private static Mob activeSoundMob;

  private ServerMorphManager() {}

  public static void registerEvents() {
    ServerLifecycleEvents.SERVER_STARTED.register(
        server -> {
          WorldMorphData data = worldData(server);
          if (!data.selectionChosen()) {
            Optional<PendingWorldSelection.PendingSelection> pendingSelection =
                PendingWorldSelection.consumeSelection();
            if (pendingSelection.isPresent()) {
              PendingWorldSelection.PendingSelection selection = pendingSelection.get();
              data.setDefinition(selection.definition());
              data.markSelectionChosen();
              if (!data.initialSpawnConfigured()) {
                MorphInitialSpawn.configure(server.overworld(), data, selection.preRandomized());
              }
            } else {
              if (data.morph() == MorphType.PLAYER) {
                data.clearInitialSpawnConfigured();
                MobLife.LOGGER.info("World morph selection is pending");
                return;
              }

              data.markSelectionChosen();
            }
          }

          if (!data.initialSpawnConfigured()) {
            MorphInitialSpawn.configure(server.overworld(), data);
          }
          setActiveDefinition(server, data.definition());
          MobLife.LOGGER.info(
              "World morph locked to {} with NBT {}",
              activeDefinition.type().id(),
              activeDefinition.nbt());
        });

    ServerLifecycleEvents.SERVER_STOPPED.register(
        server -> {
          InstinctManager.clear();
          MorphOutlineManager.clear(server);
          resetActiveMorph();
          clearServerPlayerState();
          LAST_SYNCED_AWKWARDNESS.clear();
        });
    ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
        (server, resourceManager, success) -> {
          if (!success || activeDefinition == null) {
            return;
          }
          InstinctManager.clear();
          MorphOutlineManager.clear(server);
          setActiveDefinition(server, activeDefinition);
          for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayerMorphApplier.apply(player, activeDefinition, true);
          }
        });

    ServerPlayerEvents.JOIN.register(ServerMorphManager::initializePlayer);
    ServerPlayConnectionEvents.DISCONNECT.register(
        (handler, server) -> {
          InstinctManager.remove(handler.getPlayer());
          MorphOutlineManager.remove(handler.getPlayer());
        });
    ServerPlayerEvents.AFTER_RESPAWN.register(
        (oldPlayer, newPlayer, alive) -> {
          InstinctManager.disable(newPlayer);
          InstinctManager.forget(newPlayer);
          MorphOutlineManager.remove(oldPlayer);
          MorphAwkwardness.set(newPlayer, MorphAwkwardness.get(oldPlayer));
          MorphAbility.copy(oldPlayer, newPlayer);
          initializePlayer(newPlayer);
        });

    ServerTickEvents.END_SERVER_TICK.register(
        server -> {
          if (!hasMobForm()) {
            return;
          }

          for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
          }
        });
  }

  public static MorphType activeMorph() {
    return activeDefinition != null ? activeDefinition.type() : null;
  }

  public static MorphDefinition activeDefinition() {
    return activeDefinition;
  }

  public static EntityDimensions activeDimensions() {
    return activeDimensions;
  }

  public static float activeEyeHeight() {
    return activeEyeHeight;
  }

  public static float activeWaterMovementInputScale() {
    return activeWaterMovementInputScale;
  }

  public static boolean hasMobForm() {
    return activeDefinition != null && activeDefinition.hasMobForm();
  }

  public static boolean activeMorphFallsImmune() {
    return hasMobForm() && activeFallDamageImmune;
  }

  public static boolean activeMorphHasAttackAi() {
    return hasMobForm() && activeHasAttackAi;
  }

  public static boolean playMorphHurtSound(ServerPlayer player, DamageSource source) {
    Mob soundMob = activeSoundMob;
    if (!hasMobForm() || soundMob == null) {
      return false;
    }

    AMBIENT_SOUND_TIMES.put(player.getUUID(), -soundMob.getAmbientSoundInterval());
    if (soundMob.isSilent()) {
      return true;
    }

    LivingEntitySoundAccessor sounds = (LivingEntitySoundAccessor) soundMob;
    SoundEvent sound = sounds.mobLife$getHurtSound(source);
    if (sound != null) {
      playMorphSound(player, sound, soundMob, sounds.mobLife$getSoundVolume());
    }
    return true;
  }

  public static void performChargedJump(ServerPlayer player, int chargeAmount) {
    if (!hasMobForm() || activeConfig().movement().rabbitHop().enabled()) {
      return;
    }

    syncJumpCooldown(player);
    if (!isJumpGrounded(player) || isJumpCoolingDown(player)) {
      return;
    }

    float jumpScale = MobChargedJump.jumpScale(chargeAmount);
    ((ChargedJumpingPlayer) player).mobLife$performChargedJump(jumpScale);
    player.awardStat(Stats.JUMP);
    player.causeFoodExhaustion(0.4F);
  }

  public static boolean canUseInstinctJump(ServerPlayer player) {
    syncJumpCooldown(player);
    return isJumpGrounded(player) && !isJumpCoolingDown(player);
  }

  public static void adjustAwkwardness(ServerPlayer player, float amount) {
    if (!hasMobForm()) {
      return;
    }

    boolean instinct = InstinctManager.isEnabled(player);
    if (amount > 0.0F) {
      if (instinct) {
        return;
      }
      amount =
          Math.min(amount * NORMAL_AWKWARDNESS_GAIN_MULTIPLIER, MAX_SINGLE_NORMAL_AWKWARDNESS_GAIN);
    } else if (amount < 0.0F && !instinct) {
      return;
    }

    float oldValue = MorphAwkwardness.get(player);
    float newValue = MorphAwkwardness.add(player, amount);
    if (newValue != oldValue) {
      syncAwkwardness(player, true);
    }
    InstinctManager.forceEnableAtMaximum(player);
  }

  public static void setAwkwardness(ServerPlayer player, float value) {
    MorphAwkwardness.set(player, value);
    syncAwkwardness(player, true);
    InstinctManager.forceEnableAtMaximum(player);
  }

  public static void changeMorph(MinecraftServer server, MorphDefinition definition) {
    if (!MobLifeConfig.isMorphEnabled(definition.type())) {
      MobLife.LOGGER.warn("Ignoring disabled morph selection {}", definition.type().id());
      return;
    }
    MorphDefinition resolvedDefinition =
        MorphEntityFactory.randomizeAt(
            definition, server.overworld(), server.overworld().getRespawnData().pos());
    WorldMorphData data = worldData(server);
    data.setDefinition(resolvedDefinition);
    data.markSelectionChosen();
    if (!data.initialSpawnConfigured()) {
      MorphInitialSpawn.configure(server.overworld(), data);
    }
    setActiveDefinition(server, data.definition());

    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      ServerPlayerMorphApplier.apply(player, data.definition(), true);
    }

    MobLife.LOGGER.info(
        "World morph changed to {} with NBT {}",
        data.definition().type().id(),
        data.definition().nbt());
  }

  public static void completeWorldSelection(
      MinecraftServer server, String morphId, net.minecraft.nbt.CompoundTag nbt) {
    MorphType morph = MorphType.fromId(morphId);
    if (!morph.id().equals(morphId)) {
      MobLife.LOGGER.warn("Ignoring unknown world morph selection {}", morphId);
      return;
    }

    changeMorph(server, new MorphDefinition(morph, nbt));
  }

  private static void initializePlayer(ServerPlayer player) {
    WorldMorphData data = worldData(player.level().getServer());
    if (!data.selectionChosen()) {
      sendWorldSelectionPrompt(player);
      return;
    }

    MorphDefinition definition = activeDefinition;
    if (definition == null) {
      return;
    }

    GRASS_EATING_TICKS.remove(player.getUUID());
    boolean restoreInstinct = InstinctManager.shouldRestore(player);
    InstinctManager.disable(player);
    MorphOutlineManager.remove(player);
    ServerPlayerMorphApplier.apply(player, definition, false);
    if (restoreInstinct) {
      InstinctManager.enable(player);
    }
    syncAwkwardness(player, true);
    if (!activeConfig().movement().rabbitHop().enabled()) {
      syncJumpCooldown(player);
    }
  }

  private static void sendWorldSelectionPrompt(ServerPlayer player) {
    var morphs = MobLifeConfig.selectableMorphs();
    ArrayList<MorphConfigEntry> configs = new ArrayList<>(morphs.size());
    for (MorphType morph : morphs) {
      configs.add(new MorphConfigEntry(morph.id(), MorphConfigManager.encode(morph)));
    }
    ServerPlayNetworking.send(player, new WorldMorphSelectionPromptPayload(configs));
  }

  private static WorldMorphData worldData(MinecraftServer server) {
    return server.getDataStorage().computeIfAbsent(WorldMorphData.TYPE);
  }

  private static void setActiveDefinition(MinecraftServer server, MorphDefinition definition) {
    InstinctManager.clear();
    MorphOutlineManager.clear(server);
    clearPerMorphEffectState();
    resetActiveMorph();
    activeDefinition = definition;
    activeConfig = MorphConfigManager.get(definition.type());
    if (!definition.hasMobForm()) {
      return;
    }

    Entity entity = MorphEntityFactory.create(definition, server.overworld());
    if (entity != null) {
      applyActiveEntityProperties(definition, entity);
    }
  }

  private static void resetActiveMorph() {
    activeDefinition = null;
    activeConfig = null;
    activeDimensions = null;
    activeEyeHeight = 0.0F;
    activeWaterMovementInputScale = 1.0F;
    activeFallDamageImmune = false;
    activeHasAttackAi = false;
    activeSoundMob = null;
  }

  private static void clearPerMorphEffectState() {
    JUMP_COOLDOWN_UNTIL_TICKS.clear();
    LAST_JUMP_GROUNDED_STATES.clear();
    RABBIT_HOP_COOLDOWNS.clear();
    AMBIENT_SOUND_TIMES.clear();
    GRASS_EATING_TICKS.clear();
  }

  private static void clearServerPlayerState() {
    clearPerMorphEffectState();
  }

  private static void applyActiveEntityProperties(MorphDefinition definition, Entity entity) {
    MorphConfig config = activeConfig();
    activeDimensions = entity.getDimensions(Pose.STANDING);
    activeEyeHeight = entity.getEyeHeight();
    activeFallDamageImmune = config.traits().fallDamageImmune();
    if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
      activeHasAttackAi = MorphAttackDamage.hasAttackAi(definition.type(), living);
      activeWaterMovementInputScale =
          (float)
                  living.getAttributeValue(
                      net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
              * config.movement().waterInputMultiplier();
    }
    if (entity instanceof Mob mob) {
      activeSoundMob = mob;
    }
  }

  private static void tickPlayer(ServerPlayer player) {
    InstinctManager.forceEnableAtMaximum(player);
    InstinctManager.tick(player);
    boolean instinct = InstinctManager.isEnabled(player);
    MorphOutlineManager.tick(player, activeMorph(), activeMorphHasAttackAi());
    tickGrassEating(player);
    addMovementExhaustion(player);
    tickAwkwardness(player);
    InstinctManager.forceEnableAtMaximum(player);
    instinct = InstinctManager.isEnabled(player);
    tickAmbientSound(player);
    if (player.tickCount % 20 == 0) {
      refreshChestedInventory(player);
    }
    clearMorphNightVisionEffect(player);

    MorphConfig.Movement movement = activeConfig().movement();
    if (!instinct && movement.slowFallMultiplier() < 1.0F) {
      slowChickenFall(player);
    }
    if (movement.rabbitHop().enabled()) {
      tickRabbitHop(player);
    } else {
      syncJumpCooldown(player);
    }
    if (player.tickCount % 10 == 0) {
      MorphPredation.acquirePredators(player, activeMorph());
    }
  }

  private static void tickAmbientSound(ServerPlayer player) {
    Mob soundMob = activeSoundMob;
    if (soundMob == null || soundMob.isSilent() || !player.isAlive()) {
      return;
    }

    UUID uuid = player.getUUID();
    int ambientSoundTime = AMBIENT_SOUND_TIMES.getOrDefault(uuid, 0);
    if (player.getRandom().nextInt(1000) < ambientSoundTime++) {
      SoundEvent sound = ((MobSoundAccessor) soundMob).mobLife$getAmbientSound();
      if (sound != null) {
        playMorphSound(
            player,
            sound,
            soundMob,
            ((LivingEntitySoundAccessor) soundMob).mobLife$getSoundVolume());
      }
      ambientSoundTime = -soundMob.getAmbientSoundInterval();
    }
    AMBIENT_SOUND_TIMES.put(uuid, ambientSoundTime);
  }

  private static void playMorphSound(
      ServerPlayer player, SoundEvent sound, Mob soundMob, float volume) {
    player
        .level()
        .playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            sound,
            soundMob.getSoundSource(),
            volume,
            soundMob.getVoicePitch());
  }

  private static boolean isMorphNightVision(MobEffectInstance effect) {
    return effect.getAmplifier() == 0
        && effect.isAmbient()
        && !effect.isVisible()
        && !effect.showIcon();
  }

  static void clearMorphNightVisionEffect(ServerPlayer player) {
    MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
    if (effect != null && isMorphNightVision(effect)) {
      player.removeEffect(MobEffects.NIGHT_VISION);
    }
  }

  private static void slowChickenFall(ServerPlayer player) {
    if (player.onGround() || player.isInWater() || player.getAbilities().flying) {
      return;
    }

    Vec3 velocity = player.getDeltaMovement();
    if (velocity.y < 0.0) {
      player.setDeltaMovement(
          velocity.x, velocity.y * activeConfig().movement().slowFallMultiplier(), velocity.z);
    }
  }

  private static void tickRabbitHop(ServerPlayer player) {
    UUID uuid = player.getUUID();
    if (InstinctManager.isEnabled(player)) {
      RABBIT_HOP_COOLDOWNS.remove(uuid);
      if (InstinctManager.rabbitJumped(player)) {
        player
            .level()
            .playSound(null, player, SoundEvents.RABBIT_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
      }
      return;
    }

    int cooldown = Math.max(0, RABBIT_HOP_COOLDOWNS.getOrDefault(uuid, 0) - 1);
    if (cooldown > 0) {
      RABBIT_HOP_COOLDOWNS.put(uuid, cooldown);
    } else {
      RABBIT_HOP_COOLDOWNS.remove(uuid);
    }

    Input input = player.getLastClientInput();
    boolean moving = hasMovementInput(input);
    boolean jumping = input.jump();
    boolean groundedOnLand = player.onGround() && !player.isInWater() && !player.isInLava();
    if ((!moving && !jumping)
        || cooldown > 0
        || !groundedOnLand
        || player.isPassenger()
        || player.getAbilities().flying) {
      return;
    }

    RabbitHopMovement.launch(player, input);
    RABBIT_HOP_COOLDOWNS.put(uuid, RabbitHopMovement.cooldown(player, input));
    player
        .level()
        .playSound(null, player, SoundEvents.RABBIT_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
  }

  private static void syncJumpCooldown(ServerPlayer player) {
    UUID uuid = player.getUUID();
    boolean grounded = isJumpGrounded(player);
    boolean wasGrounded = LAST_JUMP_GROUNDED_STATES.getOrDefault(uuid, grounded);
    if (grounded && !wasGrounded) {
      JUMP_COOLDOWN_UNTIL_TICKS.put(
          uuid, player.level().getGameTime() + MobChargedJump.COOLDOWN_TICKS);
    }
    LAST_JUMP_GROUNDED_STATES.put(uuid, grounded);
  }

  private static boolean isJumpCoolingDown(ServerPlayer player) {
    return player.level().getGameTime()
        < JUMP_COOLDOWN_UNTIL_TICKS.getOrDefault(player.getUUID(), 0L);
  }

  private static boolean isJumpGrounded(ServerPlayer player) {
    return player.onGround()
        && !player.isInWater()
        && !player.isInLava()
        && !player.getAbilities().flying;
  }

  private static void addMovementExhaustion(ServerPlayer player) {
    if (player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4) {
      return;
    }

    if (player.isSprinting() && !player.isShiftKeyDown() && MorphMovementSpeed.canSprint(player)) {
      player.causeFoodExhaustion(0.02F);
    } else if (player.isShiftKeyDown()) {
      player.causeFoodExhaustion(0.01F);
    }
  }

  private static void tickGrassEating(ServerPlayer player) {
    UUID uuid = player.getUUID();
    Input input = player.getLastClientInput();
    BlockPos grassPos = player.blockPosition().below();
    boolean canEat =
        !InstinctManager.isEnabled(player)
            && activeConfig().traits().eatsGrass()
            && input.shift()
            && !hasMovementInput(input)
            && player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4
            && player.onGround()
            && !player.isPassenger()
            && !player.getAbilities().flying
            && player.getFoodData().needsFood()
            && player.level().getBlockState(grassPos).is(Blocks.GRASS_BLOCK);
    if (!canEat) {
      if (GRASS_EATING_TICKS.remove(uuid) != null) {
        syncGrassEating(player, 0);
      }
      return;
    }

    int elapsed = GRASS_EATING_TICKS.merge(uuid, 1, Integer::sum);
    syncGrassEating(player, GRASS_EATING_DURATION_TICKS - elapsed + 1);
    if (elapsed < GRASS_EATING_DURATION_TICKS) {
      return;
    }

    var grassState = player.level().getBlockState(grassPos);
    if (player.level().setBlockAndUpdate(grassPos, Blocks.DIRT.defaultBlockState())) {
      player.level().levelEvent(player, 2001, grassPos, Block.getId(grassState));
      player.getFoodData().eat(GRASS_FOOD_RESTORE, 0.0F);
    }
    GRASS_EATING_TICKS.remove(uuid);
    syncGrassEating(player, 0);
  }

  private static void syncGrassEating(ServerPlayer eatingPlayer, int remainingTicks) {
    MobLifeNetworking.GrassEatingStatePayload payload =
        new MobLifeNetworking.GrassEatingStatePayload(
            eatingPlayer.getId(), Math.max(0, remainingTicks));
    for (ServerPlayer player : eatingPlayer.level().getServer().getPlayerList().getPlayers()) {
      ServerPlayNetworking.send(player, payload);
    }
  }

  private static void refreshChestedInventory(ServerPlayer player) {
    MorphType morph = activeMorph();
    if (morph == null || !morph.canEquipChest() || activeDimensions == null) {
      return;
    }

    boolean hasChest = player.getItemBySlot(EquipmentSlot.BODY).is(Items.CHEST);
    MorphInventoryCapacity expected =
        MorphInventoryCapacity.forMorph(morph, activeDimensions.height(), hasChest);
    if (MorphInventoryCapacity.hotbarSlots(player) != expected.hotbarSlots()
        || MorphInventoryCapacity.inventorySlots(player) != expected.inventorySlots()) {
      MorphInventoryCapacity.apply(player, morph, activeDimensions.height(), hasChest);
      player.inventoryMenu.sendAllDataToRemote();
    }
  }

  private static void tickAwkwardness(ServerPlayer player) {
    float delta = 0.0F;
    Input input = player.getLastClientInput();
    boolean moving = hasMovementInput(input);
    boolean instinct = InstinctManager.isEnabled(player);
    if (!instinct && moving && (input.backward() || input.left() || input.right())) {
      delta += NON_FORWARD_MOVEMENT_GAIN * NORMAL_AWKWARDNESS_GAIN_MULTIPLIER;
    }

    if (instinct && !InstinctManager.pausesAwkwardnessDecay(player) && player.tickCount % 20 == 0) {
      delta -= PASSIVE_DECAY_PER_SECOND * passiveDecayMultiplier(player);
      if (hasNearbySameMob(player)) {
        delta -= SAME_MOB_DECAY_PER_SECOND;
      }
    }

    if (delta != 0.0F) {
      MorphAwkwardness.add(player, delta);
    }
    InstinctManager.forceEnableAtMaximum(player);
    if (player.tickCount % 5 == 0) {
      syncAwkwardness(player, false);
    }
  }

  private static float passiveDecayMultiplier(ServerPlayer player) {
    int itemCount = countCarriedItems(player);
    return (1.0F
        + (EMPTY_INVENTORY_DECAY_MULTIPLIER - 1.0F) / (1.0F + itemCount / ITEM_DECAY_SCALE));
  }

  private static int countCarriedItems(ServerPlayer player) {
    Inventory inventory = player.getInventory();
    int itemCount = 0;
    for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
      if (!MorphInventoryCapacity.isActiveInventorySlot(player, slot)) {
        continue;
      }

      ItemStack stack = inventory.getItem(slot);
      if (!stack.isEmpty()) {
        itemCount += stack.getCount();
      }
    }
    return itemCount;
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
            entity -> entity.getType() == morph.entityType() || entity instanceof ServerPlayer)
        .isEmpty();
  }

  private static void syncAwkwardness(ServerPlayer player, boolean force) {
    float value = MorphAwkwardness.get(player);
    Float previous = LAST_SYNCED_AWKWARDNESS.get(player.getUUID());
    if (!force && previous != null && Math.abs(previous - value) < 0.05F) {
      return;
    }

    LAST_SYNCED_AWKWARDNESS.put(player.getUUID(), value);
    ServerPlayNetworking.send(player, new MobLifeNetworking.AwkwardnessPayload(value));
  }

  public static MorphConfig activeConfig() {
    return activeConfig != null ? activeConfig : MorphConfigManager.get(activeMorph());
  }

  private static boolean hasMovementInput(Input input) {
    return input.forward() || input.backward() || input.left() || input.right();
  }
}
