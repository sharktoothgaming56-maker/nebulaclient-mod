package com.nebulaclient.mod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NebulaClient is entirely client-side (HUDs, waypoints, keybinds) so this
 * common entrypoint doesn't do much — it exists so the mod also loads
 * cleanly if someone ever puts it on a dedicated server by mistake, without
 * crashing the server.
 */
public class NebulaClientMod implements ModInitializer {
    public static final String MOD_ID = "nebulaclient";
    public static final Logger LOGGER = LoggerFactory.getLogger("NebulaClient");

    @Override
    public void onInitialize() {
        LOGGER.info("NebulaClient common init (client-only mod, nothing to do here)");
    }
}
