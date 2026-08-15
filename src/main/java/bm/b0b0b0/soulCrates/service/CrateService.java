package bm.b0b0b0.soulCrates.service;

import bm.b0b0b0.soulCrates.animation.PhaseFactory;
import bm.b0b0b0.soulCrates.config.ConfigurationLoader;
import bm.b0b0b0.soulCrates.config.PluginConfig;
import bm.b0b0b0.soulCrates.config.settings.IdleDisplaySettings;
import bm.b0b0b0.soulCrates.engine.DisplayEngineRegistry;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.CrateInstance;
import bm.b0b0b0.soulCrates.hook.HookRegistry;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import bm.b0b0b0.soulCrates.service.reward.WinLimitService;
import bm.b0b0b0.soulCrates.service.admin.CrateAdminService;
import bm.b0b0b0.soulCrates.service.claim.ClaimService;
import bm.b0b0b0.soulCrates.service.idle.IdleCrateDisplayService;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import bm.b0b0b0.soulCrates.service.location.CrateLocationService;
import bm.b0b0b0.soulCrates.service.lootbox.LootBoxService;
import bm.b0b0b0.soulCrates.service.menu.CrateMenuService;
import bm.b0b0b0.soulCrates.service.npc.CrateNpcService;
import bm.b0b0b0.soulCrates.service.open.BulkOpenService;
import bm.b0b0b0.soulCrates.service.open.CrateOpeningService;
import bm.b0b0b0.soulCrates.service.open.OpenCooldownTracker;
import bm.b0b0b0.soulCrates.service.physical.PhysicalCrateService;
import bm.b0b0b0.soulCrates.service.player.PlayerDataService;
import bm.b0b0b0.soulCrates.service.reward.BroadcastService;
import bm.b0b0b0.soulCrates.service.reward.PityService;
import bm.b0b0b0.soulCrates.service.reward.RewardDeliveryService;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import bm.b0b0b0.soulCrates.service.reward.RewardSettlementService;
import bm.b0b0b0.soulCrates.service.reroll.RerollService;
import bm.b0b0b0.soulCrates.service.shop.KeyShopService;
import bm.b0b0b0.soulCrates.service.winner.LastWinnerService;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import bm.b0b0b0.soulCrates.session.SessionRegistry;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateService {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final CrateRegistry crateRegistry;
    private final SessionRegistry sessionRegistry;
    private final RewardRollService rewardRollService;
    private PluginConfig pluginConfig;
    private KeyService keyService;
    private PlayerDataService playerDataService;
    private CrateLocationService locationService;
    private CrateNpcService npcService;
    private ClaimService claimService;
    private LastWinnerService lastWinnerService;
    private PityService pityService;
    private BroadcastService broadcastService;
    private IdleCrateDisplayService idleCrateDisplayService;
    private KeyShopService keyShopService;
    private HookRegistry hookRegistry;
    private OpenCooldownTracker cooldownTracker;
    private CrateOpeningService openingService;
    private CrateMenuService menuService;
    private CrateAdminService adminService;
    private PhysicalCrateService physicalCrateService;
    private volatile boolean loaded;

    public CrateService(
            JavaPlugin plugin,
            ConfigurationLoader configurationLoader,
            PluginConfig pluginConfig,
            MessageService messageService,
            CrateRegistry crateRegistry,
            DisplayEngineRegistry displayEngineRegistry,
            SessionRegistry sessionRegistry,
            RewardRollService rewardRollService,
            RewardDeliveryService rewardDeliveryService,
            PhaseFactory phaseFactory
    ) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.messageService = messageService;
        this.crateRegistry = crateRegistry;
        this.sessionRegistry = sessionRegistry;
        this.rewardRollService = rewardRollService;
        this.crateRegistry.replaceAll(pluginConfig.crateDefinitions());
        this.cooldownTracker = new OpenCooldownTracker(messageService);
    }

    public void applyConfig(PluginConfig config) {
        this.pluginConfig = config;
        this.crateRegistry.replaceAll(config.crateDefinitions());
        if (broadcastService != null) {
            broadcastService.applySettings(config.cratesSettings().broadcast);
        }
        if (idleCrateDisplayService != null) {
            idleCrateDisplayService.applySettings(config.cratesSettings().idleDisplay);
            idleCrateDisplayService.spawnAll();
        }
        if (keyShopService != null) {
            keyShopService.applySettings(config.crateShopSettings());
        }
        if (openingService != null) {
            openingService.applyConfig(config);
        }
        if (menuService != null) {
            menuService.applyConfig(config);
        }
        if (physicalCrateService != null) {
            physicalCrateService.applySettings(config.cratesSettings().physicalCrates);
        }
    }

    public void attachRepository(
            CrateRepository repository,
            KeyService keyService,
            CrateLocationService locationService,
            RerollService rerollService,
            PlayerDataService playerDataService,
            BroadcastService broadcastService,
            IdleCrateDisplayService idleCrateDisplayService,
            BulkOpenService bulkOpenService,
            KeyShopService keyShopService,
            CrateNpcService npcService,
            ClaimService claimService,
            LastWinnerService lastWinnerService,
            LootBoxService lootBoxService,
            ConfigurationLoader configurationLoader,
            RewardDeliveryService rewardDeliveryService,
            DisplayEngineRegistry displayEngineRegistry,
            PhaseFactory phaseFactory,
            HookRegistry hookRegistry,
            PhysicalCrateService physicalCrateService
    ) {
        this.keyService = keyService;
        this.physicalCrateService = physicalCrateService;
        this.hookRegistry = hookRegistry;
        this.playerDataService = playerDataService;
        this.locationService = locationService;
        this.npcService = npcService;
        this.claimService = claimService;
        this.lastWinnerService = lastWinnerService;
        this.broadcastService = broadcastService;
        this.idleCrateDisplayService = idleCrateDisplayService;
        this.keyShopService = keyShopService;
        this.pityService = new PityService(repository);
        if (bulkOpenService != null && claimService != null) {
            bulkOpenService.attachClaim(claimService, lastWinnerService, pluginConfig.cratesSettings().claim);
        }
        CrateAdminService.KeyCountResolver keyCountResolver = new CrateAdminService.KeyCountResolver() {
            @Override
            public int totalKeys(Player player, String crateId) {
                return CrateService.this.totalKeys(player, crateId);
            }

            @Override
            public int virtualKeys(UUID playerId, String crateId) {
                return CrateService.this.virtualKeys(playerId, crateId);
            }
        };
        WinLimitService winLimitService = new WinLimitService(repository, messageService);
        RewardSettlementService rewardSettlementService = new RewardSettlementService(
                messageService,
                rewardDeliveryService,
                broadcastService,
                pityService,
                playerDataService,
                repository,
                claimService,
                lastWinnerService,
                winLimitService,
                () -> pluginConfig.cratesSettings().claim
        );
        openingService = new CrateOpeningService(
                plugin,
                pluginConfig,
                messageService,
                crateRegistry,
                displayEngineRegistry,
                sessionRegistry,
                rewardRollService,
                phaseFactory,
                keyService,
                bulkOpenService,
                rerollService,
                pityService,
                rewardSettlementService,
                winLimitService,
                hookRegistry,
                cooldownTracker,
                idleCrateDisplayService,
                locationService,
                lootBoxService,
                keyCountResolver
        );
        menuService = new CrateMenuService(
                plugin,
                configurationLoader,
                pluginConfig,
                messageService,
                crateRegistry,
                rewardRollService,
                keyShopService,
                claimService,
                keyService,
                locationService,
                physicalCrateService,
                openingService,
                this::applyConfig
        );
        openingService.attachMenuService(menuService);
        if (physicalCrateService != null) {
            openingService.attachPhysicalCrateService(physicalCrateService);
        }
        adminService = new CrateAdminService(
                plugin,
                messageService,
                crateRegistry,
                keyService,
                locationService,
                npcService,
                playerDataService,
                idleCrateDisplayService,
                lootBoxService,
                claimService,
                keyCountResolver,
                physicalCrateService
        );
        adminService.attachConfigurationLoader(configurationLoader);
        if (broadcastService != null) {
            broadcastService.applySettings(pluginConfig.cratesSettings().broadcast);
        }
        if (idleCrateDisplayService != null) {
            idleCrateDisplayService.applySettings(pluginConfig.cratesSettings().idleDisplay);
            idleCrateDisplayService.spawnAll();
        }
        if (keyShopService != null) {
            keyShopService.applySettings(pluginConfig.crateShopSettings());
        }
        loaded = true;
    }

    public void shutdown() {
        loaded = false;
        sessionRegistry.cancelAll();
        if (cooldownTracker != null) {
            cooldownTracker.clear();
        }
        if (keyService != null) {
            keyService.clearCache();
        }
        if (locationService != null) {
            locationService.shutdown();
            locationService.clear();
        }
        if (playerDataService != null) {
            playerDataService.clearAll();
        }
        if (idleCrateDisplayService != null) {
            idleCrateDisplayService.shutdown();
        }
        if (physicalCrateService != null) {
            physicalCrateService.shutdown();
            physicalCrateService.clearCache();
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean ready(Player player) {
        if (!loaded) {
            messageService.send(player.getUniqueId(), "startup-not-ready");
            return false;
        }
        if (sessionRegistry.isBusy(player.getUniqueId())) {
            messageService.send(player.getUniqueId(), "open-already");
            return false;
        }
        return true;
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }

    public CrateRegistry crateRegistry() {
        return crateRegistry;
    }

    public MessageService messageService() {
        return messageService;
    }

    public KeyService keyService() {
        return keyService;
    }

    public PlayerDataService playerDataService() {
        return playerDataService;
    }

    public CrateLocationService locationService() {
        return locationService;
    }

    public CrateNpcService npcService() {
        return npcService;
    }

    public ClaimService claimService() {
        return claimService;
    }

    public LastWinnerService lastWinnerService() {
        return lastWinnerService;
    }

    public IdleDisplaySettings idleDisplaySettings() {
        return pluginConfig.cratesSettings().idleDisplay;
    }

    public RewardRollService rewardRollService() {
        return rewardRollService;
    }

    public Optional<CrateDefinition> findCrate(String crateId) {
        return crateRegistry.find(crateId);
    }

    public boolean hasActiveSession(UUID playerId) {
        return sessionRegistry.isBusy(playerId);
    }

    public int virtualKeys(UUID playerId, String crateId) {
        if (keyService == null) {
            return 0;
        }
        return keyService.virtualKeys(playerId, crateId);
    }

    public int totalKeys(Player player, String crateId) {
        int total = virtualKeys(player.getUniqueId(), crateId);
        if (keyService != null) {
            total += keyService.countPhysicalKeys(player, crateId);
        }
        return total;
    }

    public void openPreview(Player player, String crateId) {
        openPreview(player, crateId, null);
    }

    public void openPreview(Player player, String crateId, Location openLocation) {
        if (!ready(player)) {
            return;
        }
        if (openingService.notifyIfBoundLocationBusy(player, openLocation)) {
            return;
        }
        menuService.openPreview(player, crateId, openLocation);
    }

    public void openPlacedCrate(Player player, CrateInstance instance, Location openLocation) {
        if (!ready(player)) {
            return;
        }
        if (physicalCrateService != null && !physicalCrateService.canOpen(player, instance)) {
            messageService.send(player.getUniqueId(), "physical-crate-open-denied");
            return;
        }
        Optional<CrateDefinition> crateOptional = crateRegistry.find(instance.crateId());
        if (crateOptional.isPresent()) {
            CrateDefinition crate = crateOptional.get();
            if (!crate.opening().permission.isBlank() && !player.hasPermission(crate.opening().permission)) {
                messageService.send(player.getUniqueId(), "no-permission");
                return;
            }
        }
        menuService.openPlacedCrate(player, instance, openLocation);
    }

    public void pickupPlacedCrate(Player player, CrateInstance instance, Location blockLocation) {
        if (!ready(player) || physicalCrateService == null || !physicalCrateService.enabled()) {
            return;
        }
        Optional<CrateDefinition> crateOptional = crateRegistry.find(instance.crateId());
        if (crateOptional.isEmpty()) {
            messageService.send(player.getUniqueId(), "crate-not-found", messageService.placeholder("crate", instance.crateId()));
            return;
        }
        physicalCrateService.pickupPlacedCrate(player, crateOptional.get(), instance, blockLocation);
    }

    public void openEditor(Player player) {
        menuService.openEditor(player);
    }

    public void reloadCrates(Player player) {
        menuService.reloadCrates(player);
    }

    public void beginOpen(Player player, String crateId, Location location) {
        beginOpen(player, crateId, location, 1);
    }

    public void beginOpen(Player player, String crateId, Location location, int amount) {
        if (!ready(player)) {
            return;
        }
        openingService.beginOpen(player, crateId, location, amount);
    }

    public void openShop(Player player) {
        if (!ready(player)) {
            return;
        }
        menuService.openShop(player);
    }

    public void openClaim(Player player) {
        if (!ready(player)) {
            return;
        }
        menuService.openClaim(player);
    }

    public void openVirtualKeys(Player player) {
        if (!ready(player)) {
            return;
        }
        menuService.openVirtualKeys(player);
    }

    public void payVirtualKeys(Player sender, Player target, String crateId, int amount) {
        if (!isLoaded()) {
            messageService.send(sender.getUniqueId(), "startup-not-ready");
            return;
        }
        Optional<CrateDefinition> crateOptional = crateRegistry.find(crateId);
        if (crateOptional.isEmpty()) {
            messageService.send(sender.getUniqueId(), "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        CrateDefinition crate = crateOptional.get();
        if (!crate.keys().enabled || !crate.keys().virtualKeys) {
            messageService.send(sender.getUniqueId(), "paykey-virtual-disabled");
            return;
        }
        if (amount <= 0) {
            messageService.send(sender.getUniqueId(), "paykey-invalid-amount");
            return;
        }
        if (keyService.virtualKeys(sender.getUniqueId(), crateId) < amount) {
            messageService.send(sender.getUniqueId(), "paykey-not-enough");
            return;
        }
        keyService.transferVirtualKeys(sender.getUniqueId(), target.getUniqueId(), crateId, amount).thenAccept(success ->
                PluginSchedulers.run(plugin, sender, () -> {
                    if (!success) {
                        messageService.send(sender.getUniqueId(), "paykey-failed");
                        return;
                    }
                    messageService.send(
                            sender.getUniqueId(),
                            "paykey-success-sender",
                            messageService.placeholder("amount", Integer.toString(amount)),
                            messageService.placeholder("crate", crate.displayName()),
                            messageService.placeholder("player", target.getName())
                    );
                    messageService.send(
                            target.getUniqueId(),
                            "paykey-success-target",
                            messageService.placeholder("amount", Integer.toString(amount)),
                            messageService.placeholder("crate", crate.displayName()),
                            messageService.placeholder("player", sender.getName())
                    );
                })
        );
    }

    public void claimAll(Player player) {
        if (!ready(player)) {
            return;
        }
        adminService.claimAll(player);
    }

    public void openLootBox(Player player, ItemStack item) {
        if (!ready(player)) {
            return;
        }
        openingService.openLootBox(player, item);
    }

    public void giveLootBox(CommandSender sender, Player target, String crateId, int amount) {
        adminService.giveLootBox(sender, target, crateId, amount);
    }

    public void givePhysicalCrate(CommandSender sender, Player target, String crateId, int amount) {
        adminService.givePhysicalCrate(sender, target, crateId, amount);
    }

    public void givePhysicalCrate(CommandSender sender, Player target, String crateId, int amount, String presetId) {
        adminService.givePhysicalCrate(sender, target, crateId, amount, presetId);
    }

    public PhysicalCrateService physicalCrateService() {
        return physicalCrateService;
    }

    public void bindCrate(Player player, String crateId, Location location) {
        adminService.bindCrate(player, crateId, location);
    }

    public void bindCrate(Player player, String crateId, Location location, String presetId) {
        adminService.bindCrate(player, crateId, location, presetId);
    }

    public void unbindCrate(Player player, Location location) {
        adminService.unbindCrate(player, location);
    }

    public void bindNpc(Player player, String crateId, int npcId) {
        adminService.bindNpc(player, crateId, npcId);
    }

    public void unbindNpc(Player player, int npcId) {
        adminService.unbindNpc(player, npcId);
    }

    public void giveKeys(CommandSender sender, Player target, String crateId, int amount, boolean physical) {
        adminService.giveKeys(sender, target, crateId, amount, physical);
    }

    public void showKeys(Player player, String crateId) {
        if (!isLoaded()) {
            messageService.send(player.getUniqueId(), "startup-not-ready");
            return;
        }
        adminService.showKeys(player, crateId);
    }

    public void showStats(Player viewer, Player target) {
        if (!isLoaded()) {
            messageService.send(viewer.getUniqueId(), "startup-not-ready");
            return;
        }
        adminService.showStats(viewer, target);
    }

    public void listLocations(Player player) {
        adminService.listLocations(player);
    }

    public void claimReward(Player player, CrateOpeningSession session) {
        openingService.claimReward(player, session);
    }
}
