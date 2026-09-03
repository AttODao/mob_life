package cc.attodao.mob_life.gameplay.inventory;

import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import cc.attodao.mob_life.gameplay.targeting.MorphRelations;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

public final class MorphMountInteraction {
  private MorphMountInteraction() {}

  public static InteractionResult interact(ServerPlayer actor, ServerPlayer target) {
    MorphType morph = MorphRelations.morphOf(target);
    if (actor == target
        || MorphRelations.morphOf(actor) != null
        || morph == null
        || !morph.isEquine()
        || !MorphInstinct.canBeRiddenByOtherPlayer(target)) {
      return InteractionResult.PASS;
    }
    if (actor.getMainHandItem().is(Items.LEAD)
        || actor.getOffhandItem().is(Items.LEAD)
        || actor.getMainHandItem().is(Items.SHEARS)
        || actor.getOffhandItem().is(Items.SHEARS)
        || ((Leashable) target).getLeashHolder() == actor) {
      return InteractionResult.PASS;
    }

    if (actor.isSecondaryUseActive()) {
      openInventory(actor, target, morph);
      return InteractionResult.SUCCESS_SERVER;
    }
    return actor.startRiding(target) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
  }

  private static void openInventory(ServerPlayer actor, ServerPlayer target, MorphType morph) {
    MorphMountContainer container = new MorphMountContainer(target, morph);
    int rows = container.rows();
    actor.openMenu(
        new SimpleMenuProvider(
            (containerId, inventory, player) ->
                new ChestMenu(
                    rows == 1 ? MenuType.GENERIC_9x1 : MenuType.GENERIC_9x2,
                    containerId,
                    inventory,
                    container,
                    rows),
            Component.translatable("container.mob_life.morph_mount", target.getDisplayName())));
  }
}
