package cc.attodao.mob_life.network;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.gameplay.sleep.MorphSleep;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class MobLifeNetworking {

  private MobLifeNetworking() {}

  public static void registerPayloads() {
    PayloadTypeRegistry.clientboundPlay()
        .register(MorphSelectionPayload.TYPE, MorphSelectionPayload.CODEC);
    PayloadTypeRegistry.clientboundPlay()
        .register(AwkwardnessPayload.TYPE, AwkwardnessPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(ChargedJumpPayload.TYPE, ChargedJumpPayload.CODEC);
    PayloadTypeRegistry.serverboundPlay()
        .register(SleepRequestPayload.TYPE, SleepRequestPayload.CODEC);
    ServerPlayNetworking.registerGlobalReceiver(
        ChargedJumpPayload.TYPE,
        (payload, context) ->
            ServerMorphManager.performChargedJump(context.player(), payload.chargeAmount()));
    ServerPlayNetworking.registerGlobalReceiver(
        SleepRequestPayload.TYPE, (payload, context) -> MorphSleep.requestSleep(context.player()));
  }

  public record MorphSelectionPayload(String morphId, CompoundTag nbt)
      implements CustomPacketPayload {
    public static final Type<MorphSelectionPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "morph_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MorphSelectionPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> {
              buffer.writeUtf(payload.morphId());
              buffer.writeNbt(payload.nbt());
            },
            buffer -> {
              String morphId = buffer.readUtf();
              CompoundTag nbt = buffer.readNbt();
              return new MorphSelectionPayload(morphId, nbt != null ? nbt : new CompoundTag());
            });

    public MorphSelectionPayload {
      nbt = nbt.copy();
    }

    @Override
    public CompoundTag nbt() {
      return nbt.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record ChargedJumpPayload(int chargeAmount) implements CustomPacketPayload {
    public static final Type<ChargedJumpPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "charged_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChargedJumpPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.chargeAmount()),
            buffer -> new ChargedJumpPayload(buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record AwkwardnessPayload(float value) implements CustomPacketPayload {
    public static final Type<AwkwardnessPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "awkwardness"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AwkwardnessPayload> CODEC =
        StreamCodec.of(
            (buffer, payload) -> buffer.writeFloat(payload.value()),
            buffer -> new AwkwardnessPayload(buffer.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record SleepRequestPayload() implements CustomPacketPayload {
    public static final Type<SleepRequestPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "sleep_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SleepRequestPayload> CODEC =
        StreamCodec.unit(new SleepRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }
}
