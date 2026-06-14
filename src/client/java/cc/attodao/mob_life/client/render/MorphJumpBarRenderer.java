package cc.attodao.mob_life.client.render;

import cc.attodao.mob_life.client.state.ClientMorphState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class MorphJumpBarRenderer {
  private static final Identifier BACKGROUND =
      Identifier.withDefaultNamespace("hud/jump_bar_background");
  private static final Identifier COOLDOWN =
      Identifier.withDefaultNamespace("hud/jump_bar_cooldown");
  private static final Identifier PROGRESS =
      Identifier.withDefaultNamespace("hud/jump_bar_progress");

  private MorphJumpBarRenderer() {}

  public static void render(GuiGraphicsExtractor graphics) {
    Minecraft client = Minecraft.getInstance();
    int left = (client.getWindow().getGuiScaledWidth() - 182) / 2;
    int top = client.getWindow().getGuiScaledHeight() - 24 - 5;
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, 182, 5);

    if (ClientMorphState.isChargedJumpCoolingDown()) {
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, COOLDOWN, left, top, 182, 5);
      return;
    }

    int progress = Mth.lerpDiscrete(ClientMorphState.chargedJumpScale(), 0, 182);
    if (progress > 0) {
      graphics.blitSprite(
          RenderPipelines.GUI_TEXTURED, PROGRESS, 182, 5, 0, 0, left, top, progress, 5);
    }
  }
}
