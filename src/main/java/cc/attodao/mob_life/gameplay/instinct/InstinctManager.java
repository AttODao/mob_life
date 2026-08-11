package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class InstinctManager {
  public static final int INTERVENE_FORWARD = 1;
  public static final int INTERVENE_JUMP = 1 << 1;
  public static final int INTERVENE_VIEW = 1 << 2;

  private static final Map<UUID, InstinctController> CONTROLLERS = new HashMap<>();
  private static final Map<Mob, InstinctController> SHADOW_CONTROLLERS = new IdentityHashMap<>();
  private static final ThreadLocal<AttackContext> ATTACK_CONTEXT = new ThreadLocal<>();

  private InstinctManager() {}

  public static boolean enable(ServerPlayer player) {
    if (isEnabled(player)) {
      return false;
    }

    MorphDefinition definition = ServerMorphManager.activeDefinition();
    if (definition == null || !definition.hasMobForm()) {
      return false;
    }
    MorphConfig config = MorphConfigManager.get(definition.type());
    if (!config.instinct().enabled()) {
      return false;
    }
    InstinctController controller = InstinctController.create(player, definition, config);
    if (controller == null) {
      return false;
    }

    CONTROLLERS.put(player.getUUID(), controller);
    SHADOW_CONTROLLERS.put(controller.shadow(), controller);
    player.stopUsingItem();
    player.setSprinting(false);
    player.setShiftKeyDown(false);
    player.closeContainer();
    ((InstinctPersistenceHolder) player).mobLife$setRestoreInstinct(true);
    sync(controller);
    return true;
  }

  public static boolean shouldRestore(ServerPlayer player) {
    return ((InstinctPersistenceHolder) player).mobLife$shouldRestoreInstinct();
  }

  public static boolean requestExit(ServerPlayer player) {
    InstinctController controller = CONTROLLERS.get(player.getUUID());
    if (controller == null || !controller.allowsExit() || MorphAwkwardness.isMaximum(player)) {
      return false;
    }
    ((InstinctPersistenceHolder) player).mobLife$setRestoreInstinct(false);
    disable(player);
    return true;
  }

  public static void forceEnableAtMaximum(ServerPlayer player) {
    if (!isEnabled(player) && MorphAwkwardness.isMaximum(player)) {
      enable(player);
    }
  }

  public static void forget(ServerPlayer player) {
    ((InstinctPersistenceHolder) player).mobLife$setRestoreInstinct(false);
  }

  public static void tick(ServerPlayer player) {
    InstinctController controller = CONTROLLERS.get(player.getUUID());
    if (controller == null) {
      return;
    }
    if (ServerMorphManager.activeDefinition() == null
        || !controller.player().isAlive()
        || controller.shadow().level() != player.level()) {
      disable(player);
      return;
    }

    player.stopUsingItem();
    player.setSprinting(false);
    player.setShiftKeyDown(false);
    if (player.containerMenu != player.inventoryMenu) {
      player.closeContainer();
    }
    controller.tick();
    sync(controller);
  }

  public static void intervene(ServerPlayer player, int flags) {
    InstinctController controller = CONTROLLERS.get(player.getUUID());
    if (controller == null) {
      return;
    }
    controller.intervene(flags);
  }

  public static boolean isEnabled(Player player) {
    return player instanceof ServerPlayer serverPlayer
        && CONTROLLERS.containsKey(serverPlayer.getUUID());
  }

  public static boolean isControllingPlayer(Mob mob, LivingEntity target) {
    InstinctController controller = SHADOW_CONTROLLERS.get(mob);
    return controller != null && controller.player() == target;
  }

  public static boolean isShadow(Mob mob) {
    return SHADOW_CONTROLLERS.containsKey(mob);
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
    remove(player);
    ServerPlayNetworking.send(
        player,
        new MobLifeNetworking.InstinctControlPayload(
            false,
            InstinctState.REST.ordinal(),
            player.getYRot(),
            player.getXRot(),
            0,
            0.0F,
            0.0F,
            0.0F));
  }

  public static void remove(ServerPlayer player) {
    InstinctController controller = CONTROLLERS.remove(player.getUUID());
    if (controller != null) {
      SHADOW_CONTROLLERS.remove(controller.shadow());
    }
  }

  public static void clear() {
    CONTROLLERS.clear();
    SHADOW_CONTROLLERS.clear();
    ATTACK_CONTEXT.remove();
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
            (float) control.nativeMovement().x,
            (float) control.nativeMovement().y,
            (float) control.nativeMovement().z));
  }

  private record AttackContext(ServerPlayer attacker, LivingEntity target, boolean suppressDrops) {}
}
