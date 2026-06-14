package cc.attodao.mob_life.client.screen;

import cc.attodao.mob_life.client.config.MobLifeClientConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class MobLifeOptionsScreen extends Screen {
  private final Screen parent;

  public MobLifeOptionsScreen(Screen parent) {
    super(Component.translatable("mob_life.options.title"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    addRenderableWidget(
        CycleButton.onOffBuilder(MobLifeClientConfig.showAwkwardnessDebug())
            .create(
                width / 2 - 100,
                height / 2 - 24,
                200,
                20,
                Component.translatable("mob_life.options.awkwardness_debug"),
                (button, value) -> MobLifeClientConfig.setShowAwkwardnessDebug(value)));
    addRenderableWidget(
        Button.builder(CommonComponents.GUI_DONE, button -> onClose())
            .bounds(width / 2 - 100, height / 2 + 8, 200, 20)
            .build());
  }

  @Override
  public void onClose() {
    minecraft.setScreen(parent);
  }
}
