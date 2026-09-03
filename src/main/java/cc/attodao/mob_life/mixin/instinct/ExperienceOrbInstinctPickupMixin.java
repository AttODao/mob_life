package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbInstinctPickupMixin {
  @Shadow private @Nullable Player followingPlayer;

  @Inject(method = "followNearbyPlayer", at = @At("HEAD"))
  private void mobLife$stopFollowingInstinctPlayer(CallbackInfo ci) {
    if (followingPlayer != null && MorphInstinct.blocksActions(followingPlayer)) {
      followingPlayer = null;
    }
  }

  @Redirect(
      method = "followNearbyPlayer",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/level/Level;getNearestPlayer(Lnet/minecraft/world/entity/Entity;D)Lnet/minecraft/world/entity/player/Player;"))
  private Player mobLife$findNearestNonInstinctPlayer(
      Level level, Entity source, double maxDistance) {
    return level.getNearestPlayer(
        source.getX(),
        source.getY(),
        source.getZ(),
        maxDistance,
        entity ->
            entity instanceof Player player
                && !player.isSpectator()
                && !MorphInstinct.blocksActions(player));
  }

  @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
  private void mobLife$disableInstinctPickup(Player player, CallbackInfo ci) {
    if (MorphInstinct.blocksActions(player)) {
      ci.cancel();
    }
  }
}
