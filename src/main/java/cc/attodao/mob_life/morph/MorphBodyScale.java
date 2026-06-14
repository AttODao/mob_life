package cc.attodao.mob_life.morph;

public final class MorphBodyScale {

  private MorphBodyScale() {}

  public static float relativeTo(float actualHeight, float referenceHeight) {
    if (referenceHeight <= 0.0F) {
      return 1.0F;
    }
    return Math.max(0.0F, actualHeight / referenceHeight);
  }
}
