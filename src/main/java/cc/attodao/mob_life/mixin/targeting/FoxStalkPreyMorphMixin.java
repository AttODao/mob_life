package cc.attodao.mob_life.mixin.targeting;

import cc.attodao.mob_life.gameplay.targeting.MorphPredation;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.fox.Fox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets Fox's native stalk and pounce goals recognize a player transformed into a rabbit. */
@Mixin(targets = "net.minecraft.world.entity.animal.fox.Fox$StalkPreyGoal")
public abstract class FoxStalkPreyMorphMixin {
  @Shadow @Final private Fox this$0;

  @Redirect(
      method = "canUse",
      at =
          @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
  private boolean mobLife$stalkRabbitMorph(Predicate<Object> predicate, Object target) {
    return predicate.test(target)
        || target instanceof ServerPlayer player && MorphPredation.isEligibleTarget(this$0, player);
  }
}
