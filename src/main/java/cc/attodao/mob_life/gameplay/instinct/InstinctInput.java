package cc.attodao.mob_life.gameplay.instinct;

import net.minecraft.util.Mth;

public record InstinctInput(
    float sideways,
    float forward,
    float cameraYaw,
    float cameraPitch,
    float cameraDelta,
    int buttons,
    int screenMode) {
  public static final int SCREEN_NONE = 0;
  public static final int SCREEN_GAMEPLAY = 1;
  public static final int SCREEN_SAFE = 2;
  public static final int JUMP = 1;
  public static final int ATTACK = 1 << 1;
  public static final int USE = 1 << 2;
  public static final int INVENTORY = 1 << 3;
  public static final int SPRINT = 1 << 4;
  public static final int SNEAK = 1 << 5;
  public static final int HOTBAR = 1 << 6;
  public static final int DROP = 1 << 7;
  public static final int SWAP = 1 << 8;
  public static final int PERSPECTIVE = 1 << 9;

  public static final InstinctInput EMPTY = new InstinctInput(0, 0, 0, 0, 0, 0, SCREEN_NONE);

  public InstinctInput {
    sideways = Mth.clamp(Float.isFinite(sideways) ? sideways : 0.0F, -1.0F, 1.0F);
    forward = Mth.clamp(Float.isFinite(forward) ? forward : 0.0F, -1.0F, 1.0F);
    cameraYaw = Float.isFinite(cameraYaw) ? Mth.wrapDegrees(cameraYaw) : 0.0F;
    cameraPitch = Mth.clamp(Float.isFinite(cameraPitch) ? cameraPitch : 0.0F, -90.0F, 90.0F);
    cameraDelta = Mth.clamp(Float.isFinite(cameraDelta) ? cameraDelta : 0.0F, 0.0F, 360.0F);
    screenMode = Mth.clamp(screenMode, SCREEN_NONE, SCREEN_SAFE);
  }

  public boolean hasAnyActivity() {
    return Math.abs(sideways) >= 0.2F || Math.abs(forward) >= 0.2F || buttons != 0;
  }

  public boolean actionEdge(int previousButtons) {
    int actions = JUMP | ATTACK | USE;
    return (buttons & actions & ~previousButtons) != 0;
  }
}
