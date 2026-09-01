package cc.attodao.mob_life.client.mixin.gameplay;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface ClientInputMoveVectorAccessor {
  @Accessor("moveVector")
  void mobLife$setMoveVector(Vec2 moveVector);
}
