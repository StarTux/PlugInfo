package com.cavetale.pluginfo;

import org.bukkit.plugin.java.JavaPlugin;

public final class PlugInfoPlugin extends JavaPlugin {
    PlugInfoCommand pluginfoCommand = new PlugInfoCommand(this);

    @Override
    public void onEnable() {
        pluginfoCommand.enable();
    }

    @Override
    public void onDisable() {
    }
}
