package cc.attodao.mob_life.mixin.gameplay;

import cc.attodao.mob_life.gameplay.movement.MorphMovementSpeed;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMorphSprintMixin {
  @Inject(method = "setSprinting", at = @At("TAIL"))
  private void mobLife$refreshMorphSprintSpeed(boolean sprinting, CallbackInfo ci) {
    if ((Object) this instanceof Player player) {
      MorphMovementSpeed.refresh(player);
    }
  }
}
