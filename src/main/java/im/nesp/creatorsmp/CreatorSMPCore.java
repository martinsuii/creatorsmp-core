package im.nesp.creatorsmp;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(CreatorSMPCore.MODID)
public class CreatorSMPCore {
    public static final String MODID = "creatorsmp_core";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreatorSMPCore(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing CreatorSMP Core mod...");

        // Register client configuration (creatorsmp_core-client.toml)
        modContainer.registerConfig(ModConfig.Type.CLIENT, CreatorSMPConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        LOGGER.info("CreatorSMP Core mod initialized successfully.");
    }
}
