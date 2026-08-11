package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityInstinctCollisionMixin {
  @Inject(method = "move", at = @At("HEAD"))
  private void mobLife$captureShadowMovement(MoverType moverType, Vec3 movement, CallbackInfo ci) {
    if (moverType == MoverType.SELF && (Object) this instanceof Mob mob) {
      InstinctManager.captureShadowMovement(mob, movement);
    }
  }

  @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
  private void mobLife$ignoreControllingPlayer(Entity other, CallbackInfoReturnable<Boolean> cir) {
    if ((Object) this instanceof Mob mob
        && other instanceof LivingEntity living
        && InstinctManager.isControllingPlayer(mob, living)) {
      cir.setReturnValue(false);
      return;
    }
    if ((Object) this instanceof LivingEntity living
        && other instanceof Mob mob
        && InstinctManager.isControllingPlayer(mob, living)) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "canSimulateMovement", at = @At("HEAD"), cancellable = true)
  private void mobLife$simulateShadowMovement(CallbackInfoReturnable<Boolean> cir) {
    if ((Object) this instanceof Mob mob && InstinctManager.isShadow(mob)) {
      cir.setReturnValue(true);
    }
  }
}
