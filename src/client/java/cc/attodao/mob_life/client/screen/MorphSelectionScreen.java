package cc.attodao.mob_life.client.screen;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.client.mixin.world.CreateWorldScreenInvoker;
import cc.attodao.mob_life.config.MobLifeConfig;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import cc.attodao.mob_life.world.PendingWorldSelection;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

public final class MorphSelectionScreen extends Screen {

  private static final int TOP = 40;
  private static final int SIDE_GAP = 16;
  private static final int PANEL_PADDING = 12;
  private static final int FOOTER_HEIGHT = 56;
  private static final int LIST_ROW_HEIGHT = 56;
  private static final int LIST_ROW_GAP = 6;
  private static final int LIST_PREVIEW_SIZE = 32;
  private static final int LIST_PREVIEW_HEIGHT = 44;
  private static final int LIST_QUESTION_SIZE = 16;
  private static final int DETAIL_HEADER_HEIGHT = 24;
  private static final int DETAIL_PREVIEW_SIZE = 72;
  private static final int DETAIL_PREVIEW_HEIGHT = 112;
  private static final int DETAIL_CLOSE_SIZE = 18;
  private static final int DETAIL_LINE_HEIGHT = 11;
  private static final int DETAIL_INPUT_HEIGHT = 20;
  private static final int DETAIL_INPUT_GAP = 8;
  private static final int LIST_SCROLLBAR_WIDTH = 6;
  private static final int DETAIL_SCROLLBAR_WIDTH = 6;
  private static final int DETAIL_SCROLLBAR_GAP = 24;
  private static final int ACCENT = 0xFF65D1B8;
  private static final int INFO = 0xFF8EC7FF;
  private static final int WARN = 0xFFE0B35A;
  private static final int PANEL = 0xFF151A22;
  private static final int PANEL_EDGE = 0xFF2A3442;
  private static final int PANEL_EDGE_SOFT = 0xFF1F2733;
  private static final int BODY_TEXT = 0xFFD8DEE9;
  private static final int SUBTLE_TEXT = 0xFF9FA9B8;
  private static final int SELECTED_BG = 0x332DC3A3;
  private static final int HOVER_BG = 0x222B3644;
  private static final int QUESTION_BG = 0x332B2110;

  private final List<MorphType> morphTypes;
  private final EnumMap<MorphType, LivingEntity> previewEntities = new EnumMap<>(MorphType.class);
  private final Screen returnScreen;
  private Level previewLevel;

  private MorphType selectedMorph = MorphType.PLAYER;
  private MorphType focusedMorph = MorphType.PLAYER;
  private String nbtText = "";
  private CompoundTag parsedNbt = new CompoundTag();
  private boolean nbtValid = true;
  private boolean createWorldRequested;
  private boolean helpOpen;
  private int listX;
  private int listY;
  private int listWidth;
  private int listHeight;
  private int detailX;
  private int detailY;
  private int detailWidth;
  private int detailHeight;
  private int confirmX;
  private int confirmY;
  private int confirmWidth;
  private int listScroll;
  private int detailScroll;
  private EditBox nbtInput;
  private Button confirmButton;

  public MorphSelectionScreen(List<MorphType> morphTypes) {
    this(null, morphTypes, MorphDefinition.of(initialMorph(morphTypes)));
  }

  public MorphSelectionScreen(Screen returnScreen) {
    this(returnScreen, MobLifeConfig.selectableMorphs(), PendingWorldSelection.peekOrDefault());
  }

  private MorphSelectionScreen(
      Screen returnScreen, List<MorphType> morphTypes, MorphDefinition initialSelection) {
    super(
        Component.translatable(
            returnScreen == null
                ? "mob_life.world_select.title"
                : "mob_life.create_world.morph.select"));
    this.returnScreen = returnScreen;
    this.morphTypes = List.copyOf(morphTypes);
    selectedMorph = normalizeSelection(initialSelection.type(), this.morphTypes);
    focusedMorph = selectedMorph;
    CompoundTag initialNbt = initialSelection.nbt();
    if (!initialNbt.isEmpty()) {
      nbtText = initialNbt.toString();
      parsedNbt = initialNbt;
    }
  }

  @Override
  protected void init() {
    super.init();
    rebuildLayout();
    buildPreviews();

    nbtInput = createNbtInput();
    addRenderableWidget(nbtInput);

    confirmWidth = 128;
    confirmX = footerButtonX();
    confirmY = footerButtonY();
    confirmButton =
        addRenderableWidget(
            Button.builder(
                    Component.translatable("mob_life.world_select.confirm"),
                    button -> submitSelection())
                .bounds(confirmX, confirmY, confirmWidth, 20)
                .build());

    selectMorph(selectedMorph, false);
    focusedMorph = selectedMorph;
    helpOpen = false;
    refreshSubmitState();
  }

  @Override
  protected void repositionElements() {
    rebuildLayout();
    if (nbtInput != null) {
      nbtInput.setX(footerInputX());
      nbtInput.setY(footerInputY());
      nbtInput.setWidth(footerInputWidth());
    }
    if (confirmButton != null) {
      confirmButton.setX(confirmX);
      confirmButton.setY(confirmY);
    }
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return returnScreen != null;
  }

