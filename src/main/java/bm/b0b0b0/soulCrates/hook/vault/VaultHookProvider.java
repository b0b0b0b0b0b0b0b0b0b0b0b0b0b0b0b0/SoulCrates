package bm.b0b0b0.soulCrates.hook.vault;

import bm.b0b0b0.soulCrates.hook.HookProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultHookProvider extends HookProvider<VaultEconomyHook> {

    public VaultHookProvider() {
        super(
                "Vault",
                "Install Vault for economy rewards and costs.",
                "https://www.spigotmc.org/resources/vault.34315/",
                false
        );
    }

    @Override
    public VaultEconomyHook createHook(JavaPlugin plugin) {
        return new VaultEconomyHook(plugin);
    }
}
