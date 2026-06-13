package cc.attodao.mob_life.network;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.server.ServerMorphManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class MobLifeNetworking {
	private MobLifeNetworking() {
	}

	public static void registerPayloads() {
		PayloadTypeRegistry.clientboundPlay().register(MorphSelectionPayload.TYPE, MorphSelectionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChargedJumpPayload.TYPE, ChargedJumpPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(
				ChargedJumpPayload.TYPE,
				(payload, context) ->
						ServerMorphManager.performChargedJump(context.player(), payload.chargeAmount())
		);
	}

	public record MorphSelectionPayload(String morphId) implements CustomPacketPayload {
		public static final Type<MorphSelectionPayload> TYPE = new Type<>(
				Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "morph_selection")
		);
		public static final StreamCodec<RegistryFriendlyByteBuf, MorphSelectionPayload> CODEC = StreamCodec.of(
				(buffer, payload) -> buffer.writeUtf(payload.morphId()),
				buffer -> new MorphSelectionPayload(buffer.readUtf())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ChargedJumpPayload(int chargeAmount) implements CustomPacketPayload {
		public static final Type<ChargedJumpPayload> TYPE = new Type<>(
				Identifier.fromNamespaceAndPath(MobLife.MOD_ID, "charged_jump")
		);
		public static final StreamCodec<RegistryFriendlyByteBuf, ChargedJumpPayload> CODEC = StreamCodec.of(
				(buffer, payload) -> buffer.writeVarInt(payload.chargeAmount()),
				buffer -> new ChargedJumpPayload(buffer.readVarInt())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
