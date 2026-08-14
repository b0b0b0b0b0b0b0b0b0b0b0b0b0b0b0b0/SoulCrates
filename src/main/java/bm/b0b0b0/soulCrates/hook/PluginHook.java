package bm.b0b0b0.soulCrates.hook;

import org.bukkit.plugin.java.JavaPlugin;

public interface PluginHook {

    String id();

    boolean enabled();

    void load(JavaPlugin plugin) throws Exception;

    void unload();
}
