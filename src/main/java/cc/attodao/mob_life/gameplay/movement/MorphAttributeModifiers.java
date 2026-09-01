package cc.attodao.mob_life.gameplay.movement;

import cc.attodao.mob_life.MobLife;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** IDs and cleanup for every transient player attribute owned by Mob Life. */
public final class MorphAttributeModifiers {
  public static final Identifier SPEED = MobLife.id("morph_speed");
  public static final Identifier SPRINT_SPEED = MobLife.id("morph_sprint_speed");
  public static final Identifier SNEAKING_SPEED = MobLife.id("morph_sneaking_speed");
  public static final Identifier MAX_HEALTH = MobLife.id("morph_max_health");
  public static final Identifier BLOCK_BREAK_SPEED = MobLife.id("morph_block_break_speed");
  public static final Identifier STEP_HEIGHT = MobLife.id("morph_step_height");
  public static final Identifier GRAVITY = MobLife.id("morph_gravity");
  public static final Identifier JUMP_STRENGTH = MobLife.id("morph_jump_strength");
  public static final Identifier SAFE_FALL_DISTANCE = MobLife.id("morph_safe_fall_distance");
  public static final Identifier FALL_DAMAGE_MULTIPLIER =
      MobLife.id("morph_fall_damage_multiplier");
  public static final Identifier BLOCK_REACH = MobLife.id("morph_block_reach");
  public static final Identifier ENTITY_REACH = MobLife.id("morph_entity_reach");
  public static final Identifier ATTACK_DAMAGE = MobLife.id("morph_attack_damage");

  private MorphAttributeModifiers() {}

  public static void removeAll(Player player) {
    remove(player, Attributes.MOVEMENT_SPEED, SPEED);
    remove(player, Attributes.MOVEMENT_SPEED, SPRINT_SPEED);
    remove(player, Attributes.SNEAKING_SPEED, SNEAKING_SPEED);
    remove(player, Attributes.MAX_HEALTH, MAX_HEALTH);
    remove(player, Attributes.BLOCK_BREAK_SPEED, BLOCK_BREAK_SPEED);
    remove(player, Attributes.STEP_HEIGHT, STEP_HEIGHT);
    remove(player, Attributes.GRAVITY, GRAVITY);
    remove(player, Attributes.JUMP_STRENGTH, JUMP_STRENGTH);
    remove(player, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL_DISTANCE);
    remove(player, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE_MULTIPLIER);
    remove(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH);
    remove(player, Attributes.ENTITY_INTERACTION_RANGE, ENTITY_REACH);
    remove(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);
  }

  public static void remove(Player player, Holder<Attribute> attributeType, Identifier modifierId) {
    AttributeInstance attribute = player.getAttribute(attributeType);
    if (attribute != null) {
      attribute.removeModifier(modifierId);
    }
  }
}
