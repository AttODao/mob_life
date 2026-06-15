package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerFovMixin {
  private static final float MOVEMENT_FOV_STRENGTH = 0.1F;

  @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
  private void mobLife$reduceMorphMovementFov(
      boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
    if (ClientMorphState.morph() == null || effectScale <= 0.0F || cir.getReturnValueF() <= 0.1F) {
      return;
    }

    AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
    float walkingSpeed = player.getAbilities().getWalkingSpeed();
    if (walkingSpeed == 0.0F) {
      return;
    }

    float movementModifier =
        ((float) player.getAttributeValue(Attributes.MOVEMENT_SPEED) / walkingSpeed + 1.0F) * 0.5F;
    if (movementModifier <= 0.0F) {
      return;
    }

    float rawModifier = 1.0F + (cir.getReturnValueF() - 1.0F) / effectScale;
    float nonMovementModifier = rawModifier / movementModifier;
    float reducedMovementModifier = Mth.lerp(MOVEMENT_FOV_STRENGTH, 1.0F, movementModifier);
    cir.setReturnValue(Mth.lerp(effectScale, 1.0F, nonMovementModifier * reducedMovementModifier));
  }
}
