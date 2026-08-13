package cc.attodao.mob_life.gameplay.instinct;

public enum InstinctState {
  REST(true, false),
  LOOK(true, false),
  WANDER(false, false),
  SCENT(false, true),
  FOLLOW(false, true),
  FLEE(false, true),
  STALK(false, true),
  CHASE(false, true),
  ATTACK(false, true),
  EAT(false, true);

  private final boolean acceptsForward;
  private final boolean locksView;

  InstinctState(boolean acceptsForward, boolean locksView) {
    this.acceptsForward = acceptsForward;
    this.locksView = locksView;
  }

  public boolean acceptsForward() {
    return acceptsForward;
  }

  public boolean acceptsView() {
    return !locksView;
  }

  public boolean locksView() {
    return locksView;
  }

  public boolean allowsEscape() {
    return this == REST || this == WANDER;
  }

  public static InstinctState byOrdinal(int ordinal) {
    InstinctState[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : REST;
  }
}
