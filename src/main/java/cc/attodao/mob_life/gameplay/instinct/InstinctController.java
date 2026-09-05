package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.mixin.instinct.LivingEntityDamageAccessor;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.morph.MorphType;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class InstinctController {
  private static final double[] GUIDED_WANDER_DISTANCES = {10.0, 8.0, 6.0, 4.0};
  private static final float[] GUIDED_WANDER_OFFSETS = {0.0F, -7.5F, 7.5F, -15.0F, 15.0F};
  private static final double RABBIT_WATER_RETAINED_HORIZONTAL_SPEED = 0.02;
  private static final int RABBIT_WATER_EXTERNAL_MOTION_TICKS = 5;
  private static final float HEAD_TRACK_PER_TICK = 10.0F;
  private static final float HEAD_RECOVERY_PER_TICK = 2.0F;

  private final MorphType morph;
  private final Mob proxy;
  private final boolean fixedBaby;
  private InstinctActivity activity = InstinctActivity.REST;
  private BlockPos hungryForageTarget;
  private LivingEntity hungryPreyTarget;
  private int forwardUrgeTicks;
  private int steeringCooldown;
  private float previousSteeringInput;
  private boolean steeringRetryPending;
  private boolean restRequested;
  private boolean restFacingLocked;
  private long lastHungrySearchTick = Long.MIN_VALUE;
  private boolean motionInitialized;
  private boolean wasInWater;
  private int rabbitWaterExternalMotionTicks;

  InstinctController(MorphDefinition definition, ServerPlayer player) {
    morph = definition.type();
    var entity = MorphEntityFactory.create(definition, player.level());
    if (!(entity instanceof Mob mob)) {
      throw new IllegalArgumentException("Instinct profile requires a Mob entity");
    }
    proxy = mob;
    proxy.setYRot(player.yBodyRot);
    proxy.yRotO = player.yBodyRot;
    proxy.yBodyRot = player.yBodyRot;
    proxy.yBodyRotO = player.yBodyRot;
    proxy.setYHeadRot(player.getYHeadRot());
    proxy.yHeadRotO = player.getYHeadRot();
    proxy.setXRot(player.getXRot());
    proxy.xRotO = player.getXRot();
    proxy.setInvulnerable(true);
    proxy.setSilent(true);
    wasInWater = player.isInWater();
    fixedBaby = proxy instanceof AgeableMob ageable && ageable.isBaby();
    if (proxy instanceof AbstractHorse horse) {
      horse.setTamed(true);
      horse.setOwner(null);
    }
    if (proxy instanceof Rabbit rabbit && rabbit.getVariant() != Rabbit.Variant.EVIL) {
      proxy.getGoalSelector().addGoal(4, new TransformedWolfAvoidGoal(rabbit, player));
    }
  }

  Mob proxy() {
    return proxy;
  }

  InstinctActivity activity() {
    return activity;
  }

  boolean canBeRiddenByOtherPlayer() {
    return proxy instanceof AbstractHorse && !fixedBaby;
  }

  boolean canLayEgg() {
    return proxy instanceof Chicken chicken && !fixedBaby && !chicken.isChickenJockey();
  }

  Output tick(
      ServerPlayer player,
      InstinctInput input,
      boolean criticallyHungry,
      InstinctStateData state,
      DamageSource pendingDamage) {
    mirror(player, pendingDamage != null);
    constrainRabbitWaterMotion(player, pendingDamage != null);
    applyBiologicalState(state);
    if (proxy instanceof Chicken chicken) {
      chicken.eggTime = Integer.MAX_VALUE;
    }
    float bodyYawBeforeTick = proxy.getYRot();
    float headYawBeforeTick = proxy.getYHeadRot();
    float headPitchBeforeTick = proxy.getXRot();
    boolean acceptsResistance =
        state.loveTicks() == 0
            && pendingDamage == null
            && (activity == InstinctActivity.REST || activity == InstinctActivity.WANDER);
    InstinctActivity activityBeforeTick = activity;
    if (acceptsResistance) {
      applyResistanceBias(input);
    } else {
      forwardUrgeTicks = 0;
      steeringCooldown = 0;
      previousSteeringInput = 0.0F;
      steeringRetryPending = false;
      restRequested = false;
    }
    if ((proxy.getTarget() == null || !proxy.getTarget().isAlive()) && player.tickCount % 20 == 0) {
      findReachableTransformedPrey(player);
    }
    boolean hungryPriority = false;
    if (criticallyHungry) {
      long gameTime = player.level().getGameTime();
      if (lastHungrySearchTick == Long.MIN_VALUE
          || hungryPreyTarget != null && !hungryPreyTarget.isAlive()
          || gameTime - lastHungrySearchTick >= 20) {
        lastHungrySearchTick = gameTime;
        findReachableHungryTarget(player);
        if (hungryPreyTarget == null
            && player.level().getGameRules().get(GameRules.MOB_GRIEFING)
            && (morph == MorphType.SHEEP || morph == MorphType.RABBIT)) {
          hasReachableForage(player);
        }
      }
      hungryPriority = hasHungryPreyTarget() || hasHungryForageTarget(player);
      applyHungryPreyTarget();
      applyHungryForagePath();
    }
    if (pendingDamage != null) {
      applyDamageReaction(pendingDamage);
    }

    Vec3 start = proxy.position();
    // Registered mobs have this reset by ServerLevel before their AI tick. The detached proxy
    // needs the same treatment or RandomStrollGoal permanently disables itself after five seconds.
    proxy.setNoActionTime(0);
    InstinctAiContext.run(player, morph, proxy, explorationYaw(input), hungryPriority, proxy::tick);
    captureBiologicalState(state);
    activity = classifyActivity();
    if (restRequested
        && (activity == InstinctActivity.REST || activity == InstinctActivity.WANDER)) {
      stopWandering();
      activity = InstinctActivity.REST;
    }
    boolean endedWander =
        activityBeforeTick == InstinctActivity.WANDER && activity == InstinctActivity.REST;
    if (endedWander) {
      proxy.getMoveControl().setWait();
      proxy.setXxa(0.0F);
      proxy.setYya(0.0F);
      proxy.setZza(0.0F);
    }
    boolean groundedRest = activity == InstinctActivity.REST && proxy.onGround();
    if (groundedRest) {
      if (endedWander && Math.abs(Mth.wrapDegrees(proxy.getYRot() - bodyYawBeforeTick)) > 90.0F) {
        restoreBodyFacing(bodyYawBeforeTick);
      }
      clearHorizontalMotion();
    }
    boolean manualRestTurn = false;
    if (!acceptsResistance || activity != InstinctActivity.REST) {
      restFacingLocked = false;
    } else if (activityBeforeTick == InstinctActivity.REST) {
      boolean restTurning = Math.abs(input.sideways()) >= 0.2F && input.forward() < 0.2F;
      if (restTurning) {
        restFacingLocked = true;
      }
      if (restFacingLocked && input.forward() < 0.2F) {
        applyRestTurn(input, bodyYawBeforeTick);
        manualRestTurn = true;
      }
    }
    Vec3 motion = proxy.position().subtract(start);
    if (groundedRest) {
      motion = new Vec3(0.0, motion.y, 0.0);
    }
    if (!Double.isFinite(motion.x) || !Double.isFinite(motion.y) || !Double.isFinite(motion.z)) {
      motion = Vec3.ZERO;
    }
    if (proxy instanceof Rabbit) {
      if (motion.horizontalDistanceSqr() > 1.0E-4) {
        float motionYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-motion.x, motion.z)));
        restoreBodyFacing(Mth.rotateIfNecessary(bodyYawBeforeTick, motionYaw, 15.0F));
      } else if (!manualRestTurn
          && acceptsResistance
          && (activityBeforeTick == InstinctActivity.REST
              || activityBeforeTick == InstinctActivity.WANDER)
          && (activity == InstinctActivity.REST || activity == InstinctActivity.WANDER)) {
        restoreBodyFacingKeepingHead(bodyYawBeforeTick);
      }
    }
    boolean lookingAtTarget = proxy.getLookControl().isLookingAtTarget();
    constrainBodyAndHead(
        input, bodyYawBeforeTick, headYawBeforeTick, headPitchBeforeTick, lookingAtTarget);
    double horizontalSpeed = proxy.getDeltaMovement().horizontalDistance();
    if (!Double.isFinite(horizontalSpeed)) {
      horizontalSpeed = 0.0;
    }
    return new Output(
        motion,
        (float) horizontalSpeed,
        proxy.getYRot(),
        proxy.getYHeadRot(),
        proxy.getXRot(),
        proxy.onGround(),
        lookingAtTarget,
        activity);
  }

  boolean hasReachableNaturalTarget(ServerPlayer player) {
    mirror(player, false);
    double range =
        Math.max(
            2.0,
            proxy.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE));
    AABB area = proxy.getBoundingBox().inflate(range, 4.0, range);
    return player
        .level()
        .getEntitiesOfClass(LivingEntity.class, area, this::isNaturalTarget)
        .stream()
        .sorted(Comparator.comparingDouble(proxy::distanceToSqr))
        .anyMatch(target -> proxy.getNavigation().createPath(target, 1) != null);
  }

  boolean hasAvoidThreat(ServerPlayer player, float awkwardness) {
    mirror(player, false);
    return InstinctProfiles.hasAvoidThreat(proxy, morph, player, awkwardness);
  }

  boolean hasReachableForage(ServerPlayer player) {
    mirror(player, false);
    lastHungrySearchTick = player.level().getGameTime();
    BlockPos origin = player.blockPosition();
    int range = morph == MorphType.RABBIT ? 16 : 8;
    for (BlockPos pos :
        BlockPos.betweenClosed(origin.offset(-range, -2, -range), origin.offset(range, 2, range))) {
      var state = player.level().getBlockState(pos);
      BlockPos destination = null;
      if (morph == MorphType.SHEEP) {
        if (state.is(BlockTags.EDIBLE_FOR_SHEEP)) {
          destination = pos;
        } else if (state.is(Blocks.GRASS_BLOCK)
            && player.level().getBlockState(pos.above()).isAir()) {
          destination = pos.above();
        }
      } else if (morph == MorphType.RABBIT
          && state.getBlock() instanceof CarrotBlock carrot
          && carrot.isMaxAge(state)) {
        destination = pos;
      }
      if (destination != null && proxy.getNavigation().createPath(destination, 1) != null) {
        hungryForageTarget = destination.immutable();
        return true;
      }
    }
    return false;
  }

  void clearForageTarget() {
    hungryForageTarget = null;
  }

  boolean hasReachableHungryTarget(ServerPlayer player) {
    mirror(player, false);
    lastHungrySearchTick = player.level().getGameTime();
    return findReachableHungryTarget(player);
  }

  boolean acceptsBreedingFood(ItemStack stack, InstinctStateData state) {
    if (fixedBaby
        || state.loveTicks() > 0
        || state.breedingCooldown() > 0
        || !(proxy instanceof Animal animal)
        || !animal.isFood(stack)) {
      return false;
    }
    return switch (morph) {
      case CAT, OCELOT, WOLF, MULE -> false;
      case HORSE, DONKEY ->
          stack.is(net.minecraft.world.item.Items.GOLDEN_CARROT)
              || stack.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
              || stack.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE);
      default -> true;
    };
  }

  private boolean findReachableHungryTarget(ServerPlayer player) {
    double range =
        Math.max(
            2.0,
            proxy.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE));
    AABB area = proxy.getBoundingBox().inflate(range, 4.0, range);
    var targets = player.level().getEntitiesOfClass(LivingEntity.class, area, this::isEdibleTarget);
    targets.sort(Comparator.comparingDouble(proxy::distanceToSqr));
    for (LivingEntity target : targets) {
      if (proxy.getNavigation().createPath(target, 1) != null) {
        hungryPreyTarget = target;
        return true;
      }
    }
    hungryPreyTarget = null;
    return false;
  }

  private void findReachableTransformedPrey(ServerPlayer owner) {
    double range =
        Math.max(
            2.0,
            proxy.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE));
    var targets =
        owner
            .level()
            .getEntitiesOfClass(
                ServerPlayer.class,
                proxy.getBoundingBox().inflate(range, range, range),
                target -> target != owner && isNaturalTarget(target));
    targets.sort(Comparator.comparingDouble(proxy::distanceToSqr));
    for (ServerPlayer target : targets) {
      if (proxy.getNavigation().createPath(target, 1) != null) {
        proxy.setTarget(target);
        return;
      }
    }
  }

  private boolean isEdibleTarget(LivingEntity target) {
    return target != proxy
        && InstinctProfiles.isEdiblePrey(morph, target)
        && proxy.canAttack(target);
  }

  private boolean isNaturalTarget(LivingEntity target) {
    return target != proxy
        && InstinctProfiles.isEnabledNaturalPrey(morph, target)
        && proxy.canAttack(target);
  }

  private void mirror(ServerPlayer player, boolean acceptExternalMotion) {
    proxy.setPos(player.getX(), player.getY(), player.getZ());
    proxy.xo = player.xo;
    proxy.yo = player.yo;
    proxy.zo = player.zo;
    proxy.setOnGround(player.onGround());
    if (!motionInitialized || acceptExternalMotion) {
      proxy.setDeltaMovement(player.getDeltaMovement());
      motionInitialized = true;
    }
    proxy.setHealth(Math.min(proxy.getMaxHealth(), player.getHealth()));
    proxy.setRemainingFireTicks(player.getRemainingFireTicks());
    proxy.setAirSupply(player.getAirSupply());
    proxy.setItemSlot(EquipmentSlot.BODY, player.getItemBySlot(EquipmentSlot.BODY).copy());
    proxy.setItemSlot(EquipmentSlot.SADDLE, player.getItemBySlot(EquipmentSlot.SADDLE).copy());
  }

  private void applyBiologicalState(InstinctStateData state) {
    if (!(proxy instanceof Animal animal) || fixedBaby) {
      return;
    }
    animal.setAge(state.breedingCooldown());
    animal.setInLoveTime(state.loveTicks());
  }

  private void captureBiologicalState(InstinctStateData state) {
    if (!(proxy instanceof Animal animal) || fixedBaby) {
      state.setLoveTicks(0);
      return;
    }
    state.setLoveTicks(animal.getInLoveTime());
    state.setBreedingCooldown(Math.max(0, animal.getAge()));
  }

  private void constrainBodyAndHead(
      InstinctInput input,
      float previousBodyYaw,
      float previousHeadYaw,
      float previousHeadPitch,
      boolean aiLookTarget) {
    float nativeBodyYaw = proxy.getYRot();
    float nativeHeadYaw = proxy.getYHeadRot();
    float nativeHeadPitch = proxy.getXRot();
    float bodyYaw = Mth.rotateIfNecessary(previousBodyYaw, nativeBodyYaw, 90.0F);
    boolean directCameraInput = input.cameraDelta() > 0.0F;
    float desiredHeadYaw =
        aiLookTarget ? nativeHeadYaw : directCameraInput ? input.cameraYaw() : bodyYaw;
    float desiredHeadPitch =
        aiLookTarget ? nativeHeadPitch : directCameraInput ? input.cameraPitch() : 0.0F;
    float headStep =
        aiLookTarget || directCameraInput ? HEAD_TRACK_PER_TICK : HEAD_RECOVERY_PER_TICK;
    float headYaw = Mth.rotateIfNecessary(previousHeadYaw, desiredHeadYaw, headStep);
    headYaw = bodyYaw + Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -75.0F, 75.0F);
    float headPitch =
        Mth.approach(previousHeadPitch, Mth.clamp(desiredHeadPitch, -40.0F, 40.0F), headStep);
    proxy.setYRot(bodyYaw);
    proxy.yRotO = bodyYaw;
    proxy.yBodyRot = bodyYaw;
    proxy.yBodyRotO = bodyYaw;
    proxy.setYHeadRot(headYaw);
    proxy.yHeadRotO = headYaw;
    proxy.setXRot(headPitch);
    proxy.xRotO = headPitch;
  }

  private void applyResistanceBias(InstinctInput input) {
    boolean steeringChanged =
        Math.abs(input.sideways()) >= 0.2F
            && (Math.abs(previousSteeringInput) < 0.2F
                || Math.signum(input.sideways()) != Math.signum(previousSteeringInput)
                || Math.abs(input.sideways() - previousSteeringInput) >= 0.25F);
    if (input.forward() >= 0.2F) {
      forwardUrgeTicks = Math.min(20, forwardUrgeTicks + 1);
    } else {
      forwardUrgeTicks = 0;
    }
    if (activity == InstinctActivity.WANDER && input.forward() <= -0.2F) {
      restRequested = true;
    } else if (input.forward() >= -0.1F) {
      restRequested = false;
    }
    if (restRequested) {
      stopWandering();
      activity = InstinctActivity.REST;
      steeringCooldown = 0;
      steeringRetryPending = false;
    }
    if (activity == InstinctActivity.REST
        && forwardUrgeTicks >= 20
        && proxy.getNavigation().isDone()
        && steeringCooldown == 0) {
      if (startGuidedWander(input)) {
        activity = InstinctActivity.WANDER;
        forwardUrgeTicks = 0;
      }
      steeringCooldown = 10;
    }
    if (steeringCooldown > 0) {
      steeringCooldown--;
    }
    if (Math.abs(input.sideways()) < 0.2F || activity != InstinctActivity.WANDER) {
      steeringRetryPending = false;
    } else if (steeringChanged || steeringRetryPending && steeringCooldown == 0) {
      steeringRetryPending = !startGuidedWander(input);
      steeringCooldown = steeringRetryPending ? 2 : 10;
    }
    previousSteeringInput = input.sideways();
  }

  private void applyRestTurn(InstinctInput input, float bodyYawBeforeTick) {
    float bodyYaw = bodyYawBeforeTick;
    if (Math.abs(input.sideways()) >= 0.2F) {
      bodyYaw = Mth.wrapDegrees(bodyYaw + input.sideways() * 10.0F);
    }
    proxy.setYRot(bodyYaw);
    proxy.yRotO = bodyYaw;
    proxy.yBodyRot = bodyYaw;
    proxy.yBodyRotO = bodyYaw;
  }

  private void restoreBodyFacing(float bodyYaw) {
    float headOffset = Mth.wrapDegrees(proxy.getYHeadRot() - proxy.getYRot());
    proxy.setYRot(bodyYaw);
    proxy.yRotO = bodyYaw;
    proxy.yBodyRot = bodyYaw;
    proxy.yBodyRotO = bodyYaw;
    float headYaw = bodyYaw + Mth.clamp(headOffset, -75.0F, 75.0F);
    proxy.setYHeadRot(headYaw);
    proxy.yHeadRotO = headYaw;
  }

  private void restoreBodyFacingKeepingHead(float bodyYaw) {
    float headYaw =
        bodyYaw + Mth.clamp(Mth.wrapDegrees(proxy.getYHeadRot() - bodyYaw), -75.0F, 75.0F);
    proxy.setYRot(bodyYaw);
    proxy.yRotO = bodyYaw;
    proxy.yBodyRot = bodyYaw;
    proxy.yBodyRotO = bodyYaw;
    proxy.setYHeadRot(headYaw);
    proxy.yHeadRotO = headYaw;
  }

  private void stopWandering() {
    for (WrappedGoal wrapped : proxy.getGoalSelector().getAvailableGoals()) {
      if (wrapped.isRunning() && wrapped.getFlags().contains(Goal.Flag.MOVE)) {
        wrapped.stop();
      }
    }
    proxy.getNavigation().stop();
    proxy.getMoveControl().setWait();
    proxy.setXxa(0.0F);
    proxy.setYya(0.0F);
    proxy.setZza(0.0F);
    if (proxy.onGround()) {
      clearHorizontalMotion();
    }
  }

  private void clearHorizontalMotion() {
    Vec3 motion = proxy.getDeltaMovement();
    proxy.setDeltaMovement(0.0, motion.y, 0.0);
  }

  private void constrainRabbitWaterMotion(ServerPlayer player, boolean preserveExternalMotion) {
    boolean inWater = player.isInWater();
    if (preserveExternalMotion) {
      rabbitWaterExternalMotionTicks = RABBIT_WATER_EXTERNAL_MOTION_TICKS;
    }
    if (proxy instanceof Rabbit rabbit && inWater && rabbitWaterExternalMotionTicks == 0) {
      Vec3 motion = proxy.getDeltaMovement();
      double horizontalSpeed = motion.horizontalDistance();
      double horizontalScale =
          horizontalSpeed > RABBIT_WATER_RETAINED_HORIZONTAL_SPEED
              ? RABBIT_WATER_RETAINED_HORIZONTAL_SPEED / horizontalSpeed
              : 1.0;
      proxy.setDeltaMovement(
          motion.x * horizontalScale, Math.min(motion.y, 0.0), motion.z * horizontalScale);
      if (!wasInWater) {
        rabbit.setJumping(false);
      }
    }
    if (rabbitWaterExternalMotionTicks > 0) {
      rabbitWaterExternalMotionTicks--;
    }
    if (!inWater) {
      rabbitWaterExternalMotionTicks = 0;
    }
    wasInWater = inWater;
  }

  private boolean startGuidedWander(InstinctInput input) {
    float centerYaw = explorationYaw(input);
    for (double distance : GUIDED_WANDER_DISTANCES) {
      for (float offset : GUIDED_WANDER_OFFSETS) {
        double radians = Math.toRadians(centerYaw + offset);
        Path path =
            proxy
                .getNavigation()
                .createPath(
                    proxy.getX() - Math.sin(radians) * distance,
                    proxy.getY(),
                    proxy.getZ() + Math.cos(radians) * distance,
                    1);
        if (path != null && path.canReach() && proxy.getNavigation().moveTo(path, 1.0)) {
          return true;
        }
      }
    }
    return false;
  }

  private static float explorationYaw(InstinctInput input) {
    return input.cameraYaw() + input.sideways() * 15.0F;
  }

  private void applyDamageReaction(DamageSource source) {
    LivingEntityDamageAccessor accessor = (LivingEntityDamageAccessor) proxy;
    accessor.mobLife$setLastDamageSource(source);
    accessor.mobLife$setLastDamageStamp(proxy.level().getGameTime());
    if (source.getEntity() instanceof LivingEntity attacker) {
      proxy.setLastHurtByMob(attacker);
    }
  }

  private void applyHungryForagePath() {
    if (hungryForageTarget == null
        || activity != InstinctActivity.REST && activity != InstinctActivity.WANDER
        || !proxy.getNavigation().isDone()) {
      return;
    }
    proxy
        .getNavigation()
        .moveTo(
            hungryForageTarget.getX() + 0.5,
            hungryForageTarget.getY(),
            hungryForageTarget.getZ() + 0.5,
            1.0);
  }

  private void applyHungryPreyTarget() {
    if (hungryPreyTarget == null
        || !hungryPreyTarget.isAlive()
        || !proxy.canAttack(hungryPreyTarget)) {
      hungryPreyTarget = null;
      return;
    }
    if (activity != InstinctActivity.PANIC
        && activity != InstinctActivity.AVOID
        && activity != InstinctActivity.SWIM) {
      proxy.setTarget(hungryPreyTarget);
    }
  }

  private boolean hasHungryPreyTarget() {
    return hungryPreyTarget != null
        && hungryPreyTarget.isAlive()
        && isEdibleTarget(hungryPreyTarget);
  }

  private boolean hasHungryForageTarget(ServerPlayer player) {
    if (hungryForageTarget == null) {
      return false;
    }
    var state = player.level().getBlockState(hungryForageTarget);
    boolean available =
        morph == MorphType.SHEEP
                && (state.is(BlockTags.EDIBLE_FOR_SHEEP)
                    || player
                        .level()
                        .getBlockState(hungryForageTarget.below())
                        .is(Blocks.GRASS_BLOCK))
            || morph == MorphType.RABBIT
                && state.getBlock() instanceof CarrotBlock carrot
                && carrot.isMaxAge(state);
    if (!available) {
      hungryForageTarget = null;
    }
    return available;
  }

  private InstinctActivity classifyActivity() {
    for (WrappedGoal wrapped : proxy.getGoalSelector().getAvailableGoals()) {
      if (!wrapped.isRunning()) {
        continue;
      }
      String name = wrapped.getGoal().getClass().getSimpleName();
      if (name.contains("Panic")) {
        return InstinctActivity.PANIC;
      }
      if (name.contains("Breed")) {
        return InstinctActivity.BREED;
      }
      if (name.contains("Tempt")) {
        return InstinctActivity.TEMPT;
      }
      if (name.contains("EatBlock") || name.contains("RaidGarden")) {
        return InstinctActivity.FORAGE;
      }
      if (name.contains("Attack") || name.contains("LeapAt")) {
        return InstinctActivity.HUNT;
      }
      if (name.contains("Float") || name.contains("PowderSnow")) {
        return InstinctActivity.SWIM;
      }
      if (name.contains("Stroll") || name.contains("Walk")) {
        return InstinctActivity.WANDER;
      }
      if (name.contains("Avoid")) {
        return InstinctActivity.AVOID;
      }
    }
    return proxy.getNavigation().isDone() ? InstinctActivity.REST : InstinctActivity.WANDER;
  }

  record Output(
      Vec3 displacement,
      float horizontalSpeed,
      float bodyYaw,
      float headYaw,
      float headPitch,
      boolean onGround,
      boolean lookingAtTarget,
      InstinctActivity activity) {}

  /** Native Rabbit avoids Wolf entities; this bridge preserves that relation for player forms. */
  private static final class TransformedWolfAvoidGoal extends Goal {
    private static final double RANGE = 10.0;
    private final Rabbit rabbit;
    private final ServerPlayer owner;
    private ServerPlayer threat;
    private Path path;

    TransformedWolfAvoidGoal(Rabbit rabbit, ServerPlayer owner) {
      this.rabbit = rabbit;
      this.owner = owner;
      setFlags(java.util.EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
      threat =
          owner
              .level()
              .getEntitiesOfClass(
                  ServerPlayer.class,
                  rabbit.getBoundingBox().inflate(RANGE, 4.0, RANGE),
                  player ->
                      player != owner
                          && player.isAlive()
                          && !player.isSpectator()
                          && cc.attodao.mob_life.gameplay.targeting.MorphRelations.morphOf(player)
                              == MorphType.WOLF)
              .stream()
              .min(Comparator.comparingDouble(rabbit::distanceToSqr))
              .orElse(null);
      return threat != null && createPathAway();
    }

    @Override
    public boolean canContinueToUse() {
      return threat != null
          && threat.isAlive()
          && rabbit.distanceToSqr(threat) <= RANGE * RANGE
          && !rabbit.getNavigation().isDone();
    }

    @Override
    public void start() {
      rabbit.getNavigation().moveTo(path, 2.2);
    }

    @Override
    public void stop() {
      threat = null;
      path = null;
    }

    private boolean createPathAway() {
      Vec3 destination = DefaultRandomPos.getPosAway(rabbit, 16, 7, threat.position());
      if (destination == null
          || threat.distanceToSqr(destination) <= threat.distanceToSqr(rabbit)) {
        return false;
      }
      path = rabbit.getNavigation().createPath(destination.x, destination.y, destination.z, 0);
      return path != null;
    }
  }
}
