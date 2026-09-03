package cc.attodao.mob_life.mixin.instinct;

import cc.attodao.mob_life.gameplay.instinct.InstinctAiContext;
import cc.attodao.mob_life.gameplay.instinct.InstinctProfiles;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsInstinctMixin {
  @Inject(method = "test", at = @At("HEAD"), cancellable = true)
  private void mobLife$ignoreBiologicallyTransformedPlayers(
      ServerLevel level,
      @Nullable LivingEntity targeter,
      LivingEntity target,
      CallbackInfoReturnable<Boolean> cir) {
    if (target instanceof Player player && InstinctAiContext.ignoresPlayer(player)) {
      cir.setReturnValue(false);
      return;
    }
    MorphType morph = InstinctAiContext.morph();
    if (morph != null
        && InstinctProfiles.isNaturalPrey(morph, target)
        && !InstinctProfiles.isEnabledPrey(target)) {
      cir.setReturnValue(false);
    }
  }
}
