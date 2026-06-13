package cc.attodao.mob_life.mixin.awkwardness;

import cc.attodao.mob_life.gameplay.awkwardness.AwkwardnessHolder;
import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;
import cc.attodao.mob_life.gameplay.sleep.MorphSleep;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerAwkwardnessMixin
		implements AwkwardnessHolder {
	@Unique
	private float mobLife$awkwardness;

	@Override
	public float mobLife$getAwkwardness() {
		return mobLife$awkwardness;
	}

	@Override
	public void mobLife$setAwkwardness(float awkwardness) {
		mobLife$awkwardness = awkwardness;
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void mobLife$readAwkwardness(
			ValueInput input,
			CallbackInfo ci
	) {
		mobLife$awkwardness = input.getFloatOr(
				"MobLifeAwkwardness",
				MorphAwkwardness.MINIMUM
		);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void mobLife$writeAwkwardness(
			ValueOutput output,
			CallbackInfo ci
	) {
		output.putFloat("MobLifeAwkwardness", mobLife$awkwardness);
	}

	@Inject(method = "attack", at = @At("HEAD"))
	private void mobLife$recordAttack(
			Entity target,
			CallbackInfo ci
	) {
		if ((Object) this instanceof ServerPlayer player) {
			ServerMorphManager.adjustAwkwardness(player, 12.0F);
		}
	}

	@Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
	private void mobLife$preventServerMobInteraction(
			Entity entity,
			InteractionHand hand,
			Vec3 location,
			CallbackInfoReturnable<InteractionResult> cir
	) {
		if (
				(Object) this instanceof ServerPlayer
						&& ServerMorphManager.hasMobForm()
						&& entity instanceof Mob
		) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	@ModifyArg(
			method = "attack",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
			),
			index = 1
	)
	private float mobLife$removeFriendlyMorphDamage(float damage) {
		return (Object) this instanceof ServerPlayer
				&& ServerMorphManager.hasMobForm()
				? 0.0F
				: damage;
	}

	@ModifyVariable(
			method = "causeFoodExhaustion",
			at = @At("HEAD"),
			argsOnly = true
	)
	private float mobLife$scaleExhaustion(float amount) {
		return (Object) this instanceof ServerPlayer player
				&& ServerMorphManager.hasMobForm()
				? amount * MorphAwkwardness.exhaustionMultiplier(player)
				: amount;
	}

	@ModifyConstant(
			method = "tick",
			constant = @Constant(intValue = Player.SLEEP_DURATION)
	)
	private int mobLife$extendSoftSurfaceSleepTimer(int duration) {
		Player player = (Player) (Object) this;
		return MorphSleep.isCustomSleep(player)
				? MorphSleep.REQUIRED_SLEEP_TICKS
				: duration;
	}

	@Inject(
			method = "isSleepingLongEnough",
			at = @At("HEAD"),
			cancellable = true
	)
	private void mobLife$requireLongerSoftSurfaceSleep(
			CallbackInfoReturnable<Boolean> cir
	) {
		Player player = (Player) (Object) this;
		if (MorphSleep.isCustomSleep(player)) {
			cir.setReturnValue(
					player.isSleeping()
							&& player.getSleepTimer()
							>= MorphSleep.REQUIRED_SLEEP_TICKS
			);
		}
	}
}
