package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.client.state.ClientVisionPass;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostPass;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(PostPass.class)
public abstract class PostPassDistanceUniformMixin {
	private static final int DISTANCE_BLUR_UBO_SIZE = 48;
	private static final float NEAR_PLANE = 0.05F;
	private static final float EFFECT_START_DISTANCE = 4.0F;
	private static final float FULL_BLUR_DISTANCE = 16.0F;
	private static final float FULL_DARKENING_DISTANCE = 32.0F;
	private static final float FULL_FOG_DISTANCE = 48.0F;
	private static final float MAXIMUM_BLUR_RADIUS = 4.0F;

	@Inject(method = "addToFrame", at = @At("HEAD"))
	private void mobLife$updateDistanceBlur(CallbackInfo ci) {
		if (ClientMorphState.morph() == null) {
			return;
		}

		PostPassAccessor accessor = (PostPassAccessor) this;
		if (!accessor.mobLife$getName().startsWith(MobLife.MOD_ID + ":")) {
			return;
		}

		Map<String, GpuBuffer> customUniforms =
				accessor.mobLife$getCustomUniforms();
		GpuBuffer buffer = customUniforms.get("DistanceBlur");
		if (buffer == null) {
			return;
		}
		if ((buffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
			GpuBuffer writableBuffer = RenderSystem.getDevice().createBuffer(
					() -> "Mob Life dynamic distance blur uniform",
					GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
					DISTANCE_BLUR_UBO_SIZE
			);
			customUniforms.put("DistanceBlur", writableBuffer);
			buffer.close();
			buffer = writableBuffer;
		}

		float farPlane = Math.max(
				FULL_FOG_DISTANCE,
				Minecraft.getInstance().options.getEffectiveRenderDistance()
						* 16.0F
		);
		float interference = MorphAwkwardness.visionInterference(
				ClientMorphState.awkwardness()
		);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			Std140Builder data = Std140Builder.onStack(
					stack,
					DISTANCE_BLUR_UBO_SIZE
			)
					.putVec4(
							EFFECT_START_DISTANCE,
							FULL_BLUR_DISTANCE,
							FULL_DARKENING_DISTANCE,
							FULL_FOG_DISTANCE
					)
					.putVec4(
							MAXIMUM_BLUR_RADIUS * (1.0F + interference),
							Math.max(
									0.015F,
									0.08F - 0.05F * interference
							),
							Math.max(
									0.05F,
									0.22F - 0.10F * interference
							),
							1.0F
					)
					.putVec4(
							NEAR_PLANE,
							farPlane,
							ClientVisionPass.isDistancePass() ? 1.0F : 0.0F,
							interference
					);
			RenderSystem.getDevice()
					.createCommandEncoder()
					.writeToBuffer(buffer.slice(), data.get());
		}
	}
}
