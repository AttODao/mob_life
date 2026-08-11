package cc.attodao.mob_life.client.screen;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.client.mixin.world.CreateWorldScreenInvoker;
import cc.attodao.mob_life.client.world.MorphSpawnSeedSelector;
import cc.attodao.mob_life.morph.MorphDefinition;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

public final class MorphWorldPreparationScreen extends Screen {

  private static final int PANEL = 0xFF151A22;
  private static final int ACCENT = 0xFF65D1B8;
  private static final int BODY_TEXT = 0xFFD8DEE9;
  private static final int SUBTLE_TEXT = 0xFF9FA9B8;

  private final CreateWorldScreen createWorldScreen;
  private final MorphDefinition selection;
  private final AtomicInteger attempts = new AtomicInteger();
  private CompletableFuture<OptionalLong> seedTask;
  private boolean continuing;

  public MorphWorldPreparationScreen(
      CreateWorldScreen createWorldScreen, MorphDefinition selection) {
    super(Component.translatable("mob_life.world_prepare.title"));
    this.createWorldScreen = createWorldScreen;
    this.selection = selection;
  }

  @Override
  protected void init() {
    super.init();
    if (seedTask == null) {
      seedTask =
          CompletableFuture.supplyAsync(
              () ->
                  MorphSpawnSeedSelector.findSeed(
                      createWorldScreen.getUiState().getSettings(), selection, attempts::set));
      seedTask.whenComplete(
          (seed, throwable) -> {
            if (minecraft == null) {
              return;
            }
            minecraft.execute(() -> finishPreparation(seed, throwable));
          });
    }
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return false;
  }

  @Override
  public void onClose() {}

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    graphics.fill(0, 0, width, height, 0xE0101218);
    int panelWidth = Math.min(360, Math.max(220, width - 48));
    int panelHeight = 96;
    int panelX = (width - panelWidth) / 2;
    int panelY = (height - panelHeight) / 2;
    graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL);
    graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, ACCENT);
    graphics.centeredText(font, title, width / 2, panelY + 18, 0xFFFFFFFF);
    graphics.centeredText(
        font,
        Component.translatable(
            "mob_life.world_prepare.message",
            Component.translatable(selection.type().translationKey())),
        width / 2,
        panelY + 42,
        BODY_TEXT);
    graphics.centeredText(
        font,
        continuing
            ? Component.translatable("mob_life.world_prepare.continuing")
            : Component.translatable(
                "mob_life.world_prepare.progress",
                attempts.get(),
                MorphSpawnSeedSelector.maxAttempts()),
        width / 2,
        panelY + 64,
        SUBTLE_TEXT);
    super.extractRenderState(graphics, mouseX, mouseY, partialTick);
  }

  private void finishPreparation(OptionalLong seed, Throwable throwable) {
    if (minecraft == null || minecraft.gui.screen() != this || continuing) {
      return;
    }

    continuing = true;
    if (throwable != null) {
      MobLife.LOGGER.warn("Could not choose a morph-friendly world seed", throwable);
    } else if (seed != null && seed.isPresent()) {
      createWorldScreen.getUiState().setSeed(Long.toString(seed.getAsLong()));
      MobLife.LOGGER.info(
          "Selected seed {} for initial {} spawn", seed.getAsLong(), selection.type().id());
    }

    if (createWorldScreen instanceof CreateWorldScreenInvoker invoker) {
      invoker.mobLife$onCreate();
    } else {
      minecraft.gui.setScreen(createWorldScreen);
    }
  }
}
