package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.mixin.instinct.EntityFluidInteractionInvoker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Keeps the hidden vanilla mob synchronized with the controlling player's physical state. */
final class InstinctShadowMob {

  private InstinctShadowMob() {}

  static void initialize(PathfinderMob shadow) {
    shadow.setInvulnerable(true);
    shadow.setSilent(true);
    shadow.setNoGravity(false);
    shadow.setCanPickUpLoot(false);
    shadow.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    shadow.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
  }

  static void prepare(
      PathfinderMob shadow,
      ServerPlayer player,
      float bodyYaw,
      boolean synchronizeHorizontalMovement) {
    // HurtByTargetGoal compares the damage timestamp with its zero-initialized goal state.
    if (shadow.tickCount == 0) {
      shadow.tickCount = Math.max(1, player.tickCount);
    }
    Vec3 nativeMovement = shadow.getDeltaMovement();
    Vec3 playerMovement = player.getDeltaMovement();
    shadow.setNoActionTime(0);
    shadow.setHealth(shadow.getMaxHealth());
    shadow.snapTo(player.position(), bodyYaw, 0.0F);
    shadow.setYHeadRot(bodyYaw);
    shadow.setYBodyRot(bodyYaw);
    shadow.setOnGround(player.onGround());
    // Keep native horizontal momentum out of the client/server player correction loop.
    shadow.setDeltaMovement(
        synchronizeHorizontalMovement ? playerMovement.x : nativeMovement.x,
        playerMovement.y,
        synchronizeHorizontalMovement ? playerMovement.z : nativeMovement.z);
    shadow.setRemainingFireTicks(player.getRemainingFireTicks());
    ((EntityFluidInteractionInvoker) shadow).mobLife$invokeUpdateFluidInteraction();
  }
}
