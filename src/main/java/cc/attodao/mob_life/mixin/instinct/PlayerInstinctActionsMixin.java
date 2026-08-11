package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerInstinctActionsMixin {
  @Inject(method = "travel", at = @At("HEAD"))
  private void mobLife$applyInstinctMovement(Vec3 input, CallbackInfo ci) {
    if ((Object) this instanceof ServerPlayer player && InstinctManager.isEnabled(player)) {
      player.setDeltaMovement(InstinctManager.nativeMovement(player));
    }
  }

  @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
  private void mobLife$preventInstinctAttack(Entity target, CallbackInfo ci) {
    if ((Object) this instanceof ServerPlayer player && InstinctManager.isEnabled(player)) {
      ci.cancel();
    }
  }

  @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
  private void mobLife$preventInstinctEntityUse(
      Entity target,
      InteractionHand hand,
      Vec3 location,
      CallbackInfoReturnable<InteractionResult> cir) {
    if ((Object) this instanceof ServerPlayer player && InstinctManager.isEnabled(player)) {
      cir.setReturnValue(InteractionResult.FAIL);
    }
  }
}
