package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.render.MobVisionRendering;
import com.mojang.blaze3d.buffers.GpuBuffer;
import java.util.Map;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostPass.class)
public abstract class PostPassDistanceUniformMixin {
  @Inject(method = "addToFrame", at = @At("HEAD"))
  private void mobLife$updateDistanceBlur(CallbackInfo ci) {
    PostPassAccessor accessor = (PostPassAccessor) this;
    Map<String, GpuBuffer> customUniforms = accessor.mobLife$getCustomUniforms();
    MobVisionRendering.updateUniforms(customUniforms);
  }
}
