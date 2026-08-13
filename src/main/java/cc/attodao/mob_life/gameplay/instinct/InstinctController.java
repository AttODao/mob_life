package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.combat.MorphAttackDamage;
import cc.attodao.mob_life.gameplay.food.MorphEatingSound;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.targeting.MorphNearbyEntities;
import cc.attodao.mob_life.mixin.instinct.MobGoalSelectorAccessor;
import cc.attodao.mob_life.mixin.instinct.RabbitCarrotAccessor;
import cc.attodao.mob_life.mixin.instinct.RandomStrollGoalAccessor;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.BegGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OcelotAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

final class InstinctController {
  private static final double FELINE_NATIVE_CHASE_RANGE_SQR = 225.0;
  private static final int GARDEN_EAT_DURATION_TICKS = 20;
  private static final int PROMPTED_WANDER_MIN_LEGS = 1;
  private static final int PROMPTED_WANDER_MAX_LEGS = 2;
  private static final int MINIMUM_FAILED_WANDER_COOLDOWN_TICKS = 10;
  private static final int LATERAL_INTERFERENCE_COOLDOWN_TICKS = 20;
  private static final int LATERAL_INTERFERENCE_FAILURE_COOLDOWN_TICKS = 10;
  private static final int LATERAL_INTERFERENCE_HOLD_TICKS = 3;
  private static final int DIRECTION_INTENT_HOLD_TICKS = 20;
  private static final int DIRECTION_INTENT_FADE_TICKS = 40;
  private static final int DIRECTION_REPLAN_INTERVAL_TICKS = 20;
  private static final int DAMAGE_RESPONSE_START_GRACE_TICKS = 3;
  private static final int DAMAGE_PANIC_SOURCE_MEMORY_TICKS = 40;
  private static final int GARDEN_SEARCH_RANGE = 16;
  private static final float LATERAL_INTERFERENCE_MIN_TURN_DEGREES = 15.0F;
  private static final float LATERAL_INTERFERENCE_MAX_TURN_DEGREES = 45.0F;
  private static final float MAX_DIRECTION_INTENT_TURN_DEGREES = 3.0F;
  private static final float MIN_DIRECTION_REPLAN_DEGREES = 15.0F;
  private static final float MAX_CLIENT_VIEW_YAW_OFFSET = 30.0F;
  private static final double MINIMUM_HORIZONTAL_MOVEMENT_SQR = 1.0E-4;

  private final ServerPlayer player;
  private final MorphDefinition definition;
  private final MorphConfig config;
  private final PathfinderMob shadow;
  private final List<FeedingGoal> feedingGoals = new ArrayList<>();
  private boolean hunting;
  private LivingEntity huntedTarget;
  private LivingEntity retaliationTarget;
  private int retaliationStartTicks;
  private boolean damageAngerResponse;
  private boolean fleeing;
  private int huntPursuitTicks;
  private int meleeAttackCooldown;
  private boolean attackPerformedThisTick;
  private int eatStateTicks;
  private int gardenEatingTicks;
  private int pendingMealTicks;
  private int pendingMealNutrition;
  private int eatingSoundTicks;
  private int scentMemoryTicks;
  private Vec3 lastPreyPosition;
  private Vec3 nativeMovement = Vec3.ZERO;
  private Vec3 capturedMovement = Vec3.ZERO;
  private float bodyYaw;
  private double nativeWanderSpeedModifier = 1.0;
  private boolean shadowJumped;
  private boolean rabbitJumped;
  private boolean shadowMovementInitialized;
  private int forwardWanderCooldown;
  private int promptedWanderTicks;
  private int promptedWanderLegs;
  private int lateralInterferenceCooldown;
  private int lateralInterferenceHeldTicks;
  private int directionInterferenceHeldTicks;
  private int interferencePauseTicks;
  private int directionIntentTicks;
  private int directionReplanCooldown;
  private int panicStartTicks;
  private int panicEscapeSourceTicks;
  private LivingEntity panicAttacker;
  private Vec3 panicAttackerPosition;
  private float directionIntentYaw;
  private float lastPathIntentYaw;
  private Vec3 promptedWanderAnchor;
  private float promptedWanderYaw;
  private final InstinctSocialController socialController;
  private PromptedWanderGoal promptedWanderGoal;
  private boolean promptedWanderWasActive;
  private InstinctState state = InstinctState.REST;
  private InstinctAction action = InstinctAction.REST;
  private Control control;
  private BlockPos feedingBlockPos;
  private BlockState feedingBlockBefore;
  private BlockState feedingBlockBelowBefore;

  private InstinctController(
      ServerPlayer player, MorphDefinition definition, MorphConfig config, PathfinderMob shadow) {
    this.player = player;
    this.definition = definition;
    this.config = config;
    this.shadow = shadow;
    this.socialController =
        new InstinctSocialController(player, definition, config.instinct().social());
    this.bodyYaw = player.getYRot();
    this.directionIntentYaw = bodyYaw;
    this.lastPathIntentYaw = bodyYaw;
    this.control = new Control(state, bodyYaw, player.getXRot(), 0, Vec3.ZERO, false);
    configureShadow();
  }

  static InstinctController create(
      ServerPlayer player, MorphDefinition definition, MorphConfig config) {
    Entity entity = MorphEntityFactory.create(definition, player.level());
    if (!(entity instanceof PathfinderMob shadow) || shadow.isNoAi()) {
      return null;
    }
    return new InstinctController(player, definition, config, shadow);
  }

  static boolean hasNearbyFeedingTarget(ServerPlayer player, MorphConfig config) {
    if (!player.level().getGameRules().get(GameRules.MOB_GRIEFING)) {
      return false;
    }

    MorphConfig.Feeding feeding = config.instinct().feeding();
    BlockPos position = player.blockPosition();
    if (feeding.eatBlock().enabled() && isEdibleForSheep(player, position)) {
      return true;
    }
    return feeding.raidGarden().enabled() && hasMatureCarrotNearby(player, position);
  }

  ServerPlayer player() {
    return player;
  }

  PathfinderMob shadow() {
    return shadow;
  }

  boolean allowsTarget(LivingEntity target) {
    // Native target and avoid goals must be allowed to perform their own sensing and conditions.
    // Configured prey filtering happens after target selection, before any redirected attack.
    return target != player && target.isAlive();
  }

  Control control() {
    return control;
  }

  boolean allowsExit() {
    return control.state().allowsEscape();
  }

  boolean triggerDamageResponse(DamageSource source) {
    prepareShadow(true);
    InstinctDamageResponse.Result response =
        InstinctDamageResponse.evaluate(shadow, source, player.level().getGameTime());
    if (response.panicking()) {
      panicStartTicks = DAMAGE_RESPONSE_START_GRACE_TICKS;
      rememberPanicAttacker(source);
    } else {
      clearPanicAttacker();
    }
    if (response.retaliationTarget() != null) {
      retaliationTarget = response.retaliationTarget();
      retaliationStartTicks = DAMAGE_RESPONSE_START_GRACE_TICKS;
    }
    if (response.resettingUniversalAnger()) {
      damageAngerResponse = true;
      retaliationStartTicks = DAMAGE_RESPONSE_START_GRACE_TICKS;
    }
    return response.changesBehavior();
  }

