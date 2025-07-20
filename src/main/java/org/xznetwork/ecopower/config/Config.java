package org.xznetwork.ecopower.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private Map<String, PlanEntry> plan = new HashMap<>();
    private Exclude exclude = new Exclude();
    private boolean broadcastChanges = true;

    public Config() {
        exclude.getPrefixes().add("bot_");
        exclude.getSuffixes().add("_bot");
    }

    public static class PlanEntry {
        private String guid;
        private int[] range = new int[2]; // 两个整数的数组
        private String broadcastMessage;

        // Getters and Setters
        public String getGuid() {
            return guid;
        }

        public void setGuid(String guid) {
            this.guid = guid;
        }

        public int[] getRange() {
            return range;
        }

        public void setRange(int[] range) {
            this.range = range;
        }

        public String getBroadcastMessage() {
            return broadcastMessage;
        }

        public void setBroadcastMessage(String broadcastMessage) {
            this.broadcastMessage = broadcastMessage;
        }
    }

    // 内部类：排除规则配置
    public static class Exclude {
        private List<String> prefixes = new ArrayList<>();
        private List<String> suffixes = new ArrayList<>();
        private List<String> regexes = new ArrayList<>();

        // Getters and Setters
        public List<String> getPrefixes() {
            return prefixes;
        }

        public void setPrefixes(List<String> prefixes) {
            this.prefixes = prefixes;
        }

        public List<String> getSuffixes() {
            return suffixes;
        }

        public void setSuffixes(List<String> suffixes) {
            this.suffixes = suffixes;
        }

        public List<String> getRegexes() {
            return regexes;
        }

        public void setRegexes(List<String> regexes) {
            this.regexes = regexes;
        }
    }

    // Getters and Setters
    public Map<String, PlanEntry> getPlan() {
        return plan;
    }

    public void setPlan(Map<String, PlanEntry> plan) {
        this.plan = plan;
    }

    public Exclude getExclude() {
        return exclude;
    }

    public void setExclude(Exclude exclude) {
        this.exclude = exclude;
    }


    public boolean isBroadcastChanges() {
        return broadcastChanges;
    }

    public void setBroadcastChanges(boolean broadcastChanges) {
        this.broadcastChanges = broadcastChanges;
    }
}