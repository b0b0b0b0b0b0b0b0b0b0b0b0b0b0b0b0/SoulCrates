package bm.b0b0b0.soulCrates.service.menu;

import bm.b0b0b0.soulCrates.config.ConfigurationLoader;
import bm.b0b0b0.soulCrates.config.PluginConfig;
import bm.b0b0b0.soulCrates.config.settings.CrateDefinitionSettings;
import bm.b0b0b0.soulCrates.gui.CrateClaimMenu;
import bm.b0b0b0.soulCrates.gui.CratePreviewMenu;
import bm.b0b0b0.soulCrates.gui.KeyShopMenu;
import bm.b0b0b0.soulCrates.gui.VirtualKeysMenu;
import bm.b0b0b0.soulCrates.gui.editor.CrateEditorListMenu;
import bm.b0b0b0.soulCrates.gui.editor.CrateEditorMenu;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.claim.ClaimService;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import bm.b0b0b0.soulCrates.service.open.CrateOpenCallbacks;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import bm.b0b0b0.soulCrates.service.shop.KeyShopService;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateMenuService {

    private final JavaPlugin plugin;
    private final ConfigurationLoader configurationLoader;
    private final MessageService messageService;
    private final CrateRegistry crateRegistry;
    private final RewardRollService rewardRollService;
    private final KeyShopService keyShopService;
    private final ClaimService claimService;
    private final KeyService keyService;
    private final CrateOpenCallbacks openCallbacks;
    private final Consumer<PluginConfig> configApplier;
    private PluginConfig pluginConfig;

    public CrateMenuService(
            JavaPlugin plugin,
            ConfigurationLoader configurationLoader,
            PluginConfig pluginConfig,
            MessageService messageService,
            CrateRegistry crateRegistry,
            RewardRollService rewardRollService,
            KeyShopService keyShopService,
            ClaimService claimService,
            KeyService keyService,
            CrateOpenCallbacks openCallbacks,
            Consumer<PluginConfig> configApplier
    ) {
        this.plugin = plugin;
        this.configurationLoader = configurationLoader;
        this.pluginConfig = pluginConfig;
        this.messageService = messageService;
        this.crateRegistry = crateRegistry;
        this.rewardRollService = rewardRollService;
        this.keyShopService = keyShopService;
        this.claimService = claimService;
        this.keyService = keyService;
        this.openCallbacks = openCallbacks;
        this.configApplier = configApplier;
    }

    public void applyConfig(PluginConfig config) {
        this.pluginConfig = config;
    }

    public void openPreview(Player player, String crateId) {
        openPreview(player, crateId, null);
    }

    public void openPreview(Player player, String crateId, Location openLocation) {
        Optional<CrateDefinition> crateOptional = crateRegistry.find(crateId);
        if (crateOptional.isEmpty()) {
            messageService.send(player.getUniqueId(), "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        CrateDefinition crate = crateOptional.get();
        Location resolved = openLocation == null ? player.getLocation() : openLocation;
        CratePreviewMenu menu = new CratePreviewMenu(
                player.getUniqueId(),
                messageService,
                pluginConfig.guiPreviewSettings(),
                pluginConfig.cratesSettings().premiumOpening,
                crate,
                rewardRollService,
                (target, amount) -> openCallbacks.proceedOpenFlow(target, crate, resolved, amount),
                null
        );
        PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
    }

    public void openEditor(Player player) {
        if (!player.hasPermission("soulcrates.command.admin")) {
            messageService.send(player.getUniqueId(), "no-permission");
            return;
        }
        CrateEditorListMenu menu = new CrateEditorListMenu(
                player.getUniqueId(),
                messageService,
                pluginConfig.guiEditorSettings(),
                crateRegistry.list(),
                this::openEditorCrate,
                openCallbacks::reloadCrates
        );
        PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
    }

    private void openEditorCrate(Player player, CrateDefinition crateDefinition) {
        CrateDefinitionSettings settings = configurationLoader.loadCrateSettings(crateDefinition.id());
        CrateEditorMenu menu = new CrateEditorMenu(
                plugin,
                player.getUniqueId(),
                messageService,
                pluginConfig.guiEditorSettings(),
                crateDefinition,
                settings,
                mutable -> PluginSchedulers.runAsync(plugin, () -> {
                    configurationLoader.saveCrateSettings(mutable);
                    PluginSchedulers.run(plugin, player, () -> reloadCrates(player));
                }),
                () -> openEditor(player)
        );
        PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
    }

    public void reloadCrates(Player player) {
        PluginConfig reloaded = configurationLoader.load();
        configApplier.accept(reloaded);
        applyConfig(reloaded);
        if (player != null) {
            messageService.send(player.getUniqueId(), "reload-success");
        }
    }

    public void openShop(Player player) {
        if (keyShopService == null || !pluginConfig.cratesSettings().shop.enabled) {
            messageService.send(player.getUniqueId(), "shop-disabled");
            return;
        }
        KeyShopMenu menu = new KeyShopMenu(
                player.getUniqueId(),
                messageService,
                pluginConfig.guiShopSettings(),
                pluginConfig.cratesSettings().shop,
                crateRegistry,
                (target, entry) -> keyShopService.purchase(target, entry)
        );
        PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
    }

    public void openVirtualKeys(Player player) {
        VirtualKeysMenu menu = new VirtualKeysMenu(
                player.getUniqueId(),
                messageService,
                pluginConfig.guiVirtualKeysSettings(),
                crateRegistry,
                keyService,
                null
        );
        PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
    }

    public void openClaim(Player player) {
        if (claimService == null || !claimService.enabled()) {
            messageService.send(player.getUniqueId(), "claim-disabled");
            return;
        }
        claimService.loadPending(player.getUniqueId()).thenAccept(claims -> PluginSchedulers.run(plugin, player, () -> {
            if (claims.isEmpty()) {
                messageService.send(player.getUniqueId(), "claim-empty");
                return;
            }
            CrateClaimMenu menu = new CrateClaimMenu(
                    plugin,
                    player.getUniqueId(),
                    messageService,
                    pluginConfig.guiClaimSettings(),
                    claimService,
                    claims
            );
            player.openInventory(menu.getInventory());
        }));
    }
}
