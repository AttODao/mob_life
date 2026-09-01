package cc.attodao.mob_life.mixin.gameplay;

import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMorphFallMixin {
  @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
  private void mobLife$applyMorphFallDamageImmunity(
      double fallDistance,
      float damageModifier,
      DamageSource damageSource,
      CallbackInfoReturnable<Boolean> cir) {
    if ((Object) this instanceof ServerPlayer player
        && ServerMorphManager.activeMorphFallsImmune()) {
      player.resetFallDistance();
      cir.setReturnValue(false);
    }
  }
}
