package cc.attodao.mob_life.mixin.targeting;

import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperAvoidMorphMixin extends Monster {

  protected CreeperAvoidMorphMixin(EntityType<? extends Monster> entityType, Level level) {
    super(entityType, level);
  }

  @Inject(method = "registerGoals", at = @At("TAIL"))
  private void mobLife$avoidConfiguredMorphs(CallbackInfo ci) {
    goalSelector.addGoal(
        3,
        new AvoidEntityGoal<>(
            this,
            Player.class,
            6.0F,
            1.0,
            1.2,
            entity ->
                entity instanceof ServerPlayer
                    && ServerMorphManager.activeMorph() != null
                    && MorphConfigManager.get(ServerMorphManager.activeMorph())
                        .combat()
                        .avoidedBy()
                        .contains(BuiltInRegistries.ENTITY_TYPE.getKey(getType()).toString())));
  }
}
