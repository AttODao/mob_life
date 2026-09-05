package cc.attodao.mob_life.server;

import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.config.ServerMobLifeConfig;
import cc.attodao.mob_life.gameplay.combat.MorphAttackDamage;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.inventory.MorphEquipment;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.gameplay.movement.MorphAttributeModifiers;
import cc.attodao.mob_life.gameplay.movement.MorphMovementSpeed;
import cc.attodao.mob_life.morph.MorphBodyScale;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

final class ServerPlayerMorphApplier {
  private static final float PLAYER_HEIGHT = EntityTypes.PLAYER.getDimensions().height();

  private ServerPlayerMorphApplier() {}

  static void apply(ServerPlayer player, MorphDefinition definition, boolean preserveHealthRatio) {
    MorphType morph = definition.type();

    if (morph.isPlayer()) {
      MorphInventoryCapacity.apply(player, morph);
      MorphFoodCapacity.apply(player, morph);
      restorePlayerAttributes(player, preserveHealthRatio);
      player.setNoGravity(false);
    } else {
      applyMobAttributes(player, definition, preserveHealthRatio);
      moveDisabledCraftingItems(player);
    }

    refreshDimensions(player);
    MorphEquipment.removeUnsupportedEquipment(player);
    MorphMovementSpeed.refresh(player);
    ServerMorphManager.clearMorphNightVisionEffect(player);
    syncInventory(player);
    ServerPlayNetworking.send(
        player, new MobLifeNetworking.MorphSelectionPayload(morph.id(), definition.nbt()));
  }

  /** Reconciles server-controlled transient modifiers after a server config change. */
  static void refreshGameplayModifiers(ServerPlayer player, MorphType morph, float morphHeight) {
    applyBlockBreakSpeed(player, morph, morphHeight);
    applyInteractionRanges(player, morph, morphHeight);
  }

  private static void applyMobAttributes(
      ServerPlayer player, MorphDefinition definition, boolean preserveHealthRatio) {
    Entity entity = MorphEntityFactory.create(definition, player.level());
    if (!(entity instanceof LivingEntity livingMorph)) {
      return;
    }

    EntityDimensions dimensions = livingMorph.getDimensions(Pose.STANDING);
    MorphInventoryCapacity.apply(player, definition.type(), dimensions.height());
    MorphFoodCapacity.apply(player, definition.type(), dimensions.height());
    setAttributeValue(
        player,
        Attributes.MOVEMENT_SPEED,
        MorphAttributeModifiers.SPEED,
        livingMorph.getAttributeValue(Attributes.MOVEMENT_SPEED));
    setAttributeValue(
        player, Attributes.SNEAKING_SPEED, MorphAttributeModifiers.SNEAKING_SPEED, 1.0);
    setAttributeValue(
        player,
        Attributes.ATTACK_DAMAGE,
        MorphAttributeModifiers.ATTACK_DAMAGE,
        MorphAttackDamage.fromMorph(definition.type(), livingMorph));
    applyMaxHealth(player, livingMorph, definition, preserveHealthRatio);
    applyBlockBreakSpeed(player, definition.type(), dimensions.height());
    applyInteractionRanges(player, definition.type(), dimensions.height());
    applyMobMovementAttributes(player, livingMorph);
    player.setNoGravity(livingMorph.isNoGravity());
  }

  private static void restorePlayerAttributes(ServerPlayer player, boolean preserveHealthRatio) {
    float oldHealth = player.getHealth();
    float oldMaxHealth = player.getMaxHealth();
    MorphAttributeModifiers.removeAll(player);
    updateHealth(player, oldHealth, oldMaxHealth, preserveHealthRatio);
  }

