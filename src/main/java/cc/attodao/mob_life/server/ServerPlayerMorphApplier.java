package cc.attodao.mob_life.server;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.config.MobLifeConfig;
import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.combat.MorphAttackDamage;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.inventory.MorphEquipment;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
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

  private static final Identifier SPEED_MODIFIER_ID = MobLife.id("morph_speed");
  private static final Identifier SNEAKING_SPEED_MODIFIER_ID = MobLife.id("morph_sneaking_speed");
  private static final Identifier MAX_HEALTH_MODIFIER_ID = MobLife.id("morph_max_health");
  private static final Identifier BLOCK_BREAK_SPEED_MODIFIER_ID =
      MobLife.id("morph_block_break_speed");
  private static final Identifier STEP_HEIGHT_MODIFIER_ID = MobLife.id("morph_step_height");
  private static final Identifier JUMP_STRENGTH_MODIFIER_ID = MobLife.id("morph_jump_strength");
  private static final Identifier SAFE_FALL_DISTANCE_MODIFIER_ID =
      MobLife.id("morph_safe_fall_distance");
  private static final Identifier FALL_DAMAGE_MODIFIER_ID =
      MobLife.id("morph_fall_damage_multiplier");
  private static final Identifier BLOCK_REACH_MODIFIER_ID = MobLife.id("morph_block_reach");
  private static final Identifier ENTITY_REACH_MODIFIER_ID = MobLife.id("morph_entity_reach");
  private static final Identifier ATTACK_DAMAGE_MODIFIER_ID = MobLife.id("morph_attack_damage");
  private static final float PLAYER_HEIGHT = EntityTypes.PLAYER.getDimensions().height();

  private ServerPlayerMorphApplier() {}

  static void apply(ServerPlayer player, MorphDefinition definition, boolean preserveHealthRatio) {
    MorphType morph = definition.type();

    if (morph.isPlayer()) {
      MorphInventoryCapacity.apply(player, morph);
      MorphFoodCapacity.apply(player, morph);
      restorePlayerAttributes(player, preserveHealthRatio);
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
        player,
        new MobLifeNetworking.MorphSelectionPayload(
            morph.id(), definition.nbt(), MorphConfigManager.encode(morph)));
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
        SPEED_MODIFIER_ID,
        MorphMovementSpeed.walkingSpeed(
            definition.type(), livingMorph.getAttributeValue(Attributes.MOVEMENT_SPEED)));
    MorphConfig.Movement movement = MorphConfigManager.get(definition.type()).movement();
    setAttributeValue(
        player, Attributes.SNEAKING_SPEED, SNEAKING_SPEED_MODIFIER_ID, sneakingSpeed(movement));
    setAttributeValue(
        player,
        Attributes.ATTACK_DAMAGE,
        ATTACK_DAMAGE_MODIFIER_ID,
        MorphAttackDamage.fromMorph(definition.type(), livingMorph));
    applyMaxHealth(player, livingMorph, definition, preserveHealthRatio);
    applyBlockBreakSpeed(player, definition.type(), dimensions.height());
    applyInteractionRanges(player, definition.type(), dimensions.height());
    applyMobMovementAttributes(player, livingMorph, definition.type(), dimensions.height());
  }

  private static void restorePlayerAttributes(ServerPlayer player, boolean preserveHealthRatio) {
    float oldHealth = player.getHealth();
    float oldMaxHealth = player.getMaxHealth();
    removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID);
    removeModifier(player, Attributes.SNEAKING_SPEED, SNEAKING_SPEED_MODIFIER_ID);
    removeModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_MODIFIER_ID);
    removeModifier(player, Attributes.BLOCK_BREAK_SPEED, BLOCK_BREAK_SPEED_MODIFIER_ID);
    removeModifier(player, Attributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER_ID);
    removeModifier(player, Attributes.JUMP_STRENGTH, JUMP_STRENGTH_MODIFIER_ID);
    removeModifier(player, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_DISTANCE_MODIFIER_ID);
    removeModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE_MODIFIER_ID);
    removeModifier(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH_MODIFIER_ID);
    removeModifier(player, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH_MODIFIER_ID);
    removeModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_MODIFIER_ID);
    updateHealth(player, oldHealth, oldMaxHealth, preserveHealthRatio);
  }

  private static void applyMobMovementAttributes(
      ServerPlayer player, LivingEntity livingMorph, MorphType morph, float morphHeight) {
    double stepHeight =
        morph.isEquine()
            ? livingMorph.getAttributeValue(Attributes.STEP_HEIGHT)
            : Math.clamp(0.6 * morphHeight / PLAYER_HEIGHT, 0.1, 1.5);
    setAttributeValue(player, Attributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER_ID, stepHeight);
    setAttributeValue(
        player,
        Attributes.SAFE_FALL_DISTANCE,
        SAFE_FALL_DISTANCE_MODIFIER_ID,
        livingMorph.getAttributeValue(Attributes.SAFE_FALL_DISTANCE));
    setAttributeValue(
        player,
        Attributes.FALL_DAMAGE_MULTIPLIER,
        FALL_DAMAGE_MODIFIER_ID,
        livingMorph.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
    if (morph.isEquine()) {
      setAttributeValue(
          player,
          Attributes.JUMP_STRENGTH,
          JUMP_STRENGTH_MODIFIER_ID,
          livingMorph.getAttributeValue(Attributes.JUMP_STRENGTH));
    } else {
      removeModifier(player, Attributes.JUMP_STRENGTH, JUMP_STRENGTH_MODIFIER_ID);
    }
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
    setAttributeValue(player, Attributes.MAX_HEALTH, MAX_HEALTH_MODIFIER_ID, targetMaxHealth);
    updateHealth(player, oldHealth, oldMaxHealth, preserveHealthRatio);
  }

  private static void applyBlockBreakSpeed(
      ServerPlayer player, MorphType morph, float morphHeight) {
    if (!MobLifeConfig.miningSpeedChangeEnabled()) {
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
            BLOCK_BREAK_SPEED_MODIFIER_ID,
            multiplier - 1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
  }

  private static void applyInteractionRanges(
      ServerPlayer player, MorphType morph, float morphHeight) {
    if (!MobLifeConfig.reachChangeEnabled()) {
      return;
    }
    double heightScale = morphHeight / PLAYER_HEIGHT;
    setScaledBaseAttribute(
        player,
        Attributes.BLOCK_INTERACTION_RANGE,
        BLOCK_REACH_MODIFIER_ID,
        heightScale * MorphConfigManager.get(morph).attributes().blockReachScale());
    setScaledBaseAttribute(
        player,
        Attributes.ENTITY_INTERACTION_RANGE,
        ENTITY_REACH_MODIFIER_ID,
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

  private static double sneakingSpeed(MorphConfig.Movement movement) {
    if (movement.walkSpeed() <= 0.0) {
      return 0.0;
    }
    return Math.clamp(movement.sneakSpeed() / movement.walkSpeed(), 0.0, 1.0);
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

  private static void removeModifier(
      ServerPlayer player, Holder<Attribute> attributeType, Identifier modifierId) {
    AttributeInstance attribute = player.getAttribute(attributeType);
    if (attribute != null) {
      attribute.removeModifier(modifierId);
    }
  }

  private static void refreshDimensions(ServerPlayer player) {
    player.refreshDimensions();
    player.setBoundingBox(
        player.getDimensions(player.getPose()).makeBoundingBox(player.position()));
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
