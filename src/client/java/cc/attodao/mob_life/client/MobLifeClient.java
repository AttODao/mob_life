package cc.attodao.mob_life.client;

import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MobLifeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(
				MobLifeNetworking.MorphSelectionPayload.TYPE,
				(payload, context) -> context.client().execute(() -> {
					ClientMorphState.setMorph(MorphType.fromId(payload.morphId()));
					context.client().gameRenderer.checkEntityPostEffect(context.client().getCameraEntity());
				})
		);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientMorphState.clear();
			client.gameRenderer.clearPostEffect();
		});
		ClientTickEvents.END_CLIENT_TICK.register(ClientMorphState::tick);
	}
}
