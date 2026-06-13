package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.gameplay.jump.ChargedJumpingPlayer;
import cc.attodao.mob_life.gameplay.jump.MobChargedJump;
import cc.attodao.mob_life.network.MobLifeNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

final class ClientChargedJumpController {
	private int chargeTicks = -1;
	private int cooldown;
	private boolean jumpKeyWasDown;

	boolean shouldShowBar() {
		return chargeTicks >= 0 || cooldown > 0;
	}

	float chargeScale() {
		return chargeTicks >= 0
				? MobChargedJump.chargeScale(chargeTicks)
				: 0.0F;
	}

	boolean isCoolingDown() {
		return cooldown > 0;
	}

	void tick(Minecraft client, boolean enabled) {
		LocalPlayer player = client.player;
		if (!enabled || player == null) {
			reset();
			return;
		}
		if (cooldown > 0) {
			cooldown--;
		}

		boolean jumpKeyDown = client.options.keyJump.isDown();
		boolean canCharge = player.onGround()
				&& !player.getAbilities().flying
				&& !player.isInWater()
				&& !player.isInLava();

		if (chargeTicks >= 0) {
			continueOrRelease(player, jumpKeyDown, canCharge);
		} else if (
				jumpKeyDown
						&& !jumpKeyWasDown
						&& cooldown == 0
						&& canCharge
		) {
			chargeTicks = 0;
		}
		jumpKeyWasDown = jumpKeyDown;
	}

	void reset() {
		chargeTicks = -1;
		cooldown = 0;
		jumpKeyWasDown = false;
	}

	private void continueOrRelease(
			LocalPlayer player,
			boolean jumpKeyDown,
			boolean canCharge
	) {
		if (!canCharge) {
			chargeTicks = -1;
		} else if (jumpKeyDown) {
			chargeTicks++;
		} else {
			performJump(player);
		}
	}

	private void performJump(LocalPlayer player) {
		int chargeAmount = MobChargedJump.chargeAmount(chargeTicks);
		float jumpScale = MobChargedJump.jumpScale(chargeAmount);
		((ChargedJumpingPlayer) player).mobLife$performChargedJump(jumpScale);
		ClientPlayNetworking.send(
				new MobLifeNetworking.ChargedJumpPayload(chargeAmount)
		);
		chargeTicks = -1;
		cooldown = MobChargedJump.COOLDOWN_TICKS;
	}
}
