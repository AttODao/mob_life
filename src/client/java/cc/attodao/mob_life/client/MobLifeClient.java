package cc.attodao.mob_life.client;

import cc.attodao.mob_life.client.config.ClientMobLifeConfig;
import cc.attodao.mob_life.client.screen.MorphSelectionScreen;
import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientMorphBodyYawState;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.client.state.ClientPredatorOutlineState;
import cc.attodao.mob_life.config.MorphConfigManager;
import cc.attodao.mob_life.config.ServerMobLifeConfig;
import cc.attodao.mob_life.morph.MorphDefinition;
import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
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
    ClientMobLifeConfig.load();
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.ServerConfigPayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(() -> ServerMobLifeConfig.installSynchronized(payload.settings())));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.MorphBodyYawPayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(
                    () -> ClientMorphBodyYawState.update(payload.entityId(), payload.bodyYaw())));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.MorphProfilesPayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(
                    () -> {
                      try {
                        MorphConfigManager.installSyncedBatch(
                            payload.generation(), payload.configs());
                        ClientMorphState.onProfilesReload();
                      } catch (RuntimeException exception) {
                        context
                            .client()
                            .getConnection()
                            .getConnection()
                            .disconnect(
                                Component.literal(
                                    "Mob Life profile/schema mismatch: " + exception.getMessage()));
                      }
                    }));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.WorldMorphSelectionPromptPayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(
                    () -> {
                      ArrayList<MorphType> morphTypes = new ArrayList<>(payload.morphIds().size());
                      for (String morphId : payload.morphIds()) {
                        MorphType morph = MorphType.fromId(morphId);
                        if (!morph.id().equals(morphId)) {
                          context
                              .client()
                              .getConnection()
                              .getConnection()
                              .disconnect(
                                  Component.literal("Mob Life sent unknown morph " + morphId));
                          return;
                        }
                        morphTypes.add(morph);
                      }
                      context.client().gui.setScreen(new MorphSelectionScreen(morphTypes));
                    }));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.MorphSelectionPayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(
                    () -> {
                      MorphType morph = MorphType.fromId(payload.morphId());
                      if (!morph.id().equals(payload.morphId())) {
                        context
                            .client()
                            .getConnection()
                            .getConnection()
                            .disconnect(
                                Component.literal(
                                    "Mob Life sent unknown morph " + payload.morphId()));
                        return;
                      }
                      ClientMorphState.setMorph(new MorphDefinition(morph, payload.nbt()));
                      context
                          .client()
                          .gameRenderer
                          .checkEntityPostEffect(context.client().getCameraEntity());
                      if (context.client().gui.screen() instanceof MorphSelectionScreen) {
                        context.client().gui.setScreen(null);
                      }
                    }));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.AwkwardnessPayload.TYPE,
        (payload, context) ->
            context.client().execute(() -> ClientMorphState.setAwkwardness(payload.value())));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.GrassEatingStatePayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(
                    () ->
                        ClientMorphState.setGrassEatingTicks(
                            payload.entityId(), payload.remainingTicks())));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.InstinctStatePayload.TYPE,
        (payload, context) ->
            context.client().execute(() -> ClientInstinctState.update(payload.toState())));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.PredatorOutlinePayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(
                    () -> ClientPredatorOutlineState.set(payload.predators(), payload.prey())));

    ClientPlayConnectionEvents.DISCONNECT.register(
        (handler, client) -> {
          ClientMorphState.clear();
          ClientInstinctState.clear();
          ClientPredatorOutlineState.clear();
          ClientMorphBodyYawState.clear();
          ServerMobLifeConfig.clearSynchronized();
          client.gameRenderer.clearPostEffect();
        });
    ClientTickEvents.END_CLIENT_TICK.register(
        client -> {
          while (ABILITY_KEY.consumeClick()) {
            if (client.player != null
                && ClientMorphState.morph() != null
                && !ClientInstinctState.active()) {
              ClientPlayNetworking.send(new MobLifeNetworking.AbilityRequestPayload());
            }
          }
          while (SLEEP_KEY.consumeClick()) {
            if (client.player != null
                && ClientMorphState.morph() != null
                && !ClientInstinctState.active()) {
              ClientPlayNetworking.send(new MobLifeNetworking.SleepRequestPayload());
            }
          }
          ClientMorphState.tick(client);
          ClientInstinctState.tick(client);
        });
  }
}
