package cc.attodao.mob_life.client.render;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.morph.MorphType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

public final class FirstPersonMorphHandRenderer {
  private static final Map<MorphType, ModelPart[]> HAND_PARTS = new EnumMap<>(MorphType.class);
  private static EntityModelSet modelSet;

  private FirstPersonMorphHandRenderer() {}

  public static boolean renderEmptyHand(
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int lightCoords,
      float inverseArmHeight,
      float attackValue,
      HumanoidArm arm) {
    if (!prepareHandTransform(poseStack, inverseArmHeight, attackValue, arm)) {
      return false;
    }

    renderPart(poseStack, collector, lightCoords, arm);
    poseStack.popPose();
    return true;
  }

  public static boolean shouldHideHandWithItem() {
    return ClientMorphState.morph() != null;
  }

  private static boolean prepareHandTransform(
      PoseStack poseStack, float inverseArmHeight, float attackValue, HumanoidArm arm) {
    if (ClientMorphState.morph() == null) {
      return false;
    }

    float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
    float attackRoot = Mth.sqrt(attackValue);
    float x = -0.3F * Mth.sin(attackRoot * (float) Math.PI);
    float y = 0.4F * Mth.sin(attackRoot * (float) (Math.PI * 2));
    float z = -0.4F * Mth.sin(attackValue * (float) Math.PI);

    poseStack.pushPose();
    poseStack.translate(side * (x + 0.64F), y - 0.6F + inverseArmHeight * -0.6F, z - 0.72F);
    poseStack.mulPose(Axis.YP.rotationDegrees(side * 35.0F));
    poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F));
    poseStack.mulPose(Axis.ZP.rotationDegrees(side * -10.0F));
    return true;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void renderPart(
      PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, HumanoidArm arm) {
    Minecraft client = Minecraft.getInstance();
    Player player = client.player;
    MorphType morph = ClientMorphState.morph();
    if (player == null || morph == null) {
      return;
    }

    Entity entity = ClientMorphState.renderEntity(player);
    if (entity == null) {
      return;
    }

    EntityRenderer renderer = client.getEntityRenderDispatcher().getRenderer(entity);
    if (!(renderer instanceof LivingEntityRenderer livingRenderer)) {
      return;
    }

    float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    EntityRenderState state = renderer.createRenderState(entity, partialTick);
    Identifier texture = livingRenderer.getTextureLocation((LivingEntityRenderState) state);
    ModelPart part = getHandPart(client.getEntityModels(), morph, arm);
    if (morph == MorphType.CHICKEN) {
      poseStack.scale(1.7F, 1.7F, 1.7F);
    } else if (morph == MorphType.CAT || morph == MorphType.OCELOT || morph == MorphType.WOLF) {
      poseStack.scale(1.35F, 1.0F, 1.35F);
    } else {
      poseStack.scale(1.0F, 0.7F, 1.0F);
    }
    collector.submitModelPart(
        part,
        poseStack,
        RenderTypes.entityCutout(texture),
        lightCoords,
        OverlayTexture.NO_OVERLAY,
        null);
  }

  private static ModelPart getHandPart(
      EntityModelSet currentModelSet, MorphType morph, HumanoidArm arm) {
    if (modelSet != currentModelSet) {
      modelSet = currentModelSet;
      HAND_PARTS.clear();
    }

    ModelPart[] parts =
        HAND_PARTS.computeIfAbsent(morph, type -> createParts(currentModelSet, type));
    return parts[arm == HumanoidArm.RIGHT ? 0 : 1];
  }

  private static ModelPart[] createParts(EntityModelSet currentModelSet, MorphType morph) {
    ModelLayerLocation layer =
        switch (morph) {
          case PLAYER ->
              throw new IllegalArgumentException("Player mode does not have a mob hand model");
          case COW -> ModelLayers.COW;
          case SHEEP -> ModelLayers.SHEEP;
          case CHICKEN -> ModelLayers.CHICKEN;
          case CAT -> ModelLayers.CAT;
          case OCELOT -> ModelLayers.OCELOT;
          case WOLF -> ModelLayers.WOLF;
          case PIG -> ModelLayers.PIG;
          case HORSE -> ModelLayers.HORSE;
          case DONKEY -> ModelLayers.DONKEY;
          case MULE -> ModelLayers.MULE;
          case RABBIT -> ModelLayers.RABBIT;
        };

    String rightPart = morph == MorphType.CHICKEN ? "right_wing" : "right_front_leg";
    String leftPart = morph == MorphType.CHICKEN ? "left_wing" : "left_front_leg";
    ModelPart root = currentModelSet.bakeLayer(layer);
    ModelPart limbRoot =
        morph == MorphType.RABBIT ? root.getChild("body").getChild("frontlegs") : root;
    ModelPart right = limbRoot.getChild(rightPart);
    ModelPart left = limbRoot.getChild(leftPart);
    right.setPos(0.0F, 0.0F, 0.0F);
    left.setPos(0.0F, 0.0F, 0.0F);
    right.setRotation(0.0F, 0.0F, 0.0F);
    left.setRotation(0.0F, 0.0F, 0.0F);
    return new ModelPart[] {right, left};
  }
}
