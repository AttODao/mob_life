package cc.attodao.mob_life.client.screen;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.morph.MorphType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class MorphSelectionDetails {
  private MorphSelectionDetails() {}

  static Component morphName(MorphType morph) {
    return Component.translatable(morph.translationKey());
  }

  static List<RenderedLine> buildWrappedLines(
      MorphType morph, Font font, int textWidth, int headerColor, int bodyColor) {
    ArrayList<RenderedLine> lines = new ArrayList<>();
    for (DetailLine line : buildLines(morph, headerColor, bodyColor)) {
      int availableWidth = Math.max(1, textWidth - line.indent());
      List<FormattedCharSequence> wrapped = font.split(line.text(), availableWidth);
      if (wrapped.isEmpty()) {
        continue;
      }

      boolean first = true;
      for (FormattedCharSequence sequence : wrapped) {
        lines.add(
            new RenderedLine(sequence, line.color(), line.indent(), first ? line.topPadding() : 0));
        first = false;
      }
    }
    return lines;
  }

  static int measureHeight(List<RenderedLine> lines, int lineHeight) {
    int height = 0;
    for (RenderedLine line : lines) {
      height += line.topPadding() + lineHeight;
    }
    return height;
  }

  private static List<DetailLine> buildLines(MorphType morph, int headerColor, int bodyColor) {
    MorphConfig config = MorphConfigManager.get(morph);
    ArrayList<DetailLine> lines = new ArrayList<>();

    addSectionHeader(lines, headerColor, "mob_life.world_select.section.movement");
    addBody(
        lines,
        bodyColor,
        Component.translatable(
            "mob_life.world_select.movement.charged_jump", yesNo(config.movement().chargedJump())));
    addBody(
        lines,
        bodyColor,
        Component.translatable(
            "mob_life.world_select.movement.slow_fall",
            formatNumber(config.movement().slowFallMultiplier())));
    if (config.movement().rabbitHop().enabled()) {
      addBody(
          lines, bodyColor, Component.translatable("mob_life.world_select.movement.rabbit_hop"));
    }

    addSectionHeader(lines, headerColor, "mob_life.world_select.section.attack");
    addBody(
        lines,
        bodyColor,
        Component.translatable(
            "mob_life.world_select.attack.damage", formatNumber(config.combat().attackDamage())));
    if (config.combat().leapAttack().verticalSpeed() > 0.0) {
      addBody(lines, bodyColor, Component.translatable("mob_life.world_select.attack.leap"));
    }

    addSectionHeader(lines, headerColor, "mob_life.world_select.section.predators");
    addBody(lines, bodyColor, entityDisplayText(config.combat().predators()));

    addSectionHeader(lines, headerColor, "mob_life.world_select.section.foods");
    ArrayList<String> foodEntries = new ArrayList<>(config.diet().foods());
    foodEntries.addAll(config.diet().huntedFoods());
    addBody(lines, bodyColor, foodDisplayText(foodEntries));

    addSectionHeader(lines, headerColor, "mob_life.world_select.section.sleep");
    addBody(
        lines,
        bodyColor,
        Component.translatable(
            "mob_life.world_select.sleep.schedule", sleepScheduleLabel(config.sleep().schedule())));
    addBody(
        lines,
        bodyColor,
        Component.translatable(
            "mob_life.world_select.sleep.without_bed", yesNo(config.sleep().withoutBed())));

    addSectionHeader(lines, headerColor, "mob_life.world_select.section.ability");
    addBody(lines, bodyColor, Component.translatable(abilityKey(config.abilities().value())));

    return lines;
  }

  private static void addSectionHeader(List<DetailLine> lines, int color, String key) {
    int padding = lines.isEmpty() ? 0 : 6;
    lines.add(new DetailLine(Component.translatable(key), color, 0, padding));
  }

  private static void addBody(List<DetailLine> lines, int color, Component text) {
    lines.add(new DetailLine(text, color, 10, 0));
  }

  private static Component foodDisplayText(List<String> entries) {
    return registryDisplayText(entries, MorphSelectionDetails::addFoodDisplayName);
  }

  private static Component entityDisplayText(List<String> entries) {
    return registryDisplayText(entries, MorphSelectionDetails::addEntityDisplayName);
  }

  private static Component registryDisplayText(
      List<String> entries, EntryDisplayCollector collector) {
    ArrayList<String> display = new ArrayList<>();
    LinkedHashSet<Identifier> seen = new LinkedHashSet<>();
    for (String entry : entries) {
      collector.add(entry, seen, display);
    }
    return display.isEmpty() ? noneValue() : Component.literal(String.join("、", display));
  }

  private static void addFoodDisplayName(
      String entry, LinkedHashSet<Identifier> seen, List<String> display) {
    if (entry.startsWith("#")) {
      Identifier tagId = Identifier.tryParse(entry.substring(1));
      if (tagId == null) {
        return;
      }

      TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
      for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
        Item item = holder.value();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId != null && seen.add(itemId)) {
          display.add(new ItemStack(item).getHoverName().getString());
        }
      }
      return;
    }

    Identifier itemId = Identifier.tryParse(entry);
    if (itemId == null) {
      return;
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

  private static void addEntityDisplayName(
      String entry, LinkedHashSet<Identifier> seen, List<String> display) {
    if (entry.startsWith("#")) {
      Identifier tagId = Identifier.tryParse(entry.substring(1));
      if (tagId == null) {
        return;
      }

      TagKey<EntityType<?>> tag =
          TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, tagId);
      for (Holder<EntityType<?>> holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tag)) {
        EntityType<?> entityType = holder.value();
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId != null && seen.add(entityId)) {
          display.add(entityType.getDescription().getString());
        }
      }
      return;
    }

    Identifier entityId = Identifier.tryParse(entry);
    if (entityId == null) {
      return;
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

  private interface EntryDisplayCollector {
    void add(String entry, LinkedHashSet<Identifier> seen, List<String> display);
  }

  private static Component noneValue() {
    return Component.translatable("mob_life.world_select.value.none");
  }

  private static Component yesNo(boolean value) {
    return Component.translatable(
        value ? "mob_life.world_select.value.yes" : "mob_life.world_select.value.no");
  }

  private static Component sleepScheduleLabel(String value) {
    return Component.translatable("mob_life.world_select.sleep.schedule." + value);
  }

  private static String abilityKey(MorphConfig.Ability ability) {
    return "mob_life.world_select.ability." + ability.id();
  }

  private static String formatNumber(double value) {
    String text = String.format(Locale.ROOT, "%.2f", value);
    while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
      text = text.substring(0, text.length() - 1);
    }
    return text;
  }

  private record DetailLine(Component text, int color, int indent, int topPadding) {}

  record RenderedLine(FormattedCharSequence text, int color, int indent, int topPadding) {}
}
