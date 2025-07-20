package org.xznetwork.ecopower.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class PowerPlanDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger("EcoPower|PlanDetector");
    private static final Pattern GUID_PATTERN = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

    public String detectHighPerformanceGuid() throws IOException {
        try {
            return detectPlanGuid("高性能");
        } catch (IOException e1) {
            LOGGER.debug("Chinese high performance plan not found, trying English...");
            try {
                return detectPlanGuid("High performance");
            } catch (IOException e2) {
                throw new IOException("Could not detect High performance power plan GUID in Chinese or English");
            }
        }
    }

    public String detectBalancedGuid() throws IOException {
        try {
            return detectPlanGuid("平衡");
        } catch (IOException e1) {
            LOGGER.debug("Chinese balanced plan not found, trying English...");
            try {
                return detectPlanGuid("Balanced");
            } catch (IOException e2) {
                throw new IOException("Could not detect Balanced power plan GUID in Chinese or English");
            }
        }
    }

    public String detectPowerSaverGuid() throws IOException {
        try {
            return detectPlanGuid("节能");
        } catch (IOException e1) {
            LOGGER.debug("Chinese balanced plan not found, trying English...");
            try {
                return detectPlanGuid("Power saver");
            } catch (IOException e2) {
                throw new IOException("Could not detect Power saver power plan GUID in Chinese or English");
            }
        }
    }

    public String getCurrentActivePlan() throws IOException {
        Process process = Runtime.getRuntime().exec("powercfg /getactivescheme");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("GUID")) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (GUID_PATTERN.matcher(part).matches()) {
                            return part;
                        }
                    }
                }
            }
        }
        throw new IOException("Could not detect current active power plan");
    }

    private String detectPlanGuid(String planName) throws IOException {
        Process process = Runtime.getRuntime().exec("powercfg /list");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(planName)) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (GUID_PATTERN.matcher(part).matches()) {
                            return part;
                        }
                    }
                }
            }
        }
        throw new IOException("Could not detect " + planName + " power plan GUID");
    }
}