package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.gameplay.combat.MorphAttackDamage;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.mixin.instinct.EntityFluidInteractionInvoker;
import cc.attodao.mob_life.mixin.instinct.MobGoalSelectorAccessor;
import cc.attodao.mob_life.mixin.instinct.RabbitCarrotAccessor;
import cc.attodao.mob_life.mixin.instinct.RandomStrollGoalAccessor;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.server.ServerMorphManager;
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
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class InstinctController {
  private static final double IMMEDIATE_FLEE_RANGE = 16.0;
  private static final double FELINE_NATIVE_CHASE_RANGE_SQR = 225.0;
  private static final int MANUAL_FORWARD_TICKS = 6;
  private static final int MANUAL_JUMP_TICKS = 6;
  private static final int GARDEN_EAT_DURATION_TICKS = 20;
  private static final double MANUAL_FORWARD_DISTANCE = 6.0;
  private static final double MANUAL_INTERVENTION_JUMP_VELOCITY = 0.42;

  private final ServerPlayer player;
  private final MorphDefinition definition;
  private final MorphConfig config;
  private final PathfinderMob shadow;
  private final List<FeedingGoal> feedingGoals = new ArrayList<>();
  private boolean hunting;
  private boolean fleeing;
  private int huntingCooldown;
  private int meleeAttackCooldown;
  private boolean attackPerformedThisTick;
  private int eatStateTicks;
  private int gardenEatingTicks;
  private int eatBlockCooldown;
  private int raidGardenCooldown;
  private int scentMemoryTicks;
  private Vec3 lastPreyPosition;
  private Vec3 nativeMovement = Vec3.ZERO;
  private Vec3 capturedMovement = Vec3.ZERO;
  private boolean shadowJumped;
  private boolean rabbitJumped;
  private int manualForwardTicks;
  private int manualJumpTicks;
  private boolean manualJumpThisTick;
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
    this.control = new Control(state, player.getYRot(), player.getXRot(), 0, Vec3.ZERO, false);
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
    return InstinctRelations.nutrition(target, config).isEmpty() || hunting && !fleeing;
  }

  Control control() {
    return control;
  }

  boolean allowsExit() {
    return control.state().acceptsView();
  }

  int intervene(int flags) {
    int accepted = 0;
    if ((flags & InstinctManager.INTERVENE_FORWARD) != 0 && control.state().acceptsForward()) {
      manualForwardTicks = MANUAL_FORWARD_TICKS;
      accepted |= InstinctManager.INTERVENE_FORWARD;
    }
    if ((flags & InstinctManager.INTERVENE_JUMP) != 0
        && control.state().acceptsJump()
        && !player.isInWater()
        && !player.isInLava()) {
      manualJumpTicks = MANUAL_JUMP_TICKS;
      accepted |= InstinctManager.INTERVENE_JUMP;
    }
    return accepted;
  }

  void jumpedFromGround() {
    shadowJumped = true;
  }

  void captureMovement(Vec3 movement) {
    capturedMovement = movement;
  }

  void tick() {
    tickCooldowns();
    if (!player.isAlive() || player.isSpectator() || player.isPassenger()) {
      stopMoving();
      return;
    }

    attackPerformedThisTick = false;
    nativeMovement = Vec3.ZERO;
    capturedMovement = Vec3.ZERO;
    shadowJumped = false;
    rabbitJumped = false;
    manualJumpThisTick = false;

    prepareShadow();
    updateHuntingState();
    validatePreyTarget();
    int scanInterval = config.instinct().senses().scanIntervalTicks();
    if ((player.tickCount + Math.floorMod(player.getId(), scanInterval)) % scanInterval == 0) {
      scanSenses();
    }
    applyScentTarget();
    syncFeedingGoals();
    applyManualJump();

    captureFeedingBlocks();
    int carrotTicksBefore = rabbitCarrotTicks();
    shadow.tickCount++;
    shadow.aiStep();
    nativeMovement = capturedMovement;
    if (manualJumpThisTick) {
      nativeMovement =
          new Vec3(
              nativeMovement.x,
              Math.max(MANUAL_INTERVENTION_JUMP_VELOCITY, nativeMovement.y),
              nativeMovement.z);
    }
    rabbitJumped = shadow instanceof Rabbit && shadowJumped;
    performImmediateMeleeAttack();
    detectGardenEating(carrotTicksBefore);
    validatePreyTarget();
    updateStateAndControl();
    tickManualInterventions();
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
      feed(nutrition);
      eatStateTicks = config.instinct().hunting().eatDurationTicks();
      huntingCooldown = config.instinct().hunting().postKillCooldownTicks();
      hunting = false;
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
        || eatBlockCooldown > 0
        || !player.getFoodData().needsFood()
        || !player.level().getGameRules().get(GameRules.MOB_GRIEFING)
        || !feedingBlockChanged()) {
      return;
    }
    feed(action.nutrition());
    eatBlockCooldown = action.cooldownTicks();
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
    selectors
        .mobLife$getGoalSelector()
        .addGoal(Math.max(1, strollPriority - 1), new ManualForwardGoal(shadow));
    selectors
        .mobLife$getGoalSelector()
        .addGoal(
            strollPriority,
            new GazeBiasedStrollGoal(shadow, player, config.instinct().wander(), strollSpeed));
  }

  private void prepareShadow() {
    shadow.setNoActionTime(0);
    shadow.setHealth(shadow.getMaxHealth());
    shadow.snapTo(player.position(), player.getYRot(), player.getXRot());
    shadow.setYHeadRot(player.getYRot());
    shadow.setYBodyRot(player.getYRot());
    shadow.setOnGround(player.onGround());
    shadow.setDeltaMovement(player.getDeltaMovement());
    ((EntityFluidInteractionInvoker) shadow).mobLife$invokeUpdateFluidInteraction();
  }

  private void updateHuntingState() {
    if (huntingCooldown > 0 || config.instinct().hunting().prey().isEmpty()) {
      hunting = false;
      return;
    }
    float foodRatio =
        player.getFoodData().getFoodLevel()
            / (float) Math.max(1, MorphFoodCapacity.maxFood(player));
    if (!hunting && foodRatio <= config.instinct().hunting().startFoodRatio()) {
      hunting = true;
    } else if (hunting && foodRatio >= config.instinct().hunting().stopFoodRatio()) {
      hunting = false;
    }
  }

  private void scanSenses() {
    MorphConfig.Senses senses = config.instinct().senses();
    List<LivingEntity> nearby = nearbyLiving(Math.max(senses.preyRange(), senses.predatorRange()));
    sensedPredator =
        nearby.stream()
            .filter(
                entity ->
                    entity.distanceToSqr(player) <= senses.predatorRange() * senses.predatorRange())
            .filter(entity -> InstinctRelations.isPredator(entity, definition.type()))
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
    if (!hunting || !MorphAttackDamage.hasAttackAi(definition.type(), shadow)) {
      return;
    }
    nearby.stream()
        .filter(entity -> entity != player)
        .filter(entity -> entity.distanceToSqr(player) <= senses.preyRange() * senses.preyRange())
        .filter(entity -> InstinctRelations.isPrey(entity, definition.type()))
        .min(Comparator.comparingDouble(player::distanceToSqr))
        .ifPresent(shadow::setTarget);
  }

  private List<LivingEntity> nearbyLiving(double range) {
    if (range <= 0.0) {
      return List.of();
    }
    AABB area = player.getBoundingBox().inflate(Math.min(128.0, range));
    return player
        .level()
        .getEntitiesOfClass(
            LivingEntity.class,
            area,
            entity ->
                entity != shadow
                    && entity.isAlive()
                    && entity.distanceToSqr(player) <= range * range);
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

    fleeing =
        sensedPredator != null
            && sensedPredator.isAlive()
            && sensedPredator.distanceToSqr(player) <= IMMEDIATE_FLEE_RANGE * IMMEDIATE_FLEE_RANGE;
    applyInstinctNavigation();
  }

  private void applyInstinctNavigation() {
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
    if (target == player || target != null && (!target.isAlive() || target.isRemoved())) {
      shadow.setTarget(null);
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
      int cooldown = entry.goal() instanceof EatBlockGoal ? eatBlockCooldown : raidGardenCooldown;
      boolean shouldBeAdded = action.enabled() && cooldown <= 0 && player.getFoodData().needsFood();
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
      if (action.enabled() && raidGardenCooldown <= 0) {
        feed(action.nutrition());
        raidGardenCooldown = action.cooldownTicks();
        eatStateTicks = Math.max(eatStateTicks, GARDEN_EAT_DURATION_TICKS);
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
    state = determineState();
    Vec3 lookTarget = lookTarget();
    float targetYaw = player.getYRot();
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
      targetYaw = state == InstinctState.LOOK ? shadow.getYHeadRot() : shadow.getYRot();
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

  private void applyManualJump() {
    if (manualJumpTicks > 0 && ServerMorphManager.canUseInstinctJump(player)) {
      manualJumpThisTick = true;
      shadow.getJumpControl().jump();
      manualJumpTicks = 0;
    }
  }

  private boolean canUseManualForward() {
    return manualForwardTicks > 0
        && !fleeing
        && shadow.getTarget() == null
        && eatStateTicks <= 0
        && state.acceptsForward();
  }

  private Vec3 manualForwardDestination() {
    Vec3 direction = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
    return direction.lengthSqr() > 1.0E-4
        ? shadow.position().add(direction.scale(MANUAL_FORWARD_DISTANCE))
        : shadow.position();
  }

  private InstinctState determineState() {
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
    if (isRunningGoal(FollowParentGoal.class) || isRunningGoal(FollowOwnerGoal.class)) {
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
    manualForwardTicks = 0;
    manualJumpTicks = 0;
    state = InstinctState.REST;
    control = new Control(state, player.getYRot(), player.getXRot(), 0, Vec3.ZERO, false);
  }

  private void tickCooldowns() {
    huntingCooldown = Math.max(0, huntingCooldown - 1);
    meleeAttackCooldown = Math.max(0, meleeAttackCooldown - 1);
    eatStateTicks = Math.max(0, eatStateTicks - 1);
    gardenEatingTicks = Math.max(0, gardenEatingTicks - 1);
    eatBlockCooldown = Math.max(0, eatBlockCooldown - 1);
    raidGardenCooldown = Math.max(0, raidGardenCooldown - 1);
    scentMemoryTicks = Math.max(0, scentMemoryTicks - 1);
    if (scentMemoryTicks == 0 && shadow.getTarget() == null) {
      lastPreyPosition = null;
    }
  }

  private void tickManualInterventions() {
    manualForwardTicks = Math.max(0, manualForwardTicks - 1);
    manualJumpTicks = Math.max(0, manualJumpTicks - 1);
  }

  private void feed(int nutrition) {
    if (nutrition > 0) {
      player.getFoodData().eat(nutrition, 0.0F);
    }
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

  record Control(
      InstinctState state,
      float targetYaw,
      float targetPitch,
      int eatTicks,
      Vec3 nativeMovement,
      boolean rabbitJumped) {}

  private final class ManualForwardGoal extends Goal {
    private final PathfinderMob mob;
    private Vec3 destination;

    private ManualForwardGoal(PathfinderMob mob) {
      this.mob = mob;
      setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
      return canUseManualForward();
    }

    @Override
    public boolean canContinueToUse() {
      return canUseManualForward();
    }

    @Override
    public void start() {
      updateDestination();
    }

    @Override
    public void tick() {
      Vec3 next = manualForwardDestination();
      if (destination == null
          || destination.distanceToSqr(next) > 2.25
          || !mob.getNavigation().isInProgress()) {
        updateDestination();
      }
    }

    @Override
    public void stop() {
      mob.getNavigation().stop();
      destination = null;
    }

    private void updateDestination() {
      destination = manualForwardDestination();
      mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0);
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
      return target != null
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
