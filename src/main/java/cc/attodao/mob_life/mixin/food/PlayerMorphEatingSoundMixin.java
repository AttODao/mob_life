package cc.attodao.mob_life.mixin.food;

import cc.attodao.mob_life.gameplay.food.MorphEatingSound;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMorphEatingSoundMixin {
  @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
  private void mobLife$suppressMorphFoodBroadcast(
      SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
    if ((Object) this instanceof ServerPlayer player
        && MorphEatingSound.isBroadcastSuppressed(player)) {
      ci.cancel();
    }
  }
}
