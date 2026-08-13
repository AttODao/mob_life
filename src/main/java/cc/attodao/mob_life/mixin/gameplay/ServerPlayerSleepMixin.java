package cc.attodao.mob_life.mixin.gameplay;

import cc.attodao.mob_life.gameplay.sleep.MorphSleep;
import cc.attodao.mob_life.server.ServerMorphManager;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerSleepMixin {
  @Inject(method = "stopSleepInBed", at = @At("TAIL"))
  private void mobLife$completeBedlessSleep(
      boolean wakeImmediately, boolean updateSleepingPlayers, CallbackInfo ci) {
    ServerMorphManager.completeBedlessSleep((ServerPlayer) (Object) this);
  }

  @Inject(
      method = "startSleepInBed",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z",
              shift = At.Shift.BEFORE),
      cancellable = true)
  private void mobLife$forbidNocturnalNightBedSleep(
      BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
    ServerPlayer player = (ServerPlayer) (Object) this;
    if (ServerMorphManager.hasMobForm()
        && ServerMorphManager.activeMorph().isNocturnal()
        && !MorphSleep.isDaytime(player)) {
      cir.setReturnValue(Either.left(MorphSleep.nocturnalBedProblem()));
    }
  }

  @Redirect(
      method = "startSleepInBed",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z"))
  private boolean mobLife$allowNocturnalDayBedSleep(BedRule bedRule, Level level) {
    ServerPlayer player = (ServerPlayer) (Object) this;
    if (ServerMorphManager.hasMobForm() && ServerMorphManager.activeMorph().isNocturnal()) {
      return MorphSleep.isDaytime(player);
    }
    return bedRule.canSleep(level);
  }
}