  @Override
  public void onClose() {
    if (returnScreen != null && minecraft != null && !createWorldRequested) {
      minecraft.setScreen(returnScreen);
    }
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
    if (helpOpen) {
      if (mouseInHelpClose(event.x(), event.y())) {
        closeHelp();
      }
      return true;
    }

    Optional<GuiEventListener> child = getChildAt(event.x(), event.y());
    if (child.isPresent()) {
      GuiEventListener listener = child.get();
      if (!listener.mouseClicked(event, handled)) {
        return true;
      }

      if (listener.shouldTakeFocusAfterInteraction()) {
        setFocused(listener);
        if (event.button() == 0) {
          setDragging(true);
        }
      }

      return true;
    }

    if (event.button() == 0) {
      MorphType clicked = rowAt(event.x(), event.y());
      if (clicked != null) {
        if (questionAt(event.x(), event.y()) == clicked) {
          focusMorph(clicked);
        } else {
          selectMorph(clicked, true);
        }
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean mouseScrolled(
      double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    if (helpOpen) {
      if (mouseInHelpBody(mouseX, mouseY) && detailScrollRange() > 0) {
        detailScroll = Math.clamp(detailScroll - (int) verticalAmount * 14, 0, detailScrollRange());
      }
      return true;
    }

    if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
      return true;
    }

    if (mouseInList(mouseX, mouseY) && listScrollRange() > 0) {
      listScroll = Math.clamp(listScroll - (int) verticalAmount * 14, 0, listScrollRange());
      return true;
    }

    return false;
  }

  @Override
  public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
    if (helpOpen) {
      if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
        closeHelp();
      }
      return true;
    }

    return super.keyPressed(event);
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    graphics.fill(0, 0, width, height, 0xD0101218);
    graphics.fill(0, 0, width, 1, 0xFF0B0D12);
    graphics.centeredText(font, title, width / 2, 18, 0xFFFFFFFF);
    graphics.fill(width / 2 - 92, 27, width / 2 + 92, 28, ACCENT);

    if (nbtInput != null) {
      nbtInput.visible = !helpOpen;
      nbtInput.active = !helpOpen;
    }
    if (confirmButton != null) {
      confirmButton.visible = !helpOpen;
    }

    renderListPanel(graphics, mouseX, mouseY);
    if (!helpOpen) {
      renderFooter(graphics);
    }

    super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    if (helpOpen) {
      renderHelpOverlay(graphics, mouseX, mouseY);
    }
  }

  private void rebuildLayout() {
    int footerTop = footerTop();
    int contentBottom = footerTop - 8;

    listX = 16;
    listY = TOP;
    listWidth = Math.max(0, width - 32);
    listHeight = Math.max(0, contentBottom - listY);

    detailX = 16;
    detailY = 16;
    detailWidth = Math.max(1, width - 32);
    detailHeight = Math.max(1, height - 32);

    confirmWidth = 128;
    confirmX = footerButtonX();
    confirmY = footerButtonY();
  }

  private void buildPreviews() {
    previewEntities.clear();
    Level level = previewEntityLevel();
    if (level == null) {
      return;
    }

    for (MorphType morph : morphTypes) {
      LivingEntity preview;
      if (morph.isPlayer()) {
        preview = createPlayerPreview(level);
      } else {
        preview = null;
        Entity candidate = MorphEntityFactory.create(MorphDefinition.of(morph), level);
        if (candidate instanceof LivingEntity livingPreview) {
          preview = livingPreview;
        }
      }
      previewEntities.put(morph, preview);
    }
  }

  private LivingEntity createPlayerPreview(Level level) {
    if (minecraft.player instanceof LivingEntity livingPreview) {
      return livingPreview;
    }
    if (level instanceof ClientLevel clientLevel) {
      GameProfile profile;
      if (minecraft.getUser() != null) {
        profile =
            new GameProfile(minecraft.getUser().getProfileId(), minecraft.getUser().getName());
      } else {
        profile = new GameProfile(UUID.randomUUID(), "preview");
      }
      PreviewPlayer preview = new PreviewPlayer(clientLevel, profile);
      preview.setPos(0.0, 0.0, 0.0);
      return preview;
    }
    return null;
  }

  private Level previewEntityLevel() {
    if (minecraft == null) {
      return null;
    }

    if (minecraft.level != null) {
      return minecraft.level;
    }

    if (previewLevel == null) {
      previewLevel = createPreviewLevel();
    }
    return previewLevel;
  }

