package im.nesp.creatorsmp.network;

import im.nesp.creatorsmp.CreatorSMPCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import io.netty.handler.codec.DecoderException;

import java.util.ArrayList;
import java.util.List;

/**
 * Client reply to the server's one-time login challenge. The report is useful
 * for policy checks, but it is client-supplied and is not tamper-proof evidence.
 */
public record ModReportPayload(String challengeId, List<ModEntry> mods) implements CustomPacketPayload {

    public static final int MAX_MODS = 512;

    public static final Type<ModReportPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreatorSMPCore.MODID, "mod_report"));

    public record ModEntry(String modId, String displayName) {}

    // StreamCodec (1.21.1 / NeoForge 21.1.x)
    public static final StreamCodec<FriendlyByteBuf, ModReportPayload> STREAM_CODEC =
            StreamCodec.of(ModReportPayload::encode, ModReportPayload::decode);

    public ModReportPayload {
        mods = List.copyOf(mods);
        if (mods.size() > MAX_MODS) {
            throw new IllegalArgumentException("Too many mods in report");
        }
    }

    private static void encode(FriendlyByteBuf buf, ModReportPayload payload) {
        buf.writeUtf(payload.challengeId(), 36);
        buf.writeVarInt(payload.mods().size());
        for (ModEntry entry : payload.mods()) {
            buf.writeUtf(entry.modId(), 128);
            buf.writeUtf(entry.displayName(), 128);
        }
    }

    private static ModReportPayload decode(FriendlyByteBuf buf) {
        String challengeId = buf.readUtf(36);
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_MODS) {
            throw new DecoderException("Invalid mod report entry count: " + count);
        }
        List<ModEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String modId = buf.readUtf(128);
            String name  = buf.readUtf(128);
            entries.add(new ModEntry(modId, name));
        }
        return new ModReportPayload(challengeId, entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
