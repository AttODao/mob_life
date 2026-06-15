package cc.attodao.mob_life.mixin.sound;

import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMorphSoundMixin {
  @Inject(method = "playHurtSound", at = @At("HEAD"), cancellable = true)
  private void mobLife$playMorphHurtSound(DamageSource source, CallbackInfo ci) {
    if ((Object) this instanceof ServerPlayer player
        && ServerMorphManager.playMorphHurtSound(player, source)) {
      ci.cancel();
    }
  }
}
