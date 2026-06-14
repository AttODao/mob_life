package cc.attodao.mob_life.client.mixin.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.util.Map;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostPass.class)
public interface PostPassAccessor {
  @Accessor("name")
  String mobLife$getName();

  @Accessor("customUniforms")
  Map<String, GpuBuffer> mobLife$getCustomUniforms();
}
