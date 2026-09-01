package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.config.MorphConfig;
import cc.attodao.mob_life.gameplay.inventory.MorphEquipment;
import cc.attodao.mob_life.gameplay.jump.ChargedJumpingPlayer;
import cc.attodao.mob_life.gameplay.jump.MobChargedJump;
import cc.attodao.mob_life.network.MobLifeNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

final class ClientChargedJumpController {
  private int chargeTicks = -1;
  private long cooldownUntilTick;
  private boolean jumpKeyWasDown;
  private boolean wasGrounded;
  private int trackedPlayerId = Integer.MIN_VALUE;

  boolean shouldShowBar() {
    return chargeTicks >= 0 || isCoolingDown();
  }

  float chargeScale() {
    LocalPlayer player = Minecraft.getInstance().player;
    return chargeTicks >= 0 && player != null
        ? MobChargedJump.chargeScale(MorphEquipment.morph(player), chargeTicks)
        : 0.0F;
  }

  boolean isCoolingDown() {
    Minecraft client = Minecraft.getInstance();
    return client.level != null && client.level.getGameTime() < cooldownUntilTick;
  }

  void tick(Minecraft client, MorphConfig.Movement movement) {
    LocalPlayer player = client.player;
    if (player == null || movement == null || movement.rabbitHop().enabled()) {
      reset();
      return;
    }

    boolean jumpKeyDown = isJumpDown(player);
    boolean grounded = isJumpGrounded(player);
    if (trackedPlayerId != player.getId()) {
      trackedPlayerId = player.getId();
      chargeTicks = -1;
      cooldownUntilTick = 0L;
      wasGrounded = grounded;
      jumpKeyWasDown = jumpKeyDown;
    } else if (grounded && !wasGrounded) {
      cooldownUntilTick = client.level.getGameTime() + MobChargedJump.COOLDOWN_TICKS;
      chargeTicks = -1;
    }
    wasGrounded = grounded;

    boolean coolingDown = client.level.getGameTime() < cooldownUntilTick;
    boolean canJump = grounded && !coolingDown;

    if (chargeTicks >= 0) {
      continueOrRelease(player, jumpKeyDown, canJump);
    } else if (movement.chargedJump()) {
      if (jumpKeyDown && !jumpKeyWasDown && canJump) {
        chargeTicks = 0;
      }
    } else if (jumpKeyDown && canJump) {
      performJump(player, 100);
    }
    jumpKeyWasDown = jumpKeyDown;
  }

  void reset() {
    chargeTicks = -1;
    cooldownUntilTick = 0L;
    jumpKeyWasDown = false;
    wasGrounded = false;
    trackedPlayerId = Integer.MIN_VALUE;
  }

  private void continueOrRelease(LocalPlayer player, boolean jumpKeyDown, boolean canCharge) {
    if (!canCharge) {
      chargeTicks = -1;
    } else if (jumpKeyDown) {
      chargeTicks++;
    } else {
      performJump(player);
    }
  }

  private void performJump(LocalPlayer player) {
    performJump(player, MobChargedJump.chargeAmount(MorphEquipment.morph(player), chargeTicks));
  }

  private void performJump(LocalPlayer player, int chargeAmount) {
    float jumpScale = MobChargedJump.jumpScale(chargeAmount);
    ((ChargedJumpingPlayer) player).mobLife$performChargedJump(jumpScale);
    ClientPlayNetworking.send(new MobLifeNetworking.ChargedJumpPayload(chargeAmount));
    chargeTicks = -1;
  }

  private static boolean isJumpGrounded(LocalPlayer player) {
    return player.onGround()
        && !player.getAbilities().flying
        && !player.isInWater()
        && !player.isInLava();
  }

  private static boolean isJumpDown(LocalPlayer player) {
    return player.input != null && player.input.keyPresses.jump();
  }
}
