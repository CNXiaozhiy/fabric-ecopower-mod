package org.xznetwork.ecopower.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xznetwork.ecopower.config.Config;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PlayerExclusion {
    private static final Logger LOGGER = LoggerFactory.getLogger("EcoPower|Exclusion");
    private Config config;

    public PlayerExclusion(Config config) {
        this.config = config;
    }

    public void updateConfig(Config config) {
        this.config = config;
    }

    public boolean shouldExcludePlayer(String playerName) {
        // Check prefixes
        for (String prefix : config.getExclude().getPrefixes()) {
            if (playerName.startsWith(prefix)) {
                return true;
            }
        }

        // Check suffixes
        for (String suffix : config.getExclude().getSuffixes()) {
            if (playerName.endsWith(suffix)) {
                return true;
            }
        }

        // Check regex patterns
        for (String regex : config.getExclude().getRegexes()) {
            try {
                if (Pattern.matches(regex, playerName)) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                LOGGER.warn("Invalid regex pattern: '{}' - {}", regex, e.getMessage());
            }
        }

        return false;
    }
}