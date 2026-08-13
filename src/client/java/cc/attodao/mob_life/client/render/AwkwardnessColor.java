package cc.attodao.mob_life.client.render;

import cc.attodao.mob_life.gameplay.awkwardness.MorphAwkwardness;

public final class AwkwardnessColor {
  private AwkwardnessColor() {}

  public static int argb(float awkwardness, int alpha) {
    float ratio = MorphAwkwardness.normalized(awkwardness);
    int red = ratio <= 0.5F ? Math.round(ratio * 2.0F * 255.0F) : 255;
    int green = ratio <= 0.5F ? 255 : Math.round((1.0F - ratio) * 2.0F * 255.0F);
    int blue = Math.round((1.0F - ratio) * 40.0F);
    return (alpha & 0xFF) << 24 | red << 16 | green << 8 | blue;
  }
}
