package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow
	@Nullable
	private Identifier postEffectId;

	@Shadow
	private boolean effectActive;

	@Inject(method = "checkEntityPostEffect", at = @At("HEAD"), cancellable = true)
	private void mobLife$selectMorphVision(@Nullable Entity cameraEntity, CallbackInfo ci) {
		MorphType morph = ClientMorphState.morph();
		if (morph == null) {
			return;
		}

		postEffectId = Identifier.fromNamespaceAndPath(MobLife.MOD_ID, morph.id() + "_vision");
		effectActive = true;
		ci.cancel();
	}
}
