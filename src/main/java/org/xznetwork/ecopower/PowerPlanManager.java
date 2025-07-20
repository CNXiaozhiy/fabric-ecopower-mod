package org.xznetwork.ecopower;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xznetwork.ecopower.config.PowerPlanDetector;

import java.io.IOException;

public class PowerPlanManager extends PowerPlanDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger("EcoPower|PowerManager");

    public PowerPlanManager() {}

    public void setActivePlan(String guid) {
        LOGGER.info("[Eco Power] Switching to {} power plan", guid);
        executePowerCfgCommand(guid);
    }

    private void executePowerCfgCommand(String guid) {
        try {
            Process process = Runtime.getRuntime().exec("powercfg /setactive " + guid);
            if (process.waitFor() != 0) {
                LOGGER.error("[Eco Power] Failed to change power plan. Admin rights required?");
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("[Eco Power] Error executing powercfg command", e);
        }
    }
}