  private static void applyMobMovementAttributes(ServerPlayer player, LivingEntity livingMorph) {
    setAttributeValue(
        player,
        Attributes.STEP_HEIGHT,
        MorphAttributeModifiers.STEP_HEIGHT,
        livingMorph.getAttributeValue(Attributes.STEP_HEIGHT));
    setAttributeValue(
        player,
        Attributes.GRAVITY,
        MorphAttributeModifiers.GRAVITY,
        livingMorph.getAttributeValue(Attributes.GRAVITY));
    copyAttribute(
        player,
        livingMorph,
        Attributes.FRICTION_MODIFIER,
        MorphAttributeModifiers.FRICTION_MODIFIER);
    copyAttribute(
        player,
        livingMorph,
        Attributes.AIR_DRAG_MODIFIER,
        MorphAttributeModifiers.AIR_DRAG_MODIFIER);
    copyAttribute(
        player,
        livingMorph,
        Attributes.MOVEMENT_EFFICIENCY,
        MorphAttributeModifiers.MOVEMENT_EFFICIENCY);
    copyAttribute(
        player,
        livingMorph,
        Attributes.WATER_MOVEMENT_EFFICIENCY,
        MorphAttributeModifiers.WATER_MOVEMENT_EFFICIENCY);
    copyAttribute(player, livingMorph, Attributes.BOUNCINESS, MorphAttributeModifiers.BOUNCINESS);
    setAttributeValue(
        player,
        Attributes.SAFE_FALL_DISTANCE,
        MorphAttributeModifiers.SAFE_FALL_DISTANCE,
        livingMorph.getAttributeValue(Attributes.SAFE_FALL_DISTANCE));
    setAttributeValue(
        player,
        Attributes.FALL_DAMAGE_MULTIPLIER,
        MorphAttributeModifiers.FALL_DAMAGE_MULTIPLIER,
        livingMorph.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
    setAttributeValue(
        player,
        Attributes.JUMP_STRENGTH,
        MorphAttributeModifiers.JUMP_STRENGTH,
        livingMorph.getAttributeValue(Attributes.JUMP_STRENGTH));
  }

  private static void copyAttribute(
      ServerPlayer player,
      LivingEntity source,
      Holder<Attribute> attribute,
      Identifier modifierId) {
    setAttributeValue(player, attribute, modifierId, source.getAttributeValue(attribute));
  }

  private static void applyMaxHealth(
      ServerPlayer player,
      LivingEntity livingMorph,
      MorphDefinition definition,
      boolean preserveHealthRatio) {
    float oldHealth = player.getHealth();
    float oldMaxHealth = player.getMaxHealth();
    double targetMaxHealth =
        definition.hasHealthOverride()
            ? Math.max(1.0, definition.nbt().getFloatOr("Health", livingMorph.getMaxHealth()))
            : livingMorph.getMaxHealth();
    setAttributeValue(
        player, Attributes.MAX_HEALTH, MorphAttributeModifiers.MAX_HEALTH, targetMaxHealth);
    updateHealth(player, oldHealth, oldMaxHealth, preserveHealthRatio);
  }

  private static void applyBlockBreakSpeed(
      ServerPlayer player, MorphType morph, float morphHeight) {
    MorphAttributeModifiers.remove(
        player, Attributes.BLOCK_BREAK_SPEED, MorphAttributeModifiers.BLOCK_BREAK_SPEED);
    if (!ServerMobLifeConfig.miningSpeedChangeEnabled()) {
      return;
    }
    AttributeInstance attribute = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
    if (attribute == null) {
      return;
    }

    double multiplier =
        MorphConfigManager.get(morph).attributes().miningSpeed()
            * MorphBodyScale.relativeTo(morphHeight, morph.entityType().getDimensions().height());
    multiplier = Math.max(0.05, multiplier);
    attribute.addOrUpdateTransientModifier(
        new AttributeModifier(
            MorphAttributeModifiers.BLOCK_BREAK_SPEED,
            multiplier - 1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
  }

  private static void applyInteractionRanges(
      ServerPlayer player, MorphType morph, float morphHeight) {
    MorphAttributeModifiers.remove(
        player, Attributes.BLOCK_INTERACTION_RANGE, MorphAttributeModifiers.BLOCK_REACH);
    MorphAttributeModifiers.remove(
        player, Attributes.ENTITY_INTERACTION_RANGE, MorphAttributeModifiers.ENTITY_REACH);
    if (!ServerMobLifeConfig.reachChangeEnabled()) {
      return;
    }
    double heightScale = morphHeight / PLAYER_HEIGHT;
    setScaledBaseAttribute(
        player,
        Attributes.BLOCK_INTERACTION_RANGE,
        MorphAttributeModifiers.BLOCK_REACH,
        heightScale * MorphConfigManager.get(morph).attributes().blockReachScale());
    setScaledBaseAttribute(
        player,
        Attributes.ENTITY_INTERACTION_RANGE,
        MorphAttributeModifiers.ENTITY_REACH,
        heightScale * MorphConfigManager.get(morph).attributes().entityReachScale());
  }

  private static void setScaledBaseAttribute(
      ServerPlayer player, Holder<Attribute> attributeType, Identifier modifierId, double scale) {
    AttributeInstance attribute = player.getAttribute(attributeType);
    if (attribute == null) {
      return;
    }
    setAttributeValue(player, attributeType, modifierId, attribute.getBaseValue() * scale);
  }

  private static void setAttributeValue(
      ServerPlayer player,
      Holder<Attribute> attributeType,
      Identifier modifierId,
      double targetValue) {
    AttributeInstance attribute = player.getAttribute(attributeType);
    if (attribute == null) {
      return;
    }
    attribute.addOrUpdateTransientModifier(
        new AttributeModifier(
            modifierId,
            targetValue - attribute.getBaseValue(),
            AttributeModifier.Operation.ADD_VALUE));
  }

  private static void updateHealth(
      ServerPlayer player, float oldHealth, float oldMaxHealth, boolean preserveHealthRatio) {
    float newMaxHealth = player.getMaxHealth();
    float newHealth =
        preserveHealthRatio && oldMaxHealth > 0.0F
            ? (newMaxHealth * oldHealth) / oldMaxHealth
            : Math.min(oldHealth, newMaxHealth);
    player.setHealth(newHealth);
  }

  private static void refreshDimensions(ServerPlayer player) {
    player.refreshDimensions();
  }

  private static void syncInventory(ServerPlayer player) {
    player.inventoryMenu.sendAllDataToRemote();
    if (player.containerMenu != player.inventoryMenu) {
      player.containerMenu.sendAllDataToRemote();
    }
  }

  private static void moveDisabledCraftingItems(ServerPlayer player) {
    var crafting = player.inventoryMenu.getCraftSlots();
    for (int slot = 1; slot < crafting.getContainerSize(); slot++) {
      ItemStack stack = crafting.removeItemNoUpdate(slot);
      if (!stack.isEmpty()) {
        player.getInventory().placeItemBackInInventory(stack);
      }
    }
    player.inventoryMenu.slotsChanged(crafting);
  }
}
