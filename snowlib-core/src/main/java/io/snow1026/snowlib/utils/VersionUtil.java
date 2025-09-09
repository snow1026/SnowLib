package io.snow1026.snowlib.utils;

import org.bukkit.Bukkit;

public class VersionUtil {

    public enum MappingsVersion {
        v1_17_R1("1.17", "1.17.1"),
        v1_18_R1("1.18", "1.18.1"),
        v1_18_R2("1.18.2"),
        v1_19_R1("1.19", "1.19.1", "1.19.2"),
        v1_19_R2("1.19.3"),
        v1_19_R3("1.19.4"),
        v1_20_R1("1.20", "1.20.1"),
        v1_20_R2("1.20.2"),
        v1_20_R3("1.20.3", "1.20.4"),
        v1_20_R4("1.20.5", "1.20.6"),
        v1_21_R1("1.21", "1.21.1"),
        v1_21_R2("1.21.2", "1.21.3"),
        v1_21_R3("1.21.4"),
        v1_21_R4("1.21.5"),
        v1_21_R5("1.21.6", "1.21.7", "1.21.8");

        private final String[] bukkitVersions;

        MappingsVersion(String... bukkitVersions) {
            this.bukkitVersions = bukkitVersions;
        }

        public static MappingsVersion fromBukkitVersion(String bukkitVersion) {
            if (bukkitVersion == null) {
                throw new IllegalArgumentException("Bukkit version is null");
            }

            String clean = bukkitVersion.split("-")[0];
            for (MappingsVersion ver : values()) {
                for (String bVer : ver.bukkitVersions) {
                    if (bVer.equals(clean)) {
                        return ver;
                    }
                }
            }
            throw new RuntimeException("Unsupported Bukkit version: " + bukkitVersion);
        }
    }

    private static final MappingsVersion currentVersion;

    static {
        String bukkitVersion = Bukkit.getBukkitVersion();
        currentVersion = MappingsVersion.fromBukkitVersion(bukkitVersion);
    }

    public static MappingsVersion getNmsVersion() {
        return currentVersion;
    }
}
