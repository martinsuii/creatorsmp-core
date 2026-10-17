package im.nesp.creatorsmp.network;

import im.nesp.creatorsmp.CreatorSMPCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerChallengePayload(String challengeId) implements CustomPacketPayload {
    public static final Type<ServerChallengePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreatorSMPCore.MODID, "server_challenge"));
    public static final StreamCodec<FriendlyByteBuf, ServerChallengePayload> STREAM_CODEC =
            StreamCodec.of(ServerChallengePayload::encode, ServerChallengePayload::decode);

    private static void encode(FriendlyByteBuf buf, ServerChallengePayload payload) {
        buf.writeUtf(payload.challengeId(), 36);
    }

    private static ServerChallengePayload decode(FriendlyByteBuf buf) {
        return new ServerChallengePayload(buf.readUtf(36));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}