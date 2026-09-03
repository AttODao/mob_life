package cc.attodao.mob_life.mixin.gameplay;

import cc.attodao.mob_life.gameplay.instinct.MorphInstinct;
import cc.attodao.mob_life.gameplay.inventory.MorphMountInteraction;
import cc.attodao.mob_life.gameplay.targeting.MorphRelations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMorphInteractionMixin {
  @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
  private void mobLife$preventMobInteraction(
      Entity entity,
      InteractionHand hand,
      Vec3 location,
      CallbackInfoReturnable<InteractionResult> cir) {
    if (!((Object) this instanceof ServerPlayer player)) {
      return;
    }
    if (entity instanceof ServerPlayer target) {
      InteractionResult love = MorphInstinct.tryFeedForLove(player, target, hand);
      if (love.consumesAction()) {
        cir.setReturnValue(love);
        return;
      }
      InteractionResult mount = MorphMountInteraction.interact(player, target);
      if (mount.consumesAction()) {
        cir.setReturnValue(mount);
        return;
      }
    }
    boolean biologicalMob =
        entity instanceof Mob
            || entity instanceof Player target && MorphRelations.morphOf(target) != null;
    if (MorphInstinct.blocksActions(player)
        || MorphRelations.morphOf(player) != null && biologicalMob) {
      cir.setReturnValue(InteractionResult.FAIL);
    }
  }
}
