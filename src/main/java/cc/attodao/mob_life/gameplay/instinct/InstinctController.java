package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.combat.MorphAttackDamage;
import cc.attodao.mob_life.gameplay.targeting.MorphNearbyEntities;
import cc.attodao.mob_life.mixin.instinct.EntityFluidInteractionInvoker;
import cc.attodao.mob_life.mixin.instinct.MobGoalSelectorAccessor;
import cc.attodao.mob_life.mixin.instinct.RabbitCarrotAccessor;
import cc.attodao.mob_life.mixin.instinct.RandomStrollGoalAccessor;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import java.util.ArrayList;
import java.util.Comparator;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
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
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

final class InstinctController {
  private static final double IMMEDIATE_FLEE_RANGE = 16.0;
  private static final double FELINE_NATIVE_CHASE_RANGE_SQR = 225.0;
  private static final int GARDEN_EAT_DURATION_TICKS = 20;
  private static final int PROMPTED_WANDER_MIN_LEGS = 1;
  private static final int PROMPTED_WANDER_MAX_LEGS = 2;
  private static final int WANDER_CANDIDATE_COUNT = 12;
  private static final int MINIMUM_FAILED_WANDER_COOLDOWN_TICKS = 10;
  private static final int LATERAL_INTERFERENCE_COOLDOWN_TICKS = 20;
  private static final int LATERAL_INTERFERENCE_FAILURE_COOLDOWN_TICKS = 10;
  private static final int LATERAL_INTERFERENCE_HOLD_TICKS = 3;
  private static final int DIRECTION_INTENT_HOLD_TICKS = 20;
  private static final int DIRECTION_INTENT_FADE_TICKS = 40;
  private static final int DIRECTION_REPLAN_INTERVAL_TICKS = 20;
  private static final int EXIT_REST_HOLD_TICKS = 4;
  private static final int DAMAGE_PANIC_MEMORY_TICKS = 40;
  private static final int GARDEN_SEARCH_RANGE = 16;
  private static final float LATERAL_INTERFERENCE_MIN_TURN_DEGREES = 15.0F;
  private static final float LATERAL_INTERFERENCE_MAX_TURN_DEGREES = 45.0F;
  private static final float MAX_DIRECTION_INTENT_TURN_DEGREES = 3.0F;
  private static final float MIN_DIRECTION_REPLAN_DEGREES = 15.0F;
  private static final float DIRECTION_INTENT_WEIGHT = 0.7F;
  private static final float DIRECTION_INTENT_PRIMARY_CONE_DEGREES = 15.0F;
  private static final float DIRECTION_INTENT_FALLBACK_CONE_DEGREES = 45.0F;
  private static final float MAX_CLIENT_VIEW_YAW_OFFSET = 30.0F;
  private static final double MINIMUM_HORIZONTAL_MOVEMENT_SQR = 1.0E-4;

  private final ServerPlayer player;
  private final MorphDefinition definition;
  private final MorphConfig config;
  private final PathfinderMob shadow;
  private final List<FeedingGoal> feedingGoals = new ArrayList<>();
  private boolean hunting;
  private LivingEntity huntedTarget;
  private boolean fleeing;
  private int huntPursuitTicks;
  private int meleeAttackCooldown;
  private boolean attackPerformedThisTick;
  private int eatStateTicks;
  private int gardenEatingTicks;
  private int pendingMealTicks;
  private int pendingMealNutrition;
  private int scentMemoryTicks;
  private Vec3 lastPreyPosition;
  private Vec3 nativeMovement = Vec3.ZERO;
  private Vec3 capturedMovement = Vec3.ZERO;
  private float bodyYaw;
  private double wanderSpeedModifier = 1.0;
  private boolean shadowJumped;
  private boolean rabbitJumped;
  private int forwardWanderCooldown;
  private int promptedWanderTicks;
  private int promptedWanderLegs;
  private int lateralInterferenceCooldown;
  private int lateralInterferenceHeldTicks;
  private int directionInterferenceHeldTicks;
  private int interferencePauseTicks;
  private int directionIntentTicks;
  private int directionReplanCooldown;
  private int exitRestHoldTicks;
  private int damagePanicTicks;
  private float directionIntentYaw;
  private float lastPathIntentYaw;
  private Vec3 promptedWanderAnchor;
  private Vec3 panicSourcePosition;
  private float promptedWanderYaw;
  private Vec3 herdCenter;
  private PromptedWanderGoal promptedWanderGoal;
  private boolean promptedWanderWasActive;
  private boolean hasFelineAttackGoal;
  private InstinctState state = InstinctState.REST;
  private Control control;
  private LivingEntity sensedPredator;
  private BlockPos feedingBlockPos;
  private BlockState feedingBlockBefore;
  private BlockState feedingBlockBelowBefore;

