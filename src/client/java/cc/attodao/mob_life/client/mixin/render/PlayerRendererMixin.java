package cc.attodao.mob_life.client.mixin.render;

import cc.attodao.mob_life.client.state.ClientMorphState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class PlayerRendererMixin {
	@Inject(
			method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("HEAD"),
			cancellable = true
	)
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void mobLife$renderMorph(
			LivingEntityRenderState state,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			CameraRenderState camera,
			CallbackInfo ci
	) {
		if (!(state instanceof AvatarRenderState avatarState) || avatarState.isSpectator) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return;
		}

		Entity sourceEntity = client.level.getEntity(avatarState.id);
		if (!(sourceEntity instanceof Player player)) {
			return;
		}

		Entity morphEntity = ClientMorphState.renderEntity(player);
		if (morphEntity == null) {
			return;
		}

		mobLife$copyPlayerState(player, morphEntity);

		EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
		EntityRenderer renderer = dispatcher.getRenderer(morphEntity);
		float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		EntityRenderState morphState = renderer.createRenderState(morphEntity, partialTick);
		morphState.lightCoords = state.lightCoords;
		morphState.nameTag = state.nameTag;
		morphState.scoreText = state.scoreText;
		morphState.outlineColor = state.outlineColor;
		renderer.submit(morphState, poseStack, submitNodeCollector, camera);
		ci.cancel();
	}

	private static void mobLife$copyPlayerState(Player player, Entity morphEntity) {
		morphEntity.setPos(player.position());
		morphEntity.xOld = player.getX();
		morphEntity.yOld = player.getY();
		morphEntity.zOld = player.getZ();
		morphEntity.setYRot(player.getYRot());
		morphEntity.setXRot(player.getXRot());
		morphEntity.yRotO = player.yRotO;
		morphEntity.xRotO = player.xRotO;
		morphEntity.setYHeadRot(player.getYHeadRot());
		morphEntity.setOnGround(player.onGround());
		morphEntity.setDeltaMovement(player.getDeltaMovement());
		morphEntity.tickCount = player.tickCount;
		morphEntity.fallDistance = player.fallDistance;

		if (!(morphEntity instanceof LivingEntity livingMorph)) {
			return;
		}

		livingMorph.yBodyRot = player.yBodyRot;
		livingMorph.yBodyRotO = player.yBodyRotO;
		livingMorph.yHeadRotO = player.yHeadRotO;
		livingMorph.attackAnim = player.attackAnim;
		livingMorph.oAttackAnim = player.oAttackAnim;
		livingMorph.swinging = player.swinging;
		livingMorph.swingTime = player.swingTime;
		livingMorph.swingingArm = player.swingingArm;
		livingMorph.hurtTime = player.hurtTime;
		livingMorph.deathTime = player.deathTime;
		livingMorph.setPose(Pose.STANDING);

		WalkAnimationStateAccessor sourceAnimation = (WalkAnimationStateAccessor) player.walkAnimation;
		WalkAnimationStateAccessor targetAnimation = (WalkAnimationStateAccessor) livingMorph.walkAnimation;
		targetAnimation.mobLife$setSpeedOld(sourceAnimation.mobLife$getSpeedOld());
		targetAnimation.mobLife$setSpeed(sourceAnimation.mobLife$getSpeed());
		targetAnimation.mobLife$setPosition(sourceAnimation.mobLife$getPosition());
	}
}
