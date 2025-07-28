package org.xznetwork.ecopower.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("EcoPower|Config");
    private final Path configPath;

    public ConfigManager(Path configDir) {
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                LOGGER.error("Failed to create config directory", e);
            }
        }
        this.configPath = configDir.resolve("config.yaml");
    }

    public Config loadOrCreateConfig() {
        Config config = new Config();

        if (!Files.exists(configPath)) {
            try {
                PowerPlanDetector detector = new PowerPlanDetector();

                // Create Default Plans
                Map<String, Config.PlanEntry> defaultPlans = new LinkedHashMap<>();

                // Power Saver
                Config.PlanEntry powerSaver = new Config.PlanEntry();
                powerSaver.setGuid(detector.detectPowerSaverGuid());
                powerSaver.setRange(new int[]{Integer.MIN_VALUE, 0});
                powerSaver.setBroadcastMessage("§a[Eco Power] §7witched to §aPower Saver Mode");
                defaultPlans.put("power_saver", powerSaver);

                // Balanced
                Config.PlanEntry balanced = new Config.PlanEntry();
                balanced.setGuid(detector.detectBalancedGuid());
                balanced.setRange(new int[]{1, 2});
                balanced.setBroadcastMessage("§a[Eco Power] §7Switched to §eBalanced Mode");
                defaultPlans.put("balanced", balanced);

                // High Performance
                Config.PlanEntry highPerformance = new Config.PlanEntry();
                highPerformance.setGuid(detector.detectHighPerformanceGuid());
                highPerformance.setRange(new int[]{3, Integer.MAX_VALUE});
                highPerformance.setBroadcastMessage("§a[Eco Power] §7Switched to §cHigh Performance Mode");
                defaultPlans.put("high_performance", highPerformance);

                config.setPlan(defaultPlans);

                // Save to file
                saveConfig(config);
                LOGGER.info("[Eco Power] Created new config file with detected power plans");
            } catch (Exception e) {
                LOGGER.warn("[Eco Power] Could not auto-detect power plans. Please configure manually and use /ecopower reload", e);
                saveConfig(config); // 保存空配置
            }
        } else {
            try (InputStream input = Files.newInputStream(configPath)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(input);

                if (data != null) {
                    Map<String, Object> planData = (Map<String, Object>) data.get("plan");
                    if (planData != null) {
                        Map<String, Config.PlanEntry> plans = new LinkedHashMap<>();
                        for (Map.Entry<String, Object> entry : planData.entrySet()) {
                            Map<String, Object> planEntry = (Map<String, Object>) entry.getValue();

                            Config.PlanEntry plan = new Config.PlanEntry();
                            plan.setGuid((String) planEntry.get("guid"));

                            List<Integer> rangeList = (List<Integer>) planEntry.get("range");
                            if (rangeList != null && rangeList.size() == 2) {
                                plan.setRange(new int[]{rangeList.get(0), rangeList.get(1)});
                            } else {
                                LOGGER.warn("[Eco Power] Invalid range format for plan: " + entry.getKey());
                            }

                            plan.setBroadcastMessage((String) planEntry.get("broadcastMessage"));
                            plans.put(entry.getKey(), plan);
                        }
                        config.setPlan(plans);
                    }

                    Map<String, Object> exclude = (Map<String, Object>) data.get("exclude");
                    if (exclude != null) {
                        Config.Exclude excludeObj = new Config.Exclude();
                        excludeObj.setPrefixes(getStringList(exclude, "prefixes"));
                        excludeObj.setSuffixes(getStringList(exclude, "suffixes"));
                        excludeObj.setRegexes(getStringList(exclude, "regexes"));
                        config.setExclude(excludeObj);
                    }

                    if (data.containsKey("broadcastChanges")) {
                        config.setBroadcastChanges((boolean) data.get("broadcastChanges"));
                    }
                }
            } catch (IOException e) {
                LOGGER.error("[Eco Power] Error loading config file", e);
            }
        }

        return config;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return new ArrayList<>();
    }

    private void saveConfig(Config config) {
        try (Writer writer = new FileWriter(configPath.toFile())) {
            Map<String, Object> configMap = new LinkedHashMap<>();

            // 保存计划配置
            Map<String, Object> planMap = new LinkedHashMap<>();
            for (Map.Entry<String, Config.PlanEntry> entry : config.getPlan().entrySet()) {
                Map<String, Object> planEntry = new LinkedHashMap<>();
                planEntry.put("guid", entry.getValue().getGuid());

                // 转换范围数组为列表
                int[] range = entry.getValue().getRange();
                planEntry.put("range", Arrays.asList(range[0], range[1]));

                planEntry.put("broadcastMessage", entry.getValue().getBroadcastMessage());
                planMap.put(entry.getKey(), planEntry);
            }
            configMap.put("plan", planMap);

            // 保存排除规则
            Config.Exclude exclude = config.getExclude();
            Map<String, Object> excludeMap = new LinkedHashMap<>();
            excludeMap.put("prefixes", exclude.getPrefixes());
            excludeMap.put("suffixes", exclude.getSuffixes());
            excludeMap.put("regexes", exclude.getRegexes());
            configMap.put("exclude", excludeMap);

            // 保存广播开关
            configMap.put("broadcastChanges", config.isBroadcastChanges());

            // 设置YAML格式选项
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Yaml yaml = new Yaml(options);
            yaml.dump(configMap, writer);
        } catch (IOException e) {
            LOGGER.error("[Eco Power] Failed to save config file", e);
        }
    }
}