  private Level createPreviewLevel() {
    try {
      if (!(returnScreen instanceof CreateWorldScreen createWorldScreen)) {
        return null;
      }

      WorldCreationContext worldCreationContext = createWorldScreen.getUiState().getSettings();
      RegistryAccess.Frozen registryAccess = worldCreationContext.worldgenLoadContext();
      var dimensionLookup = registryAccess.lookupOrThrow(Registries.DIMENSION_TYPE);
      Holder<DimensionType> dimensionType =
          dimensionLookup
              .get(BuiltinDimensionTypes.OVERWORLD)
              .orElseGet(() -> dimensionLookup.getOrThrow(BuiltinDimensionTypes.OVERWORLD_CAVES));

      GameProfile profile;
      if (minecraft.getUser() != null) {
        profile =
            new GameProfile(minecraft.getUser().getProfileId(), minecraft.getUser().getName());
      } else {
        profile = new GameProfile(UUID.randomUUID(), "preview");
      }

      WorldSessionTelemetryManager telemetryManager =
          new WorldSessionTelemetryManager(
              TelemetryEventSender.DISABLED, false, Duration.ZERO, "mob_life_preview");
      CommonListenerCookie cookie =
          new CommonListenerCookie(
              new LevelLoadTracker(),
              profile,
              telemetryManager,
              registryAccess,
              worldCreationContext.dataConfiguration().enabledFeatures(),
              "",
              null,
              null,
              Map.of(),
              new ChatComponent.State(List.of(), List.of(), List.of()),
              Map.of(),
              ServerLinks.EMPTY,
              Map.of(),
              false);
      ClientPacketListener listener =
          new ClientPacketListener(
              minecraft,
              new Connection(net.minecraft.network.protocol.PacketFlow.CLIENTBOUND),
              cookie);
      ClientLevelData data =
          new ClientLevelData(net.minecraft.world.Difficulty.NORMAL, false, false);
      return new ClientLevel(
          listener,
          data,
          Level.OVERWORLD,
          dimensionType,
          3,
          3,
          minecraft.levelRenderer,
          false,
          0L,
          63);
    } catch (RuntimeException exception) {
      MobLife.LOGGER.warn("Failed to create morph selection preview level", exception);
      return null;
    }
  }

  private EditBox createNbtInput() {
    int inputWidth = footerInputWidth();
    int inputY = footerInputY();
    EditBox input =
        new EditBox(
            font,
            footerInputX(),
            inputY,
            inputWidth,
            DETAIL_INPUT_HEIGHT,
            Component.translatable("mob_life.world_select.nbt"));
    input.setMaxLength(2048);
    input.setHint(Component.translatable("mob_life.world_select.nbt.hint"));
    input.setValue(nbtText);
    input.setResponder(this::parseNbt);
    input.setTooltip(Tooltip.create(Component.translatable("mob_life.world_select.nbt.tooltip")));
    return input;
  }

  private void selectMorph(MorphType morph, boolean focusDetails) {
    selectedMorph = morph;
    if (focusDetails) {
      focusedMorph = morph;
      detailScroll = 0;
    }

    if (nbtInput != null) {
      boolean editable = !morph.isPlayer();
      nbtInput.setEditable(editable);
      if (editable) {
        parseNbt(nbtInput.getValue());
      } else {
        nbtValid = true;
        parsedNbt = new CompoundTag();
        nbtInput.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
      }
    }

    refreshSubmitState();
  }

  private void focusMorph(MorphType morph) {
    focusedMorph = morph;
    detailScroll = 0;
    helpOpen = true;
  }

  private void parseNbt(String value) {
    nbtText = value;
    if (nbtInput == null) {
      return;
    }

    if (selectedMorph.isPlayer() || value.isBlank()) {
      parsedNbt = new CompoundTag();
      nbtValid = true;
      nbtInput.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
      refreshSubmitState();
      return;
    }

    try {
      parsedNbt = TagParser.parseCompoundFully(value);
      nbtValid = true;
      nbtInput.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
    } catch (CommandSyntaxException exception) {
      nbtValid = false;
      nbtInput.setTextColor(0xFFFF6666);
    }
    refreshSubmitState();
  }

  private void refreshSubmitState() {
    if (confirmButton != null) {
      confirmButton.active = selectedMorph.isPlayer() || nbtValid;
    }
  }

  private static MorphType normalizeSelection(MorphType morph, List<MorphType> morphTypes) {
    if (morphTypes.contains(morph)) {
      return morph;
    }
    return initialMorph(morphTypes);
  }

  private static MorphType initialMorph(List<MorphType> morphTypes) {
    return morphTypes.isEmpty() ? MobLifeConfig.defaultMorph() : morphTypes.get(0);
  }

  private void submitSelection() {
    if (!selectedMorph.isPlayer() && !nbtValid) {
      if (nbtInput != null) {
        nbtInput.setFocused(true);
      }
      return;
    }

    MorphDefinition selection =
        new MorphDefinition(
            selectedMorph, selectedMorph.isPlayer() ? new CompoundTag() : parsedNbt);
    if (returnScreen != null) {
      PendingWorldSelection.setForNextWorld(selection);
      if (returnScreen instanceof CreateWorldScreen createWorldScreen
          && returnScreen instanceof CreateWorldScreenInvoker invoker) {
        createWorldRequested = true;
        if (shouldPrepareWorldSeed(createWorldScreen, selection) && minecraft != null) {
          minecraft.setScreen(new MorphWorldPreparationScreen(createWorldScreen, selection));
        } else {
          invoker.mobLife$onCreate();
        }
      } else if (minecraft != null) {
        minecraft.setScreen(returnScreen);
      }
      return;
    }

    ClientPlayNetworking.send(
        new MobLifeNetworking.WorldMorphSelectionSubmitPayload(
            selection.type().id(), selection.nbt()));
    if (minecraft != null) {
      minecraft.setScreen(null);
    }
  }

