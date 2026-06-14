package cc.attodao.mob_life.gameplay.inventory;

import cc.attodao.mob_life.morph.MorphType;

public interface MorphInventoryCapacityHolder {
  MorphType mobLife$getMorph();

  int mobLife$getHotbarSlots();

  int mobLife$getInventorySlots();

  void mobLife$setInventoryCapacity(MorphType morph, int hotbarSlots, int inventorySlots);
}
