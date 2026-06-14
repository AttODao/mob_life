package cc.attodao.mob_life.client.screen;

import cc.attodao.mob_life.morph.MorphType;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class MorphSelectionScreen extends Screen {

  private static final int COLUMNS = 3;
  private static final int BUTTON_WIDTH = 100;
  private static final int BUTTON_HEIGHT = 20;
  private static final int SPACING = 6;

  private final Screen parent;
  private final MorphType selected;
  private final Consumer<MorphType> selectionHandler;

  public MorphSelectionScreen(
      Screen parent, MorphType selected, Consumer<MorphType> selectionHandler) {
    super(Component.translatable("mob_life.create_world.morph.select"));
    this.parent = parent;
    this.selected = selected;
    this.selectionHandler = selectionHandler;
  }

  @Override
  protected void init() {
    MorphType[] morphs = MorphType.values();
    int gridWidth = COLUMNS * BUTTON_WIDTH + (COLUMNS - 1) * SPACING;
    int startX = (width - gridWidth) / 2;
    int startY = 42;

    for (int index = 0; index < morphs.length; index++) {
      MorphType morph = morphs[index];
      int column = index % COLUMNS;
      int row = index / COLUMNS;
      Button button =
          Button.builder(
                  Component.translatable(morph.translationKey()),
                  pressed -> {
                    selectionHandler.accept(morph);
                    minecraft.setScreen(parent);
                  })
              .bounds(
                  startX + column * (BUTTON_WIDTH + SPACING),
                  startY + row * (BUTTON_HEIGHT + SPACING),
                  BUTTON_WIDTH,
                  BUTTON_HEIGHT)
              .build();
      button.active = morph != selected;
      addRenderableWidget(button);
    }

    addRenderableWidget(
        Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
            .bounds(width / 2 - 100, height - 28, 200, 20)
            .build());
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    graphics.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
  }

  @Override
  public void onClose() {
    minecraft.setScreen(parent);
  }
}