  private boolean shouldPrepareWorldSeed(
      CreateWorldScreen createWorldScreen, MorphDefinition selection) {
    return selection.hasMobForm() && createWorldScreen.getUiState().getSeed().isBlank();
  }

  private void renderListPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    graphics.fill(listX, listY, listX + listWidth, listY + listHeight, PANEL);
    graphics.fill(listX, listY, listX + listWidth, listY + 1, ACCENT);
    graphics.fill(listX, listY + 1, listX + 1, listY + listHeight, PANEL_EDGE);
    graphics.fill(
        listX + listWidth - 1, listY + 1, listX + listWidth, listY + listHeight, PANEL_EDGE);
    graphics.text(
        font,
        Component.translatable("mob_life.world_select.list"),
        listX + PANEL_PADDING,
        listY + 8,
        0xFFFFFFFF,
        false);

    int innerX = listX + PANEL_PADDING;
    int innerY = listY + 24;
    int innerWidth = listWidth - PANEL_PADDING * 2;
    int viewportHeight = Math.max(0, listHeight - 32);
    int contentHeight = morphTypes.size() * (LIST_ROW_HEIGHT + LIST_ROW_GAP) - LIST_ROW_GAP;
    int maxScroll = Math.max(0, contentHeight - viewportHeight);
    listScroll = Math.clamp(listScroll, 0, maxScroll);

    int visibleWidth = innerWidth - (maxScroll > 0 ? LIST_SCROLLBAR_WIDTH + 4 : 0);
    graphics.enableScissor(innerX, innerY, innerX + visibleWidth, innerY + viewportHeight);

    int y = innerY - listScroll;
    for (MorphType morph : morphTypes) {
      renderListRow(graphics, morph, innerX, y, visibleWidth, mouseX, mouseY);
      y += LIST_ROW_HEIGHT + LIST_ROW_GAP;
    }

    graphics.disableScissor();

