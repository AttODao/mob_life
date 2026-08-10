package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.gameplay.movement.MorphMovementSpeed;
import cc.attodao.mob_life.gameplay.movement.RabbitHopMovement;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.morph.MorphType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class ClientMorphState {
  private static final Map<UUID, Entity> RENDER_ENTITIES = new HashMap<>();
  private static final Map<Integer, Integer> GRASS_EATING_TICKS = new HashMap<>();
  private static final ClientChargedJumpController CHARGED_JUMP = new ClientChargedJumpController();
  private static MorphDefinition definition;
  private static MorphType morph;
  private static EntityDimensions dimensions;
  private static float eyeHeight;
  private static float waterMovementInputScale = 1.0F;
  private static float awkwardness;
  private static int rabbitHopCooldown;
  private static boolean nightVision;

  private ClientMorphState() {}

  public static void setMorph(MorphDefinition newDefinition) {
    MorphType newMorph = newDefinition.type();
    definition = newMorph.isPlayer() ? null : newDefinition;
    morph = newMorph.isPlayer() ? null : newMorph;
    nightVision = !newMorph.isPlayer() && MorphConfigManager.get(newMorph).traits().nightVision();
    dimensions = null;
    eyeHeight = 0.0F;
    waterMovementInputScale = 1.0F;
    RENDER_ENTITIES.clear();
    GRASS_EATING_TICKS.clear();
    CHARGED_JUMP.reset();
    rabbitHopCooldown = 0;

    Minecraft client = Minecraft.getInstance();
    if (client.level == null) {
      return;
    }

    if (definition != null) {
      Entity template = MorphEntityFactory.create(definition, client.level);
      if (template != null) {
        dimensions = template.getDimensions(Pose.STANDING);
        eyeHeight = template.getEyeHeight();
        if (template instanceof LivingEntity livingTemplate) {
          waterMovementInputScale =
              (float) livingTemplate.getAttributeValue(Attributes.MOVEMENT_SPEED);
        }
      }
    }
    float morphHeight =
        dimensions != null ? dimensions.height() : newMorph.entityType().getDimensions().height();
    for (Player player : client.level.players()) {
      MorphInventoryCapacity.apply(player, newMorph, morphHeight);
      MorphFoodCapacity.apply(player, newMorph, morphHeight);
      MorphMovementSpeed.refresh(player);
      player.refreshDimensions();
      player.setBoundingBox(
          player.getDimensions(player.getPose()).makeBoundingBox(player.position()));
    }
  }

  public static MorphType morph() {
    return morph;
  }

  public static float awkwardness() {
    return awkwardness;
  }

  public static boolean nightVision() {
    return nightVision;
  }

  public static void setAwkwardness(float value) {
    awkwardness = value;
  }

  public static void setGrassEatingTicks(int entityId, int ticks) {
    if (ticks > 0) {
      GRASS_EATING_TICKS.put(entityId, ticks);
    } else {
      GRASS_EATING_TICKS.remove(entityId);
    }
  }

  public static int grassEatingTicks(Player player) {
    return GRASS_EATING_TICKS.getOrDefault(player.getId(), 0);
  }

  public static boolean shouldShowChargedJumpBar() {
    return morph != null && CHARGED_JUMP.shouldShowBar();
  }

  public static float chargedJumpScale() {
    return CHARGED_JUMP.chargeScale();
  }

  public static boolean isChargedJumpCoolingDown() {
    return CHARGED_JUMP.isCoolingDown();
  }

  public static EntityDimensions dimensions() {
    return dimensions;
  }

  public static float eyeHeight() {
    return eyeHeight;
  }

  public static float waterMovementInputScale() {
    return morph == null
        ? 1.0F
        : waterMovementInputScale * MorphConfigManager.get(morph).movement().waterInputMultiplier();
  }

  public static Entity renderEntity(Player player) {
    if (morph == null) {
      return null;
    }

    return RENDER_ENTITIES.computeIfAbsent(
        player.getUUID(),
        uuid -> {
          Entity entity = MorphEntityFactory.create(definition, player.level());
          if (entity != null) {
            entity.setPos(player.position());
          }
          return entity;
        });
  }

  public static void tick(Minecraft client) {
    if (client.level == null || client.isPaused()) {
      return;
    }

    MorphConfig.Movement movement = morph != null ? MorphConfigManager.get(morph).movement() : null;
    CHARGED_JUMP.tick(client, movement);
    refreshChestedInventory(client.player);
    if (movement != null && movement.slowFallMultiplier() < 1.0F) {
      slowChickenFall(client.player);
    }
    if (movement != null && movement.wingAnimation()) {
      tickChickenWings(client);
    }
    if (movement != null && movement.rabbitHop().enabled()) {
      tickRabbitHop(client.player);
    }
  }

  private static void refreshChestedInventory(LocalPlayer player) {
    if (player == null || morph == null || !morph.canEquipChest() || dimensions == null) {
      return;
    }

    boolean hasChest = player.getItemBySlot(EquipmentSlot.BODY).is(Items.CHEST);
    MorphInventoryCapacity expected =
        MorphInventoryCapacity.forMorph(morph, dimensions.height(), hasChest);
    if (MorphInventoryCapacity.hotbarSlots(player) != expected.hotbarSlots()
        || MorphInventoryCapacity.inventorySlots(player) != expected.inventorySlots()) {
      MorphInventoryCapacity.apply(player, morph, dimensions.height(), hasChest);
    }
  }

  public static void clear() {
    definition = null;
    morph = null;
    dimensions = null;
    eyeHeight = 0.0F;
    waterMovementInputScale = 1.0F;
    awkwardness = 0.0F;
    nightVision = false;
    GRASS_EATING_TICKS.clear();
    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null) {
      MorphMovementSpeed.refresh(player);
    }
    RENDER_ENTITIES.clear();
    CHARGED_JUMP.reset();
    rabbitHopCooldown = 0;
  }

  private static void slowChickenFall(LocalPlayer player) {
    if (player == null || player.onGround() || player.isInWater() || player.getAbilities().flying) {
      return;
    }

    Vec3 velocity = player.getDeltaMovement();
    if (velocity.y < 0.0) {
      player.setDeltaMovement(
          velocity.x,
          velocity.y * MorphConfigManager.get(morph).movement().slowFallMultiplier(),
          velocity.z);
    }
  }

  private static void tickChickenWings(Minecraft client) {
    for (Player player : client.level.players()) {
      Entity entity = RENDER_ENTITIES.get(player.getUUID());
      if (!(entity instanceof Chicken chicken)) {
        continue;
      }

      chicken.oFlap = chicken.flap;
      chicken.oFlapSpeed = chicken.flapSpeed;
      if (player.onGround()) {
        chicken.flapSpeed = Math.max(chicken.flapSpeed - 0.3F, 0.0F);
      } else {
        chicken.flapSpeed = Math.min(chicken.flapSpeed + 0.3F, 1.0F);
        chicken.flap += chicken.flapSpeed * 1.8F;
      }
    }
  }

  private static void tickRabbitHop(LocalPlayer player) {
    if (player == null) {
      return;
    }
    if (rabbitHopCooldown > 0) {
      rabbitHopCooldown--;
    }

    var keys = player.input.keyPresses;
    boolean moving = keys.forward() || keys.backward() || keys.left() || keys.right();
    boolean jumping = keys.jump();
    boolean groundedOnLand = player.onGround() && !player.isInWater() && !player.isInLava();
    if ((!moving && !jumping)
        || rabbitHopCooldown > 0
        || !groundedOnLand
        || player.isPassenger()
        || player.getAbilities().flying) {
      return;
    }

    RabbitHopMovement.launch(player, keys);
    rabbitHopCooldown = RabbitHopMovement.cooldown(player, keys);
  }
}
