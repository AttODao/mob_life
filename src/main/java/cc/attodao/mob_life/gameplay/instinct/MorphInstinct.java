package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.ability.MorphAbility;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.gameplay.movement.MorphBodyYawSync;
import cc.attodao.mob_life.gameplay.targeting.MorphRelations;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

public final class MorphInstinct {
  private static final int CAMERA_ACTIVITY_WINDOW = 5;
  private static final int CALM_GRACE_TICKS = 5;
  private static final int FEEDING_TICKS = 40;
  private static final int EGG_MIN_TICKS = 6000;
  private static final int EGG_RANDOM_TICKS = 6001;
  private static final Map<UUID, Session> SESSIONS = new HashMap<>();

  private MorphInstinct() {}

  public static boolean isActive(net.minecraft.world.entity.player.Player player) {
    return InstinctState.isActive(player);
  }

  public static boolean blocksActions(net.minecraft.world.entity.player.Player player) {
    return isActive(player);
  }

  public static boolean enter(ServerPlayer player, MorphDefinition definition) {
    if (!InstinctState.enter(player, definition.type())) {
      return false;
    }
    Session session = session(player);
    if (session.controller == null
        || session.morph != definition.type()
        || session.controller.proxy().level() != player.level()) {
      session.resetController(definition, player);
    }
    session.previousButtons = session.input.buttons();
    session.releaseGate = session.input.buttons();
    session.lastInterferenceTick = player.level().getGameTime();
    session.movementInterference = false;
    session.resetCameraTracking();
    player.stopUsingItem();
    if (player.containerMenu != player.inventoryMenu) {
      player.closeContainer();
    }
    sync(player, session, InstinctActivity.REST);
    return true;
  }

  public static void exit(ServerPlayer player) {
    InstinctState.exit(player);
    Session session = session(player);
    session.releaseGate = session.input.buttons();
    session.controller = null;
    session.cancelFeeding();
    session.pendingDamage = null;
    session.pendingPanic = false;
    sync(player, session, InstinctActivity.REST);
  }

  public static void receiveInput(ServerPlayer player, InstinctInput input) {
    Session session = session(player);
    session.input = input;
    session.lastInputTick = player.level().getGameTime();
  }

  public static void onDamage(
      ServerPlayer player, net.minecraft.world.damagesource.DamageSource source) {
    if (!player.isAlive() || player.isSpectator()) {
      return;
    }
    Session session = session(player);
    InstinctStateData state = InstinctState.get(player);
    state.setLoveTicks(0);
    session.cancelFeeding();
    boolean panic = InstinctProfiles.panicsFromDamage(MorphRelations.morphOf(player), source);
    if (isActive(player) || panic && !player.isCreative()) {
      session.pendingDamage = source;
      session.pendingPanic = panic;
    }
  }

  public static InteractionResult tryFeedForLove(
      ServerPlayer actor, ServerPlayer target, InteractionHand hand) {
    MorphType morph = MorphRelations.morphOf(target);
    if (actor == target
        || MorphRelations.morphOf(actor) != null
        || morph == null
        || !target.isAlive()
        || target.isSpectator()) {
      return InteractionResult.PASS;
    }
    MorphDefinition definition = cc.attodao.mob_life.server.ServerMorphManager.activeDefinition();
    if (definition == null || definition.type() != morph) {
      return InteractionResult.PASS;
    }

    Session session = session(target);
    if (session.controller == null
        || session.morph != morph
        || session.controller.proxy().level() != target.level()) {
      session.resetController(definition, target);
    }
    InstinctStateData state = InstinctState.get(target);
    ItemStack stack = actor.getItemInHand(hand);
    if (!session.controller.acceptsBreedingFood(stack, state)) {
      return InteractionResult.PASS;
    }

    stack.consume(1, actor);
    state.setLoveTicks(600);
    if (target.level() instanceof ServerLevel level) {
      level.sendParticles(
          ParticleTypes.HEART,
          target.getX(),
          target.getY() + target.getBbHeight() * 0.75,
          target.getZ(),
          7,
          target.getBbWidth() * 0.25,
          target.getBbHeight() * 0.15,
          target.getBbWidth() * 0.25,
          0.02);
      level.playSound(
          null,
          target.getX(),
          target.getY(),
          target.getZ(),
          net.minecraft.sounds.SoundEvents.GENERIC_EAT,
          SoundSource.PLAYERS,
          1.0F,
          1.0F);
    }
    if (!target.isCreative()) {
      enter(target, definition);
    }
    return InteractionResult.SUCCESS_SERVER;
  }

