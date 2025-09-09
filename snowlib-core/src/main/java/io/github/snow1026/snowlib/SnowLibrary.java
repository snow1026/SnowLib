package io.github.snow1026.snowlib;

import org.bukkit.plugin.java.JavaPlugin;

public class SnowLibrary extends JavaPlugin {
    public static SnowLibrary instance;

    @Override
    public void onEnable() {
        instance = this;
    }
}