  private InstinctController(
      ServerPlayer player, MorphDefinition definition, MorphConfig config, PathfinderMob shadow) {
    this.player = player;
    this.definition = definition;
    this.config = config;
    this.shadow = shadow;
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
    if (target == player || fleeing) {
      return false;
    }
    if (hunting) {
      return huntedTarget == null
          ? InstinctRelations.nutrition(target, config).isPresent()
          : target == huntedTarget;
    }
    return InstinctRelations.nutrition(target, config).isEmpty() || hunting && !fleeing;
  }

  Control control() {
    return control;
  }

  boolean allowsExit() {
    return control.state().acceptsView();
  }

  boolean triggerPanic(DamageSource source) {
    prepareShadow();
    shadow.setInvulnerable(false);
    try {
      shadow.hurtServer((ServerLevel) player.level(), source, 0.0F);
    } finally {
      shadow.setInvulnerable(true);
    }

    boolean canPanic =
        ((MobGoalSelectorAccessor) shadow)
            .mobLife$getGoalSelector().getAvailableGoals().stream()
                .map(WrappedGoal::getGoal)
                .filter(PanicGoal.class::isInstance)
                .map(PanicGoal.class::cast)
                .anyMatch(PanicGoal::canUse);
    if (canPanic) {
      damagePanicTicks = DAMAGE_PANIC_MEMORY_TICKS;
      panicSourcePosition = source.getSourcePosition();
    }
    return canPanic;
  }

  boolean isPanicking() {
    return damagePanicTicks > 0 || isRunningGoal(PanicGoal.class);
  }

