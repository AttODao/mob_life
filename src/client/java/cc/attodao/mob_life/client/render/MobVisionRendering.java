package cc.attodao.mob_life.client.render;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.client.config.ClientMobLifeConfig;
import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.vision.MobVisionPolicy;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

/** Owns Mob vision frame derivation, uniform layout, and dynamic buffer updates. */
public final class MobVisionRendering {
  public static final Identifier BASE_POST_EFFECT = MobLife.id("vision_base");
  public static final Identifier DISTANCE_POST_EFFECT = MobLife.id("vision_distance");

  private MobVisionRendering() {}

  public static boolean enabled() {
    return ClientMobLifeConfig.shaderEnabled() && hasMorph();
  }

  public static boolean hasMorph() {
    return ClientMorphState.morph() != null;
  }

  public static void updateUniforms(Map<String, GpuBuffer> customUniforms) {
    if (!hasMorph()) {
      return;
    }

    Minecraft client = Minecraft.getInstance();
    MorphConfig.Vision vision = MorphConfigManager.get(ClientMorphState.morph()).vision();
    float skyDarken = client.level == null ? 0.0F : client.level.getSkyDarken();
    MobVisionPolicy.Frame frame =
        MobVisionPolicy.derive(
            vision,
            new MobVisionPolicy.Environment(
                client.options.getEffectiveRenderDistance() * 16.0F,
                skyDarken,
                ClientMorphState.awkwardness(),
                ClientInstinctState.active(),
                ClientInstinctState.level()));
    writeInstinctVignette(customUniforms, frame.instinctVignette());
    writeDistanceUniform(customUniforms, frame.distance());
  }

  private static void writeDistanceUniform(
      Map<String, GpuBuffer> customUniforms, MobVisionPolicy.DistanceUniform values) {
    GpuBuffer uniform =
        writableUniform(
            customUniforms,
            MobVisionPolicy.DISTANCE_UNIFORM,
            MobVisionPolicy.DISTANCE_UNIFORM_BYTES);
    if (uniform == null) {
      return;
    }

    try (MemoryStack stack = MemoryStack.stackPush()) {
      Std140Builder data = Std140Builder.onStack(stack, MobVisionPolicy.DISTANCE_UNIFORM_BYTES);
      for (MobVisionPolicy.Vec4 value : values.values()) {
        data.putVec4(value.x(), value.y(), value.z(), value.w());
      }
      RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniform.slice(), data.get());
    }
  }

  private static void writeInstinctVignette(
      Map<String, GpuBuffer> customUniforms, MobVisionPolicy.Vec4 value) {
    GpuBuffer uniform =
        writableUniform(
            customUniforms,
            MobVisionPolicy.INSTINCT_UNIFORM,
            MobVisionPolicy.INSTINCT_UNIFORM_BYTES);
    if (uniform == null) {
      return;
    }

    try (MemoryStack stack = MemoryStack.stackPush()) {
      var data =
          Std140Builder.onStack(stack, MobVisionPolicy.INSTINCT_UNIFORM_BYTES)
              .putVec4(value.x(), value.y(), value.z(), value.w());
      RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniform.slice(), data.get());
    }
  }

  private static GpuBuffer writableUniform(
      Map<String, GpuBuffer> customUniforms, String name, int size) {
    GpuBuffer buffer = customUniforms.get(name);
    if (buffer == null) {
      return null;
    }
    if ((buffer.usage() & GpuBuffer.USAGE_COPY_DST) != 0 && buffer.size() >= size) {
      return buffer;
    }

    GpuBuffer writableBuffer =
        RenderSystem.getDevice()
            .createBuffer(
                () -> "Mob Life dynamic " + name + " uniform",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                size);
    customUniforms.put(name, writableBuffer);
    buffer.close();
    return writableBuffer;
  }
}
