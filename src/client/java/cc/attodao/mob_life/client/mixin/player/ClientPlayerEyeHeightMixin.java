package cc.attodao.mob_life.client.mixin.player;

import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ClientPlayerEyeHeightMixin {
	@Inject(method = "getEyeHeight()F", at = @At("HEAD"), cancellable = true)
	private void mobLife$useMorphEyeHeight(CallbackInfoReturnable<Float> cir) {
		if ((Object) this != Minecraft.getInstance().player) {
			return;
		}

		EntityDimensions dimensions = ClientMorphState.dimensions();
		if (dimensions != null) {
			cir.setReturnValue(ClientMorphState.eyeHeight());
		}
	}
}