  void holdRestForExit() {
    if (allowsExit()) {
      exitRestHoldTicks = EXIT_REST_HOLD_TICKS;
    }
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
    if (state != InstinctState.REST || fleeing || hunting || shadow.getTarget() != null) {
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
    return (state == InstinctState.REST || state == InstinctState.LOOK)
        && !fleeing
        && !hunting
        && shadow.getTarget() == null
        && eatStateTicks <= 0
        && promptedWanderTicks <= 0
        && !canFollowHerd();
  }

  private boolean canUsePromptedWander() {
    return promptedWanderTicks > 0
        && !fleeing
        && !hunting
        && shadow.getTarget() == null
        && eatStateTicks <= 0
        && !canFollowHerd();
  }

  private boolean canInterfereWithWander() {
    return state == InstinctState.WANDER
        && !fleeing
        && !hunting
        && shadow.getTarget() == null
        && eatStateTicks <= 0
        && !canFollowHerd();
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
    if (fleeing || hunting || shadow.getTarget() != null || eatStateTicks > 0 || canFollowHerd()) {
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
    if (exitRestHoldTicks > 0) {
      tickExitRestHold();
      return;
    }

    attackPerformedThisTick = false;
    nativeMovement = Vec3.ZERO;
    capturedMovement = Vec3.ZERO;
    shadowJumped = false;
    rabbitJumped = false;
    prepareShadow();
    updateHuntingState();
    int scanInterval = config.instinct().senses().scanIntervalTicks();
    if ((player.tickCount + Math.floorMod(player.getId(), scanInterval)) % scanInterval == 0) {
      scanSenses();
    }
    updateFleeingState();
    validatePreyTarget();
    applyScentTarget();
    cancelPromptedWanderIfOverridden();
    syncFeedingGoals();
    captureFeedingBlocks();
    int carrotTicksBefore = rabbitCarrotTicks();
    shadow.tickCount++;
    shadow.aiStep();
    nativeMovement = capturedMovement;
    bodyYaw = movementYaw();
    player.setYBodyRot(bodyYaw);
    rabbitJumped = shadow instanceof Rabbit && shadowJumped;
    updateFleeingState();
    validatePreyTarget();
    performImmediateMeleeAttack();
    detectGardenEating(carrotTicksBefore);
    trackHuntPursuit();
    updateStateAndControl();
    replanDirectionIntent();
  }

  boolean attack(ServerLevel level, Entity target) {
    if (!(target instanceof LivingEntity living)
        || target == player
        || !living.isAlive()
        || !player.isAlive()
        || meleeAttackCooldown > 0) {
      return false;
    }

    double damage = MorphAttackDamage.fromMorph(definition.type(), shadow);
    if (damage <= 0.0) {
      return false;
    }

    attackPerformedThisTick = true;
    meleeAttackCooldown = config.instinct().hunting().attackCooldownTicks();

    int nutrition = InstinctRelations.nutrition(target, config).orElse(-1);
    boolean prey = nutrition >= 0;
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
    shadow.setInvulnerable(true);
    shadow.setSilent(true);
    shadow.setNoGravity(false);
    shadow.setCanPickUpLoot(false);
    shadow.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    shadow.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    MobGoalSelectorAccessor selectors = (MobGoalSelectorAccessor) shadow;
    selectors
        .mobLife$getGoalSelector()
        .removeAllGoals(InstinctController::isPlayerIncompatibleGoal);
    selectors
        .mobLife$getTargetSelector()
        .removeAllGoals(InstinctController::isPlayerIncompatibleGoal);
    int strollPriority = Integer.MAX_VALUE;
    double strollSpeed = 1.0;
    for (WrappedGoal wrapped :
        List.copyOf(selectors.mobLife$getGoalSelector().getAvailableGoals())) {
      Goal goal = wrapped.getGoal();
      if (goal instanceof EatBlockGoal) {
        HungerAwareEatBlockGoal eatBlockGoal = new HungerAwareEatBlockGoal(shadow);
        selectors.mobLife$getGoalSelector().removeGoal(goal);
        feedingGoals.add(new FeedingGoal(eatBlockGoal, wrapped.getPriority(), false));
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
        hasFelineAttackGoal = true;
        continue;
      }
      if (goal instanceof RandomStrollGoal strollGoal) {
        if (wrapped.getPriority() < strollPriority) {
          strollPriority = wrapped.getPriority();
          strollSpeed = ((RandomStrollGoalAccessor) strollGoal).mobLife$getSpeedModifier();
        }
        selectors.mobLife$getGoalSelector().removeGoal(goal);
      }
    }
    if (strollPriority == Integer.MAX_VALUE) {
      strollPriority = 7;
    }
    wanderSpeedModifier = strollSpeed;
    promptedWanderGoal = new PromptedWanderGoal(shadow, strollSpeed);
    selectors
        .mobLife$getGoalSelector()
        .addGoal(Math.max(1, strollPriority - 1), promptedWanderGoal);
    if (config.instinct().social().enabled()) {
      selectors
          .mobLife$getGoalSelector()
          .addGoal(Math.max(1, strollPriority - 2), new HerdCohesionGoal(shadow, strollSpeed));
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
                strollSpeed));
  }

  private void prepareShadow() {
    shadow.setNoActionTime(0);
    shadow.setHealth(shadow.getMaxHealth());
    shadow.snapTo(player.position(), bodyYaw, 0.0F);
    shadow.setYHeadRot(bodyYaw);
    shadow.setYBodyRot(bodyYaw);
    shadow.setOnGround(player.onGround());
    shadow.setDeltaMovement(player.getDeltaMovement());
    shadow.setRemainingFireTicks(player.getRemainingFireTicks());
    ((EntityFluidInteractionInvoker) shadow).mobLife$invokeUpdateFluidInteraction();
  }

  private void updateHuntingState() {
    if (config.instinct().hunting().prey().isEmpty() || isEatingMeal()) {
      hunting = false;
      huntedTarget = null;
      shadow.setTarget(null);
      return;
    }
    hunting = InstinctManager.canHunt(player);
    if (!hunting) {
      huntedTarget = null;
      shadow.setTarget(null);
    }
  }

  private void scanSenses() {
    MorphConfig.Senses senses = config.instinct().senses();
    List<LivingEntity> nearby =
        nearbyLiving(
            Math.max(
                Math.max(senses.preyRange(), senses.predatorRange()),
                config.instinct().social().searchRange()));
    updateHerdCenter(nearby);
    sensedPredator =
        nearby.stream()
            .filter(
                entity ->
                    entity.distanceToSqr(player) <= senses.predatorRange() * senses.predatorRange())
            .filter(entity -> InstinctRelations.isPredator(entity, definition.type()))
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
    if (!hunting
        || isFleeingThreat()
        || !MorphAttackDamage.hasAttackAi(definition.type(), shadow)) {
      return;
    }
    if (huntedTarget == null && shadow.getTarget() == null) {
      nearby.stream()
          .filter(entity -> entity != player)
          .filter(entity -> entity.distanceToSqr(player) <= senses.preyRange() * senses.preyRange())
          .filter(entity -> InstinctRelations.isPrey(entity, definition.type()))
          .min(Comparator.comparingDouble(player::distanceToSqr))
          .ifPresent(
              target -> {
                huntedTarget = target;
                shadow.setTarget(target);
                huntPursuitTicks = 0;
              });
    }
  }

  private void updateHerdCenter(List<LivingEntity> nearby) {
    MorphConfig.Social social = config.instinct().social();
    if (!social.enabled()) {
      herdCenter = null;
      return;
    }

    Vec3 sum = Vec3.ZERO;
    int members = 0;
    for (LivingEntity entity : nearby) {
      if (entity.getType() != definition.type().entityType()
          || entity.distanceToSqr(player) > social.searchRange() * social.searchRange()) {
        continue;
      }
      sum = sum.add(entity.position());
      members++;
    }
    herdCenter =
        members >= social.minimumGroupSize()
            ? new Vec3(sum.x / members, player.getY(), sum.z / members)
            : null;
  }

  private boolean canFollowHerd() {
    if (herdCenter == null
        || fleeing
        || hunting
        || shadow.getTarget() != null
        || eatStateTicks > 0) {
      return false;
    }
    Vec3 delta = herdCenter.subtract(player.position()).multiply(1.0, 0.0, 1.0);
    double preferredRange = config.instinct().social().preferredRange();
    return delta.lengthSqr() > preferredRange * preferredRange;
  }

  private List<LivingEntity> nearbyLiving(double range) {
    return MorphNearbyEntities.living(player, range).stream()
        .filter(entity -> entity != shadow)
        .toList();
  }

  private void applyScentTarget() {
    LivingEntity target = shadow.getTarget();
    if (target != null
        && target.isAlive()
        && !target.isRemoved()
        && InstinctRelations.nutrition(target, config).isPresent()) {
      lastPreyPosition = target.position();
      scentMemoryTicks = config.instinct().senses().memoryTicks();
    }

    updateFleeingState();
    applyInstinctNavigation();
  }

  private void applyInstinctNavigation() {
    if (isPanicking()) {
      if (shadow.getTarget() != null) {
        shadow.setTarget(null);
      }
      if (!isRunningGoal(PanicGoal.class) && !shadow.getNavigation().isInProgress()) {
        Vec3 destination =
            panicSourcePosition != null
                ? LandRandomPos.getPosAway(shadow, 5, 4, panicSourcePosition)
                : LandRandomPos.getPos(shadow, 5, 4);
        if (destination != null) {
          shadow.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.4);
        }
      }
      return;
    }
    if (fleeing) {
      LivingEntity currentTarget = shadow.getTarget();
      if (currentTarget != null) {
        shadow.setTarget(null);
      }
      if (shadow.getNavigation().isInProgress()
          && (isRunningGoal(AvoidEntityGoal.class) || isRunningGoal(PanicGoal.class))) {
        return;
      }
      Vec3 away =
          player.position().subtract(sensedPredator.position()).multiply(1.0, 0.0, 1.0).normalize();
      if (away.lengthSqr() > 1.0E-4) {
        Vec3 destination = player.position().add(away.scale(16.0));
        shadow.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.5);
      }
      return;
    }
    LivingEntity target = shadow.getTarget();
    if (hunting
        && hasFelineAttackGoal
        && target != null
        && target.isAlive()
        && shadow.distanceToSqr(target) > FELINE_NATIVE_CHASE_RANGE_SQR) {
      double sprintRange = config.instinct().hunting().felineSprintStartDistance();
      double speed = shadow.distanceToSqr(target) <= sprintRange * sprintRange ? 1.33 : 1.0;
      shadow.getNavigation().moveTo(target, speed);
      return;
    }
    if (hunting
        && scentMemoryTicks > 0
        && lastPreyPosition != null
        && shadow.getTarget() == null
        && !isRunningGoal(EatBlockGoal.class)
        && !isRunningGoalNamed("RaidGardenGoal")) {
      shadow
          .getNavigation()
          .moveTo(lastPreyPosition.x, lastPreyPosition.y, lastPreyPosition.z, 1.0);
    }
  }

  private void validatePreyTarget() {
    LivingEntity target = shadow.getTarget();
    if (fleeing) {
      if (target != null) {
        shadow.setTarget(null);
      }
      return;
    }
    if (hunting && huntedTarget != null) {
      if (huntedTarget.isAlive()
          && !huntedTarget.isRemoved()
          && InstinctRelations.nutrition(huntedTarget, config).isPresent()) {
        if (target != huntedTarget) {
          shadow.setTarget(huntedTarget);
        }
        return;
      }
      huntedTarget = null;
      shadow.setTarget(null);
      return;
    }
    if (target == player || target != null && (!target.isAlive() || target.isRemoved())) {
      shadow.setTarget(null);
      return;
    }
    if (hunting && target != null && InstinctRelations.nutrition(target, config).isPresent()) {
      huntedTarget = target;
      return;
    }
    if (target != null && InstinctRelations.nutrition(target, config).isPresent() && !hunting) {
      shadow.setTarget(null);
      shadow.getNavigation().stop();
    }
  }

  private void syncFeedingGoals() {
    var selector = ((MobGoalSelectorAccessor) shadow).mobLife$getGoalSelector();
    for (FeedingGoal entry : feedingGoals) {
      MorphConfig.FeedingAction action = feedingAction(entry.goal());
      boolean coolingDown =
          entry.goal() instanceof EatBlockGoal
              ? InstinctManager.isEatBlockCooldownActive(player)
              : InstinctManager.isRaidGardenCooldownActive(player);
      boolean shouldBeAdded =
          action.enabled() && !coolingDown && !isEatingMeal() && player.getFoodData().needsFood();
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
      shadow.getNavigation().moveTo(path, wanderSpeedModifier);
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
    if (isPanicking()) {
      return InstinctState.FLEE;
    }
    if (sensedPredator != null
        && sensedPredator.isAlive()
        && sensedPredator.distanceToSqr(player) <= IMMEDIATE_FLEE_RANGE * IMMEDIATE_FLEE_RANGE) {
      return InstinctState.FLEE;
    }
    if (isRunningGoal(PanicGoal.class) || isRunningGoal(AvoidEntityGoal.class)) {
      return InstinctState.FLEE;
    }
    if (eatStateTicks > 0) {
      return InstinctState.EAT;
    }
    if (isRunningGoal(EatBlockGoal.class)) {
      return InstinctState.EAT;
    }
    if (isRunningGoalNamed("RaidGardenGoal") && shadow.getNavigation().isInProgress()) {
      return InstinctState.SCENT;
    }
    LivingEntity target = shadow.getTarget();
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
    if (isRunningGoal(HerdCohesionGoal.class)
        || isRunningGoal(FollowParentGoal.class)
        || isRunningGoal(FollowOwnerGoal.class)) {
      return InstinctState.FOLLOW;
    }
    if (isRunningGoal(MeleeAttackGoal.class)
        || isRunningGoal(FelineAttackGoal.class)
        || isRunningGoal(LeapAtTargetGoal.class)) {
      return InstinctState.CHASE;
    }
    if (shadow.getNavigation().isInProgress() || isRunningGoal(RandomStrollGoal.class)) {
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
    LivingEntity target = shadow.getTarget();
    if (target != null && target.isAlive()) {
      return target.getEyePosition();
    }
    if (state == InstinctState.SCENT && lastPreyPosition != null) {
      return lastPreyPosition.add(0.0, shadow.getEyeHeight(), 0.0);
    }
    return null;
  }

  private boolean isRunningGoal(Class<? extends Goal> goalClass) {
    return runningGoals().stream().anyMatch(goal -> goalClass.isInstance(goal));
  }

  private boolean isRunningGoalNamed(String simpleName) {
    return runningGoals().stream()
        .anyMatch(goal -> goal.getClass().getSimpleName().equals(simpleName));
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
    exitRestHoldTicks = 0;
    state = InstinctState.REST;
    control = new Control(state, bodyYaw, player.getXRot(), 0, Vec3.ZERO, false);
  }

  private void tickExitRestHold() {
    Vec3 playerMovement = player.getDeltaMovement();
    nativeMovement = new Vec3(0.0, playerMovement.y, 0.0);
    capturedMovement = Vec3.ZERO;
    shadowJumped = false;
    rabbitJumped = false;
    shadow.getNavigation().stop();
    clearPromptedWander();
    clearDirectionIntent();
    player.setDeltaMovement(nativeMovement);
    player.setYBodyRot(bodyYaw);
    state = InstinctState.REST;
    control = new Control(state, bodyYaw, player.getXRot(), 0, nativeMovement, false);
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
    exitRestHoldTicks = Math.max(0, exitRestHoldTicks - 1);
    damagePanicTicks = Math.max(0, damagePanicTicks - 1);
    if (scentMemoryTicks == 0 && shadow.getTarget() == null) {
      lastPreyPosition = null;
    }
    if (damagePanicTicks == 0) {
      panicSourcePosition = null;
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
      return;
    }

    pendingMealNutrition = nutrition;
    pendingMealTicks = durationTicks;
    eatStateTicks = Math.max(eatStateTicks, durationTicks);
  }

  private void trackHuntPursuit() {
    LivingEntity target = shadow.getTarget();
    if (!hunting || target == null || InstinctRelations.nutrition(target, config).isEmpty()) {
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
    huntedTarget = null;
    huntPursuitTicks = 0;
    shadow.setTarget(null);
    shadow.getNavigation().stop();
    scentMemoryTicks = 0;
    lastPreyPosition = null;
  }

  private void updateFleeingState() {
    fleeing = isFleeingThreat();
  }

  private boolean isFleeingThreat() {
    return isPanicking()
        || isRunningGoal(AvoidEntityGoal.class)
        || sensedPredator != null
            && sensedPredator.isAlive()
            && sensedPredator.distanceToSqr(player) <= IMMEDIATE_FLEE_RANGE * IMMEDIATE_FLEE_RANGE;
  }

  private void performImmediateMeleeAttack() {
    if (attackPerformedThisTick
        || meleeAttackCooldown > 0
        || !hunting
        || fleeing
        || !MorphAttackDamage.hasAttackAi(definition.type(), shadow)) {
      return;
    }
    LivingEntity target = shadow.getTarget();
    if (target == null
        || !target.isAlive()
        || target.isRemoved()
        || !InstinctRelations.nutrition(target, config).isPresent()
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
    return goal instanceof EatBlockGoal || goal.getClass().getSimpleName().equals("RaidGardenGoal");
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
    MorphConfig.Wander wander = config.instinct().wander();
    Vec3 direction = directionFromYaw(headingYaw);
    float directionWeight = Mth.lerp(intentStrength, wander.gazeWeight(), DIRECTION_INTENT_WEIGHT);
    float primaryConeDegrees =
        Mth.lerp(intentStrength, 180.0F, DIRECTION_INTENT_PRIMARY_CONE_DEGREES);
    float fallbackConeDegrees =
        Mth.lerp(intentStrength, 180.0F, DIRECTION_INTENT_FALLBACK_CONE_DEGREES);
    Vec3 primary = null;
    Vec3 fallback = null;
    Vec3 unrestricted = null;
    double primaryScore = Double.NEGATIVE_INFINITY;
    double fallbackScore = Double.NEGATIVE_INFINITY;
    double unrestrictedScore = Double.NEGATIVE_INFINITY;
    for (int index = 0; index < WANDER_CANDIDATE_COUNT; index++) {
      Vec3 candidate =
          index == 0
              ? LandRandomPos.getPosTowards(
                  shadow,
                  wander.horizontalRange(),
                  wander.verticalRange(),
                  shadow.position().add(direction.scale(wander.horizontalRange())))
              : LandRandomPos.getPos(shadow, wander.horizontalRange(), wander.verticalRange());
      if (candidate == null) {
        continue;
      }
      Vec3 fromAnchor =
          anchor == null ? Vec3.ZERO : candidate.subtract(anchor).multiply(1.0, 0.0, 1.0);
      if (anchor != null
          && fromAnchor.lengthSqr() > wander.horizontalRange() * wander.horizontalRange()) {
        continue;
      }
      Vec3 towardCandidate = candidate.subtract(shadow.position()).multiply(1.0, 0.0, 1.0);
      if (towardCandidate.lengthSqr() < MINIMUM_HORIZONTAL_MOVEMENT_SQR) {
        continue;
      }
      double alignment = direction.dot(towardCandidate.normalize());
      double distanceScore =
          1.0 - Math.min(1.0, towardCandidate.length() / Math.max(1.0, wander.horizontalRange()));
      double score =
          directionWeight * ((alignment + 1.0) * 0.5) + (1.0F - directionWeight) * distanceScore;
      if (score > unrestrictedScore) {
        unrestrictedScore = score;
        unrestricted = candidate;
      }
      double angle = Math.acos(Math.clamp(alignment, -1.0, 1.0)) * Mth.RAD_TO_DEG;
      if (angle <= fallbackConeDegrees && score > fallbackScore) {
        fallbackScore = score;
        fallback = candidate;
      }
      if (angle <= primaryConeDegrees && score > primaryScore) {
        primaryScore = score;
        primary = candidate;
      }
    }
    return primary != null ? primary : fallback != null ? fallback : unrestricted;
  }

  private static Vec3 directionFromYaw(float yaw) {
    float radians = yaw * Mth.DEG_TO_RAD;
    return new Vec3(-Mth.sin(radians), 0.0, Mth.cos(radians));
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
      if (destination == null || destination.distanceToSqr(herdCenter) > 2.25) {
        updateDestination();
      }
    }

    @Override
    public void stop() {
      mob.getNavigation().stop();
      destination = null;
    }

    private void updateDestination() {
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

    private HungerAwareEatBlockGoal(Mob mob) {
      super(mob);
      this.mob = mob;
    }

    @Override
    public boolean canUse() {
      BlockPos position = mob.blockPosition();
      return mob.level().getBlockState(position).is(BlockTags.EDIBLE_FOR_SHEEP)
          || mob.level().getBlockState(position.below()).is(Blocks.GRASS_BLOCK);
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
