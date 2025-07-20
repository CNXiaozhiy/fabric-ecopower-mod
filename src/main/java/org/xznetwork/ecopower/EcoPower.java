package org.xznetwork.ecopower;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xznetwork.ecopower.config.Config;
import org.xznetwork.ecopower.config.ConfigManager;
import org.xznetwork.ecopower.config.Config.PlanEntry;
import org.xznetwork.ecopower.util.PlayerExclusion;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class EcoPower implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("EcoPower");
    private static final String MOD_ID = "ecopower";
    private PowerPlanManager powerPlanManager;
    private PlayerExclusion playerExclusion;
    private Config config;
    private String currentPlanGuid = null;
    private String originalPlanGuid = null;
    private boolean manualOverride = false;

    @Override
    public void onInitialize() {
        LOGGER.info("[Eco Power] Initializing EcoPower...");
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            LOGGER.info("[Eco Power] This mod only works on Windows platforms. Mod will not function.");
            return;
        }

        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        ConfigManager configManager = new ConfigManager(configDir);
        this.config = configManager.loadOrCreateConfig();
        this.powerPlanManager = new PowerPlanManager();
        this.playerExclusion = new PlayerExclusion(config);

        registerCommands();
        registerEvents();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            EcoPowerCommand.register(dispatcher, this);
        });
    }

    private void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (manualOverride) return;

            int realPlayerCount = getRealPlayerCount(server);

            PlanEntry matchedPlan = findMatchingPlan(realPlayerCount);

            if (matchedPlan != null) {
                if (currentPlanGuid == null || !currentPlanGuid.equals(matchedPlan.getGuid())) {
                    powerPlanManager.setActivePlan(matchedPlan.getGuid());
                    currentPlanGuid = matchedPlan.getGuid();

                    LOGGER.info("[Eco Power] Switched to power plan: {} (UUID: {}) for {} players",
                            getPlanName(matchedPlan), matchedPlan.getGuid(), realPlayerCount);

                    if (config.isBroadcastChanges()) {
                        broadcastMessage(server, matchedPlan.getBroadcastMessage());
                    }
                }
            } else {
                LOGGER.warn("[Eco Power] No matching power plan found for {} players", realPlayerCount);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (originalPlanGuid != null && !originalPlanGuid.equals(currentPlanGuid)) {
                powerPlanManager.setActivePlan(originalPlanGuid);
                LOGGER.info("[Eco Power] Server stopping - Restored original power plan: {}", originalPlanGuid);
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                originalPlanGuid = powerPlanManager.getCurrentActivePlan();
                LOGGER.info("[Eco Power] Original power plan detected: {}", originalPlanGuid);
            } catch (IOException e) {
                LOGGER.error("[Eco Power] Failed to detect original power plan", e);
            }

            int realPlayerCount = getRealPlayerCount(server);
            PlanEntry initialPlan = findMatchingPlan(realPlayerCount);

            if (initialPlan != null) {
                powerPlanManager.setActivePlan(initialPlan.getGuid());
                currentPlanGuid = initialPlan.getGuid();
                LOGGER.info("[Eco Power] Initial power plan set to: {} for {} players",
                        getPlanName(initialPlan), realPlayerCount);
            }
        });
    }


    /**
     * 根据玩家数量查找匹配的电源计划
     * 按配置顺序检查，返回第一个匹配的计划
     */
    private PlanEntry findMatchingPlan(int playerCount) {
        for (Map.Entry<String, PlanEntry> entry : config.getPlan().entrySet()) {
            PlanEntry plan = entry.getValue();
            int[] range = plan.getRange();

            // [min, max]
            if (playerCount >= range[0] && playerCount <= range[1]) {
                return plan;
            }
        }
        return null;
    }

    /**
     * 获取计划名称（用于日志）
     */
    private String getPlanName(PlanEntry plan) {
        for (Map.Entry<String, PlanEntry> entry : config.getPlan().entrySet()) {
            if (entry.getValue() == plan) {
                return entry.getKey();
            }
        }
        return "Unknown Plan";
    }

    private int getRealPlayerCount(MinecraftServer server) {
        int count = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!playerExclusion.shouldExcludePlayer(player.getName().getString())) {
                count++;
            }
        }
        return count;
    }

    private void broadcastMessage(MinecraftServer server, String message) {
        try {
            server.getPlayerManager().broadcast(Text.literal(message), false);
        } catch (Exception e) {
            LOGGER.error("[Eco Power] Failed to broadcast message: {}", message, e);
        }
    }

    public void reloadConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        this.config = new ConfigManager(configDir).loadOrCreateConfig();
        this.playerExclusion.updateConfig(config);
        LOGGER.info("[Eco Power] Configuration reloaded");
    }

    public Config getConfig() {
        return config;
    }

    public PlayerExclusion getPlayerExclusion() {
        return playerExclusion;
    }

    public PowerPlanManager getPowerPlanManager() { return powerPlanManager; }

    public boolean getManualOverride() { return manualOverride; }

    public void enableManualOverride() {
        LOGGER.debug("[Eco Power] ");
        manualOverride = true;
    }

    public void disableManualOverride() {
        manualOverride = false;
        currentPlanGuid = null;
    }
}