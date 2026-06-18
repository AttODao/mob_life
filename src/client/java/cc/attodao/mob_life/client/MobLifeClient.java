package cc.attodao.mob_life.client;

import cc.attodao.mob_life.client.config.MobLifeClientConfig;
import cc.attodao.mob_life.client.screen.MorphSelectionScreen;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class MobLifeClient implements ClientModInitializer {
  private static final KeyMapping ABILITY_KEY =
      KeyMappingHelper.registerKeyMapping(
          new KeyMapping(
              "key.mob_life.ability",
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_V,
              KeyMapping.Category.GAMEPLAY));
  private static final KeyMapping SLEEP_KEY =
      KeyMappingHelper.registerKeyMapping(
          new KeyMapping(
              "key.mob_life.sleep",
              InputConstants.Type.KEYSYM,
              GLFW.GLFW_KEY_N,
              KeyMapping.Category.GAMEPLAY));

  @Override
  public void onInitializeClient() {
    MobLifeClientConfig.load();
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.WorldMorphSelectionPromptPayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(
                    () -> {
                      for (MobLifeNetworking.MorphConfigEntry entry : payload.configs()) {
                        MorphType morph = MorphType.fromId(entry.morphId());
                        MorphConfigManager.installSynced(morph, entry.configJson());
                      }
                      context.client().setScreen(new MorphSelectionScreen());
                    }));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.MorphSelectionPayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(
                    () -> {
                      MorphType morph = MorphType.fromId(payload.morphId());
                      MorphConfigManager.installSynced(morph, payload.configJson());
                      ClientMorphState.setMorph(new MorphDefinition(morph, payload.nbt()));
                      context
                          .client()
                          .gameRenderer
                          .checkEntityPostEffect(context.client().getCameraEntity());
                      if (context.client().screen instanceof MorphSelectionScreen) {
                        context.client().setScreen(null);
                      }
                    }));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.AwkwardnessPayload.TYPE,
        (payload, context) ->
            context.client().execute(() -> ClientMorphState.setAwkwardness(payload.value())));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.FastSprintStatePayload.TYPE,
        (payload, context) ->
            context.client().execute(() -> ClientMorphState.setFastSprintActive(payload.active())));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.GrassEatingStatePayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(
                    () ->
                        ClientMorphState.setGrassEatingTicks(
                            payload.entityId(), payload.remainingTicks())));

    ClientPlayConnectionEvents.DISCONNECT.register(
        (handler, client) -> {
          ClientMorphState.clear();
          client.gameRenderer.clearPostEffect();
        });
    ClientTickEvents.END_CLIENT_TICK.register(
        client -> {
          while (ABILITY_KEY.consumeClick()) {
            if (client.player != null && ClientMorphState.morph() != null) {
              ClientPlayNetworking.send(new MobLifeNetworking.AbilityRequestPayload());
            }
          }
          while (SLEEP_KEY.consumeClick()) {
            if (client.player != null && ClientMorphState.morph() != null) {
              ClientPlayNetworking.send(new MobLifeNetworking.SleepRequestPayload());
            }
          }
          ClientMorphState.tick(client);
        });
  }
}