  public static boolean canBeRiddenByOtherPlayer(ServerPlayer target) {
    MorphType morph = MorphRelations.morphOf(target);
    if (morph == null || !morph.isEquine() || target.isPassenger()) {
      return false;
    }
    Session session = SESSIONS.get(target.getUUID());
    return session != null
        && session.controller != null
        && session.controller.canBeRiddenByOtherPlayer();
  }

  public static void onPreyKilled(
      ServerPlayer player, LivingEntity prey, InstinctPreyManager.FoodValue foodValue) {
    if (!isActive(player) || prey instanceof net.minecraft.world.entity.player.Player) {
      return;
    }
    Session session = session(player);
    session.feedingTicks = FEEDING_TICKS;
    session.feedingOrigin = player.position();
    session.feedingPoint = prey.position();
    session.feedingFood = foodValue;
  }

  public static void onForageConsumed(ServerPlayer player) {
    if (!isActive(player) || !player.level().getGameRules().get(GameRules.MOB_GRIEFING)) {
      return;
    }
    Session session = session(player);
    long gameTime = player.level().getGameTime();
    if (session.lastForageRewardTick == gameTime) {
      return;
    }
    MorphConfig.Forage forage = MorphConfigManager.get(session.morph).instinct().forage();
    feed(
        player, new InstinctPreyManager.FoodValue(forage.nutrition(), forage.saturationModifier()));
    if (session.controller != null) {
      session.controller.clearForageTarget();
    }
    session.lastForageRewardTick = gameTime;
  }

  public static void tick(ServerPlayer player, MorphDefinition definition) {
    InstinctStateData state = InstinctState.get(player);
    Session session = session(player);
    MorphType morph = definition.type();

    if (!player.isAlive() || player.isSpectator() || !InstinctState.isSupported(morph)) {
      if (state.active()) {
        exit(player);
      }
      return;
    }
    if (player.isCreative()) {
      MorphAwkwardness.set(player, MorphAwkwardness.MINIMUM);
    }

    if (session.controller == null
        || session.morph != morph
        || session.controller.proxy().level() != player.level()) {
      session.resetController(definition, player);
    }
    tickEggClock(player, state, morph, session.controller.canLayEgg());
    if (!state.active()) {
      if (state.breedingCooldown() > 0) {
        state.setBreedingCooldown(state.breedingCooldown() - 1);
      }
      tickEntry(player, definition, state, session);
      return;
    }

    tickActive(player, definition, state, session);
  }

  public static void removeRuntime(ServerPlayer player) {
    SESSIONS.remove(player.getUUID());
  }

  public static void onMorphChanged(ServerPlayer player) {
    InstinctState.get(player).clearForMorphChange();
    SESSIONS.remove(player.getUUID());
  }

  public static void onReload(ServerPlayer player, MorphDefinition definition) {
    Session old = SESSIONS.remove(player.getUUID());
    if (!InstinctState.isSupported(definition.type())) {
      InstinctState.exit(player);
    }
    Session replacement = session(player);
    if (old != null) {
      replacement.input = old.input;
      replacement.previousButtons = old.previousButtons;
      replacement.releaseGate = old.releaseGate;
    }
    replacement.lastInterferenceTick = player.level().getGameTime();
    sync(player, replacement, InstinctActivity.REST);
  }

  public static void clear() {
    SESSIONS.clear();
  }

