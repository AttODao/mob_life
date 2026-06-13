package cc.attodao.mob_life.client.mixin.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(PostPass.class)
public interface PostPassAccessor {
	@Accessor("name")
	String mobLife$getName();

	@Accessor("customUniforms")
	Map<String, GpuBuffer> mobLife$getCustomUniforms();
}
