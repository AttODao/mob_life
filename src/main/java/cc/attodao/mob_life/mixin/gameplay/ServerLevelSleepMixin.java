package cc.attodao.mob_life.mixin.gameplay;

import cc.attodao.mob_life.gameplay.sleep.MorphSleep;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ClockTimeMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerLevel.class)
public abstract class ServerLevelSleepMixin {
  @ModifyArg(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/clock/ServerClockManager;moveToTimeMarker(Lnet/minecraft/core/Holder;Lnet/minecraft/resources/ResourceKey;)Z"),
      index = 1)
  private ResourceKey<ClockTimeMarker> mobLife$skipDayForNocturnalSleep(
      ResourceKey<ClockTimeMarker> original) {
    return MorphSleep.wakeUpMarker(original);
  }
}
