package bm.b0b0b0.soulCrates.placeholder;

import bm.b0b0b0.soulCrates.service.CrateService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public final class SoulCratesPlaceholderExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final CrateService crateService;

    public SoulCratesPlaceholderExpansion(JavaPlugin plugin, CrateService crateService) {
        this.plugin = plugin;
        this.crateService = crateService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "soulcrates";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SoulCrates";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        if ("active_session".equals(params)) {
            return crateService.hasActiveSession(player.getUniqueId()) ? "true" : "false";
        }
        if (params.startsWith("keys_")) {
            String crateId = params.substring("keys_".length());
            return Integer.toString(crateService.virtualKeys(player.getUniqueId(), crateId));
        }
        if (params.startsWith("physical_keys_")) {
            String crateId = params.substring("physical_keys_".length());
            if (!player.isOnline()) {
                return "0";
            }
            Player online = player.getPlayer();
            if (online == null || crateService.keyService() == null) {
                return "0";
            }
            return Integer.toString(crateService.keyService().countPhysicalKeys(online, crateId));
        }
        if (params.startsWith("total_keys_")) {
            String crateId = params.substring("total_keys_".length());
            if (!player.isOnline()) {
                return Integer.toString(crateService.virtualKeys(player.getUniqueId(), crateId));
            }
            Player online = player.getPlayer();
            if (online == null) {
                return Integer.toString(crateService.virtualKeys(player.getUniqueId(), crateId));
            }
            return Integer.toString(crateService.totalKeys(online, crateId));
        }
        if (params.startsWith("opens_")) {
            String crateId = params.substring("opens_".length());
            if (crateService.playerDataService() == null) {
                return "0";
            }
            return Integer.toString(crateService.playerDataService().opens(player.getUniqueId(), crateId));
        }
        if (params.startsWith("pity_")) {
            String crateId = params.substring("pity_".length());
            if (crateService.playerDataService() == null) {
                return "0";
            }
            return Integer.toString(crateService.playerDataService().pity(player.getUniqueId(), crateId));
        }
        if (params.startsWith("last_reward_")) {
            String crateId = params.substring("last_reward_".length());
            if (crateService.playerDataService() == null) {
                return "";
            }
            return crateService.playerDataService().lastReward(player.getUniqueId(), crateId);
        }
        return null;
    }
}
