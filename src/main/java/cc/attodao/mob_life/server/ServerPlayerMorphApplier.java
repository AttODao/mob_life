package cc.attodao.mob_life.server;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

final class ServerPlayerMorphApplier {
	private static final Identifier SPEED_MODIFIER_ID =
			MobLife.id("morph_speed");
	private static final Identifier MAX_HEALTH_MODIFIER_ID =
			MobLife.id("morph_max_health");
	private static final Identifier BLOCK_BREAK_SPEED_MODIFIER_ID =
			MobLife.id("morph_block_break_speed");
	private static final double PLAYER_MOVEMENT_SCALE = 0.25;
	private static final float PLAYER_HEIGHT =
			EntityType.PLAYER.getDimensions().height();

	private ServerPlayerMorphApplier() {
	}

	static void apply(
			ServerPlayer player,
			MorphType morph,
			boolean preserveHealthRatio
	) {
		MorphInventoryCapacity.apply(player, morph);
		MorphFoodCapacity.apply(player, morph);

		if (morph.isPlayer()) {
			restorePlayerAttributes(player, preserveHealthRatio);
		} else {
			applyMobAttributes(player, morph, preserveHealthRatio);
		}

		refreshDimensions(player);
		syncInventory(player);
		ServerPlayNetworking.send(
				player,
				new MobLifeNetworking.MorphSelectionPayload(morph.id())
		);
	}

	private static void applyMobAttributes(
			ServerPlayer player,
			MorphType morph,
			boolean preserveHealthRatio
	) {
		Entity entity = morph.entityType().create(
				player.level(),
				EntitySpawnReason.LOAD
		);
		if (!(entity instanceof LivingEntity livingMorph)) {
			return;
		}

		setAttributeValue(
				player,
				Attributes.MOVEMENT_SPEED,
				SPEED_MODIFIER_ID,
				livingMorph.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)
						* PLAYER_MOVEMENT_SCALE
		);
		applyMaxHealth(player, livingMorph, preserveHealthRatio);
		applyBlockBreakSpeed(player, morph);
	}

	private static void restorePlayerAttributes(
			ServerPlayer player,
			boolean preserveHealthRatio
	) {
		float oldHealth = player.getHealth();
		float oldMaxHealth = player.getMaxHealth();
		removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID);
		removeModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_MODIFIER_ID);
		removeModifier(
				player,
				Attributes.BLOCK_BREAK_SPEED,
				BLOCK_BREAK_SPEED_MODIFIER_ID
		);
		updateHealth(
				player,
				oldHealth,
				oldMaxHealth,
				preserveHealthRatio
		);
	}

	private static void applyMaxHealth(
			ServerPlayer player,
			LivingEntity livingMorph,
			boolean preserveHealthRatio
	) {
		float oldHealth = player.getHealth();
		float oldMaxHealth = player.getMaxHealth();
		setAttributeValue(
				player,
				Attributes.MAX_HEALTH,
				MAX_HEALTH_MODIFIER_ID,
				livingMorph.getAttributeBaseValue(Attributes.MAX_HEALTH)
		);
		updateHealth(
				player,
				oldHealth,
				oldMaxHealth,
				preserveHealthRatio
		);
	}

	private static void applyBlockBreakSpeed(
			ServerPlayer player,
			MorphType morph
	) {
		AttributeInstance attribute = player.getAttribute(
				Attributes.BLOCK_BREAK_SPEED
		);
		if (attribute == null) {
			return;
		}

		double multiplier = Math.clamp(
				morph.entityType().getDimensions().height() / PLAYER_HEIGHT,
				0.5,
				1.0
		);
		attribute.addOrUpdateTransientModifier(
				new AttributeModifier(
						BLOCK_BREAK_SPEED_MODIFIER_ID,
						multiplier - 1.0,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
				)
		);
	}

	private static void setAttributeValue(
			ServerPlayer player,
			Holder<Attribute> attributeType,
			Identifier modifierId,
			double targetValue
	) {
		AttributeInstance attribute = player.getAttribute(attributeType);
		if (attribute == null) {
			return;
		}
		attribute.addOrUpdateTransientModifier(
				new AttributeModifier(
						modifierId,
						targetValue - attribute.getBaseValue(),
						AttributeModifier.Operation.ADD_VALUE
				)
		);
	}

	private static void updateHealth(
			ServerPlayer player,
			float oldHealth,
			float oldMaxHealth,
			boolean preserveHealthRatio
	) {
		float newMaxHealth = player.getMaxHealth();
		float newHealth = preserveHealthRatio && oldMaxHealth > 0.0F
				? newMaxHealth * oldHealth / oldMaxHealth
				: Math.min(oldHealth, newMaxHealth);
		player.setHealth(newHealth);
	}

	private static void removeModifier(
			ServerPlayer player,
			Holder<Attribute> attributeType,
			Identifier modifierId
	) {
		AttributeInstance attribute = player.getAttribute(attributeType);
		if (attribute != null) {
			attribute.removeModifier(modifierId);
		}
	}

	private static void refreshDimensions(ServerPlayer player) {
		player.refreshDimensions();
		player.setBoundingBox(
				player.getDimensions(player.getPose())
						.makeBoundingBox(player.position())
		);
	}

	private static void syncInventory(ServerPlayer player) {
		player.inventoryMenu.sendAllDataToRemote();
		if (player.containerMenu != player.inventoryMenu) {
			player.containerMenu.sendAllDataToRemote();
		}
	}
}
