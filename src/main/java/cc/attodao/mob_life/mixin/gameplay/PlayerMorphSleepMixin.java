package cc.attodao.mob_life.mixin.gameplay;

import cc.attodao.mob_life.gameplay.sleep.MorphSleep;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMorphSleepMixin {
  @ModifyConstant(method = "tick", constant = @Constant(intValue = Player.SLEEP_DURATION))
  private int mobLife$extendSoftSurfaceSleepTimer(int duration) {
    Player player = (Player) (Object) this;
    return MorphSleep.isCustomSleep(player) ? MorphSleep.requiredSleepTicks() : duration;
  }

  @Redirect(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z"))
  private boolean mobLife$preserveNocturnalAndSoftSleep(BedRule bedRule, Level level) {
    Player player = (Player) (Object) this;
    if (!ServerMorphManager.hasMobForm()) {
      return bedRule.canSleep(level);
    }
    if (MorphSleep.isCustomSleep(player)) {
      return true;
    }
    if (ServerMorphManager.activeMorph().isNocturnal()) {
      return MorphSleep.isDaytime(player);
    }
    return bedRule.canSleep(level);
  }

  @Inject(method = "isSleepingLongEnough", at = @At("HEAD"), cancellable = true)
  private void mobLife$requireLongerSoftSurfaceSleep(CallbackInfoReturnable<Boolean> cir) {
    Player player = (Player) (Object) this;
    if (MorphSleep.isCustomSleep(player)) {
      cir.setReturnValue(
          player.isSleeping() && player.getSleepTimer() >= MorphSleep.requiredSleepTicks());
    }
  }
}