    if (maxScroll > 0) {
      int barX = innerX + visibleWidth + 4;
      renderScrollbar(
          graphics, barX, innerY, LIST_SCROLLBAR_WIDTH, viewportHeight, listScroll, maxScroll);
    }
  }

  private void renderListRow(
      GuiGraphicsExtractor graphics,
      MorphType morph,
      int x,
      int y,
      int width,
      int mouseX,
      int mouseY) {
    int rowBottom = y + LIST_ROW_HEIGHT;
    if (rowBottom < listY + 24 || y > listY + listHeight) {
      return;
    }

    boolean selected = morph == selectedMorph;
    boolean focused = morph == focusedMorph;
    boolean hovered =
        mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + LIST_ROW_HEIGHT;
    boolean questionHovered = isQuestionHovered(morph, mouseX, mouseY);

    int background = selected ? SELECTED_BG : hovered ? HOVER_BG : 0x141A2230;
    int border = selected ? ACCENT : focused ? INFO : hovered ? 0xFF4B5A6B : PANEL_EDGE_SOFT;

    graphics.fill(x, y, x + width, y + LIST_ROW_HEIGHT, background);
    graphics.fill(x, y, x + width, y + 1, border);
    graphics.fill(x, y + LIST_ROW_HEIGHT - 1, x + width, y + LIST_ROW_HEIGHT, border);
    graphics.fill(x, y, x + 1, y + LIST_ROW_HEIGHT, border);
    graphics.fill(x + width - 1, y, x + width, y + LIST_ROW_HEIGHT, border);

    LivingEntity preview = previewEntities.get(morph);
    if (preview != null) {
      int previewLeft = x + 6;
      int previewTop = y + 4;
      InventoryScreen.extractEntityInInventoryFollowsMouse(
          graphics,
          previewLeft,
          previewTop,
          previewLeft + LIST_PREVIEW_SIZE,
          previewTop + LIST_PREVIEW_HEIGHT,
          rowPreviewScale(preview),
          0.0625F,
          mouseX,
          mouseY,
          preview);
    } else {
      graphics.fill(
          x + 6, y + 4, x + 6 + LIST_PREVIEW_SIZE, y + 4 + LIST_PREVIEW_HEIGHT, 0x22000000);
      graphics.centeredText(
          font,
          Component.literal("?"),
          x + 6 + LIST_PREVIEW_SIZE / 2,
          y + 4 + LIST_PREVIEW_HEIGHT / 2 - 4,
          SUBTLE_TEXT);
    }

    graphics.text(font, morphName(morph), x + 52, y + 11, 0xFFFFFFFF, false);

    int questionX = x + width - LIST_QUESTION_SIZE - 6;
    int questionY = y + (LIST_ROW_HEIGHT - LIST_QUESTION_SIZE) / 2;
    graphics.fill(
        questionX,
        questionY,
        questionX + LIST_QUESTION_SIZE,
        questionY + LIST_QUESTION_SIZE,
        questionHovered ? WARN : QUESTION_BG);
    graphics.fill(questionX, questionY, questionX + LIST_QUESTION_SIZE, questionY + 1, WARN);
    graphics.fill(
        questionX,
        questionY + LIST_QUESTION_SIZE - 1,
        questionX + LIST_QUESTION_SIZE,
        questionY + LIST_QUESTION_SIZE,
        WARN);
    graphics.fill(questionX, questionY, questionX + 1, questionY + LIST_QUESTION_SIZE, WARN);
    graphics.fill(
        questionX + LIST_QUESTION_SIZE - 1,
        questionY,
        questionX + LIST_QUESTION_SIZE,
        questionY + LIST_QUESTION_SIZE,
        WARN);
    graphics.centeredText(
        font,
        Component.literal("?"),
        questionX + LIST_QUESTION_SIZE / 2,
        questionY + 4,
        questionHovered ? 0xFF241700 : WARN);
  }

  private void renderFooter(GuiGraphicsExtractor graphics) {
    int footerTop = footerTop();
    graphics.fill(0, footerTop, width, height, PANEL);
    graphics.fill(0, footerTop, width, footerTop + 1, ACCENT);
    graphics.fill(0, footerTop, 1, height, PANEL_EDGE);
    graphics.fill(width - 1, footerTop, width, height, PANEL_EDGE);
    graphics.text(
        font,
        Component.translatable("mob_life.world_select.nbt.label"),
        16,
        footerTop + 6,
        SUBTLE_TEXT,
        false);
  }

  private void renderHelpOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    graphics.fill(0, 0, width, height, 0xC0101218);
    graphics.fill(detailX, detailY, detailX + detailWidth, detailY + detailHeight, PANEL);
    graphics.fill(detailX, detailY, detailX + detailWidth, detailY + 1, INFO);
    graphics.fill(detailX, detailY + 1, detailX + 1, detailY + detailHeight, PANEL_EDGE);
    graphics.fill(
        detailX + detailWidth - 1,
        detailY + 1,
        detailX + detailWidth,
        detailY + detailHeight,
        PANEL_EDGE);

    graphics.text(
        font, morphName(focusedMorph), detailX + PANEL_PADDING, detailY + 10, 0xFFFFFFFF, false);

    renderHelpCloseButton(graphics, mouseX, mouseY);
    renderHelpPreview(graphics, mouseX, mouseY);
    renderHelpText(graphics, mouseX, mouseY);
  }

  private void renderHelpCloseButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    int closeX = detailCloseX();
    int closeY = detailCloseY();
    boolean hovered = mouseInHelpClose(mouseX, mouseY);
    int border = hovered ? 0xFFE06B6B : WARN;
    int background = hovered ? 0x44E06B6B : QUESTION_BG;
    graphics.fill(
        closeX, closeY, closeX + DETAIL_CLOSE_SIZE, closeY + DETAIL_CLOSE_SIZE, background);
    graphics.fill(closeX, closeY, closeX + DETAIL_CLOSE_SIZE, closeY + 1, border);
    graphics.fill(
        closeX,
        closeY + DETAIL_CLOSE_SIZE - 1,
        closeX + DETAIL_CLOSE_SIZE,
        closeY + DETAIL_CLOSE_SIZE,
        border);
    graphics.fill(closeX, closeY, closeX + 1, closeY + DETAIL_CLOSE_SIZE, border);
    graphics.fill(
        closeX + DETAIL_CLOSE_SIZE - 1,
        closeY,
        closeX + DETAIL_CLOSE_SIZE,
        closeY + DETAIL_CLOSE_SIZE,
        border);
    graphics.centeredText(
        font, Component.literal("X"), closeX + DETAIL_CLOSE_SIZE / 2, closeY + 4, border);
  }

  private void renderHelpPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    LivingEntity preview = previewEntities.get(focusedMorph);
    int previewLeft = detailPreviewX();
    int previewTop = detailPreviewY();
    if (preview != null) {
      InventoryScreen.extractEntityInInventoryFollowsMouse(
          graphics,
          previewLeft,
          previewTop,
          previewLeft + DETAIL_PREVIEW_SIZE,
          previewTop + DETAIL_PREVIEW_HEIGHT,
          detailPreviewScale(preview),
          0.0625F,
          mouseX,
          mouseY,
          preview);
    } else {
      graphics.fill(
          previewLeft,
          previewTop,
          previewLeft + DETAIL_PREVIEW_SIZE,
          previewTop + DETAIL_PREVIEW_HEIGHT,
          0x22000000);
    }
  }

  private void renderHelpText(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    int textX = detailTextX();
    int textTop = detailTextTop();
    int textBottom = detailTextBottom();
    int viewportHeight = Math.max(0, textBottom - textTop);
    int textWidth = detailTextWidth();

    List<RenderedDetailLine> lines = buildWrappedDetailLines(focusedMorph, textWidth);
    int contentHeight = measureDetailContentHeight(lines);
    int maxScroll = Math.max(0, contentHeight - viewportHeight);
    detailScroll = Math.clamp(detailScroll, 0, maxScroll);

    int visibleWidth = textWidth;
    graphics.enableScissor(textX, textTop, textX + visibleWidth, textTop + viewportHeight);

    int y = textTop - detailScroll;
    for (RenderedDetailLine line : lines) {
      y += line.topPadding();
      graphics.text(font, line.text(), textX + line.indent(), y, line.color(), false);
      y += DETAIL_LINE_HEIGHT;
    }

    graphics.disableScissor();

    if (maxScroll > 0) {
      int barX = textX + visibleWidth + 4;
      renderScrollbar(
          graphics, barX, textTop, DETAIL_SCROLLBAR_WIDTH, viewportHeight, detailScroll, maxScroll);
    }
  }

  private List<RenderedDetailLine> buildWrappedDetailLines(MorphType morph, int textWidth) {
    ArrayList<RenderedDetailLine> lines = new ArrayList<>();
    for (DetailLine line : buildDetailLines(morph)) {
      int availableWidth = Math.max(1, textWidth - line.indent());
      List<net.minecraft.util.FormattedCharSequence> wrapped =
          font.split(line.text(), availableWidth);
      if (wrapped.isEmpty()) {
        continue;
      }
      boolean first = true;
      for (net.minecraft.util.FormattedCharSequence sequence : wrapped) {
        lines.add(
            new RenderedDetailLine(
                sequence, line.color(), line.indent(), first ? line.topPadding() : 0));
        first = false;
      }
    }
    return lines;
  }

  private List<DetailLine> buildDetailLines(MorphType morph) {
    MorphConfig config = MorphConfigManager.get(morph);
    ArrayList<DetailLine> lines = new ArrayList<>();

    addSectionHeader(lines, "mob_life.world_select.section.movement");
    addBody(
        lines,
        Component.translatable(
            "mob_life.world_select.movement.charged_jump", yesNo(config.movement().chargedJump())));
    addBody(
        lines,
        Component.translatable(
            "mob_life.world_select.movement.slow_fall",
            formatNumber(config.movement().slowFallMultiplier())));
    if (config.movement().rabbitHop().enabled()) {
      addBody(lines, Component.translatable("mob_life.world_select.movement.rabbit_hop"));
    }

    addSectionHeader(lines, "mob_life.world_select.section.attack");
    addBody(
        lines,
        Component.translatable(
            "mob_life.world_select.attack.damage", formatNumber(config.combat().attackDamage())));
    if (config.combat().leapAttack().verticalSpeed() > 0.0) {
      addBody(lines, Component.translatable("mob_life.world_select.attack.leap"));
    }

    addSectionHeader(lines, "mob_life.world_select.section.predators");
    addBody(lines, entityDisplayText(config.combat().predators()));

    addSectionHeader(lines, "mob_life.world_select.section.foods");
    ArrayList<String> foodEntries = new ArrayList<>(config.diet().foods());
    foodEntries.addAll(config.diet().huntedFoods());
    addBody(lines, foodDisplayText(foodEntries));

    addSectionHeader(lines, "mob_life.world_select.section.sleep");
    addBody(
        lines,
        Component.translatable(
            "mob_life.world_select.sleep.schedule", sleepScheduleLabel(config.sleep().schedule())));
    addBody(
        lines,
        Component.translatable(
            "mob_life.world_select.sleep.without_bed", yesNo(config.sleep().withoutBed())));

    addSectionHeader(lines, "mob_life.world_select.section.ability");
    addBody(lines, Component.translatable(abilityKey(config.abilities().value())));

    return lines;
  }

  private int measureDetailContentHeight(List<RenderedDetailLine> lines) {
    int height = 0;
    for (RenderedDetailLine line : lines) {
      height += line.topPadding() + DETAIL_LINE_HEIGHT;
    }
    return height;
  }

  private int detailTextX() {
    return detailPreviewX() + DETAIL_PREVIEW_SIZE + SIDE_GAP;
  }

  private int detailTextTop() {
    return detailY + DETAIL_HEADER_HEIGHT;
  }

  private int detailTextBottom() {
    return detailY + detailHeight - PANEL_PADDING;
  }

  private int detailTextWidth() {
    return Math.max(
        1,
        detailX
            + detailWidth
            - PANEL_PADDING
            - detailTextX()
            - DETAIL_SCROLLBAR_WIDTH
            - DETAIL_SCROLLBAR_GAP);
  }

  private int detailViewportHeight() {
    return Math.max(0, detailTextBottom() - detailTextTop());
  }

  private int detailScrollRange() {
    return Math.max(
        0,
        measureDetailContentHeight(buildWrappedDetailLines(focusedMorph, detailTextWidth()))
            - detailViewportHeight());
  }

  private int detailPreviewX() {
    return detailX + PANEL_PADDING;
  }

  private int detailPreviewY() {
    return detailY + DETAIL_HEADER_HEIGHT;
  }

  private int detailCloseX() {
    return detailX + detailWidth - DETAIL_CLOSE_SIZE - PANEL_PADDING;
  }

  private int detailCloseY() {
    return detailY + 10;
  }

  private int rowPreviewScale(LivingEntity preview) {
    return previewScale(preview, 24.0F, 12, 20);
  }

  private int detailPreviewScale(LivingEntity preview) {
    return previewScale(preview, 26.0F, 12, 24);
  }

  private int previewScale(LivingEntity preview, float base, int min, int max) {
    float maxDimension = Math.max(preview.getBbWidth(), preview.getBbHeight());
    if (maxDimension <= 0.0F) {
      return max;
    }
    int scale = Math.round(base / Math.max(0.75F, maxDimension));
    return Math.clamp(scale, min, max);
  }

  private boolean mouseInHelpModal(double mouseX, double mouseY) {
    return mouseX >= detailX
        && mouseX < detailX + detailWidth
        && mouseY >= detailY
        && mouseY < detailY + detailHeight;
  }

  private boolean mouseInHelpBody(double mouseX, double mouseY) {
    return mouseInHelpModal(mouseX, mouseY);
  }

  private boolean mouseInHelpClose(double mouseX, double mouseY) {
    int closeX = detailCloseX();
    int closeY = detailCloseY();
    return mouseX >= closeX
        && mouseX < closeX + DETAIL_CLOSE_SIZE
        && mouseY >= closeY
        && mouseY < closeY + DETAIL_CLOSE_SIZE;
  }

  private void closeHelp() {
    helpOpen = false;
    detailScroll = 0;
  }

  private int footerTop() {
    return height - FOOTER_HEIGHT;
  }

  private int footerInputX() {
    return 16;
  }

  private int footerInputY() {
    return footerTop() + 26;
  }

  private int footerInputWidth() {
    return Math.max(120, width - 32 - confirmWidth - 12);
  }

  private int footerButtonY() {
    return footerTop() + 26;
  }

  private int footerButtonX() {
    return width - 16 - confirmWidth;
  }

  private void addSectionHeader(List<DetailLine> lines, String key) {
    int padding = lines.isEmpty() ? 0 : 6;
    lines.add(new DetailLine(Component.translatable(key), ACCENT, 0, padding));
  }

  private void addBody(List<DetailLine> lines, Component text) {
    lines.add(new DetailLine(text, BODY_TEXT, 10, 0));
  }

  private Component foodDisplayText(List<String> entries) {
    ArrayList<String> display = new ArrayList<>();
    LinkedHashSet<Identifier> seen = new LinkedHashSet<>();

    for (String entry : entries) {
      if (entry.startsWith("#")) {
        Identifier tagId = Identifier.tryParse(entry.substring(1));
        if (tagId == null) {
          continue;
        }

        TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
          Item item = holder.value();
          Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
          if (itemId != null && seen.add(itemId)) {
            display.add(new ItemStack(item).getHoverName().getString());
          }
        }
        continue;
      }

      Identifier itemId = Identifier.tryParse(entry);
      if (itemId == null) {
        continue;
      }

      BuiltInRegistries.ITEM
          .getOptional(itemId)
          .ifPresent(
              item -> {
                if (seen.add(itemId)) {
                  display.add(new ItemStack(item).getHoverName().getString());
                }
              });
    }

    return display.isEmpty() ? noneValue() : Component.literal(String.join("、", display));
  }

  private Component entityDisplayText(List<String> entries) {
    ArrayList<String> display = new ArrayList<>();
    LinkedHashSet<Identifier> seen = new LinkedHashSet<>();

    for (String entry : entries) {
      if (entry.startsWith("#")) {
        Identifier tagId = Identifier.tryParse(entry.substring(1));
        if (tagId == null) {
          continue;
        }

        TagKey<net.minecraft.world.entity.EntityType<?>> tag =
            TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, tagId);
        for (Holder<net.minecraft.world.entity.EntityType<?>> holder :
            BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tag)) {
          net.minecraft.world.entity.EntityType<?> entityType = holder.value();
          Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
          if (entityId != null && seen.add(entityId)) {
            display.add(entityType.getDescription().getString());
          }
        }
        continue;
      }

      Identifier entityId = Identifier.tryParse(entry);
      if (entityId == null) {
        continue;
      }

      BuiltInRegistries.ENTITY_TYPE
          .getOptional(entityId)
          .ifPresent(
              entityType -> {
                if (seen.add(entityId)) {
                  display.add(entityType.getDescription().getString());
                }
              });
    }

    return display.isEmpty() ? noneValue() : Component.literal(String.join("、", display));
  }

  private Component noneValue() {
    return Component.translatable("mob_life.world_select.value.none");
  }

  private Component yesNo(boolean value) {
    return Component.translatable(
        value ? "mob_life.world_select.value.yes" : "mob_life.world_select.value.no");
  }

  private Component sleepScheduleLabel(String value) {
    String key = "mob_life.world_select.sleep.schedule." + value;
    return Component.translatable(key);
  }

  private String abilityKey(MorphConfig.Ability ability) {
    return "mob_life.world_select.ability." + ability.id();
  }

  private String traitKey(MorphConfig.Trait trait) {
    return "mob_life.world_select.trait." + trait.id();
  }

  private Component morphName(MorphType morph) {
    return Component.translatable(morph.translationKey());
  }

  private MorphType rowAt(double mouseX, double mouseY) {
    int innerX = listX + PANEL_PADDING;
    int innerY = listY + 24;
    int innerWidth = listWidth - PANEL_PADDING * 2;
    int viewportHeight = Math.max(0, listHeight - 32);
    int contentHeight = morphTypes.size() * (LIST_ROW_HEIGHT + LIST_ROW_GAP) - LIST_ROW_GAP;
    int maxScroll = Math.max(0, contentHeight - viewportHeight);
    int visibleWidth = innerWidth - (maxScroll > 0 ? LIST_SCROLLBAR_WIDTH + 4 : 0);
    if (mouseX < innerX
        || mouseX >= innerX + visibleWidth
        || mouseY < innerY
        || mouseY >= innerY + viewportHeight) {
      return null;
    }
    int y = innerY - listScroll;
    for (MorphType morph : morphTypes) {
      if (mouseY >= y && mouseY < y + LIST_ROW_HEIGHT) {
        return morph;
      }
      y += LIST_ROW_HEIGHT + LIST_ROW_GAP;
    }
    return null;
  }

  private MorphType questionAt(double mouseX, double mouseY) {
    int innerX = listX + PANEL_PADDING;
    int innerY = listY + 24;
    int innerWidth = listWidth - PANEL_PADDING * 2;
    int viewportHeight = Math.max(0, listHeight - 32);
    int contentHeight = morphTypes.size() * (LIST_ROW_HEIGHT + LIST_ROW_GAP) - LIST_ROW_GAP;
    int maxScroll = Math.max(0, contentHeight - viewportHeight);
    int visibleWidth = innerWidth - (maxScroll > 0 ? LIST_SCROLLBAR_WIDTH + 4 : 0);
    if (mouseY < innerY || mouseY >= innerY + viewportHeight) {
      return null;
    }
    int y = innerY - listScroll;
    for (MorphType morph : morphTypes) {
      int questionX = innerX + visibleWidth - LIST_QUESTION_SIZE - 6;
      int questionY = y + (LIST_ROW_HEIGHT - LIST_QUESTION_SIZE) / 2;
      if (mouseX >= questionX
          && mouseX < questionX + LIST_QUESTION_SIZE
          && mouseY >= questionY
          && mouseY < questionY + LIST_QUESTION_SIZE) {
        return morph;
      }
      y += LIST_ROW_HEIGHT + LIST_ROW_GAP;
    }
    return null;
  }

  private boolean mouseInList(double mouseX, double mouseY) {
    return mouseX >= listX
        && mouseX < listX + listWidth
        && mouseY >= listY
        && mouseY < listY + listHeight;
  }

  private int listScrollRange() {
    int viewportHeight = Math.max(0, listHeight - 32);
    int contentHeight = morphTypes.size() * (LIST_ROW_HEIGHT + LIST_ROW_GAP) - LIST_ROW_GAP;
    return Math.max(0, contentHeight - viewportHeight);
  }

  private boolean isQuestionHovered(MorphType morph, int mouseX, int mouseY) {
    int innerX = listX + PANEL_PADDING;
    int innerY = listY + 24;
    int innerWidth = listWidth - PANEL_PADDING * 2;
    int viewportHeight = listHeight - 32;
    int contentHeight = morphTypes.size() * (LIST_ROW_HEIGHT + LIST_ROW_GAP) - LIST_ROW_GAP;
    int maxScroll = Math.max(0, contentHeight - viewportHeight);
    int visibleWidth = innerWidth - (maxScroll > 0 ? LIST_SCROLLBAR_WIDTH + 4 : 0);
    int y = innerY - listScroll;
    for (MorphType rowMorph : morphTypes) {
      int questionX = innerX + visibleWidth - LIST_QUESTION_SIZE - 6;
      int questionY = y + (LIST_ROW_HEIGHT - LIST_QUESTION_SIZE) / 2;
      if (rowMorph == morph
          && mouseX >= questionX
          && mouseX < questionX + LIST_QUESTION_SIZE
          && mouseY >= questionY
          && mouseY < questionY + LIST_QUESTION_SIZE) {
        return true;
      }
      y += LIST_ROW_HEIGHT + LIST_ROW_GAP;
    }
    return false;
  }

  private void renderScrollbar(
      GuiGraphicsExtractor graphics,
      int x,
      int y,
      int width,
      int viewportHeight,
      int scroll,
      int maxScroll) {
    graphics.fill(x, y, x + width, y + viewportHeight, 0x66212A36);
    int thumbHeight =
        Math.max(
            18, (int) ((viewportHeight / (float) (viewportHeight + maxScroll)) * viewportHeight));
    int thumbY = y + (int) ((scroll / (float) maxScroll) * (viewportHeight - thumbHeight));
    graphics.fill(x, thumbY, x + width, thumbY + thumbHeight, ACCENT);
  }

  private String formatNumber(double value) {
    String text = String.format(Locale.ROOT, "%.2f", value);
    while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
      text = text.substring(0, text.length() - 1);
    }
    return text;
  }

  private record DetailLine(Component text, int color, int indent, int topPadding) {}

  private record RenderedDetailLine(
      net.minecraft.util.FormattedCharSequence text, int color, int indent, int topPadding) {}

  private static final class PreviewPlayer extends RemotePlayer {
    private PreviewPlayer(ClientLevel level, GameProfile gameProfile) {
      super(level, gameProfile);
    }

    @Override
    protected PlayerInfo getPlayerInfo() {
      return null;
    }
  }
}