  private static void tickEntry(
      ServerPlayer player, MorphDefinition definition, InstinctStateData state, Session session) {
    InstinctInput input = currentInput(player, session);
    boolean freeze = input.screenMode() == InstinctInput.SCREEN_SAFE;
    if (freeze) {
      session.resetCameraTracking();
    }
    boolean camera = !freeze && cameraActivity(input, session);
    boolean activity =
        !freeze && (session.hasInactivityMovement(input) || input.buttons() != 0 || camera);
    if (freeze) {
      // Safe UI screens pause the inactivity timer without resetting it.
    } else if (input.screenMode() == InstinctInput.SCREEN_GAMEPLAY || activity) {
      state.setIdleTicks(0);
    } else {
      state.setIdleTicks(state.idleTicks() + 1);
    }

    if (player.isCreative()) {
      if (state.loveTicks() > 0) {
        state.setLoveTicks(state.loveTicks() - 1);
      }
      session.previousButtons = input.buttons();
      return;
    }

    float awkwardness = MorphAwkwardness.get(player);
    int idleLimit = Math.max(0, Math.round(200.0F * (1.0F - awkwardness / 100.0F)));
    boolean forced = awkwardness >= 100.0F || session.pendingPanic || state.loveTicks() > 0;
    InstinctController probe = session.controller;
    if (probe == null
        || session.morph != definition.type()
        || probe.proxy().level() != player.level()) {
      session.resetController(definition, player);
      probe = session.controller;
    }
    if (!forced && MorphFoodCapacity.isCriticallyHungry(player)) {
      forced = probe.hasReachableHungryTarget(player);
      if (!forced
          && player.level().getGameRules().get(GameRules.MOB_GRIEFING)
          && (definition.type() == MorphType.SHEEP || definition.type() == MorphType.RABBIT)) {
        forced = probe.hasReachableForage(player);
      }
    }
    if (!forced) {
      forced = probe.hasAvoidThreat(player, awkwardness);
    }
    if (forced || state.idleTicks() >= idleLimit) {
      enter(player, definition);
    }
    session.previousButtons = input.buttons();
  }

