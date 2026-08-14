package bm.b0b0b0.soulCrates.hook.vault;

import bm.b0b0b0.soulCrates.hook.PluginHook;
import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultEconomyHook implements PluginHook {

    private static final String ECONOMY_CLASS = "net.milkbowl.vault.economy.Economy";

    private final Object economy;
    private final Method hasMethod;
    private final Method withdrawMethod;
    private final Method depositMethod;
    private boolean enabled;

    public VaultEconomyHook(JavaPlugin plugin) {
        Object loadedEconomy = null;
        Method loadedHasMethod = null;
        Method loadedWithdrawMethod = null;
        Method loadedDepositMethod = null;
        Plugin vaultPlugin = plugin.getServer().getPluginManager().getPlugin("Vault");
        if (vaultPlugin != null && vaultPlugin.isEnabled()) {
            try {
                ClassLoader vaultLoader = vaultPlugin.getClass().getClassLoader();
                Class<?> economyClass = Class.forName(ECONOMY_CLASS, true, vaultLoader);
                RegisteredServiceProvider<?> provider = plugin.getServer().getServicesManager().getRegistration(economyClass);
                if (provider != null) {
                    loadedEconomy = provider.getProvider();
                    loadedHasMethod = economyClass.getMethod("has", OfflinePlayer.class, double.class);
                    loadedWithdrawMethod = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
                    loadedDepositMethod = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
                }
            } catch (ReflectiveOperationException ignored) {
                loadedEconomy = null;
            }
        }
        this.economy = loadedEconomy;
        this.hasMethod = loadedHasMethod;
        this.withdrawMethod = loadedWithdrawMethod;
        this.depositMethod = loadedDepositMethod;
        this.enabled = economy != null;
    }

    @Override
    public String id() {
        return "vault";
    }

    @Override
    public boolean enabled() {
        return enabled && economy != null;
    }

    @Override
    public void load(JavaPlugin plugin) {
    }

    @Override
    public void unload() {
        enabled = false;
    }

    public boolean has(UUID playerId, double amount) {
        if (!enabled()) {
            return false;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        try {
            Object result = hasMethod.invoke(economy, player, amount);
            return result instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    public boolean withdraw(UUID playerId, double amount) {
        if (!enabled()) {
            return false;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        try {
            Object response = withdrawMethod.invoke(economy, player, amount);
            if (response == null) {
                return false;
            }
            Method successMethod = response.getClass().getMethod("transactionSuccess");
            Object success = successMethod.invoke(response);
            return success instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    public boolean deposit(UUID playerId, double amount) {
        if (!enabled()) {
            return false;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        try {
            Object response = depositMethod.invoke(economy, player, amount);
            if (response == null) {
                return false;
            }
            Method successMethod = response.getClass().getMethod("transactionSuccess");
            Object success = successMethod.invoke(response);
            return success instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}
