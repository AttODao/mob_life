package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.gameplay.movement.MorphAttributeModifiers;
import cc.attodao.mob_life.gameplay.movement.MorphMovementSpeed;
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
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;

public final class ClientMorphState {
  private static final Map<UUID, Entity> RENDER_ENTITIES = new HashMap<>();
  private static final Map<Integer, Integer> GRASS_EATING_TICKS = new HashMap<>();
  private static final ClientLocomotionController LOCOMOTION = new ClientLocomotionController();
  private static MorphDefinition definition;
  private static MorphType morph;
  private static EntityDimensions dimensions;
  private static float eyeHeight;
  private static boolean baby;
  private static float awkwardness;
  private static boolean nightVision;

  private ClientMorphState() {}

  public static void setMorph(MorphDefinition newDefinition) {
    ClientInstinctState.clear();
    ClientPredatorOutlineState.clear();
    MorphType newMorph = newDefinition.type();
    definition = newMorph.isPlayer() ? null : newDefinition;
    morph = newMorph.isPlayer() ? null : newMorph;
    nightVision = !newMorph.isPlayer() && MorphConfigManager.get(newMorph).traits().nightVision();
    dimensions = null;
    eyeHeight = 0.0F;
    baby = false;
    RENDER_ENTITIES.clear();
    GRASS_EATING_TICKS.clear();
    LOCOMOTION.reset();

    Minecraft client = Minecraft.getInstance();
    if (client.level == null) {
      return;
    }

    if (definition != null) {
      Entity template = MorphEntityFactory.create(definition, client.level);
      if (template != null) {
        dimensions = template.getDimensions(Pose.STANDING);
        eyeHeight = template.getEyeHeight();
        baby = template instanceof LivingEntity livingTemplate && livingTemplate.isBaby();
      }
    }
    float morphHeight =
        dimensions != null ? dimensions.height() : newMorph.entityType().getDimensions().height();
    for (Player player : client.level.players()) {
      MorphInventoryCapacity.apply(player, newMorph, morphHeight);
      MorphFoodCapacity.apply(player, newMorph, morphHeight);
      if (newMorph.isPlayer()) {
        MorphAttributeModifiers.removeAll(player);
        if (player.isSprinting()) {
          player.setSprinting(false);
        }
      }
      MorphMovementSpeed.refresh(player);
      player.refreshDimensions();
    }
  }

  public static void onProfilesReload() {
    LOCOMOTION.reset();
    RENDER_ENTITIES.clear();
    if (morph != null) {
      nightVision = MorphConfigManager.get(morph).traits().nightVision();
    }
  }

  public static void resetLocomotion() {
    LOCOMOTION.reset();
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
    return morph != null && LOCOMOTION.shouldShowJumpBar();
  }

  public static float chargedJumpScale() {
    return LOCOMOTION.jumpBarScale();
  }

  public static boolean isChargedJumpCoolingDown() {
    return LOCOMOTION.isJumpBarCoolingDown();
  }

  public static EntityDimensions dimensions() {
    return dimensions;
  }

  public static float eyeHeight() {
    return eyeHeight;
  }

  public static boolean rabbitHopEnabled() {
    return morph == MorphType.RABBIT;
  }

  public static void captureMovementInput(net.minecraft.world.entity.player.Input input) {
    LOCOMOTION.capture(input);
  }

  public static MovementInput applyMovement(LocalPlayer player) {
    ClientLocomotionController.MotionInput output = LOCOMOTION.apply(player, morph, baby);
    return new MovementInput(
        output.sideways(), output.forward(), output.jumping(), output.isVanilla());
  }

  public static void afterMovement(LocalPlayer player) {
    LOCOMOTION.afterTick(player, morph);
  }

  public static boolean captureLookInput(
      LocalPlayer player, double rawYawInput, double rawPitchInput) {
    return LOCOMOTION.captureLook(player, morph, rawYawInput, rawPitchInput);
  }

  public static float bodyYaw() {
    return LOCOMOTION.bodyYaw();
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
            // 26.2 render-state extraction reads the proxy ID for item-model state.
            entity.setId(player.getId());
            entity.setPos(player.position());
          }
          return entity;
        });
  }

  public static void tick(Minecraft client) {
    if (client.level == null || client.isPaused()) {
      return;
    }

    if (morph == null && client.player != null) {
      // Attribute synchronization can arrive after the form payload. Keep local collision
      // prediction free of stale Mob Life modifiers while the world uses the player form.
      MorphAttributeModifiers.removeAll(client.player);
    }

    refreshChestedInventory(client.player);
    if (morph == MorphType.CHICKEN) {
      tickChickenWings(client);
    }
    if (client.player != null) {
      LOCOMOTION.recoverView(client.player, morph, isAimingInteractionActive(client));
    }
  }

  private static boolean isAimingInteractionActive(Minecraft client) {
    HitResult target = client.hitResult;
    return target != null
        && target.getType() != HitResult.Type.MISS
        && (client.options.keyAttack.isDown() || client.options.keyUse.isDown());
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
    ClientInstinctState.clear();
    ClientPredatorOutlineState.clear();
    definition = null;
    morph = null;
    dimensions = null;
    eyeHeight = 0.0F;
    baby = false;
    awkwardness = 0.0F;
    nightVision = false;
    GRASS_EATING_TICKS.clear();
    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null) {
      MorphAttributeModifiers.removeAll(player);
    }
    RENDER_ENTITIES.clear();
    LOCOMOTION.reset();
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

  public record MovementInput(float sideways, float forward, boolean jumping, boolean vanilla) {}
}
