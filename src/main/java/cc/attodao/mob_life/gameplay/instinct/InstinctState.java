package cc.attodao.mob_life.gameplay.instinct;

public enum InstinctState {
  REST(true, true, false),
  LOOK(false, false, false),
  WANDER(true, true, false),
  SCENT(false, false, true),
  FOLLOW(false, false, true),
  FLEE(false, false, true),
  STALK(false, false, true),
  CHASE(false, false, true),
  ATTACK(false, false, true),
  EAT(false, false, true);

  private final boolean acceptsForward;
  private final boolean acceptsJump;
  private final boolean locksView;

  InstinctState(boolean acceptsForward, boolean acceptsJump, boolean locksView) {
    this.acceptsForward = acceptsForward;
    this.acceptsJump = acceptsJump;
    this.locksView = locksView;
  }

  public boolean acceptsForward() {
    return acceptsForward;
  }

  public boolean acceptsJump() {
    return acceptsJump;
  }

  public boolean acceptsView() {
    return !locksView;
  }

  public boolean locksView() {
    return locksView;
  }

  public static InstinctState byOrdinal(int ordinal) {
    InstinctState[] values = values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : REST;
  }
}
