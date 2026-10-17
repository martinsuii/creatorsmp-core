package im.nesp.creatorsmp.client;

import im.nesp.creatorsmp.network.ModReportPayload;
import im.nesp.creatorsmp.network.ServerChallengePayload;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;

public final class ClientHandshakeHandler {
    private ClientHandshakeHandler() {}

    public static void handleChallenge(ServerChallengePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var mods = new ArrayList<ModReportPayload.ModEntry>();
            ModList.get().getMods().stream()
                    .limit(ModReportPayload.MAX_MODS)
                    .forEach(mod -> mods.add(new ModReportPayload.ModEntry(
                            mod.getModId(), mod.getDisplayName())));
            PacketDistributor.sendToServer(new ModReportPayload(payload.challengeId(), mods));
        });
    }
}