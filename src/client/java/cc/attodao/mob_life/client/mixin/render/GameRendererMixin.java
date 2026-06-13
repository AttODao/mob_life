package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.client.state.ClientVisionPass;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.client.state.ClientMorphState;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
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

	@Shadow
	private Minecraft minecraft;

	@Shadow
	private CrossFrameResourcePool resourcePool;

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

	@Inject(
			method = "renderLevel",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V"
			)
	)
	private void mobLife$applyDistanceVisionBeforeDepthClear(
			net.minecraft.client.DeltaTracker deltaTracker,
			CallbackInfo ci
	) {
		if (
				ClientMorphState.morph() == null
						|| postEffectId == null
						|| !effectActive
		) {
			return;
		}

		PostChain chain = minecraft.getShaderManager().getPostChain(
				postEffectId,
				LevelTargetBundle.MAIN_TARGETS
		);
		if (chain == null) {
			return;
		}

		ClientVisionPass.setDistancePass(true);
		try {
			chain.process(minecraft.getMainRenderTarget(), resourcePool);
		} finally {
			ClientVisionPass.setDistancePass(false);
		}
	}
}