  private static void tickActive(
      ServerPlayer player, MorphDefinition definition, InstinctStateData state, Session session) {
    InstinctInput rawInput = controllingInput(player, definition.type(), session);
    InstinctInput input = rawInput;
    int released = session.releaseGate & ~input.buttons();
    session.releaseGate &= ~released;
    int acceptedButtons = input.buttons() & ~session.releaseGate;
    input =
        new InstinctInput(
            input.sideways(),
            input.forward(),
            input.cameraYaw(),
            input.cameraPitch(),
            input.cameraDelta(),
            acceptedButtons,
            input.screenMode());

    state.setLevel(state.level() + 1.0F);
    boolean resistanceAllowed =
        session.feedingTicks == 0
            && session.pendingDamage == null
            && state.loveTicks() == 0
            && !player.isPassenger()
            && (session.controller.activity() == InstinctActivity.REST
                || session.controller.activity() == InstinctActivity.WANDER);
    boolean actionResistance = resistanceAllowed && input.actionEdge(session.previousButtons);
    if (actionResistance) {
      float reduction = 10.0F * (1.0F - MorphAwkwardness.get(player) / 100.0F);
      state.setLevel(state.level() - reduction);
    }

    boolean camera = cameraActivity(input, session);
    boolean interference =
        session.hasMovementInterference(input)
            || rawInput.buttons() != 0
            || actionResistance
            || camera;
    if (interference) {
      session.lastInterferenceTick = player.level().getGameTime();
    } else if (player.level().getGameTime() - session.lastInterferenceTick > CALM_GRACE_TICKS) {
      float decayPerSecond = 2.0F - 1.5F * inventoryFillRatio(player);
      MorphAwkwardness.add(player, -decayPerSecond / 20.0F);
    }

    if (state.level() <= 0.0F) {
      exit(player);
      return;
    }

    if (session.feedingTicks > 0
        && (session.pendingDamage != null || session.controller.hasAvoidThreat(player, 100.0F))) {
      session.cancelFeeding();
    }

    if (session.feedingTicks > 0) {
      tickFeeding(player, session);
      sync(player, session, InstinctActivity.FEED, true);
    } else if (player.isPassenger()) {
      player.setDeltaMovement(Vec3.ZERO);
      sync(player, session, InstinctActivity.REST);
    } else {
      tryAutomaticVehicleMount(player);
      if (player.isPassenger()) {
        player.setDeltaMovement(Vec3.ZERO);
        sync(player, session, InstinctActivity.REST);
      } else {
        InstinctController.Output output =
            session.controller.tick(
                player,
                input,
                MorphFoodCapacity.isCriticallyHungry(player),
                state,
                session.pendingDamage);
        session.pendingDamage = null;
        session.pendingPanic = false;
        Vec3 target = player.position().add(output.displacement());
        player.setPos(target.x, target.y, target.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(output.onGround());
        player.yBodyRot = output.bodyYaw();
        player.yBodyRotO = output.bodyYaw();
        MorphBodyYawSync.receive(player, output.bodyYaw());
        player.setYHeadRot(output.headYaw());
        player.yHeadRotO = output.headYaw();
        session.headPitch = output.headPitch();
        sync(
            player,
            session,
            output.activity(),
            output.lookingAtTarget(),
            (float) output.displacement().horizontalDistance(),
            output.horizontalSpeed());
      }
    }

    tickAutomaticEgg(player, state, session.controller.canLayEgg());
    if (player.tickCount % 5 == 0) {
      ServerPlayNetworking.send(
          player, new MobLifeNetworking.AwkwardnessPayload(MorphAwkwardness.get(player)));
    }
    session.previousButtons = input.buttons();
  }

  private static void tryAutomaticVehicleMount(ServerPlayer player) {
    if (player.isPassenger()) {
      return;
    }
    for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(0.2))) {
      if ((entity instanceof AbstractBoat || entity instanceof AbstractMinecart)
          && player.startRiding(entity)) {
        return;
      }
    }
  }

  private static void tickFeeding(ServerPlayer player, Session session) {
    if (session.feedingOrigin == null
        || session.feedingFood == null
        || player.isPassenger()
        || player.position().distanceToSqr(session.feedingOrigin) > 4.0) {
      session.cancelFeeding();
      return;
    }
    player.setDeltaMovement(Vec3.ZERO);
    if (session.feedingPoint != null) {
      double dx = session.feedingPoint.x - player.getX();
      double dy = session.feedingPoint.y - (player.getY() + player.getEyeHeight());
      double dz = session.feedingPoint.z - player.getZ();
      float desiredHeadYaw = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
      float headYaw = InstinctAngles.approachYaw(player.getYHeadRot(), desiredHeadYaw, 10.0F);
      player.setYHeadRot(InstinctAngles.clampHeadYawToBody(headYaw, player.yBodyRot, 75.0F));
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float desiredPitch =
          Mth.clamp((float) -(Mth.atan2(dy, horizontal) * 180.0 / Math.PI), -40.0F, 40.0F);
      session.headPitch = Mth.approach(session.headPitch, desiredPitch, 10.0F);
    }
    if (session.feedingTicks % 10 == 0) {
      player.connection.send(
          new ClientboundSoundPacket(
              net.minecraft.sounds.SoundEvents.GENERIC_EAT,
              SoundSource.PLAYERS,
              player.getX(),
              player.getY(),
              player.getZ(),
              0.7F,
              1.0F,
              player.getRandom().nextLong()));
    }
    session.feedingTicks--;
    if (session.feedingTicks == 0) {
      InstinctPreyManager.FoodValue food = session.feedingFood;
      session.cancelFeeding();
      feed(player, food);
    }
  }

  private static void tickAutomaticEgg(
      ServerPlayer player, InstinctStateData state, boolean canLayEgg) {
    if (!canLayEgg) {
      return;
    }
    if (state.eggTimer() > 0) {
      return;
    }
    if (MorphAbility.tryLayAutomaticEgg(player)) {
      state.setEggTimer(EGG_MIN_TICKS + player.getRandom().nextInt(EGG_RANDOM_TICKS));
    }
  }

  private static void tickEggClock(
      ServerPlayer player, InstinctStateData state, MorphType morph, boolean canLayEgg) {
    if (morph != MorphType.CHICKEN || !canLayEgg) {
      return;
    }
    if (!state.eggTimerInitialized()) {
      state.setEggTimer(EGG_MIN_TICKS + player.getRandom().nextInt(EGG_RANDOM_TICKS));
      state.setEggTimerInitialized(true);
    } else if (state.eggTimer() > 0) {
      state.setEggTimer(state.eggTimer() - 1);
    }
  }

  private static void feed(ServerPlayer player, InstinctPreyManager.FoodValue value) {
    FoodData food = player.getFoodData();
    if (food.getFoodLevel() >= MorphFoodCapacity.maxFood(player)) {
      return;
    }
    food.eat(value.nutrition(), value.saturationModifier());
  }

  private static float inventoryFillRatio(ServerPlayer player) {
    int hotbar = MorphInventoryCapacity.hotbarSlots(player);
    int inventory = MorphInventoryCapacity.inventorySlots(player);
    float filled = 0.0F;
    int slots = 0;
    for (int index = 0; index < hotbar; index++) {
      filled += stackFraction(player.getInventory().getItem(index));
      slots++;
    }
    for (int index = 9; index < 9 + inventory; index++) {
      filled += stackFraction(player.getInventory().getItem(index));
      slots++;
    }
    filled += stackFraction(player.getOffhandItem());
    slots++;
    return slots == 0 ? 0.0F : Mth.clamp(filled / slots, 0.0F, 1.0F);
  }

  private static float stackFraction(ItemStack stack) {
    return stack.isEmpty() ? 0.0F : (float) stack.getCount() / stack.getMaxStackSize();
  }

  private static InstinctInput currentInput(ServerPlayer player, Session session) {
    return player.level().getGameTime() - session.lastInputTick <= 5
        ? session.input
        : InstinctInput.EMPTY;
  }

  private static InstinctInput controllingInput(
      ServerPlayer player, MorphType morph, Session ownerSession) {
    ServerPlayer controller = player;
    if (morph.isEquine()
        && !player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.SADDLE).isEmpty()
        && player.getFirstPassenger() instanceof ServerPlayer rider) {
      controller = rider;
    }
    UUID controllerId = controller.getUUID();
    InstinctInput input = currentInput(controller, session(controller));
    if (!controllerId.equals(ownerSession.inputController)) {
      ownerSession.inputController = controllerId;
      ownerSession.previousButtons = input.buttons();
      ownerSession.releaseGate = input.buttons();
      ownerSession.movementInterference = false;
      ownerSession.resetCameraTracking();
    }
    return input;
  }

  private static boolean cameraActivity(InstinctInput input, Session session) {
    session.cameraDeltas[session.cameraIndex] = input.cameraDelta();
    session.cameraIndex = (session.cameraIndex + 1) % CAMERA_ACTIVITY_WINDOW;
    float total = 0.0F;
    for (float value : session.cameraDeltas) {
      total += value;
    }
    return total >= 1.0F;
  }

  private static void sync(ServerPlayer player, Session session, InstinctActivity activity) {
    sync(player, session, activity, false);
  }

  private static void sync(
      ServerPlayer player, Session session, InstinctActivity activity, boolean lookingAtTarget) {
    sync(player, session, activity, lookingAtTarget, 0.0F, 0.0F);
  }

  private static void sync(
      ServerPlayer player,
      Session session,
      InstinctActivity activity,
      boolean lookingAtTarget,
      float horizontalDisplacement,
      float horizontalSpeed) {
    float level = InstinctState.get(player).level();
    ServerPlayNetworking.send(
        player,
        new MobLifeNetworking.InstinctStatePayload(
            InstinctState.isActive(player),
            level,
            player.getX(),
            player.getY(),
            player.getZ(),
            player.onGround(),
            player.yBodyRot,
            player.getYHeadRot(),
            session.headPitch,
            lookingAtTarget,
            activity.ordinal(),
            horizontalDisplacement,
            horizontalSpeed));
  }

  private static Session session(ServerPlayer player) {
    return SESSIONS.computeIfAbsent(player.getUUID(), ignored -> new Session());
  }

  private static final class Session {
    InstinctInput input = InstinctInput.EMPTY;
    long lastInputTick = Long.MIN_VALUE;
    int previousButtons;
    int releaseGate;
    long lastInterferenceTick = Long.MIN_VALUE;
    net.minecraft.world.damagesource.DamageSource pendingDamage;
    boolean pendingPanic;
    MorphType morph;
    InstinctController controller;
    int feedingTicks;
    long lastForageRewardTick = Long.MIN_VALUE;
    Vec3 feedingOrigin;
    Vec3 feedingPoint;
    InstinctPreyManager.FoodValue feedingFood;
    final float[] cameraDeltas = new float[CAMERA_ACTIVITY_WINDOW];
    int cameraIndex;
    float headPitch;
    boolean movementInterference;
    boolean inactivityMovement;
    UUID inputController;

    void resetController(MorphDefinition definition, ServerPlayer player) {
      morph = definition.type();
      controller = new InstinctController(definition, player);
      headPitch = player.getXRot();
      cancelFeeding();
    }

    void cancelFeeding() {
      feedingTicks = 0;
      feedingOrigin = null;
      feedingPoint = null;
      feedingFood = null;
    }

    void resetCameraTracking() {
      cameraIndex = 0;
      java.util.Arrays.fill(cameraDeltas, 0.0F);
    }

    boolean hasMovementInterference(InstinctInput current) {
      float magnitude = Math.max(Math.abs(current.sideways()), Math.abs(current.forward()));
      movementInterference = movementInterference ? magnitude > 0.1F : magnitude >= 0.2F;
      return movementInterference;
    }

    boolean hasInactivityMovement(InstinctInput current) {
      float magnitude = Math.max(Math.abs(current.sideways()), Math.abs(current.forward()));
      inactivityMovement = inactivityMovement ? magnitude > 0.1F : magnitude >= 0.2F;
      return inactivityMovement;
    }
  }
}
