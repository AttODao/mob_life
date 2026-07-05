package cc.attodao.mob_life.client.screen;

import cc.attodao.mob_life.client.mixin.world.CreateWorldScreenInvoker;
import cc.attodao.mob_life.config.MobLifeConfig;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import cc.attodao.mob_life.world.InitialWorldVariantSelector;
import cc.attodao.mob_life.world.PendingWorldSelection;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

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
  private final MorphPreviewFactory previewFactory;

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
  private boolean listScrollbarDragging;
  private int listScrollbarDragOffset;
  private final EnumMap<MorphType, MorphSelectionWidget> rowWidgets =
      new EnumMap<>(MorphType.class);
  private final EnumMap<MorphType, MorphSelectionWidget> questionWidgets =
      new EnumMap<>(MorphType.class);
  private EditBox nbtInput;
  private Button confirmButton;
  private MorphSelectionWidget helpCloseWidget;

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
    this.previewFactory = new MorphPreviewFactory(returnScreen);
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
    buildInteractiveWidgets();

    nbtInput = createNbtInput();
    nbtInput.setTabOrderGroup(morphTypes.size() * 2);
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
    confirmButton.setTabOrderGroup(morphTypes.size() * 2 + 1);

    selectMorph(selectedMorph, false);
    focusedMorph = selectedMorph;
    helpOpen = false;
    updateWidgetVisibility();
    refreshSubmitState();
    setInitialFocus();
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
    syncInteractiveWidgetBounds();
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return returnScreen != null || helpOpen;
  }

  @Override
  public void onClose() {
    if (helpOpen) {
      closeHelp();
      return;
    }

    listScrollbarDragging = false;
    listScrollbarDragOffset = 0;

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

    if (event.button() == 0 && beginListScrollbarDrag(event.x(), event.y())) {
      return true;
    }

    if (event.button() == 0) {
      MorphType questionClicked = questionAt(event.x(), event.y());
      if (questionClicked != null) {
        openHelpForMorph(questionClicked);
        return true;
      }

      MorphType clicked = rowAt(event.x(), event.y());
      if (clicked != null) {
        selectMorph(clicked, true);
        return true;
      }
    }

    if (mouseInFooterInput(event.x(), event.y()) && nbtInput != null) {
      return nbtInput.mouseClicked(event, handled);
    }

    if (mouseInConfirmButton(event.x(), event.y()) && confirmButton != null) {
      return confirmButton.mouseClicked(event, handled);
    }

    return false;
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
    if (helpOpen) {
      return true;
    }

    if (listScrollbarDragging && event.button() == 0) {
      updateListScrollbarDrag(event.y());
      return true;
    }

    return super.mouseDragged(event, deltaX, deltaY);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    if (listScrollbarDragging && event.button() == 0) {
      listScrollbarDragging = false;
      listScrollbarDragOffset = 0;
      return true;
    }

    return super.mouseReleased(event);
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
        return true;
      }
      return super.keyPressed(event);
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

    updateWidgetVisibility();

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
    previewEntities.putAll(previewFactory.build(minecraft, morphTypes));
  }

  private void buildInteractiveWidgets() {
    rowWidgets.clear();
    questionWidgets.clear();

    for (int index = 0; index < morphTypes.size(); index++) {
      MorphType morph = morphTypes.get(index);
      MorphSelectionWidget rowWidget =
          addRenderableWidget(
              new MorphSelectionWidget(
                  MorphSelectionWidget.Kind.ROW,
                  morph,
                  morphName(morph),
                  this::selectCurrentMorph));
      rowWidget.setTabOrderGroup(index * 2);
      rowWidgets.put(morph, rowWidget);

      MorphSelectionWidget questionWidget =
          addRenderableWidget(
              new MorphSelectionWidget(
                  MorphSelectionWidget.Kind.QUESTION,
                  morph,
                  Component.translatable("mob_life.world_select.details"),
                  this::openHelpForMorph));
      questionWidget.setTabOrderGroup(index * 2 + 1);
      questionWidgets.put(morph, questionWidget);
    }

    helpCloseWidget =
        addRenderableWidget(
            new MorphSelectionWidget(
                MorphSelectionWidget.Kind.CLOSE,
                null,
                Component.translatable("gui.close"),
                () -> closeHelp()));
    helpCloseWidget.setTabOrderGroup(0);
    syncInteractiveWidgetBounds();
  }

  private EditBox createNbtInput() {
    int inputWidth = footerInputWidth();
    int inputY = footerInputY();
    EditBox input =
        new MorphSelectionEditBox(
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
      focusMorphDetails(morph);
      focusMorphWidget(morph);
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

  private void selectCurrentMorph(MorphType morph) {
    selectMorph(morph, true);
  }

  private void openHelpForMorph(MorphType morph) {
    focusMorphDetails(morph);
    listScrollbarDragging = false;
    listScrollbarDragOffset = 0;
    helpOpen = true;
    updateWidgetVisibility();
    refreshSubmitState();
    focusHelpCloseButton();
  }

  private void closeHelp() {
    helpOpen = false;
    detailScroll = 0;
    listScrollbarDragging = false;
    listScrollbarDragOffset = 0;
    updateWidgetVisibility();
    refreshSubmitState();
    focusSelectedMorphWidget();
  }

  private void focusMorphDetails(MorphType morph) {
    focusedMorph = morph;
    detailScroll = 0;
    ensureMorphVisible(morph);
  }

  private void focusMorphWidget(MorphType morph) {
    if (helpOpen) {
      return;
    }

    MorphSelectionWidget widget = rowWidgets.get(morph);
    if (widget != null) {
      setFocused(widget);
    }
  }

  private void focusSelectedMorphWidget() {
    focusMorphWidget(selectedMorph);
  }

  private void focusHelpCloseButton() {
    if (helpOpen && helpCloseWidget != null) {
      setFocused(helpCloseWidget);
    }
  }

  private void onMorphWidgetFocusGained(MorphType morph) {
    focusedMorph = morph;
    detailScroll = 0;
    ensureMorphVisible(morph);
  }

  private void ensureMorphVisible(MorphType morph) {
    if (helpOpen) {
      return;
    }

    int index = morphTypes.indexOf(morph);
    if (index < 0) {
      return;
    }

    int viewportHeight = Math.max(0, listHeight - 32);
    int contentTop = index * (LIST_ROW_HEIGHT + LIST_ROW_GAP);
    int contentBottom = contentTop + LIST_ROW_HEIGHT;
    int newScroll = listScroll;
    if (contentTop < listScroll) {
      newScroll = contentTop;
    } else if (contentBottom > listScroll + viewportHeight) {
      newScroll = contentBottom - viewportHeight;
    }

    setListScroll(newScroll);
  }

  private void setListScroll(int scroll) {
    int clamped = Math.clamp(scroll, 0, listScrollRange());
    if (clamped != listScroll) {
      listScroll = clamped;
      syncInteractiveWidgetBounds();
    }
  }

  private void updateWidgetVisibility() {
    syncInteractiveWidgetBounds();

    boolean interactiveVisible = !helpOpen;
    for (MorphSelectionWidget widget : rowWidgets.values()) {
      widget.visible = interactiveVisible;
      widget.active = interactiveVisible;
    }
    for (MorphSelectionWidget widget : questionWidgets.values()) {
      widget.visible = interactiveVisible;
      widget.active = interactiveVisible;
    }

    if (nbtInput != null) {
      nbtInput.visible = interactiveVisible;
      nbtInput.active = interactiveVisible;
    }

    if (confirmButton != null) {
      confirmButton.visible = interactiveVisible;
    }

    if (helpCloseWidget != null) {
      helpCloseWidget.visible = helpOpen;
      helpCloseWidget.active = helpOpen;
    }
  }

  private void syncInteractiveWidgetBounds() {
    int maxScroll = listScrollRange();
    listScroll = Math.clamp(listScroll, 0, maxScroll);

    int innerX = listX + PANEL_PADDING;
    int innerY = listY + 24;
    int innerWidth = listWidth - PANEL_PADDING * 2;
    int visibleWidth = innerWidth - (maxScroll > 0 ? LIST_SCROLLBAR_WIDTH + 4 : 0);
    int questionX = innerX + visibleWidth - LIST_QUESTION_SIZE - 6;
    int rowWidth = Math.max(1, questionX - innerX - 4);

    int y = innerY - listScroll;
    for (MorphType morph : morphTypes) {
      MorphSelectionWidget rowWidget = rowWidgets.get(morph);
      if (rowWidget != null) {
        rowWidget.setRectangle(innerX, y, rowWidth, LIST_ROW_HEIGHT);
      }

      MorphSelectionWidget questionWidget = questionWidgets.get(morph);
      if (questionWidget != null) {
        questionWidget.setRectangle(
            questionX,
            y + (LIST_ROW_HEIGHT - LIST_QUESTION_SIZE) / 2,
            LIST_QUESTION_SIZE,
            LIST_QUESTION_SIZE);
      }

      y += LIST_ROW_HEIGHT + LIST_ROW_GAP;
    }

    if (helpCloseWidget != null) {
      helpCloseWidget.setRectangle(
          detailCloseX(), detailCloseY(), DETAIL_CLOSE_SIZE, DETAIL_CLOSE_SIZE);
    }
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
      confirmButton.active = !helpOpen && (selectedMorph.isPlayer() || nbtValid);
    }
  }

  @Override
  protected void setInitialFocus() {
    if (helpOpen) {
      focusHelpCloseButton();
      if (getFocused() == helpCloseWidget) {
        return;
      }
    } else {
      focusSelectedMorphWidget();
      if (getFocused() == rowWidgets.get(selectedMorph)) {
        return;
      }
    }

    if (nbtInput != null && nbtInput.active) {
      setFocused(nbtInput);
      return;
    }

    if (confirmButton != null && confirmButton.active) {
      setFocused(confirmButton);
      return;
    }

    super.setInitialFocus();
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
      MorphDefinition worldSelection = selection;
      if (returnScreen instanceof CreateWorldScreen createWorldScreen) {
        worldSelection =
            InitialWorldVariantSelector.randomize(
                createWorldScreen.getUiState().getSettings().worldgenLoadContext(), selection);
      }
      PendingWorldSelection.setForNextWorld(worldSelection, true);
      if (returnScreen instanceof CreateWorldScreen createWorldScreen
          && returnScreen instanceof CreateWorldScreenInvoker invoker) {
        createWorldRequested = true;
        if (shouldPrepareWorldSeed(createWorldScreen, worldSelection) && minecraft != null) {
          minecraft.setScreen(new MorphWorldPreparationScreen(createWorldScreen, worldSelection));
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
    setListScroll(listScroll);
    maxScroll = listScrollRange();

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
    MorphSelectionWidget rowWidget = rowWidgets.get(morph);
    boolean rowFocused = rowWidget != null && rowWidget.isFocused();
    boolean hovered =
        mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + LIST_ROW_HEIGHT;
    boolean questionHovered = isQuestionHovered(morph, mouseX, mouseY);
    MorphSelectionWidget questionWidget = questionWidgets.get(morph);
    boolean questionFocused = questionWidget != null && questionWidget.isFocused();

    int background = selected ? SELECTED_BG : hovered ? HOVER_BG : 0x141A2230;
    int border = selected ? ACCENT : rowFocused ? INFO : hovered ? 0xFF4B5A6B : PANEL_EDGE_SOFT;

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
    int questionBorder = questionFocused ? INFO : WARN;
    int questionBackground = questionFocused ? HOVER_BG : questionHovered ? WARN : QUESTION_BG;
    int questionTextColor = questionFocused ? INFO : questionHovered ? 0xFF241700 : WARN;
    graphics.fill(
        questionX,
        questionY,
        questionX + LIST_QUESTION_SIZE,
        questionY + LIST_QUESTION_SIZE,
        questionBackground);
    graphics.fill(
        questionX, questionY, questionX + LIST_QUESTION_SIZE, questionY + 1, questionBorder);
    graphics.fill(
        questionX,
        questionY + LIST_QUESTION_SIZE - 1,
        questionX + LIST_QUESTION_SIZE,
        questionY + LIST_QUESTION_SIZE,
        questionBorder);
    graphics.fill(
        questionX, questionY, questionX + 1, questionY + LIST_QUESTION_SIZE, questionBorder);
    graphics.fill(
        questionX + LIST_QUESTION_SIZE - 1,
        questionY,
        questionX + LIST_QUESTION_SIZE,
        questionY + LIST_QUESTION_SIZE,
        questionBorder);
    graphics.centeredText(
        font,
        Component.literal("?"),
        questionX + LIST_QUESTION_SIZE / 2,
        questionY + 4,
        questionTextColor);
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
    boolean focused = helpCloseWidget != null && helpCloseWidget.isFocused();
    boolean hovered = focused || mouseInHelpClose(mouseX, mouseY);
    int border = focused ? INFO : hovered ? 0xFFE06B6B : WARN;
    int background = focused ? HOVER_BG : hovered ? 0x44E06B6B : QUESTION_BG;
    int textColor = focused ? INFO : border;
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
        font, Component.literal("X"), closeX + DETAIL_CLOSE_SIZE / 2, closeY + 4, textColor);
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

    List<MorphSelectionDetails.RenderedLine> lines =
        MorphSelectionDetails.buildWrappedLines(focusedMorph, font, textWidth, ACCENT, BODY_TEXT);
    int contentHeight = MorphSelectionDetails.measureHeight(lines, DETAIL_LINE_HEIGHT);
    int maxScroll = Math.max(0, contentHeight - viewportHeight);
    detailScroll = Math.clamp(detailScroll, 0, maxScroll);

    int visibleWidth = textWidth;
    graphics.enableScissor(textX, textTop, textX + visibleWidth, textTop + viewportHeight);

    int y = textTop - detailScroll;
    for (MorphSelectionDetails.RenderedLine line : lines) {
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
        MorphSelectionDetails.measureHeight(
                MorphSelectionDetails.buildWrappedLines(
                    focusedMorph, font, detailTextWidth(), ACCENT, BODY_TEXT),
                DETAIL_LINE_HEIGHT)
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

  private Component morphName(MorphType morph) {
    return MorphSelectionDetails.morphName(morph);
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

  private boolean mouseInFooterInput(double mouseX, double mouseY) {
    return mouseX >= footerInputX()
        && mouseX < footerInputX() + footerInputWidth()
        && mouseY >= footerInputY()
        && mouseY < footerInputY() + DETAIL_INPUT_HEIGHT;
  }

  private boolean mouseInConfirmButton(double mouseX, double mouseY) {
    return mouseX >= confirmX
        && mouseX < confirmX + confirmWidth
        && mouseY >= confirmY
        && mouseY < confirmY + 20;
  }

  private boolean beginListScrollbarDrag(double mouseX, double mouseY) {
    if (listScrollRange() <= 0) {
      return false;
    }

    int scrollbarX = listScrollbarX();
    int scrollbarY = listScrollbarY();
    int scrollbarHeight = listViewportHeight();
    if (mouseX < scrollbarX
        || mouseX >= scrollbarX + LIST_SCROLLBAR_WIDTH
        || mouseY < scrollbarY
        || mouseY >= scrollbarY + scrollbarHeight) {
      return false;
    }

    listScrollbarDragging = true;
    int thumbY = listScrollbarThumbY();
    int thumbHeight = listScrollbarThumbHeight();
    if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
      listScrollbarDragOffset = (int) mouseY - thumbY;
    } else {
      listScrollbarDragOffset = thumbHeight / 2;
      updateListScrollbarDrag(mouseY);
    }
    return true;
  }

  private void updateListScrollbarDrag(double mouseY) {
    int maxScroll = listScrollRange();
    int scrollbarY = listScrollbarY();
    int scrollbarHeight = listViewportHeight();
    int thumbHeight = listScrollbarThumbHeight();
    int thumbTravel = Math.max(0, scrollbarHeight - thumbHeight);
    if (maxScroll <= 0 || thumbTravel <= 0) {
      setListScroll(0);
      return;
    }

    int desiredThumbY =
        Math.clamp((int) mouseY - listScrollbarDragOffset, scrollbarY, scrollbarY + thumbTravel);
    int newScroll = Math.round(((desiredThumbY - scrollbarY) / (float) thumbTravel) * maxScroll);
    setListScroll(newScroll);
  }

  private int listScrollRange() {
    int viewportHeight = Math.max(0, listHeight - 32);
    int contentHeight = morphTypes.size() * (LIST_ROW_HEIGHT + LIST_ROW_GAP) - LIST_ROW_GAP;
    return Math.max(0, contentHeight - viewportHeight);
  }

  private int listInnerX() {
    return listX + PANEL_PADDING;
  }

  private int listInnerY() {
    return listY + 24;
  }

  private int listInnerWidth() {
    return listWidth - PANEL_PADDING * 2;
  }

  private int listViewportHeight() {
    return Math.max(0, listHeight - 32);
  }

  private int listScrollbarY() {
    return listInnerY();
  }

  private int listScrollbarX() {
    return listInnerX() + listVisibleWidth() + 4;
  }

  private int listVisibleWidth() {
    return listInnerWidth() - (listScrollRange() > 0 ? LIST_SCROLLBAR_WIDTH + 4 : 0);
  }

  private int listScrollbarThumbHeight() {
    int viewportHeight = listViewportHeight();
    int maxScroll = listScrollRange();
    if (maxScroll <= 0 || viewportHeight <= 0) {
      return 0;
    }

    return Math.max(
        18, (int) ((viewportHeight / (float) (viewportHeight + maxScroll)) * viewportHeight));
  }

  private int listScrollbarThumbY() {
    int scrollbarY = listScrollbarY();
    int viewportHeight = listViewportHeight();
    int maxScroll = listScrollRange();
    int thumbHeight = listScrollbarThumbHeight();
    if (maxScroll <= 0 || thumbHeight <= 0) {
      return scrollbarY;
    }

    int thumbTravel = Math.max(0, viewportHeight - thumbHeight);
    if (thumbTravel <= 0) {
      return scrollbarY;
    }

    return scrollbarY + (int) ((listScroll / (float) maxScroll) * thumbTravel);
  }

  private boolean isQuestionHovered(MorphType morph, int mouseX, int mouseY) {
    int innerX = listInnerX();
    int innerY = listInnerY();
    int visibleWidth = listVisibleWidth();
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
    if (maxScroll <= 0 || viewportHeight <= 0) {
      return;
    }

    graphics.fill(x, y, x + width, y + viewportHeight, 0x66212A36);
    int thumbHeight =
        Math.max(
            18, (int) ((viewportHeight / (float) (viewportHeight + maxScroll)) * viewportHeight));
    int thumbTravel = Math.max(0, viewportHeight - thumbHeight);
    int thumbY = y + (thumbTravel <= 0 ? 0 : (int) ((scroll / (float) maxScroll) * thumbTravel));
    graphics.fill(x, thumbY, x + width, thumbY + thumbHeight, ACCENT);
  }

  private final class MorphSelectionWidget extends AbstractWidget {
    private final Kind kind;
    private final MorphType morph;
    private final Consumer<MorphType> morphAction;
    private final Runnable action;

    private MorphSelectionWidget(
        Kind kind, MorphType morph, Component message, Consumer<MorphType> morphAction) {
      super(0, 0, 1, 1, message);
      this.kind = kind;
      this.morph = morph;
      this.morphAction = morphAction;
      this.action = null;
    }

    private MorphSelectionWidget(Kind kind, MorphType morph, Component message, Runnable action) {
      super(0, 0, 1, 1, message);
      this.kind = kind;
      this.morph = morph;
      this.morphAction = null;
      this.action = action;
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
      return kind == Kind.ROW || kind == Kind.CLOSE;
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      if (event instanceof FocusNavigationEvent.ArrowNavigation arrow) {
        return switch (kind) {
          case ROW ->
              switch (arrow.direction()) {
                case UP -> focusPath(rowWidgets.get(neighborMorph(morph, -1)));
                case DOWN -> focusPath(rowWidgets.get(neighborMorph(morph, 1)));
                case RIGHT -> focusPath(questionWidgets.get(morph));
                default -> null;
              };
          case QUESTION ->
              switch (arrow.direction()) {
                case UP -> focusPath(questionWidgets.get(neighborMorph(morph, -1)));
                case DOWN -> focusPath(questionWidgets.get(neighborMorph(morph, 1)));
                case LEFT -> focusPath(rowWidgets.get(morph));
                case RIGHT -> focusPath(nbtInput);
                default -> null;
              };
          case CLOSE -> null;
        };
      }

      return super.nextFocusPath(event);
    }

    @Override
    public void setFocused(boolean focused) {
      super.setFocused(focused);
      if (focused && morph != null && kind != Kind.CLOSE) {
        onMorphWidgetFocusGained(morph);
      }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
      if (!isActive() || !isActivationKey(event)) {
        return false;
      }

      playDownSound(MorphSelectionScreen.this.minecraft.getSoundManager());
      activate();
      return true;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean handled) {
      activate();
    }

    @Override
    protected void extractWidgetRenderState(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
      defaultButtonNarrationText(output);
    }

    private boolean isActivationKey(KeyEvent event) {
      int key = event.key();
      return key == GLFW.GLFW_KEY_ENTER
          || key == GLFW.GLFW_KEY_KP_ENTER
          || key == GLFW.GLFW_KEY_SPACE;
    }

    private void activate() {
      if (morphAction != null && morph != null) {
        morphAction.accept(morph);
      } else if (action != null) {
        action.run();
      }
    }

    private enum Kind {
      ROW,
      QUESTION,
      CLOSE
    }
  }

  private final class MorphSelectionEditBox extends EditBox {
    private MorphSelectionEditBox(int x, int y, int width, int height, Component message) {
      super(MorphSelectionScreen.this.font, x, y, width, height, message);
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      if (event instanceof FocusNavigationEvent.ArrowNavigation arrow) {
        return switch (arrow.direction()) {
          case LEFT -> focusPath(questionWidgets.get(selectedMorph));
          case RIGHT -> focusPath(confirmButton);
          default -> null;
        };
      }

      return super.nextFocusPath(event);
    }
  }

  private ComponentPath focusPath(GuiEventListener listener) {
    if (listener instanceof AbstractWidget widget && !widget.isActive()) {
      return null;
    }

    return listener == null ? null : ComponentPath.leaf(listener);
  }

  private MorphType neighborMorph(MorphType morph, int offset) {
    int index = morphTypes.indexOf(morph);
    if (index < 0) {
      return morph;
    }

    int neighborIndex = index + offset;
    if (neighborIndex < 0 || neighborIndex >= morphTypes.size()) {
      return morph;
    }

    return morphTypes.get(neighborIndex);
  }
}
