package bm.b0b0b0.soulCrates.listener;

import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.player.PlayerDataService;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataService playerDataService;
    private final CrateRegistry crateRegistry;

    public PlayerJoinListener(JavaPlugin plugin, PlayerDataService playerDataService, CrateRegistry crateRegistry) {
        this.plugin = plugin;
        this.playerDataService = playerDataService;
        this.crateRegistry = crateRegistry;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        List<String> crateIds = crateRegistry.list().stream().map(crate -> crate.id()).toList();
        PluginSchedulers.runAsync(plugin, () -> playerDataService.preload(event.getPlayer().getUniqueId(), crateIds));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataService.unload(event.getPlayer().getUniqueId());
    }
}
