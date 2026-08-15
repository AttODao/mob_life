package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.network.MobLifeNetworking;
import cc.attodao.mob_life.server.ServerMorphManager;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class InstinctManager {
  public static final int INTERVENE_FORWARD = 1;
  public static final int INTERVENE_LEFT = 1 << 1;
  public static final int INTERVENE_RIGHT = 1 << 2;
  public static final int ESCAPE_ATTACK = 1;
  public static final int ESCAPE_USE = 1 << 1;
  public static final int ESCAPE_JUMP = 1 << 2;
  public static final int ESCAPE_ALL = ESCAPE_ATTACK | ESCAPE_USE | ESCAPE_JUMP;
  public static final float MAXIMUM_LEVEL = 100.0F;

  private static final float ACTIVITY_ROTATION_EPSILON = 0.01F;
  private static final float INSTINCT_REGENERATION_PER_TICK = 10.0F / 20.0F;
  private static final Map<UUID, InstinctController> CONTROLLERS = new HashMap<>();
  private static final Map<Mob, InstinctController> SHADOW_CONTROLLERS = new IdentityHashMap<>();
  private static final Map<UUID, EntryReason> ENTRY_REASONS = new HashMap<>();
  private static final Map<UUID, TransitionState> TRANSITIONS = new HashMap<>();
  private static final ThreadLocal<AttackContext> ATTACK_CONTEXT = new ThreadLocal<>();

  private InstinctManager() {}

  public static boolean restore(ServerPlayer player) {
    return enable(player, EntryReason.RESTORED);
  }

  private static boolean enable(ServerPlayer player, EntryReason reason) {
    InstinctController controller = createController(player, reason);
    if (controller == null) {
      return false;
    }

    installController(player, controller, reason);
    return true;
  }

  public static void respondToDamage(
      ServerPlayer player, net.minecraft.world.damagesource.DamageSource source) {
    if (!ServerMorphManager.hasMobForm()) {
      return;
    }
    InstinctController controller = CONTROLLERS.get(player.getUUID());
    if (controller == null) {
      controller = createController(player, EntryReason.DAMAGE_RESPONSE);
      if (controller == null || !controller.triggerDamageResponse(source)) {
        return;
      }
      installController(player, controller, EntryReason.DAMAGE_RESPONSE);
      return;
    }

    if (controller.triggerDamageResponse(source)) {
      ENTRY_REASONS.put(player.getUUID(), EntryReason.DAMAGE_RESPONSE);
      ((InstinctPersistenceHolder) player).mobLife$setRestoreInstinct(false);
    }
  }

  private static void installController(
      ServerPlayer player, InstinctController controller, EntryReason reason) {
    CONTROLLERS.put(player.getUUID(), controller);
    SHADOW_CONTROLLERS.put(controller.shadow(), controller);
    ENTRY_REASONS.put(player.getUUID(), reason);
    transition(player).resetForEntry();
    player.stopUsingItem();
    player.setSprinting(false);
    player.setShiftKeyDown(false);
    player.closeContainer();
    ((InstinctPersistenceHolder) player)
        .mobLife$setRestoreInstinct(reason.persistsAcrossReconnect());
    sync(controller);
  }

  public static boolean shouldRestore(ServerPlayer player) {
    return ((InstinctPersistenceHolder) player).mobLife$shouldRestoreInstinct();
  }

  static boolean canHunt(ServerPlayer player) {
    return !isAbandonedHuntCooldownActive(player)
        && (MorphFoodCapacity.isCriticallyHungry(player) || !isPostKillHuntCooldownActive(player));
  }

  static boolean isAbandonedHuntCooldownActive(ServerPlayer player) {
    return cooldowns(player).mobLife$getAbandonedHuntCooldownUntil() > gameTime(player);
  }

  static boolean isEatBlockCooldownActive(ServerPlayer player) {
    return cooldowns(player).mobLife$getEatBlockCooldownUntil() > gameTime(player);
  }

  static boolean isRaidGardenCooldownActive(ServerPlayer player) {
    return cooldowns(player).mobLife$getRaidGardenCooldownUntil() > gameTime(player);
  }

  static void startPostKillHuntCooldown(ServerPlayer player, int ticks) {
    InstinctPersistenceHolder cooldowns = cooldowns(player);
    cooldowns.mobLife$setPostKillHuntCooldownUntil(
        extendCooldown(cooldowns.mobLife$getPostKillHuntCooldownUntil(), player, ticks));
  }

  static void startAbandonedHuntCooldown(ServerPlayer player, int ticks) {
    InstinctPersistenceHolder cooldowns = cooldowns(player);
    cooldowns.mobLife$setAbandonedHuntCooldownUntil(
        extendCooldown(cooldowns.mobLife$getAbandonedHuntCooldownUntil(), player, ticks));
  }

  static void startEatBlockCooldown(ServerPlayer player, int ticks) {
    InstinctPersistenceHolder cooldowns = cooldowns(player);
    cooldowns.mobLife$setEatBlockCooldownUntil(
        extendCooldown(cooldowns.mobLife$getEatBlockCooldownUntil(), player, ticks));
  }

  static void startRaidGardenCooldown(ServerPlayer player, int ticks) {
    InstinctPersistenceHolder cooldowns = cooldowns(player);
    cooldowns.mobLife$setRaidGardenCooldownUntil(
        extendCooldown(cooldowns.mobLife$getRaidGardenCooldownUntil(), player, ticks));
  }

  public static void attemptEscape(ServerPlayer player, int flags) {
    if (!ServerMorphManager.hasMobForm()) {
      return;
    }
    InstinctController controller = CONTROLLERS.get(player.getUUID());
    if (controller == null) {
      return;
    }
    EntryReason reason = ENTRY_REASONS.getOrDefault(player.getUUID(), EntryReason.IDLE);
    if (reason.forcesControl() || !controller.allowsExit()) {
      return;
    }

    TransitionState transition = transition(player);
    int accepted = transition.acceptEscapeInputs(player.tickCount, flags & ESCAPE_ALL);
    if (accepted == 0) {
      return;
    }
    float reduction =
        MorphAwkwardness.instinctEscapeReduction(MorphAwkwardness.get(player))
            * Integer.bitCount(accepted);
    if (transition.reduceInstinct(reduction) <= 0.0F) {
      forget(player);
      disable(player);
    } else {
      sync(controller);
    }
  }

  public static void forceEnableAtMaximum(ServerPlayer player) {
    if (!ServerMorphManager.hasMobForm() || !MorphAwkwardness.isMaximum(player)) {
      return;
    }
    if (isEnabled(player)) {
      ENTRY_REASONS.put(player.getUUID(), EntryReason.MAXIMUM_AWKWARDNESS);
    } else {
      enable(player, EntryReason.MAXIMUM_AWKWARDNESS);
    }
  }

  public static void recordActivity(ServerPlayer player) {
    if (ServerMorphManager.hasMobForm() && !isEnabled(player)) {
      TransitionState transition = transition(player);
      transition.recordActivity(player.tickCount, transition.isGuiOpen(player));
    }
  }

  /**
   * Marks a use action that may resolve into a GUI without prematurely discarding idle progress.
   */
  public static void recordPotentialGuiActivity(ServerPlayer player) {
    if (ServerMorphManager.hasMobForm() && !isEnabled(player)) {
      TransitionState transition = transition(player);
      transition.recordPotentialGuiActivity(player.tickCount, transition.isGuiOpen(player));
    }
  }

  /** Called by the server menu path after an interaction has actually opened a container. */
  public static void openedServerContainer(ServerPlayer player) {
    transition(player).openedServerContainer(player.tickCount);
  }

  /** Receives the state of client-only screens such as inventory and Mod Menu. */
  public static void setClientGuiOpen(ServerPlayer player, boolean open) {
    transition(player).setClientGuiOpen(player.tickCount, open);
  }

  public static boolean isGuiOpen(ServerPlayer player) {
    return transition(player).isGuiOpen(player);
  }

  public static void forget(ServerPlayer player) {
    ((InstinctPersistenceHolder) player).mobLife$setRestoreInstinct(false);
  }

  public static void tick(ServerPlayer player) {
    MorphDefinition definition = ServerMorphManager.activeDefinition();
    if (!canRemainInInstinct(player, definition)) {
      if (isEnabled(player)) {
        forget(player);
        disable(player);
      }
      return;
    }

    TransitionState transition = transition(player);
    transition.observeActivity(player);
    EntryReason forcedReason = forcedReason(player, definition, transition);
    InstinctController controller = CONTROLLERS.get(player.getUUID());
    if (controller == null) {
      if (forcedReason != null) {
        enable(player, forcedReason);
      } else if (transition.advanceIdle(player)) {
        enable(player, EntryReason.IDLE);
      }
      controller = CONTROLLERS.get(player.getUUID());
      if (controller == null) {
        return;
      }
    } else {
      updateEntryReason(player, controller, forcedReason);
    }

    transition.regenerateInstinct();

    player.stopUsingItem();
    player.setSprinting(false);
    player.setShiftKeyDown(false);
    if (player.containerMenu != player.inventoryMenu) {
      player.closeContainer();
    }
    controller.tick();
    sync(controller);
  }

  public static void intervene(ServerPlayer player, int flags, float viewYaw) {
    if (!ServerMorphManager.hasMobForm()) {
      return;
    }
    InstinctController controller = CONTROLLERS.get(player.getUUID());
    if (controller == null) {
      return;
    }
    controller.intervene(flags, viewYaw);
  }

  public static boolean pausesAwkwardnessDecay(ServerPlayer player) {
    InstinctController controller = CONTROLLERS.get(player.getUUID());
    return controller != null && controller.pausesAwkwardnessDecay();
  }

  public static boolean isThreatResponse(ServerPlayer player) {
    InstinctController controller = CONTROLLERS.get(player.getUUID());
    return controller != null && controller.isThreatResponse();
  }

  public static boolean isEnabled(Player player) {
    return ServerMorphManager.hasMobForm()
        && player instanceof ServerPlayer serverPlayer
        && CONTROLLERS.containsKey(serverPlayer.getUUID());
  }

  public static boolean isControllingPlayer(Mob mob, LivingEntity target) {
    InstinctController controller = SHADOW_CONTROLLERS.get(mob);
    return controller != null && controller.player() == target;
  }

  public static boolean isShadow(Mob mob) {
    return SHADOW_CONTROLLERS.containsKey(mob);
  }

  public static Vec3 panicEscapeSource(Mob mob) {
    InstinctController controller = SHADOW_CONTROLLERS.get(mob);
    return controller != null ? controller.panicEscapeSource() : null;
  }

  public static Vec3 nativeMovement(ServerPlayer player) {
    InstinctController controller = CONTROLLERS.get(player.getUUID());
    return controller != null ? controller.control().nativeMovement() : Vec3.ZERO;
  }

  public static boolean rabbitJumped(ServerPlayer player) {
    InstinctController controller = CONTROLLERS.get(player.getUUID());
    return controller != null && controller.control().rabbitJumped();
  }

  public static Boolean allowsShadowTarget(Mob mob, LivingEntity target) {
    InstinctController controller = SHADOW_CONTROLLERS.get(mob);
    return controller != null ? controller.allowsTarget(target) : null;
  }

  public static Boolean attackFromShadow(Mob mob, ServerLevel level, Entity target) {
    InstinctController controller = SHADOW_CONTROLLERS.get(mob);
    return controller != null ? controller.attack(level, target) : null;
  }

  public static void shadowAte(Mob mob) {
    InstinctController controller = SHADOW_CONTROLLERS.get(mob);
    if (controller != null) {
      controller.ateBlock();
    }
  }

  public static void shadowJumped(Mob mob) {
    InstinctController controller = SHADOW_CONTROLLERS.get(mob);
    if (controller != null) {
      controller.jumpedFromGround();
    }
  }

  public static void captureShadowMovement(Mob mob, Vec3 movement) {
    InstinctController controller = SHADOW_CONTROLLERS.get(mob);
    if (controller != null) {
      controller.captureMovement(movement);
    }
  }

  public static void beginInstinctAttack(
      ServerPlayer attacker, LivingEntity target, boolean suppressDrops) {
    ATTACK_CONTEXT.set(new AttackContext(attacker, target, suppressDrops));
  }

  public static void endInstinctAttack() {
    ATTACK_CONTEXT.remove();
  }

  public static boolean shouldSuppressDrops(LivingEntity entity) {
    AttackContext context = ATTACK_CONTEXT.get();
    return context != null && context.target() == entity && context.suppressDrops();
  }

  public static void disable(ServerPlayer player) {
    TransitionState transition = transition(player);
    float finalInstinctLevel = transition.instinctLevel();
    removeController(player);
    ENTRY_REASONS.remove(player.getUUID());
    transition.resetForExit();
    sendDisabled(player, finalInstinctLevel);
  }

  private static void sendDisabled(ServerPlayer player, float finalInstinctLevel) {
    ServerPlayNetworking.send(
        player,
        new MobLifeNetworking.InstinctControlPayload(
            false,
            InstinctState.REST.ordinal(),
            player.getYRot(),
            player.getXRot(),
            0,
            finalInstinctLevel,
            false,
            0.0F,
            0.0F,
            0.0F));
  }

  public static void remove(ServerPlayer player) {
    removeController(player);
    ENTRY_REASONS.remove(player.getUUID());
    TRANSITIONS.remove(player.getUUID());
  }

  public static void resetAfterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
    remove(oldPlayer);
    if (!oldPlayer.getUUID().equals(newPlayer.getUUID())) {
      remove(newPlayer);
    }
    forget(oldPlayer);
    forget(newPlayer);
    sendDisabled(newPlayer, MAXIMUM_LEVEL);
  }

  public static void clear() {
    CONTROLLERS.clear();
    SHADOW_CONTROLLERS.clear();
    ENTRY_REASONS.clear();
    TRANSITIONS.clear();
    ATTACK_CONTEXT.remove();
  }

  private static InstinctController createController(ServerPlayer player, EntryReason reason) {
    if (isEnabled(player)) {
      return null;
    }

    MorphDefinition definition = ServerMorphManager.activeDefinition();
    if (!canEnterInstinct(player, definition, reason)) {
      return null;
    }
    return InstinctController.create(player, definition, MorphConfigManager.get(definition.type()));
  }

  private static boolean canEnterInstinct(
      ServerPlayer player, MorphDefinition definition, EntryReason reason) {
    return canRemainInInstinct(player, definition)
        && (!player.isPassenger() || reason.forcesControl());
  }

  private static boolean canRemainInInstinct(ServerPlayer player, MorphDefinition definition) {
    return definition != null
        && definition.hasMobForm()
        && player.isAlive()
        && !player.isSpectator()
        && !player.isSleeping()
        && MorphConfigManager.get(definition.type()).instinct().enabled();
  }

  private static EntryReason forcedReason(
      ServerPlayer player, MorphDefinition definition, TransitionState transition) {
    if (MorphAwkwardness.isMaximum(player)) {
      return EntryReason.MAXIMUM_AWKWARDNESS;
    }
    MorphConfig config = MorphConfigManager.get(definition.type());
    int scanInterval = config.instinct().senses().scanIntervalTicks();
    if ((player.tickCount + Math.floorMod(player.getId(), scanInterval)) % scanInterval == 0) {
      transition.setPredatorNearby(
          InstinctThreats.hasNearbyTargetingPredator(player, definition, config));
    }
    if (transition.predatorNearby()) {
      return EntryReason.PREDATOR_NEARBY;
    }
    if (!MorphFoodCapacity.isCriticallyHungry(player)) {
      return null;
    }

    if ((player.tickCount + Math.floorMod(player.getId(), scanInterval)) % scanInterval == 0) {
      transition.setPreyNearby(
          ServerMorphManager.activeMorphHasAttackAi()
              && !config.instinct().hunting().prey().isEmpty()
              && !isAbandonedHuntCooldownActive(player)
              && InstinctHunting.hasNearbyNativePrey(player, definition, config));
      transition.setFeedingNearby(InstinctController.hasNearbyFeedingTarget(player, config));
    }

    if (transition.preyNearby()) {
      return EntryReason.HUNGER_PREY;
    }
    return transition.feedingNearby() ? EntryReason.HUNGER_FEEDING : null;
  }

  private static void updateEntryReason(
      ServerPlayer player, InstinctController controller, EntryReason forcedReason) {
    if (forcedReason != null) {
      ENTRY_REASONS.put(player.getUUID(), forcedReason);
      ((InstinctPersistenceHolder) player).mobLife$setRestoreInstinct(false);
      return;
    }

    if (controller.isRespondingToDamage()) {
      ENTRY_REASONS.put(player.getUUID(), EntryReason.DAMAGE_RESPONSE);
      ((InstinctPersistenceHolder) player).mobLife$setRestoreInstinct(false);
      return;
    }

    EntryReason previous = ENTRY_REASONS.get(player.getUUID());
    if (previous != null && previous.forcesControl()) {
      ENTRY_REASONS.put(player.getUUID(), EntryReason.IDLE);
      ((InstinctPersistenceHolder) player).mobLife$setRestoreInstinct(true);
    }
  }

  private static TransitionState transition(ServerPlayer player) {
    return TRANSITIONS.computeIfAbsent(player.getUUID(), ignored -> new TransitionState());
  }

  private static void removeController(ServerPlayer player) {
    InstinctController controller = CONTROLLERS.remove(player.getUUID());
    if (controller != null) {
      SHADOW_CONTROLLERS.remove(controller.shadow());
    }
  }

  private static boolean isPostKillHuntCooldownActive(ServerPlayer player) {
    return cooldowns(player).mobLife$getPostKillHuntCooldownUntil() > gameTime(player);
  }

  private static InstinctPersistenceHolder cooldowns(ServerPlayer player) {
    return (InstinctPersistenceHolder) player;
  }

  private static long gameTime(ServerPlayer player) {
    return player.level().getGameTime();
  }

  private static long extendCooldown(long currentUntil, ServerPlayer player, int ticks) {
    return Math.max(currentUntil, gameTime(player) + Math.max(0, ticks));
  }

  private static void sync(InstinctController controller) {
    InstinctController.Control control = controller.control();
    ServerPlayNetworking.send(
        controller.player(),
        new MobLifeNetworking.InstinctControlPayload(
            true,
            control.state().ordinal(),
            control.targetYaw(),
            control.targetPitch(),
            control.eatTicks(),
            transition(controller.player()).instinctLevel(),
            control.playerInterventionAllowed(),
            (float) control.nativeMovement().x,
            (float) control.nativeMovement().y,
            (float) control.nativeMovement().z));
  }

  private enum EntryReason {
    IDLE(false, true),
    RESTORED(false, true),
    MAXIMUM_AWKWARDNESS(true, false),
    PREDATOR_NEARBY(true, false),
    HUNGER_PREY(true, false),
    HUNGER_FEEDING(true, false),
    DAMAGE_RESPONSE(true, false);

    private final boolean forcesControl;
    private final boolean persistsAcrossReconnect;

    EntryReason(boolean forcesControl, boolean persistsAcrossReconnect) {
      this.forcesControl = forcesControl;
      this.persistsAcrossReconnect = persistsAcrossReconnect;
    }

    boolean forcesControl() {
      return forcesControl;
    }

    boolean persistsAcrossReconnect() {
      return persistsAcrossReconnect;
    }
  }

  private static final class TransitionState {
    private static final int GUI_OPEN_CONFIRMATION_TICKS = 10;

    private int idleTicks;
    private float instinctLevel = MAXIMUM_LEVEL;
    private int lastEscapeInputTick = Integer.MIN_VALUE;
    private int acceptedEscapeInputs;
    private boolean predatorNearby;
    private boolean preyNearby;
    private boolean feedingNearby;
    private boolean activityInitialized;
    private float lastYaw;
    private float lastPitch;
    private int lastActivityTick = Integer.MIN_VALUE;
    private int idleTicksBeforeLastActivity;
    private boolean clientGuiOpen;
    private int pendingGuiActivityIdleTicks = -1;
    private int pendingGuiActivityExpiryTick = Integer.MIN_VALUE;

    void observeActivity(ServerPlayer player) {
      Input input = player.getLastClientInput();
      boolean inputActive =
          input.forward()
              || input.backward()
              || input.left()
              || input.right()
              || input.jump()
              || input.shift()
              || input.sprint();
      boolean viewChanged =
          activityInitialized
              && (Math.abs(Mth.wrapDegrees(player.getYRot() - lastYaw)) > ACTIVITY_ROTATION_EPSILON
                  || Math.abs(player.getXRot() - lastPitch) > ACTIVITY_ROTATION_EPSILON);
      if (inputActive || viewChanged) {
        recordActivity(player.tickCount, isGuiOpen(player));
      }
      activityInitialized = true;
      lastYaw = player.getYRot();
      lastPitch = player.getXRot();
    }

    void recordActivity(int tick, boolean guiOpen) {
      if (guiOpen) {
        return;
      }
      idleTicksBeforeLastActivity = idleTicks;
      lastActivityTick = tick;
      idleTicks = 0;
    }

    void recordPotentialGuiActivity(int tick, boolean guiOpen) {
      if (!guiOpen) {
        pendingGuiActivityIdleTicks = idleTicks;
        pendingGuiActivityExpiryTick = tick + GUI_OPEN_CONFIRMATION_TICKS;
      }
      recordActivity(tick, guiOpen);
    }

    void openedServerContainer(int tick) {
      if (lastActivityTick == tick) {
        idleTicks = idleTicksBeforeLastActivity;
        lastActivityTick = Integer.MIN_VALUE;
      }
      restoreIdleForConfirmedGui(tick);
    }

    void setClientGuiOpen(int tick, boolean open) {
      if (open && !clientGuiOpen) {
        restoreIdleForConfirmedGui(tick);
      }
      clientGuiOpen = open;
    }

    boolean isGuiOpen(ServerPlayer player) {
      return clientGuiOpen || player.hasContainerOpen();
    }

    boolean advanceIdle(ServerPlayer player) {
      if (isGuiOpen(player)) {
        return false;
      }
      if (lastActivityTick == player.tickCount) {
        idleTicks = 0;
        return false;
      }

      idleTicks++;
      return idleTicks >= MorphAwkwardness.instinctEntryDelayTicks(MorphAwkwardness.get(player));
    }

    int acceptEscapeInputs(int tick, int flags) {
      if (lastEscapeInputTick != tick) {
        lastEscapeInputTick = tick;
        acceptedEscapeInputs = 0;
      }
      int accepted = flags & ~acceptedEscapeInputs;
      acceptedEscapeInputs |= accepted;
      return accepted;
    }

    float reduceInstinct(float amount) {
      instinctLevel = Math.max(0.0F, instinctLevel - Math.max(0.0F, amount));
      return instinctLevel;
    }

    void regenerateInstinct() {
      instinctLevel = Math.min(MAXIMUM_LEVEL, instinctLevel + INSTINCT_REGENERATION_PER_TICK);
    }

    float instinctLevel() {
      return instinctLevel;
    }

    void resetForEntry() {
      idleTicks = 0;
      clearGuiActivityCandidate();
      resetInstinctLevel();
    }

    void resetForExit() {
      idleTicks = 0;
      clearGuiActivityCandidate();
      resetInstinctLevel();
    }

    private void resetInstinctLevel() {
      instinctLevel = MAXIMUM_LEVEL;
      lastEscapeInputTick = Integer.MIN_VALUE;
      acceptedEscapeInputs = 0;
    }

    private void restoreIdleForConfirmedGui(int tick) {
      if (pendingGuiActivityIdleTicks >= 0 && tick <= pendingGuiActivityExpiryTick) {
        idleTicks = pendingGuiActivityIdleTicks;
        lastActivityTick = Integer.MIN_VALUE;
      }
      clearGuiActivityCandidate();
    }

    private void clearGuiActivityCandidate() {
      pendingGuiActivityIdleTicks = -1;
      pendingGuiActivityExpiryTick = Integer.MIN_VALUE;
    }

    boolean predatorNearby() {
      return predatorNearby;
    }

    void setPredatorNearby(boolean predatorNearby) {
      this.predatorNearby = predatorNearby;
    }

    boolean preyNearby() {
      return preyNearby;
    }

    void setPreyNearby(boolean preyNearby) {
      this.preyNearby = preyNearby;
    }

    boolean feedingNearby() {
      return feedingNearby;
    }

    void setFeedingNearby(boolean feedingNearby) {
      this.feedingNearby = feedingNearby;
    }
  }

  private record AttackContext(ServerPlayer attacker, LivingEntity target, boolean suppressDrops) {}
}
