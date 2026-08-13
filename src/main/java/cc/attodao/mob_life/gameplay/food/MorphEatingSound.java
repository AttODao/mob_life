package cc.attodao.mob_life.gameplay.food;

import java.util.function.Supplier;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/** Plays morph meal sounds only for the player who ate. */
public final class MorphEatingSound {
  private static final int CONTINUOUS_EATING_SOUND_INTERVAL_TICKS = 4;
  private static final ThreadLocal<ServerPlayer> SUPPRESSED_BROADCAST_PLAYER = new ThreadLocal<>();

  private MorphEatingSound() {}

  public static void playForEater(ServerPlayer player) {
    var random = player.getRandom();
    float volume = random.nextBoolean() ? 0.5F : 1.0F;
    float pitch = random.triangle(1.0F, 0.2F);
    player.connection.send(
        new ClientboundSoundEntityPacket(
            SoundEvents.GENERIC_EAT,
            player.getSoundSource(),
            player,
            volume,
            pitch,
            random.nextLong()));
  }

  public static void playContinuousTickForEater(ServerPlayer player, int elapsedTicks) {
    if (elapsedTicks % CONTINUOUS_EATING_SOUND_INTERVAL_TICKS == 0) {
      playForEater(player);
    }
  }

  public static <T> T finishItemConsumption(ServerPlayer player, Supplier<T> operation) {
    ServerPlayer previous = SUPPRESSED_BROADCAST_PLAYER.get();
    SUPPRESSED_BROADCAST_PLAYER.set(player);
    T result;
    try {
      result = operation.get();
    } finally {
      if (previous != null) {
        SUPPRESSED_BROADCAST_PLAYER.set(previous);
      } else {
        SUPPRESSED_BROADCAST_PLAYER.remove();
      }
    }
    playForEater(player);
    return result;
  }

  public static boolean isBroadcastSuppressed(ServerPlayer player) {
    return SUPPRESSED_BROADCAST_PLAYER.get() == player;
  }
}
