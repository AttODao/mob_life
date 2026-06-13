package cc.attodao.mob_life.client.state;

import cc.attodao.mob_life.gameplay.food.MorphFoodCapacity;
import cc.attodao.mob_life.gameplay.inventory.MorphInventoryCapacity;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphEntityFactory;
import cc.attodao.mob_life.morph.MorphType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientMorphState {
	private static final Map<UUID, Entity> RENDER_ENTITIES = new HashMap<>();
	private static final ClientChargedJumpController CHARGED_JUMP =
			new ClientChargedJumpController();
	private static MorphDefinition definition;
	private static MorphType morph;
	private static EntityDimensions dimensions;
	private static float eyeHeight;
	private static float awkwardness;

	private ClientMorphState() {
	}

	public static void setMorph(MorphDefinition newDefinition) {
		MorphType newMorph = newDefinition.type();
		definition = newMorph.isPlayer() ? null : newDefinition;
		morph = newMorph.isPlayer() ? null : newMorph;
		dimensions = null;
		eyeHeight = 0.0F;
		RENDER_ENTITIES.clear();
		CHARGED_JUMP.reset();

		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return;
		}

		if (definition != null) {
			Entity template = MorphEntityFactory.create(
					definition,
					client.level
			);
			if (template != null) {
				dimensions = template.getDimensions(Pose.STANDING);
				eyeHeight = template.getEyeHeight();
			}
		}
		float morphHeight = dimensions != null
				? dimensions.height()
				: newMorph.entityType().getDimensions().height();
		for (Player player : client.level.players()) {
			MorphInventoryCapacity.apply(player, newMorph, morphHeight);
			MorphFoodCapacity.apply(player, newMorph, morphHeight);
			player.refreshDimensions();
			player.setBoundingBox(player.getDimensions(player.getPose()).makeBoundingBox(player.position()));
		}
	}

	public static MorphType morph() {
		return morph;
	}

	public static float awkwardness() {
		return awkwardness;
	}

	public static void setAwkwardness(float value) {
		awkwardness = value;
	}

	public static boolean shouldShowChargedJumpBar() {
		return morph != null && CHARGED_JUMP.shouldShowBar();
	}

	public static float chargedJumpScale() {
		return CHARGED_JUMP.chargeScale();
	}

	public static boolean isChargedJumpCoolingDown() {
		return CHARGED_JUMP.isCoolingDown();
	}

	public static EntityDimensions dimensions() {
		return dimensions;
	}

	public static float eyeHeight() {
		return eyeHeight;
	}

	public static Entity renderEntity(Player player) {
		if (morph == null) {
			return null;
		}

		return RENDER_ENTITIES.computeIfAbsent(player.getUUID(), uuid -> {
			Entity entity = MorphEntityFactory.create(definition, player.level());
			if (entity != null) {
				entity.setPos(player.position());
			}
			return entity;
		});
	}

	public static void tick(Minecraft client) {
		if (client.level == null || client.isPaused()) {
			return;
		}

		CHARGED_JUMP.tick(client, morph != null);
		if (morph == MorphType.CHICKEN) {
			slowChickenFall(client.player);
			tickChickenWings(client);
		}
	}

	public static void clear() {
		definition = null;
		morph = null;
		dimensions = null;
		eyeHeight = 0.0F;
		awkwardness = 0.0F;
		RENDER_ENTITIES.clear();
		CHARGED_JUMP.reset();
	}

	private static void slowChickenFall(LocalPlayer player) {
		if (player == null || player.onGround() || player.getAbilities().flying) {
			return;
		}

		Vec3 velocity = player.getDeltaMovement();
		if (velocity.y < 0.0) {
			player.setDeltaMovement(velocity.x, velocity.y * 0.6, velocity.z);
		}
	}

	private static void tickChickenWings(Minecraft client) {
		for (Player player : client.level.players()) {
			Entity entity = RENDER_ENTITIES.get(player.getUUID());
			if (!(entity instanceof Chicken chicken)) {
				continue;
			}

			chicken.oFlap = chicken.flap;
			chicken.oFlapSpeed = chicken.flapSpeed;
			if (player.onGround()) {
				chicken.flapSpeed = Math.max(chicken.flapSpeed - 0.3F, 0.0F);
			} else {
				chicken.flapSpeed = Math.min(chicken.flapSpeed + 0.3F, 1.0F);
				chicken.flap += chicken.flapSpeed * 1.8F;
			}
		}
	}
}
