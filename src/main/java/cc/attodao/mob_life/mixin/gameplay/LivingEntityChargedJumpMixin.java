package cc.attodao.mob_life.mixin.gameplay;

import cc.attodao.mob_life.gameplay.jump.ChargedJumpingPlayer;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityChargedJumpMixin implements ChargedJumpingPlayer {
  @Shadow
  protected abstract float getJumpPower(float multiplier);

  @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
  private void mobLife$preventRepeatedMobJump(CallbackInfo ci) {
    if ((Object) this instanceof ServerPlayer && ServerMorphManager.hasMobForm()) {
      ci.cancel();
    }
  }

  @Override
  public boolean mobLife$performMorphJump(float scale, float forwardBoost, boolean forwardInput) {
    LivingEntity entity = (LivingEntity) (Object) this;
    float jumpPower = getJumpPower(scale);
    if (jumpPower <= 1.0E-5F) {
      return false;
    }

    Vec3 movement = entity.getDeltaMovement();
    entity.setDeltaMovement(movement.x, Math.max(jumpPower, movement.y), movement.z);
    if (forwardInput && forwardBoost > 0.0F) {
      float angle = entity.getYRot() * (float) (Math.PI / 180.0);
      entity.addDeltaMovement(
          new Vec3(-Mth.sin(angle) * forwardBoost, 0.0, Mth.cos(angle) * forwardBoost));
    }
    return true;
  }
}
