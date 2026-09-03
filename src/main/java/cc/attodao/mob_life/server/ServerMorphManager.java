package cc.attodao.mob_life.server;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.config.ServerMobLifeConfig;
import cc.attodao.mob_life.gameplay.ability.MorphAbility;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardnessBehavior;
import cc.attodao.mob_life.gameplay.combat.MorphAttackDamage;
import cc.attodao.mob_life.gameplay.food.MorphEatingSound;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.instinct.InstinctState;
import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.gameplay.jump.ChargedJumpingPlayer;
import cc.attodao.mob_life.gameplay.jump.MobChargedJump;
import cc.attodao.mob_life.gameplay.movement.MorphMovementSpeed;
import cc.attodao.mob_life.gameplay.movement.RabbitHopMovement;
import cc.attodao.mob_life.gameplay.targeting.MorphNearbyEntities;
import cc.attodao.mob_life.gameplay.targeting.MorphPredatorOutlineManager;
import cc.attodao.mob_life.mixin.sound.LivingEntitySoundAccessor;
import cc.attodao.mob_life.mixin.sound.MobSoundAccessor;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import cc.attodao.mob_life.network.MobLifeNetworking.MorphConfigEntry;
import cc.attodao.mob_life.network.MobLifeNetworking.WorldMorphSelectionPromptPayload;
import cc.attodao.mob_life.world.MorphInitialSpawn;
import cc.attodao.mob_life.world.MorphVariantRequest;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class ServerMorphManager {

  private static final Map<UUID, PlayerMorphRuntimeState> RUNTIME_STATES = new HashMap<>();

  private static final float NON_FORWARD_MOVEMENT_GAIN = 0.04F;
  private static final float NORMAL_AWKWARDNESS_GAIN_MULTIPLIER = 2.0F;
  private static final float CRITICAL_HUNGER_AWKWARDNESS_GAIN_MULTIPLIER = 2.0F;
  private static final float MAX_SINGLE_NORMAL_AWKWARDNESS_GAIN = 20.0F;
  private static final float DAMAGE_AWKWARDNESS_MIN_GAIN = 20.0F;
  private static final float DAMAGE_AWKWARDNESS_MAX_GAIN = 50.0F;
  private static final int DAMAGE_AWKWARDNESS_COOLDOWN_TICKS = 20 * 6;
  private static final float BEDLESS_SLEEP_AWKWARDNESS_COST = 70.0F;
  private static final float GRASS_EATING_MIN_PITCH = 30.0F;
  private static final int GRASS_EATING_DURATION_TICKS = 40;
  private static final int GRASS_EATING_COOLDOWN_TICKS = 20 * 60;
  private static final int GRASS_FOOD_RESTORE = 2;
  private static final int INITIAL_SPAWN_RETRY_TICKS = 20 * 30;

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
              data.setDefinition(
                  selection.variantRequest().resolve(server.overworld(), selection.morph()));
              data.markSelectionChosen();
              if (!data.initialSpawnConfigured()) {
                MorphInitialSpawn.configure(server.overworld(), data);
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
          MorphPredatorOutlineManager.clear(server);
          MorphNearbyEntities.clear();
          MorphInstinct.clear();
          resetActiveMorph();
          clearServerPlayerState();
        });
    ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
        (server, resourceManager, success) -> {
          if (!success || activeDefinition == null) {
            return;
          }
          MorphPredatorOutlineManager.clear(server);
          MorphNearbyEntities.clear();
          setActiveDefinition(server, activeDefinition);
          for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayerMorphApplier.apply(player, activeDefinition, true);
            MorphInstinct.onReload(player, activeDefinition);
          }
        });

    ServerPlayerEvents.JOIN.register(ServerMorphManager::initializePlayer);
    ServerPlayConnectionEvents.DISCONNECT.register(
        (handler, server) -> {
          MorphPredatorOutlineManager.remove(handler.getPlayer());
          MorphNearbyEntities.remove(handler.getPlayer());
          MorphInstinct.removeRuntime(handler.getPlayer());
          RUNTIME_STATES.remove(handler.getPlayer().getUUID());
        });
    ServerPlayerEvents.AFTER_RESPAWN.register(
        (oldPlayer, newPlayer, alive) -> {
          MorphPredatorOutlineManager.remove(oldPlayer);
          MorphNearbyEntities.remove(oldPlayer);
          RUNTIME_STATES.remove(newPlayer.getUUID());
          MorphAwkwardness.set(newPlayer, MorphAwkwardness.MINIMUM);
          MorphAbility.copy(oldPlayer, newPlayer);
          InstinctState.get(newPlayer).copyEggStateFrom(InstinctState.get(oldPlayer));
          InstinctState.get(newPlayer).clearForMorphChange();
          MorphInstinct.removeRuntime(oldPlayer);
          initializePlayer(newPlayer);
        });

    ServerTickEvents.END_SERVER_TICK.register(
        server -> {
          retryInitialSpawn(server);
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

  private static void retryInitialSpawn(MinecraftServer server) {
    WorldMorphData data = worldData(server);
    if (!data.selectionChosen()
        || data.initialSpawnConfigured()
        || server.overworld().getGameTime() % INITIAL_SPAWN_RETRY_TICKS != 0) {
      return;
    }

    MorphDefinition previousDefinition = data.definition();
    MorphInitialSpawn.configure(server.overworld(), data);
    if (data.definition().equals(previousDefinition) && activeDefinition != null) {
      return;
    }

    setActiveDefinition(server, data.definition());
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      ServerPlayerMorphApplier.apply(player, data.definition(), true);
    }
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

    runtimeState(player).ambientSoundTime = -soundMob.getAmbientSoundInterval();
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

  public static void adjustAwkwardness(ServerPlayer player, float amount) {
    if (!hasMobForm() || MorphInstinct.isActive(player) || player.isCreative()) {
      return;
    }

    if (amount > 0.0F) {
      amount =
          Math.min(amount * NORMAL_AWKWARDNESS_GAIN_MULTIPLIER, MAX_SINGLE_NORMAL_AWKWARDNESS_GAIN);
      amount = applyCriticalHungerAwkwardnessMultiplier(player, amount);
    } else {
      return;
    }

    float oldValue = MorphAwkwardness.get(player);
    float newValue = MorphAwkwardness.add(player, amount);
    if (newValue != oldValue) {
      syncAwkwardness(player, true);
    }
  }

  public static void increaseAwkwardnessFromDamage(ServerPlayer player, float finalDamage) {
    if (!hasMobForm()
        || MorphInstinct.isActive(player)
        || player.isCreative()
        || finalDamage <= 0.0F) {
      return;
    }

    long gameTime = player.level().getGameTime();
    PlayerMorphRuntimeState runtimeState = runtimeState(player);
    if (gameTime < runtimeState.damageAwkwardnessCooldownUntilTick) {
      return;
    }
    runtimeState.damageAwkwardnessCooldownUntilTick = gameTime + DAMAGE_AWKWARDNESS_COOLDOWN_TICKS;

    float damageRatio = Math.clamp(finalDamage / Math.max(1.0F, player.getMaxHealth()), 0.0F, 1.0F);
    float gain =
        DAMAGE_AWKWARDNESS_MIN_GAIN
            + damageRatio * (DAMAGE_AWKWARDNESS_MAX_GAIN - DAMAGE_AWKWARDNESS_MIN_GAIN);
    gain = applyCriticalHungerAwkwardnessMultiplier(player, gain);
    float oldValue = MorphAwkwardness.get(player);
    float newValue = MorphAwkwardness.add(player, gain);
    if (newValue != oldValue) {
      syncAwkwardness(player, true);
    }
  }

  public static void markBedlessSleepStarted(ServerPlayer player) {
    runtimeState(player).bedlessSleepPending = true;
  }

  public static void completeBedlessSleep(ServerPlayer player) {
    PlayerMorphRuntimeState state = RUNTIME_STATES.get(player.getUUID());
    if (state == null || !state.bedlessSleepPending) {
      return;
    }

    state.bedlessSleepPending = false;
    if (hasMobForm() && !MorphInstinct.isActive(player)) {
      setAwkwardness(player, MorphAwkwardness.get(player) + BEDLESS_SLEEP_AWKWARDNESS_COST);
    }
  }

  public static void setAwkwardness(ServerPlayer player, float value) {
    MorphAwkwardness.set(player, value);
    syncAwkwardness(player, true);
  }

  public static void changeMorph(MinecraftServer server, MorphDefinition definition) {
    if (!ServerMobLifeConfig.isMorphEnabled(definition.type())) {
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

  /**
   * Completes the one-time world selection from a client-supplied ID and limited variant request.
   */
  public static void completeWorldSelection(
      MinecraftServer server,
      ServerPlayer submittingPlayer,
      String morphId,
      MorphVariantRequest variantRequest) {
    WorldMorphData data = worldData(server);
    if (data.selectionChosen()) {
      MobLife.LOGGER.warn(
          "Ignoring repeat world morph selection {} from {}",
          morphId,
          submittingPlayer.getGameProfile().name());
      return;
    }

    MorphType morph = MorphType.fromId(morphId);
    if (!morph.id().equals(morphId) || !ServerMobLifeConfig.selectableMorphs().contains(morph)) {
      MobLife.LOGGER.warn(
          "Ignoring unavailable world morph selection {} from {}",
          morphId,
          submittingPlayer.getGameProfile().name());
      return;
    }

    // The payload cannot carry a CompoundTag. Only registry-backed cosmetic values from the
    // fixed request are retained; vanilla finalization generates every remaining spawn property.
    data.setDefinition(
        (variantRequest != null ? variantRequest : MorphVariantRequest.empty())
            .resolve(server.overworld(), morph));
    data.markSelectionChosen();
    if (!data.initialSpawnConfigured()) {
      MorphInitialSpawn.configure(server.overworld(), data);
    }
    setActiveDefinition(server, data.definition());
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      ServerPlayerMorphApplier.apply(player, data.definition(), true);
    }

    MobLife.LOGGER.info(
        "World morph selected by {}: {} with server-generated NBT {}",
        submittingPlayer.getGameProfile().name(),
        data.definition().type().id(),
        data.definition().nbt());
  }

  private static void initializePlayer(ServerPlayer player) {
    sendServerConfig(player);
    WorldMorphData data = worldData(player.level().getServer());
    if (!data.selectionChosen()) {
      sendWorldSelectionPrompt(player);
      return;
    }

    MorphDefinition definition = activeDefinition;
    if (definition == null) {
      return;
    }

    boolean mobForm = definition.hasMobForm();
    if (mobForm) {
      runtimeState(player).grassEatingTicks = 0;
    }
    MorphPredatorOutlineManager.remove(player);
    MorphNearbyEntities.remove(player);
    ServerPlayerMorphApplier.apply(player, definition, false);
    if (!mobForm) {
      RUNTIME_STATES.remove(player.getUUID());
      return;
    }
    syncAwkwardness(player, true);
    MorphInstinct.onReload(player, definition);
    if (!activeConfig().movement().rabbitHop().enabled()) {
      syncJumpCooldown(player);
    }
  }

  private static void sendWorldSelectionPrompt(ServerPlayer player) {
    var morphs = ServerMobLifeConfig.selectableMorphs();
    ArrayList<MorphConfigEntry> configs = new ArrayList<>(morphs.size());
    for (MorphType morph : morphs) {
      configs.add(new MorphConfigEntry(morph.id(), MorphConfigManager.encode(morph)));
    }
    ServerPlayNetworking.send(player, new WorldMorphSelectionPromptPayload(configs));
  }

  private static void sendServerConfig(ServerPlayer player) {
    ServerMobLifeConfig.Settings settings = ServerMobLifeConfig.settings();
    ServerPlayNetworking.send(
        player,
        new MobLifeNetworking.ServerConfigPayload(
            settings.morphEnabled(),
            settings.playerMorphEnabled(),
            settings.hotbarLimitEnabled(),
            settings.inventorySlotLimitEnabled(),
            settings.offhandLimitEnabled(),
            settings.miningSpeedChangeEnabled(),
            settings.reachChangeEnabled()));
  }

  private static WorldMorphData worldData(MinecraftServer server) {
    return server.getDataStorage().computeIfAbsent(WorldMorphData.TYPE);
  }

  private static void setActiveDefinition(MinecraftServer server, MorphDefinition definition) {
    if (activeDefinition != null && !activeDefinition.equals(definition)) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
        MorphInstinct.onMorphChanged(player);
      }
    }
    MorphPredatorOutlineManager.clear(server);
    MorphNearbyEntities.clear();
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
    RUNTIME_STATES.clear();
  }

  private static void clearServerPlayerState() {
    RUNTIME_STATES.clear();
    MorphInstinct.clear();
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
    MorphPredatorOutlineManager.tick(
        player, cc.attodao.mob_life.gameplay.targeting.MorphRelations.morphOf(player));
    MorphInstinct.tick(player, activeDefinition);
    if (MorphInstinct.isActive(player)) {
      tickAmbientSound(player);
      if (player.tickCount % 20 == 0) {
        refreshChestedInventory(player);
        if (activeDimensions != null) {
          ServerPlayerMorphApplier.refreshGameplayModifiers(
              player, activeMorph(), activeDimensions.height());
        }
      }
      clearMorphNightVisionEffect(player);
      return;
    }
    tickGrassEating(player);
    addMovementExhaustion(player);
    tickAwkwardness(player);
    tickAmbientSound(player);
    if (player.tickCount % 20 == 0) {
      refreshChestedInventory(player);
      if (activeDimensions != null) {
        ServerPlayerMorphApplier.refreshGameplayModifiers(
            player, activeMorph(), activeDimensions.height());
      }
    }
    clearMorphNightVisionEffect(player);

    MorphConfig.Movement movement = activeConfig().movement();
    if (movement.slowFallMultiplier() < 1.0F) {
      slowChickenFall(player);
    }
    if (movement.rabbitHop().enabled()) {
      tickRabbitHop(player);
    } else {
      syncJumpCooldown(player);
    }
  }

  private static void tickAmbientSound(ServerPlayer player) {
    Mob soundMob = activeSoundMob;
    if (soundMob == null || soundMob.isSilent() || !player.isAlive()) {
      return;
    }

    PlayerMorphRuntimeState runtimeState = runtimeState(player);
    int ambientSoundTime = runtimeState.ambientSoundTime;
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
    runtimeState.ambientSoundTime = ambientSoundTime;
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
    PlayerMorphRuntimeState runtimeState = runtimeState(player);
    Input input = player.getLastClientInput();
    boolean moving = hasMovementInput(input);
    boolean jumping = input.jump();
    boolean groundedOnLand = player.onGround() && !player.isInWater() && !player.isInLava();
    boolean wasGrounded =
        runtimeState.rabbitHopGroundedKnown ? runtimeState.rabbitHopGrounded : groundedOnLand;
    if (groundedOnLand && !wasGrounded) {
      runtimeState.rabbitHopCooldown = RabbitHopMovement.landingCooldown(player, input);
    } else if (runtimeState.rabbitHopCooldown > 0) {
      runtimeState.rabbitHopCooldown--;
    }
    runtimeState.rabbitHopGrounded = groundedOnLand;
    runtimeState.rabbitHopGroundedKnown = true;
    if ((!moving && !jumping)
        || runtimeState.rabbitHopCooldown > 0
        || !groundedOnLand
        || player.isPassenger()
        || player.getAbilities().flying) {
      return;
    }

    RabbitHopMovement.launch(player, input);
    player
        .level()
        .playSound(null, player, SoundEvents.RABBIT_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
  }

  private static void syncJumpCooldown(ServerPlayer player) {
    PlayerMorphRuntimeState runtimeState = runtimeState(player);
    boolean grounded = isJumpGrounded(player);
    boolean wasGrounded = runtimeState.jumpGroundedKnown ? runtimeState.jumpGrounded : grounded;
    if (grounded && !wasGrounded) {
      runtimeState.jumpCooldownUntilTick =
          player.level().getGameTime() + MobChargedJump.COOLDOWN_TICKS;
    }
    runtimeState.jumpGrounded = grounded;
    runtimeState.jumpGroundedKnown = true;
  }

  private static boolean isJumpCoolingDown(ServerPlayer player) {
    return player.level().getGameTime() < runtimeState(player).jumpCooldownUntilTick;
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
    PlayerMorphRuntimeState runtimeState = runtimeState(player);
    Input input = player.getLastClientInput();
    BlockPos grassPos = player.blockPosition().below();
    long gameTime = player.level().getGameTime();
    boolean canEat =
        activeConfig().traits().eatsGrass()
            && runtimeState.grassEatingCooldownUntilTick <= gameTime
            && player.isCrouching()
            && player.getXRot() >= GRASS_EATING_MIN_PITCH
            && !hasMovementInput(input)
            && player.onGround()
            && !player.isPassenger()
            && !player.getAbilities().flying
            && player.getFoodData().needsFood()
            && player.level().getBlockState(grassPos).is(Blocks.GRASS_BLOCK);
    if (!canEat) {
      if (runtimeState.grassEatingTicks > 0) {
        runtimeState.grassEatingTicks = 0;
        syncGrassEating(player, 0);
      }
      return;
    }

    int elapsed = ++runtimeState.grassEatingTicks;
    MorphEatingSound.playContinuousTickForEater(player, elapsed - 1);
    syncGrassEating(player, GRASS_EATING_DURATION_TICKS - elapsed + 1);
    if (elapsed < GRASS_EATING_DURATION_TICKS) {
      return;
    }

    var grassState = player.level().getBlockState(grassPos);
    if (player.level().setBlockAndUpdate(grassPos, Blocks.DIRT.defaultBlockState())) {
      player.level().levelEvent(player, 2001, grassPos, Block.getId(grassState));
      player.getFoodData().eat(GRASS_FOOD_RESTORE, 0.0F);
      runtimeState.grassEatingCooldownUntilTick = gameTime + GRASS_EATING_COOLDOWN_TICKS;
    }
    runtimeState.grassEatingTicks = 0;
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
    if (player.isCreative()) {
      float previous = MorphAwkwardness.get(player);
      MorphAwkwardness.set(player, MorphAwkwardness.MINIMUM);
      if (previous != MorphAwkwardness.MINIMUM) {
        syncAwkwardness(player, true);
      }
      return;
    }
    float delta = 0.0F;
    Input input = player.getLastClientInput();
    boolean moving = hasMovementInput(input);
    if (moving && (input.backward() || input.left() || input.right())) {
      delta += NON_FORWARD_MOVEMENT_GAIN * NORMAL_AWKWARDNESS_GAIN_MULTIPLIER;
    }

    if (player.tickCount % 20 == 0) {
      delta += MorphAwkwardnessBehavior.threatGainPerSecond(player, activeMorph());
      delta +=
          MorphAwkwardnessBehavior.unfavorableLightGainPerSecond(
              player, activeConfig().sleep().schedule());
    }

    if (delta != 0.0F) {
      MorphAwkwardness.add(player, applyCriticalHungerAwkwardnessMultiplier(player, delta));
    }
    if (player.tickCount % 5 == 0) {
      syncAwkwardness(player, false);
    }
  }

  private static float applyCriticalHungerAwkwardnessMultiplier(ServerPlayer player, float amount) {
    return amount > 0.0F && MorphFoodCapacity.isCriticallyHungry(player)
        ? amount * CRITICAL_HUNGER_AWKWARDNESS_GAIN_MULTIPLIER
        : amount;
  }

  private static void syncAwkwardness(ServerPlayer player, boolean force) {
    float value = MorphAwkwardness.get(player);
    PlayerMorphRuntimeState runtimeState = runtimeState(player);
    Float previous = runtimeState.lastSyncedAwkwardness;
    if (!force && previous != null && Math.abs(previous - value) < 0.05F) {
      return;
    }

    runtimeState.lastSyncedAwkwardness = value;
    ServerPlayNetworking.send(player, new MobLifeNetworking.AwkwardnessPayload(value));
  }

  public static MorphConfig activeConfig() {
    return activeConfig != null ? activeConfig : MorphConfigManager.get(activeMorph());
  }

  private static boolean hasMovementInput(Input input) {
    return input.forward() || input.backward() || input.left() || input.right();
  }

  private static PlayerMorphRuntimeState runtimeState(ServerPlayer player) {
    return RUNTIME_STATES.computeIfAbsent(
        player.getUUID(), ignored -> new PlayerMorphRuntimeState());
  }
}
