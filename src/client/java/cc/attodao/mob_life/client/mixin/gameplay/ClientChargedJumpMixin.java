package cc.attodao.mob_life.client.mixin.gameplay;

import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ClientChargedJumpMixin {
  @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
  private void mobLife$preventRepeatedMobJump(CallbackInfo ci) {
    if ((Object) this instanceof Player player
        && ClientMorphState.morph() != null
        && !player.isInLava()) {
      ci.cancel();
    }
  }
}