  Vec3 panicEscapeSource() {
    if (panicEscapeSourceTicks <= 0) {
      return null;
    }
    if (panicAttacker != null && panicAttacker.isAlive() && !panicAttacker.isRemoved()) {
      return panicAttacker.position();
    }
    return panicAttackerPosition;
  }

  private void rememberPanicAttacker(DamageSource source) {
    if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == player) {
      clearPanicAttacker();
      return;
    }
    panicAttacker = attacker;
    panicAttackerPosition = attacker.position();
    panicEscapeSourceTicks = DAMAGE_PANIC_SOURCE_MEMORY_TICKS;
  }

  private void clearPanicAttacker() {
    panicAttacker = null;
    panicAttackerPosition = null;
    panicEscapeSourceTicks = 0;
  }

  boolean isPanicking() {
    return panicStartTicks > 0 || isRunningGoal(PanicGoal.class);
  }

  boolean isRespondingToDamage() {
    return isPanicking() || isRetaliating() || isAngryFromDamage();
  }

  boolean isThreatResponse() {
    return action.isThreatResponse() || InstinctThreats.isFleeing(shadow, isPanicking());
  }

  boolean pausesAwkwardnessDecay() {
    return interferencePauseTicks > 0
        || lateralInterferenceHeldTicks > 0
        || directionInterferenceHeldTicks > 0;
  }

  int intervene(int flags, float viewYaw) {
    int accepted = 0;
    float requestedViewYaw = constrainedViewYaw(viewYaw);
    if ((flags & InstinctManager.INTERVENE_FORWARD) != 0) {
      if (canInterfereWithWander()) {
        steerDirectionIntent(requestedViewYaw);
        accepted |= InstinctManager.INTERVENE_FORWARD;
      } else if (tryStartPromptedWander(requestedViewYaw)) {
        accepted |= InstinctManager.INTERVENE_FORWARD;
      }
    }
    boolean left = (flags & InstinctManager.INTERVENE_LEFT) != 0;
    boolean right = (flags & InstinctManager.INTERVENE_RIGHT) != 0;
    if (left != right) {
      if (tryRestLateralInterference(left) || tryLateralWanderInterference(left)) {
        accepted |= left ? InstinctManager.INTERVENE_LEFT : InstinctManager.INTERVENE_RIGHT;
      }
    }
    return accepted;
  }

  private boolean tryStartPromptedWander(float viewYaw) {
    if (!canRequestPromptedWander()) {
      return false;
    }
    directionInterferenceHeldTicks = LATERAL_INTERFERENCE_HOLD_TICKS;
    if (forwardWanderCooldown > 0) {
      return true;
    }
    setDirectionIntent(viewYaw);
    registerInterference();
    if (shadow.getRandom().nextFloat()
        >= adjustedControlChance(config.instinct().intervention().forwardWanderChance())) {
      forwardWanderCooldown =
          Math.max(MINIMUM_FAILED_WANDER_COOLDOWN_TICKS, adjustedForwardWanderCooldown() / 4);
      return true;
    }
    forwardWanderCooldown = adjustedForwardWanderCooldown();
    startPromptedWander(directionIntentYaw);
    return true;
  }

  private boolean tryRestLateralInterference(boolean left) {
    if (!InstinctPlayerIntervention.canTurnAtRest(state, action)) {
      return false;
    }
    lateralInterferenceHeldTicks = LATERAL_INTERFERENCE_HOLD_TICKS;
    if (lateralInterferenceCooldown > 0) {
      return true;
    }
    if (shadow.getRandom().nextFloat()
        >= adjustedControlChance(config.instinct().intervention().forwardWanderChance())) {
      lateralInterferenceCooldown = adjustedLateralInterferenceFailureCooldown();
      return true;
    }

    float turnDegrees = randomLateralTurnDegrees();
    bodyYaw = Mth.wrapDegrees(bodyYaw + (left ? -turnDegrees : turnDegrees));
    shadow.setYRot(bodyYaw);
    shadow.setYHeadRot(bodyYaw);
    shadow.setYBodyRot(bodyYaw);
    player.setYBodyRot(bodyYaw);
    setDirectionIntent(bodyYaw);
    lateralInterferenceCooldown = adjustedLateralInterferenceCooldown();
    return true;
  }

  private boolean tryLateralWanderInterference(boolean left) {
    if (!canInterfereWithWander()) {
      return false;
    }
    lateralInterferenceHeldTicks = LATERAL_INTERFERENCE_HOLD_TICKS;
    if (lateralInterferenceCooldown > 0) {
      return true;
    }
    if (shadow.getRandom().nextFloat()
        >= adjustedControlChance(config.instinct().intervention().forwardWanderChance())) {
      lateralInterferenceCooldown = adjustedLateralInterferenceFailureCooldown();
      return true;
    }

    rotateDirectionIntent(left, randomLateralTurnDegrees());
    lateralInterferenceCooldown = adjustedLateralInterferenceCooldown();
    return true;
  }

  private boolean canRequestPromptedWander() {
    return InstinctPlayerIntervention.canRequestWander(
        state, action, promptedWanderTicks, canFollowHerd());
  }

  private boolean canUsePromptedWander() {
    return promptedWanderTicks > 0
        && InstinctPlayerIntervention.canContinueWander(
            state, action, promptedWanderTicks, canFollowHerd());
  }

  private boolean canInterfereWithWander() {
    return state == InstinctState.WANDER
        && InstinctPlayerIntervention.canContinueWander(
            state, action, promptedWanderTicks, canFollowHerd());
  }

  private void startPromptedWander(float headingYaw) {
    MorphConfig.Intervention intervention = config.instinct().intervention();
    int durationRange =
        intervention.forwardWanderDurationMaxTicks()
            - intervention.forwardWanderDurationMinTicks()
            + 1;
    promptedWanderTicks =
        intervention.forwardWanderDurationMinTicks() + shadow.getRandom().nextInt(durationRange);
    promptedWanderLegs =
        PROMPTED_WANDER_MIN_LEGS
            + shadow.getRandom().nextInt(PROMPTED_WANDER_MAX_LEGS - PROMPTED_WANDER_MIN_LEGS + 1);
    promptedWanderAnchor = player.position();
    promptedWanderYaw = Mth.wrapDegrees(headingYaw);
    lastPathIntentYaw = promptedWanderYaw;
    directionReplanCooldown = DIRECTION_REPLAN_INTERVAL_TICKS;
  }

  private void cancelPromptedWanderIfOverridden() {
    if (promptedWanderTicks <= 0) {
      return;
    }
    if (InstinctPlayerIntervention.isOverridden(action)) {
      clearPromptedWander();
    }
  }

  private void clearPromptedWander() {
    promptedWanderTicks = 0;
    promptedWanderLegs = 0;
    promptedWanderAnchor = null;
    if (promptedWanderGoal != null) {
      promptedWanderGoal.clearDestination();
    }
  }

  private float constrainedViewYaw(float viewYaw) {
    if (!Float.isFinite(viewYaw)) {
      return bodyYaw;
    }
    float offset = Mth.wrapDegrees(viewYaw - bodyYaw);
    return Mth.wrapDegrees(
        bodyYaw + Mth.clamp(offset, -MAX_CLIENT_VIEW_YAW_OFFSET, MAX_CLIENT_VIEW_YAW_OFFSET));
  }

  private void setDirectionIntent(float yaw) {
    boolean wasInactive = !hasDirectionIntent();
    directionIntentYaw = Mth.wrapDegrees(yaw);
    directionIntentTicks = DIRECTION_INTENT_HOLD_TICKS + DIRECTION_INTENT_FADE_TICKS;
    if (wasInactive) {
      directionReplanCooldown = DIRECTION_REPLAN_INTERVAL_TICKS;
    }
  }

  private void steerDirectionIntent(float targetYaw) {
    if (!hasDirectionIntent()) {
      directionIntentYaw = bodyYaw;
      directionReplanCooldown = DIRECTION_REPLAN_INTERVAL_TICKS;
    }
    directionIntentYaw =
        approachYaw(directionIntentYaw, targetYaw, MAX_DIRECTION_INTENT_TURN_DEGREES);
    directionIntentTicks = DIRECTION_INTENT_HOLD_TICKS + DIRECTION_INTENT_FADE_TICKS;
  }

  private void rotateDirectionIntent(boolean left, float turnDegrees) {
    if (!hasDirectionIntent()) {
      directionIntentYaw = bodyYaw;
      directionReplanCooldown = DIRECTION_REPLAN_INTERVAL_TICKS;
    }
    directionIntentYaw = Mth.wrapDegrees(directionIntentYaw + (left ? -turnDegrees : turnDegrees));
    directionIntentTicks = DIRECTION_INTENT_HOLD_TICKS + DIRECTION_INTENT_FADE_TICKS;
  }

  private boolean hasDirectionIntent() {
    return directionIntentTicks > 0;
  }

  private float directionIntentStrength() {
    if (directionIntentTicks <= DIRECTION_INTENT_FADE_TICKS) {
      return directionIntentTicks / (float) DIRECTION_INTENT_FADE_TICKS;
    }
    return 1.0F;
  }

  private float wanderDirectionYaw() {
    return hasDirectionIntent() ? directionIntentYaw : bodyYaw;
  }

  private void clearDirectionIntent() {
    directionIntentTicks = 0;
    directionIntentYaw = bodyYaw;
    lastPathIntentYaw = bodyYaw;
  }

  private static float approachYaw(float currentYaw, float targetYaw, float maximumChange) {
    return Mth.wrapDegrees(
        currentYaw
            + Mth.clamp(Mth.wrapDegrees(targetYaw - currentYaw), -maximumChange, maximumChange));
  }

  private float randomLateralTurnDegrees() {
    return Mth.lerp(
        shadow.getRandom().nextFloat(),
        LATERAL_INTERFERENCE_MIN_TURN_DEGREES,
        LATERAL_INTERFERENCE_MAX_TURN_DEGREES);
  }

  private float adjustedControlChance(float chance) {
    float awkwardness = MorphAwkwardness.get(player) / MorphAwkwardness.MAXIMUM;
    return chance * Mth.lerp(Math.clamp(awkwardness, 0.0F, 1.0F), 1.0F, 0.45F);
  }

  private int adjustedForwardWanderCooldown() {
    float awkwardness = MorphAwkwardness.get(player) / MorphAwkwardness.MAXIMUM;
    return Math.round(
        config.instinct().intervention().forwardWanderCooldownTicks()
            * Mth.lerp(Math.clamp(awkwardness, 0.0F, 1.0F), 1.0F, 1.6F));
  }

  private int adjustedLateralInterferenceCooldown() {
    float awkwardness = MorphAwkwardness.get(player) / MorphAwkwardness.MAXIMUM;
    return Math.round(
        LATERAL_INTERFERENCE_COOLDOWN_TICKS
            * Mth.lerp(Math.clamp(awkwardness, 0.0F, 1.0F), 1.0F, 1.6F));
  }

  private int adjustedLateralInterferenceFailureCooldown() {
    float awkwardness = MorphAwkwardness.get(player) / MorphAwkwardness.MAXIMUM;
    return Math.round(
        LATERAL_INTERFERENCE_FAILURE_COOLDOWN_TICKS
            * Mth.lerp(Math.clamp(awkwardness, 0.0F, 1.0F), 1.0F, 1.6F));
  }

  private void registerInterference() {
    interferencePauseTicks =
        Math.max(interferencePauseTicks, config.instinct().intervention().decayPauseTicks());
  }

  void jumpedFromGround() {
    shadowJumped = true;
  }

  void captureMovement(Vec3 movement) {
    capturedMovement = movement;
  }

  void tick() {
    tickCooldowns();
    if (!player.isAlive() || player.isSpectator()) {
      stopMoving();
      return;
    }
    attackPerformedThisTick = false;
    nativeMovement = Vec3.ZERO;
    capturedMovement = Vec3.ZERO;
    shadowJumped = false;
    rabbitJumped = false;
    prepareShadow();
    updateRetaliationState();
    int scanInterval = config.instinct().senses().scanIntervalTicks();
    if ((player.tickCount + Math.floorMod(player.getId(), scanInterval)) % scanInterval == 0) {
      scanSenses();
    }
    updateFleeingState();
    updateHuntingState();
    selectAction();
    enforceActionBeforeAi();
    cancelPromptedWanderIfOverridden();
    syncFeedingGoals();
    captureFeedingBlocks();
    int carrotTicksBefore = rabbitCarrotTicks();
    shadow.tickCount++;
    shadow.aiStep();
    updateRetaliationState();
    nativeMovement = capturedMovement;
    bodyYaw = movementYaw();
    player.setYBodyRot(bodyYaw);
    rabbitJumped = shadow instanceof Rabbit && shadowJumped;
    updateFleeingState();
    updateHuntingState();
    selectAction();
    enforceActionAfterAi();
    selectAction();
    settleRestMovement();
    recordNativePreyTarget();
    performImmediateMeleeAttack();
    detectGardenEating(carrotTicksBefore);
    trackHuntPursuit();
    updateStateAndControl();
    tickEatingSound();
    replanDirectionIntent();
  }

  boolean attack(ServerLevel level, Entity target) {
    if (!(target instanceof LivingEntity living)) {
      return false;
    }
    boolean retaliation = action == InstinctAction.RETALIATE && isActiveRetaliationTarget(living);
    int nutrition = InstinctRelations.nutrition(target, definition.type(), config).orElse(-1);
    boolean prey = action == InstinctAction.HUNT && hunting && nutrition >= 0;
    if (target == player
        || !living.isAlive()
        || !player.isAlive()
        || action.isThreatResponse()
        || isFleeingThreat()
        || (!retaliation && !prey)
        || (prey && meleeAttackCooldown > 0)) {
      return false;
    }

    double damage = MorphAttackDamage.fromMorph(definition.type(), shadow);
    if (damage <= 0.0) {
      return false;
    }

    attackPerformedThisTick = true;
    if (prey) {
      meleeAttackCooldown = config.instinct().hunting().attackCooldownTicks();
    }

    boolean wasAlive = living.isAlive();
    InstinctManager.beginInstinctAttack(player, living, prey && !(living instanceof ServerPlayer));
    boolean hurt;
    try {
      hurt = living.hurtServer(level, player.damageSources().playerAttack(player), (float) damage);
    } finally {
      InstinctManager.endInstinctAttack();
    }
    if (hurt && wasAlive && !living.isAlive() && prey) {
      beginMeal(nutrition, config.instinct().hunting().eatDurationTicks());
      InstinctManager.startPostKillHuntCooldown(
          player, config.instinct().hunting().postKillCooldownTicks());
      hunting = false;
      huntedTarget = null;
      shadow.setTarget(null);
      shadow.getNavigation().stop();
      scentMemoryTicks = 0;
      lastPreyPosition = null;
    }
    return hurt;
  }

  void ateBlock() {
    MorphConfig.FeedingAction action = config.instinct().feeding().eatBlock();
    if (!action.enabled()
        || InstinctManager.isEatBlockCooldownActive(player)
        || !player.getFoodData().needsFood()
        || !player.level().getGameRules().get(GameRules.MOB_GRIEFING)
        || !feedingBlockChanged()) {
      return;
    }
    feed(action.nutrition());
    InstinctManager.startEatBlockCooldown(player, action.cooldownTicks());
  }

  private void configureShadow() {
    InstinctShadowMob.initialize(shadow);
    MobGoalSelectorAccessor selectors = (MobGoalSelectorAccessor) shadow;
    selectors
        .mobLife$getGoalSelector()
        .removeAllGoals(InstinctController::isPlayerIncompatibleGoal);
    selectors
        .mobLife$getTargetSelector()
        .removeAllGoals(InstinctController::isPlayerIncompatibleGoal);
    // Use vanilla AvoidEntityGoal rather than a player-specific distance scan. Its targeting
    // conditions supply LOS/sensing and its pathing supplies the mob's normal flee movement.
    InstinctThreats.installAvoidance(shadow, player, definition, config);
    int strollPriority = Integer.MAX_VALUE;
    double nativeStrollSpeed = 1.0;
    for (WrappedGoal wrapped :
        List.copyOf(selectors.mobLife$getGoalSelector().getAvailableGoals())) {
      Goal goal = wrapped.getGoal();
      if (goal instanceof EatBlockGoal) {
        HungerAwareEatBlockGoal eatBlockGoal = new HungerAwareEatBlockGoal(shadow, player);
        selectors.mobLife$getGoalSelector().removeGoal(goal);
        feedingGoals.add(new FeedingGoal(eatBlockGoal, wrapped.getPriority(), false));
        continue;
      }
      if (isNativeRabbitGardenGoal(goal) && shadow instanceof Rabbit rabbit) {
        HungerAwareRabbitGardenGoal gardenGoal = new HungerAwareRabbitGardenGoal(rabbit);
        selectors.mobLife$getGoalSelector().removeGoal(goal);
        feedingGoals.add(new FeedingGoal(gardenGoal, wrapped.getPriority(), false));
        continue;
      }
      if (isFeedingGoal(goal)) {
        feedingGoals.add(new FeedingGoal(goal, wrapped.getPriority(), true));
      }
      if (goal instanceof OcelotAttackGoal) {
        selectors.mobLife$getGoalSelector().removeGoal(goal);
        selectors
            .mobLife$getGoalSelector()
            .addGoal(
                wrapped.getPriority(),
                new FelineAttackGoal(
                    shadow, config.instinct().hunting().felineSprintStartDistance()));
        continue;
      }
      if (goal instanceof RandomStrollGoal strollGoal) {
        if (wrapped.getPriority() < strollPriority) {
          strollPriority = wrapped.getPriority();
          nativeStrollSpeed = ((RandomStrollGoalAccessor) strollGoal).mobLife$getSpeedModifier();
        }
        selectors.mobLife$getGoalSelector().removeGoal(goal);
      }
    }
    if (strollPriority == Integer.MAX_VALUE) {
      strollPriority = 7;
    }
    nativeWanderSpeedModifier = nativeStrollSpeed;
    promptedWanderGoal = new PromptedWanderGoal(shadow, nativeStrollSpeed);
    selectors
        .mobLife$getGoalSelector()
        .addGoal(Math.max(1, strollPriority - 1), promptedWanderGoal);
    if (config.instinct().social().enabled()) {
      selectors
          .mobLife$getGoalSelector()
          .addGoal(
              Math.max(1, strollPriority - 2), new HerdCohesionGoal(shadow, nativeStrollSpeed));
    }
    selectors
        .mobLife$getGoalSelector()
        .addGoal(
            strollPriority,
            new GazeBiasedStrollGoal(
                shadow,
                this::wanderDirectionYaw,
                this::directionIntentStrength,
                config.instinct().wander(),
                nativeStrollSpeed));
  }

  private void prepareShadow() {
    prepareShadow(!shadowMovementInitialized);
  }

  private void prepareShadow(boolean synchronizeHorizontalMovement) {
    InstinctShadowMob.prepare(shadow, player, bodyYaw, synchronizeHorizontalMovement);
    shadowMovementInitialized = true;
  }

  private void updateRetaliationState() {
    if (retaliationTarget != null
        && (!retaliationTarget.isAlive()
            || retaliationTarget.isRemoved()
            || retaliationTarget == player)) {
      retaliationTarget = null;
      retaliationStartTicks = 0;
      return;
    }

    if (damageAngerResponse) {
      if (shadow instanceof NeutralMob neutralMob && neutralMob.isAngry()) {
        LivingEntity target = shadow.getTarget();
        retaliationTarget =
            target instanceof net.minecraft.world.entity.player.Player
                    && target.isAlive()
                    && !target.isRemoved()
                    && target != player
                ? target
                : null;
        return;
      }
      if (retaliationStartTicks <= 0) {
        damageAngerResponse = false;
      }
    }

    if (isRunningTargetGoal(HurtByTargetGoal.class)) {
      LivingEntity target = shadow.getTarget();
      if (target != null && target.isAlive() && !target.isRemoved() && target != player) {
        retaliationTarget = target;
        return;
      }
    }

    if (retaliationStartTicks <= 0) {
      LivingEntity expiredTarget = retaliationTarget;
      retaliationTarget = null;
      if (expiredTarget != null && shadow.getTarget() == expiredTarget) {
        shadow.setTarget(null);
        shadow.getNavigation().stop();
      }
    }
  }

  private boolean isRetaliating() {
    return retaliationTarget != null
        && retaliationTarget.isAlive()
        && !retaliationTarget.isRemoved()
        && (retaliationStartTicks > 0
            || isRunningTargetGoal(HurtByTargetGoal.class)
            || isAngryFromDamage());
  }

  private boolean isAngryFromDamage() {
    return damageAngerResponse
        && (retaliationStartTicks > 0
            || shadow instanceof NeutralMob neutralMob && neutralMob.isAngry());
  }

  private boolean isActiveRetaliationTarget(LivingEntity target) {
    return target == retaliationTarget && isRetaliating();
  }

  private void updateHuntingState() {
    if (config.instinct().hunting().prey().isEmpty()
        || isEatingMeal()
        || fleeing
        || isPanicking()
        || isRetaliating()) {
      hunting = false;
      clearHuntTarget(false);
      return;
    }
    hunting = InstinctManager.canHunt(player);
    if (!hunting) {
      clearHuntTarget(false);
    }
  }

  private void scanSenses() {
    MorphConfig.Senses senses = config.instinct().senses();
    List<LivingEntity> nearby =
        nearbyLiving(Math.max(senses.predatorRange(), config.instinct().social().searchRange()));
    socialController.update(nearby);
  }

  private boolean canFollowHerd() {
    return socialController.canFollow(fleeing, hasActiveNativeHuntTarget(), eatStateTicks > 0);
  }

  private List<LivingEntity> nearbyLiving(double range) {
    return MorphNearbyEntities.living(player, range).stream()
        .filter(entity -> entity != shadow)
        .toList();
  }

  private void selectAction() {
    // This is the cross-system order. When two native goals themselves conflict, GoalSelector
    // still resolves them using the source mob's vanilla priority.
    boolean eating =
        InstinctFeeding.isEating(
            isEatingMeal(), isRunningGoal(EatBlockGoal.class), isRunningRabbitGardenGoal());
    boolean huntingTarget = hunting && hasActiveNativeHuntTarget();
    boolean wandering =
        shadow.getNavigation().isInProgress() || isRunningGoal(RandomStrollGoal.class);
    action =
        InstinctActionArbiter.select(
            isPanicking(),
            fleeing,
            isRetaliating(),
            eating,
            huntingTarget,
            canFollowHerd(),
            wandering);
  }

  private void enforceActionBeforeAi() {
    if (action.isThreatResponse() || action == InstinctAction.RETALIATE) {
      // Clear a prior prey route before a higher-priority native response gets its movement tick.
      clearHuntTarget(true);
    } else if (action == InstinctAction.EAT) {
      clearHuntTarget(false);
    }
  }

  private void enforceActionAfterAi() {
    if (action.isThreatResponse() || action == InstinctAction.RETALIATE) {
      // A target selector may have run this tick; retain only a native retaliation target.
      clearHuntTarget(false);
      return;
    }
    if (action == InstinctAction.EAT) {
      clearHuntTarget(false);
      return;
    }
    acceptNativePreyTarget();
  }

  private void settleRestMovement() {
    if (action != InstinctAction.REST || !shadow.onGround()) {
      return;
    }
    Vec3 shadowMovement = shadow.getDeltaMovement();
    shadow.setDeltaMovement(0.0, shadowMovement.y, 0.0);
    nativeMovement = new Vec3(0.0, nativeMovement.y, 0.0);
  }

  private void acceptNativePreyTarget() {
    LivingEntity target = shadow.getTarget();
    if (target != null && isActiveRetaliationTarget(target)) {
      huntedTarget = null;
      huntPursuitTicks = 0;
      return;
    }
    if (!hunting) {
      if (target != null || huntedTarget != null) {
        clearHuntTarget(true);
      }
      return;
    }
    if (target == null) {
      huntedTarget = null;
      return;
    }
    if (target == player
        || !target.isAlive()
        || target.isRemoved()
        || InstinctRelations.nutrition(target, definition.type(), config).isEmpty()) {
      clearHuntTarget(true);
      return;
    }
    if (target != huntedTarget) {
      huntPursuitTicks = 0;
    }
    huntedTarget = target;
  }

  private void recordNativePreyTarget() {
    LivingEntity target = shadow.getTarget();
    if (action == InstinctAction.HUNT
        && target != null
        && target.isAlive()
        && !target.isRemoved()
        && InstinctRelations.nutrition(target, definition.type(), config).isPresent()) {
      lastPreyPosition = target.position();
      scentMemoryTicks = config.instinct().senses().memoryTicks();
    }
  }

  private boolean hasActiveNativeHuntTarget() {
    LivingEntity target = shadow.getTarget();
    return target != null
        && !isActiveRetaliationTarget(target)
        && target.isAlive()
        && !target.isRemoved()
        && InstinctRelations.nutrition(target, definition.type(), config).isPresent();
  }

  private void clearHuntTarget(boolean stopNavigation) {
    LivingEntity currentTarget = shadow.getTarget();
    boolean clearCurrentTarget = currentTarget != null && !isActiveRetaliationTarget(currentTarget);
    boolean hadTarget = huntedTarget != null || clearCurrentTarget;
    huntedTarget = null;
    if (clearCurrentTarget) {
      shadow.setTarget(null);
    }
    scentMemoryTicks = 0;
    lastPreyPosition = null;
    if (stopNavigation && hadTarget) {
      shadow.getNavigation().stop();
    }
  }

  private void syncFeedingGoals() {
    var selector = ((MobGoalSelectorAccessor) shadow).mobLife$getGoalSelector();
    for (FeedingGoal entry : feedingGoals) {
      MorphConfig.FeedingAction feedingAction = feedingAction(entry.goal());
      boolean coolingDown =
          entry.goal() instanceof EatBlockGoal
              ? InstinctManager.isEatBlockCooldownActive(player)
              : InstinctManager.isRaidGardenCooldownActive(player);
      boolean shouldBeAdded =
          InstinctFeeding.shouldEnable(
              feedingAction,
              coolingDown,
              isEatingMeal(),
              this.action,
              player.getFoodData().needsFood());
      if (shouldBeAdded == entry.added()) {
        continue;
      }
      if (shouldBeAdded) {
        selector.addGoal(entry.priority(), entry.goal());
      } else {
        selector.removeGoal(entry.goal());
      }
      entry.setAdded(shouldBeAdded);
    }
  }

  private void detectGardenEating(int carrotTicksBefore) {
    int carrotTicksAfter = rabbitCarrotTicks();
    if (carrotTicksAfter > carrotTicksBefore) {
      MorphConfig.FeedingAction action = config.instinct().feeding().raidGarden();
      if (action.enabled()
          && !InstinctManager.isRaidGardenCooldownActive(player)
          && !isEatingMeal()) {
        beginMeal(action.nutrition(), GARDEN_EAT_DURATION_TICKS);
        InstinctManager.startRaidGardenCooldown(player, action.cooldownTicks());
        gardenEatingTicks = GARDEN_EAT_DURATION_TICKS;
      }
    }
  }

  private void captureFeedingBlocks() {
    feedingBlockPos = shadow.blockPosition();
    feedingBlockBefore = player.level().getBlockState(feedingBlockPos);
    feedingBlockBelowBefore = player.level().getBlockState(feedingBlockPos.below());
  }

  private boolean feedingBlockChanged() {
    return feedingBlockPos != null
        && (!player.level().getBlockState(feedingBlockPos).equals(feedingBlockBefore)
            || !player
                .level()
                .getBlockState(feedingBlockPos.below())
                .equals(feedingBlockBelowBefore));
  }

  private int rabbitCarrotTicks() {
    return shadow instanceof Rabbit rabbit
        ? ((RabbitCarrotAccessor) rabbit).mobLife$getMoreCarrotTicks()
        : 0;
  }

  private void updateStateAndControl() {
    InstinctState previousState = state;
    boolean previousPromptedWander = promptedWanderWasActive;
    state = determineState();
    if (previousState == InstinctState.WANDER
        && state != InstinctState.WANDER
        && !previousPromptedWander) {
      forwardWanderCooldown = Math.max(forwardWanderCooldown, adjustedForwardWanderCooldown());
    }
    if (state != InstinctState.REST
        && state != InstinctState.LOOK
        && state != InstinctState.WANDER) {
      clearDirectionIntent();
    }
    promptedWanderWasActive = isRunningGoal(PromptedWanderGoal.class);
    Vec3 lookTarget = lookTarget();
    float targetYaw = bodyYaw;
    float targetPitch = player.getXRot();
    if (lookTarget != null) {
      Vec3 delta = lookTarget.subtract(player.getEyePosition());
      double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
      targetYaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
      targetPitch = (float) -(Mth.atan2(delta.y, horizontal) * Mth.RAD_TO_DEG);
    } else if (state == InstinctState.LOOK
        || state == InstinctState.WANDER
        || state == InstinctState.FOLLOW
        || state == InstinctState.FLEE) {
      targetYaw = state == InstinctState.LOOK ? shadow.getYHeadRot() : bodyYaw;
      if (state == InstinctState.LOOK) {
        targetPitch = shadow.getXRot();
      }
    }
    if (isGrazing()) {
      // Match the grazing animation by directing the player's view toward the food.
      targetPitch = 40.0F;
    }

    int eatTicks = state == InstinctState.EAT ? Math.max(1, instinctEatTicks()) : 0;
    control = new Control(state, targetYaw, targetPitch, eatTicks, nativeMovement, rabbitJumped);
  }

  private void replanDirectionIntent() {
    if (!canInterfereWithWander()
        || !hasDirectionIntent()
        || !shadow.getNavigation().isInProgress()
        || directionReplanCooldown > 0
        || Math.abs(Mth.wrapDegrees(directionIntentYaw - lastPathIntentYaw))
            < MIN_DIRECTION_REPLAN_DEGREES) {
      return;
    }

    Vec3 anchor = promptedWanderTicks > 0 ? promptedWanderAnchor : null;
    Vec3 next = wanderDestination(directionIntentYaw, directionIntentStrength(), anchor);
    Path path = next == null ? null : shadow.getNavigation().createPath(next.x, next.y, next.z, 1);
    if (path != null) {
      shadow.getNavigation().moveTo(path, nativeWanderSpeedModifier);
      lastPathIntentYaw = directionIntentYaw;
    }
    directionReplanCooldown = DIRECTION_REPLAN_INTERVAL_TICKS;
  }

  private float movementYaw() {
    Vec3 horizontal = nativeMovement.multiply(1.0, 0.0, 1.0);
    if (horizontal.lengthSqr() < MINIMUM_HORIZONTAL_MOVEMENT_SQR) {
      return shadow.getYRot();
    }
    return (float) (Mth.atan2(horizontal.z, horizontal.x) * Mth.RAD_TO_DEG) - 90.0F;
  }

  private InstinctState determineState() {
    if (action.isThreatResponse()) {
      return InstinctState.FLEE;
    }
    if (action == InstinctAction.EAT || eatStateTicks > 0) {
      return InstinctState.EAT;
    }
    if (isRunningGoal(EatBlockGoal.class)) {
      return InstinctState.EAT;
    }
    if (isRunningRabbitGardenGoal() && shadow.getNavigation().isInProgress()) {
      return InstinctState.SCENT;
    }
    LivingEntity target = combatTarget();
    if (target != null && target.isAlive()) {
      if (shadow.isWithinMeleeAttackRange(target)) {
        return InstinctState.ATTACK;
      }
      double distance = shadow.distanceTo(target);
      if (distance > 24.0) {
        return InstinctState.SCENT;
      }
      return distance > 8.0 ? InstinctState.STALK : InstinctState.CHASE;
    }
    if (action == InstinctAction.HERD
        || isRunningGoal(HerdCohesionGoal.class)
        || isRunningGoal(FollowParentGoal.class)
        || isRunningGoal(FollowOwnerGoal.class)) {
      return InstinctState.FOLLOW;
    }
    if (isRunningGoal(MeleeAttackGoal.class)
        || isRunningGoal(FelineAttackGoal.class)
        || isRunningGoal(LeapAtTargetGoal.class)) {
      return InstinctState.CHASE;
    }
    if (action == InstinctAction.WANDER
        || shadow.getNavigation().isInProgress()
        || isRunningGoal(RandomStrollGoal.class)) {
      if (scentMemoryTicks > 0 && lastPreyPosition != null) {
        return InstinctState.SCENT;
      }
      return InstinctState.WANDER;
    }
    if (isRunningGoal(RandomLookAroundGoal.class)) {
      return InstinctState.LOOK;
    }
    return InstinctState.REST;
  }

  private Vec3 lookTarget() {
    LivingEntity target = combatTarget();
    if (target != null && target.isAlive()) {
      return target.getEyePosition();
    }
    if (state == InstinctState.SCENT && lastPreyPosition != null) {
      return lastPreyPosition.add(0.0, shadow.getEyeHeight(), 0.0);
    }
    return null;
  }

  private LivingEntity combatTarget() {
    if (action == InstinctAction.RETALIATE && isRetaliating()) {
      return retaliationTarget;
    }
    return shadow.getTarget();
  }

  private boolean isRunningGoal(Class<? extends Goal> goalClass) {
    return runningGoals().stream().anyMatch(goal -> goalClass.isInstance(goal));
  }

  private boolean isRunningGoalNamed(String simpleName) {
    return runningGoals().stream()
        .anyMatch(goal -> goal.getClass().getSimpleName().equals(simpleName));
  }

  private boolean isRunningTargetGoal(Class<? extends Goal> goalClass) {
    return ((MobGoalSelectorAccessor) shadow)
        .mobLife$getTargetSelector().getAvailableGoals().stream()
            .filter(WrappedGoal::isRunning)
            .map(WrappedGoal::getGoal)
            .anyMatch(goalClass::isInstance);
  }

  private List<Goal> runningGoals() {
    return ((MobGoalSelectorAccessor) shadow)
        .mobLife$getGoalSelector().getAvailableGoals().stream()
            .filter(WrappedGoal::isRunning)
            .map(WrappedGoal::getGoal)
            .toList();
  }

  private int instinctEatTicks() {
    int result = eatStateTicks;
    for (Goal goal : runningGoals()) {
      if (goal instanceof EatBlockGoal eatBlockGoal) {
        result = Math.max(result, eatBlockGoal.getEatAnimationTick());
      }
    }
    return result;
  }

  private boolean isGrazing() {
    return shadow instanceof Sheep && isRunningGoal(EatBlockGoal.class)
        || shadow instanceof Rabbit && gardenEatingTicks > 0;
  }

  private void stopMoving() {
    shadow.getNavigation().stop();
    clearPromptedWander();
    clearDirectionIntent();
    state = InstinctState.REST;
    action = InstinctAction.REST;
    control = new Control(state, bodyYaw, player.getXRot(), 0, Vec3.ZERO, false);
  }

  private void tickCooldowns() {
    meleeAttackCooldown = Math.max(0, meleeAttackCooldown - 1);
    eatStateTicks = Math.max(0, eatStateTicks - 1);
    gardenEatingTicks = Math.max(0, gardenEatingTicks - 1);
    if (pendingMealTicks > 0 && --pendingMealTicks == 0) {
      feed(pendingMealNutrition);
      pendingMealNutrition = 0;
    }
    scentMemoryTicks = Math.max(0, scentMemoryTicks - 1);
    forwardWanderCooldown = Math.max(0, forwardWanderCooldown - 1);
    promptedWanderTicks = Math.max(0, promptedWanderTicks - 1);
    lateralInterferenceCooldown = Math.max(0, lateralInterferenceCooldown - 1);
    lateralInterferenceHeldTicks = Math.max(0, lateralInterferenceHeldTicks - 1);
    directionInterferenceHeldTicks = Math.max(0, directionInterferenceHeldTicks - 1);
    interferencePauseTicks = Math.max(0, interferencePauseTicks - 1);
    directionIntentTicks = Math.max(0, directionIntentTicks - 1);
    directionReplanCooldown = Math.max(0, directionReplanCooldown - 1);
    panicStartTicks = Math.max(0, panicStartTicks - 1);
    panicEscapeSourceTicks = Math.max(0, panicEscapeSourceTicks - 1);
    retaliationStartTicks = Math.max(0, retaliationStartTicks - 1);
    if (panicEscapeSourceTicks == 0) {
      panicAttacker = null;
      panicAttackerPosition = null;
    }
    if (scentMemoryTicks == 0 && shadow.getTarget() == null) {
      lastPreyPosition = null;
    }
  }

  private void feed(int nutrition) {
    if (nutrition > 0) {
      player.getFoodData().eat(nutrition, 0.0F);
    }
  }

  private boolean isEatingMeal() {
    return pendingMealTicks > 0;
  }

  private void beginMeal(int nutrition, int durationTicks) {
    if (nutrition <= 0) {
      return;
    }
    if (durationTicks <= 0) {
      feed(nutrition);
      MorphEatingSound.playForEater(player);
      return;
    }

    pendingMealNutrition = nutrition;
    pendingMealTicks = durationTicks;
    eatStateTicks = Math.max(eatStateTicks, durationTicks);
  }

  private void tickEatingSound() {
    boolean stationaryEating =
        action == InstinctAction.EAT && (isEatingMeal() || isRunningGoal(EatBlockGoal.class));
    if (!stationaryEating) {
      eatingSoundTicks = 0;
      return;
    }
    MorphEatingSound.playContinuousTickForEater(player, eatingSoundTicks++);
  }

  private void trackHuntPursuit() {
    LivingEntity target = shadow.getTarget();
    if (!hunting
        || target == null
        || InstinctRelations.nutrition(target, definition.type(), config).isEmpty()) {
      huntPursuitTicks = 0;
      return;
    }

    huntPursuitTicks++;
    if (huntPursuitTicks < config.instinct().hunting().pursuitTimeoutTicks()) {
      return;
    }

    InstinctManager.startAbandonedHuntCooldown(
        player, config.instinct().hunting().abandonedHuntCooldownTicks());
    hunting = false;
    huntPursuitTicks = 0;
    clearHuntTarget(true);
  }

  private void updateFleeingState() {
    fleeing = isFleeingThreat();
  }

  private boolean isFleeingThreat() {
    return InstinctThreats.isFleeing(shadow, isPanicking());
  }

  private void performImmediateMeleeAttack() {
    if (attackPerformedThisTick
        || meleeAttackCooldown > 0
        || !hunting
        || fleeing
        || action != InstinctAction.HUNT
        || !MorphAttackDamage.hasAttackAi(definition.type(), shadow)) {
      return;
    }
    LivingEntity target = shadow.getTarget();
    if (target == null
        || !target.isAlive()
        || target.isRemoved()
        || InstinctRelations.nutrition(target, definition.type(), config).isEmpty()
        || !shadow.isWithinMeleeAttackRange(target)) {
      return;
    }
    shadow.swing(InteractionHand.MAIN_HAND);
    shadow.doHurtTarget(player.level(), target);
  }

  private MorphConfig.FeedingAction feedingAction(Goal goal) {
    return goal instanceof EatBlockGoal
        ? config.instinct().feeding().eatBlock()
        : config.instinct().feeding().raidGarden();
  }

  private static boolean isPlayerIncompatibleGoal(Goal goal) {
    return goal instanceof BreedGoal
        || goal instanceof BegGoal
        || goal instanceof TemptGoal
        || goal instanceof LookAtPlayerGoal
        || goal instanceof FollowOwnerGoal
        || goal instanceof SitWhenOrderedToGoal;
  }

  private static boolean isFeedingGoal(Goal goal) {
    return goal instanceof EatBlockGoal || isNativeRabbitGardenGoal(goal);
  }

  private static boolean isNativeRabbitGardenGoal(Goal goal) {
    return goal.getClass().getSimpleName().equals("RaidGardenGoal");
  }

  private boolean isRunningRabbitGardenGoal() {
    return runningGoals().stream()
        .anyMatch(
            goal -> isNativeRabbitGardenGoal(goal) || goal instanceof HungerAwareRabbitGardenGoal);
  }

  private static boolean isEdibleForSheep(ServerPlayer player, BlockPos position) {
    return player.level().getBlockState(position).is(BlockTags.EDIBLE_FOR_SHEEP)
        || player.level().getBlockState(position.below()).is(Blocks.GRASS_BLOCK);
  }

  private static boolean hasMatureCarrotNearby(ServerPlayer player, BlockPos origin) {
    for (BlockPos position :
        BlockPos.betweenClosed(
            origin.offset(-GARDEN_SEARCH_RANGE, -2, -GARDEN_SEARCH_RANGE),
            origin.offset(GARDEN_SEARCH_RANGE, 2, GARDEN_SEARCH_RANGE))) {
      BlockState state = player.level().getBlockState(position);
      if (player.level().getBlockState(position.below()).is(BlockTags.SUPPORTS_CROPS)
          && state.getBlock() instanceof CarrotBlock carrot
          && carrot.isMaxAge(state)) {
        return true;
      }
    }
    return false;
  }

  record Control(
      InstinctState state,
      float targetYaw,
      float targetPitch,
      int eatTicks,
      Vec3 nativeMovement,
      boolean rabbitJumped) {}

  private final class PromptedWanderGoal extends Goal {
    private final PathfinderMob mob;
    private final double speedModifier;
    private Vec3 destination;

    private PromptedWanderGoal(PathfinderMob mob, double speedModifier) {
      this.mob = mob;
      this.speedModifier = speedModifier;
      setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
      return canUsePromptedWander();
    }

    @Override
    public boolean canContinueToUse() {
      return canUsePromptedWander();
    }

    @Override
    public void start() {
      moveToNextDestination();
    }

    @Override
    public void tick() {
      if (!mob.getNavigation().isInProgress()) {
        moveToNextDestination();
      }
    }

    @Override
    public void stop() {
      mob.getNavigation().stop();
      destination = null;
    }

    private void clearDestination() {
      destination = null;
    }

    private boolean moveToNextDestination() {
      if (promptedWanderLegs <= 0) {
        clearPromptedWander();
        mob.getNavigation().stop();
        return false;
      }
      boolean hasDirectionIntent = hasDirectionIntent();
      float headingYaw = hasDirectionIntent ? directionIntentYaw : promptedWanderYaw;
      float intentStrength = hasDirectionIntent ? directionIntentStrength() : 0.0F;
      Vec3 next = wanderDestination(headingYaw, intentStrength, promptedWanderAnchor);
      if (next == null) {
        clearPromptedWander();
        mob.getNavigation().stop();
        return false;
      }
      Path path = mob.getNavigation().createPath(next.x, next.y, next.z, 1);
      if (path == null) {
        clearPromptedWander();
        mob.getNavigation().stop();
        return false;
      }
      promptedWanderLegs--;
      destination = next;
      mob.getNavigation().moveTo(path, speedModifier);
      lastPathIntentYaw = headingYaw;
      directionReplanCooldown = DIRECTION_REPLAN_INTERVAL_TICKS;
      return true;
    }
  }

  private Vec3 wanderDestination(float headingYaw, float intentStrength, Vec3 anchor) {
    return InstinctWandering.destination(
        shadow, config.instinct().wander(), headingYaw, intentStrength, anchor);
  }

  private final class HerdCohesionGoal extends Goal {
    private final PathfinderMob mob;
    private final double speedModifier;
    private Vec3 destination;

    private HerdCohesionGoal(PathfinderMob mob, double speedModifier) {
      this.mob = mob;
      this.speedModifier = speedModifier;
      setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
      return canFollowHerd();
    }

    @Override
    public boolean canContinueToUse() {
      return canFollowHerd();
    }

    @Override
    public void start() {
      updateDestination();
    }

    @Override
    public void tick() {
      Vec3 herdCenter = socialController.herdCenter();
      if (herdCenter != null
          && (destination == null || destination.distanceToSqr(herdCenter) > 2.25)) {
        updateDestination();
      }
    }

    @Override
    public void stop() {
      mob.getNavigation().stop();
      destination = null;
    }

    private void updateDestination() {
      Vec3 herdCenter = socialController.herdCenter();
      if (herdCenter == null) {
        return;
      }
      destination = herdCenter;
      mob.getNavigation().moveTo(destination.x, destination.y, destination.z, speedModifier);
    }
  }

  private static final class FelineAttackGoal extends Goal {
    private final Mob mob;
    private final double sprintStartDistanceSqr;
    private LivingEntity target;
    private int attackTime;

    private FelineAttackGoal(Mob mob, double sprintStartDistance) {
      this.mob = mob;
      sprintStartDistanceSqr = sprintStartDistance * sprintStartDistance;
      setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
      LivingEntity target = mob.getTarget();
      if (target == null) {
        return false;
      }
      this.target = target;
      return mob.distanceToSqr(target) <= FELINE_NATIVE_CHASE_RANGE_SQR;
    }

    @Override
    public boolean canContinueToUse() {
      return mob.getTarget() == target
          && target != null
          && target.isAlive()
          && mob.distanceToSqr(target) <= FELINE_NATIVE_CHASE_RANGE_SQR
          && (!mob.getNavigation().isDone() || canUse());
    }

    @Override
    public void stop() {
      target = null;
      mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
      return true;
    }

    @Override
    public void tick() {
      mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
      double attackRangeSqr = Math.pow(mob.getBbWidth() * 2.0F, 2.0);
      double distanceSqr = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
      double speed = 0.8;
      if (distanceSqr > attackRangeSqr) {
        speed = distanceSqr < sprintStartDistanceSqr ? 1.33 : 0.6;
      }
      mob.getNavigation().moveTo(target, speed);
      attackTime = Math.max(attackTime - 1, 0);
      if (distanceSqr <= attackRangeSqr && attackTime <= 0) {
        attackTime = 20;
        mob.doHurtTarget((ServerLevel) mob.level(), target);
      }
    }
  }

  private static final class HungerAwareEatBlockGoal extends EatBlockGoal {
    private final Mob mob;
    private final ServerPlayer player;

    private HungerAwareEatBlockGoal(Mob mob, ServerPlayer player) {
      super(mob);
      this.mob = mob;
      this.player = player;
    }

    @Override
    public boolean canUse() {
      BlockPos position = mob.blockPosition();
      if (!MorphFoodCapacity.isCriticallyHungry(player)
          && mob.getRandom().nextInt(adjustedTickDelay(mob.isBaby() ? 50 : 1000)) != 0) {
        return false;
      }
      return mob.level().getBlockState(position).is(BlockTags.EDIBLE_FOR_SHEEP)
          || mob.level().getBlockState(position.below()).is(Blocks.GRASS_BLOCK);
    }
  }

  private final class HungerAwareRabbitGardenGoal
      extends net.minecraft.world.entity.ai.goal.MoveToBlockGoal {
    private final Rabbit rabbit;
    private boolean wantsToRaid;
    private boolean canRaid;

    private HungerAwareRabbitGardenGoal(Rabbit rabbit) {
      super(rabbit, 0.7, GARDEN_SEARCH_RANGE);
      this.rabbit = rabbit;
    }

    @Override
    public boolean canUse() {
      if (!(rabbit.level() instanceof ServerLevel level)
          || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
        return false;
      }
      boolean forced = MorphFoodCapacity.isCriticallyHungry(player);
      if (!forced && nextStartTick > 0) {
        nextStartTick--;
        return false;
      }
      nextStartTick = forced ? 0 : nextStartTick(rabbit);
      canRaid = false;
      wantsToRaid = rabbitCarrotTicks() <= 0;
      return findNearestBlock();
    }

    @Override
    public boolean canContinueToUse() {
      return canRaid && super.canContinueToUse();
    }

    @Override
    public void tick() {
      super.tick();
      BlockPos target = blockPos.above();
      rabbit
          .getLookControl()
          .setLookAt(
              target.getX() + 0.5,
              target.getY() + 1.0,
              target.getZ() + 0.5,
              10.0F,
              rabbit.getMaxHeadXRot());
      if (!isReachedTarget()) {
        return;
      }

      BlockState state = rabbit.level().getBlockState(target);
      if (!canRaid || !(state.getBlock() instanceof CarrotBlock carrot)) {
        return;
      }
      int age = state.getValue(CarrotBlock.AGE);
      if (age == 0) {
        rabbit.level().setBlock(target, Blocks.AIR.defaultBlockState(), 2);
        rabbit.level().destroyBlock(target, true, rabbit);
      } else {
        rabbit.level().setBlock(target, state.setValue(CarrotBlock.AGE, age - 1), 2);
        rabbit.level().gameEvent(GameEvent.BLOCK_CHANGE, target, GameEvent.Context.of(rabbit));
        rabbit.level().levelEvent(2001, target, Block.getId(state));
      }
      ((RabbitCarrotAccessor) rabbit).mobLife$setMoreCarrotTicks(40);
      canRaid = false;
      nextStartTick = 10;
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos position) {
      if (!wantsToRaid) {
        return false;
      }
      BlockState support = level.getBlockState(position);
      BlockState crop = level.getBlockState(position.above());
      if (!support.is(BlockTags.SUPPORTS_CROPS)
          || !(crop.getBlock() instanceof CarrotBlock carrot)
          || !carrot.isMaxAge(crop)) {
        return false;
      }
      canRaid = true;
      return true;
    }
  }

  private static final class FeedingGoal {
    private final Goal goal;
    private final int priority;
    private boolean added;

    private FeedingGoal(Goal goal, int priority, boolean added) {
      this.goal = goal;
      this.priority = priority;
      this.added = added;
    }

    Goal goal() {
      return goal;
    }

    int priority() {
      return priority;
    }

    boolean added() {
      return added;
    }

    void setAdded(boolean added) {
      this.added = added;
    }
  }
}
