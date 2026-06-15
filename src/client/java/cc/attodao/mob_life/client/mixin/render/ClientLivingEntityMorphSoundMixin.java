package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.mixin.sound.LivingEntitySoundAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class ClientLivingEntityMorphSoundMixin {
  @Redirect(
      method = "handleDamageEvent",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/entity/LivingEntity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
  private void mobLife$playMorphHurtSound(
      LivingEntity entity,
      SoundEvent originalSound,
      float originalVolume,
      float originalPitch,
      DamageSource source) {
    if (entity instanceof Player player && ClientMorphState.morph() != null) {
      Entity template = ClientMorphState.renderEntity(player);
      if (template instanceof LivingEntity livingTemplate) {
        if (livingTemplate.isSilent()) {
          return;
        }
        LivingEntitySoundAccessor sounds = (LivingEntitySoundAccessor) livingTemplate;
        SoundEvent sound = sounds.mobLife$getHurtSound(source);
        if (sound != null) {
          entity.playSound(sound, sounds.mobLife$getSoundVolume(), livingTemplate.getVoicePitch());
        }
        return;
      }
    }
    entity.playSound(originalSound, originalVolume, originalPitch);
  }
}
