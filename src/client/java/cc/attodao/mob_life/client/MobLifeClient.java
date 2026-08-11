package cc.attodao.mob_life.client;

import cc.attodao.mob_life.client.screen.MorphSelectionScreen;
import cc.attodao.mob_life.client.state.ClientInstinctState;
import cc.attodao.mob_life.client.state.ClientMorphState;
import cc.attodao.mob_life.client.state.ClientOutlineState;
import cc.attodao.mob_life.config.MorphConfigManager;
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
import net.minecraft.client.Minecraft;
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
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.WorldMorphSelectionPromptPayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(
                    () -> {
                      ArrayList<MorphType> morphTypes = new ArrayList<>(payload.configs().size());
                      for (MobLifeNetworking.MorphConfigEntry entry : payload.configs()) {
                        MorphType morph = MorphType.fromId(entry.morphId());
                        morphTypes.add(morph);
                        MorphConfigManager.installSynced(morph, entry.configJson());
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
                      MorphConfigManager.installSynced(morph, payload.configJson());
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
        MobLifeNetworking.InstinctControlPayload.TYPE,
        (payload, context) -> context.client().execute(() -> ClientInstinctState.apply(payload)));
    ClientPlayNetworking.registerGlobalReceiver(
        MobLifeNetworking.OutlinePayload.TYPE,
        (payload, context) ->
            context
                .client()
                .execute(() -> ClientOutlineState.set(payload.predators(), payload.prey())));

    ClientPlayConnectionEvents.DISCONNECT.register(
        (handler, client) -> {
          ClientMorphState.clear();
          ClientInstinctState.clear();
          ClientOutlineState.clear();
          client.gameRenderer.clearPostEffect();
        });
    ClientTickEvents.END_CLIENT_TICK.register(
        client -> {
          while (ABILITY_KEY.consumeClick()) {
            ClientInstinctState.recordActivity();
            if (client.player != null
                && ClientMorphState.morph() != null
                && !ClientInstinctState.enabled()) {
              ClientPlayNetworking.send(new MobLifeNetworking.AbilityRequestPayload());
            }
          }
          while (SLEEP_KEY.consumeClick()) {
            ClientInstinctState.recordActivity();
            if (client.player != null
                && ClientMorphState.morph() != null
                && !ClientInstinctState.enabled()) {
              ClientPlayNetworking.send(new MobLifeNetworking.SleepRequestPayload());
            }
          }
          mobLife$recordDirectActivity(client);
          if (ClientInstinctState.shouldRequestEntry(client)) {
            ClientPlayNetworking.send(new MobLifeNetworking.InstinctEnterPayload());
          }
          if (ClientInstinctState.shouldRequestExit(client)) {
            ClientPlayNetworking.send(new MobLifeNetworking.InstinctExitPayload());
          }
          int interventionFlags = ClientInstinctState.consumeInterventions();
          if (interventionFlags != 0) {
            ClientPlayNetworking.send(
                new MobLifeNetworking.InstinctInterventionPayload(interventionFlags));
          }
          ClientInstinctState.tick(client);
          ClientMorphState.tick(client);
        });
  }

  private static void mobLife$recordDirectActivity(Minecraft client) {
    if (client.options.keyAttack.isDown()
        || client.options.keyUse.isDown()
        || client.options.keyPickItem.isDown()
        || client.options.keyDrop.isDown()
        || client.options.keyInventory.isDown()
        || client.options.keySwapOffhand.isDown()) {
      ClientInstinctState.recordActivity();
      return;
    }
    for (KeyMapping key : client.options.keyHotbarSlots) {
      if (key.isDown()) {
        ClientInstinctState.recordActivity();
        return;
      }
    }
  }
}
