package bm.b0b0b0.soulCrates.bootstrap;

import bm.b0b0b0.soulCrates.animation.PhaseFactory;
import bm.b0b0b0.soulCrates.api.SoulCratesApi;
import bm.b0b0b0.soulCrates.command.CratesCommand;
import bm.b0b0b0.soulCrates.config.ConfigurationLoader;
import bm.b0b0b0.soulCrates.config.PluginConfig;
import bm.b0b0b0.soulCrates.database.DatabaseBootstrap;
import bm.b0b0b0.soulCrates.engine.DisplayEngineRegistry;
import bm.b0b0b0.soulCrates.gui.SoulGuiListener;
import bm.b0b0b0.soulCrates.hook.HookRegistry;
import bm.b0b0b0.soulCrates.hook.modelengine.ModelEngineHookProvider;
import bm.b0b0b0.soulCrates.hook.placeholder.PlaceholderHookProvider;
import bm.b0b0b0.soulCrates.hook.vault.VaultHookProvider;
import bm.b0b0b0.soulCrates.config.settings.MessagesSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.listener.CrateBlockProtectListener;
import bm.b0b0b0.soulCrates.listener.CrateChunkListener;
import bm.b0b0b0.soulCrates.listener.CrateInteractListener;
import bm.b0b0b0.soulCrates.listener.LootBoxListener;
import bm.b0b0b0.soulCrates.listener.MobPickListener;
import bm.b0b0b0.soulCrates.listener.PhysicalCrateListener;
import bm.b0b0b0.soulCrates.listener.ShulkerPickListener;
import bm.b0b0b0.soulCrates.listener.NpcInteractListener;
import bm.b0b0b0.soulCrates.listener.PlayerJoinListener;
import bm.b0b0b0.soulCrates.redis.RedisPlayerMirror;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.CrateService;
import bm.b0b0b0.soulCrates.service.claim.ClaimService;
import bm.b0b0b0.soulCrates.service.hologram.CrateHologramService;
import bm.b0b0b0.soulCrates.service.idle.IdleCrateDisplayService;
import bm.b0b0b0.soulCrates.service.idle.IdleParticleService;
import bm.b0b0b0.soulCrates.service.lootbox.LootBoxService;
import bm.b0b0b0.soulCrates.service.physical.PhysicalCrateService;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import bm.b0b0b0.soulCrates.service.location.CrateLocationService;
import bm.b0b0b0.soulCrates.service.npc.CrateNpcService;
import bm.b0b0b0.soulCrates.service.open.BulkOpenService;
import bm.b0b0b0.soulCrates.service.player.PlayerDataService;
import bm.b0b0b0.soulCrates.service.reward.BroadcastService;
import bm.b0b0b0.soulCrates.service.reward.PityService;
import bm.b0b0b0.soulCrates.service.reward.RewardDeliveryService;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import bm.b0b0b0.soulCrates.service.reroll.RerollService;
import bm.b0b0b0.soulCrates.service.shop.KeyShopService;
import bm.b0b0b0.soulCrates.service.winner.LastWinnerService;
import bm.b0b0b0.soulCrates.session.SessionRegistry;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulCratesCore {

    private final JavaPlugin plugin;
    private final SoulCratesStartupLog startupLog;
    private ConfigurationLoader configurationLoader;
    private PluginConfig pluginConfig;
    private MessageService messageService;
    private HookRegistry hookRegistry;
    private CrateRegistry crateRegistry;
    private DisplayEngineRegistry displayEngineRegistry;
    private SessionRegistry sessionRegistry;
    private RewardRollService rewardRollService;
    private RewardDeliveryService rewardDeliveryService;
    private PhaseFactory phaseFactory;
    private CrateService crateService;
    private KeyService keyService;
    private CrateLocationService locationService;
    private PlayerDataService playerDataService;
    private BroadcastService broadcastService;
    private IdleCrateDisplayService idleCrateDisplayService;
    private CrateHologramService hologramService;
    private BulkOpenService bulkOpenService;
    private KeyShopService keyShopService;
    private CrateNpcService npcService;
    private ClaimService claimService;
    private LastWinnerService lastWinnerService;
    private LootBoxService lootBoxService;
    private PhysicalCrateService physicalCrateService;
    private RedisPlayerMirror redisMirror;
    private RerollService rerollService;
    private CrateInteractListener crateInteractListener;
    private CrateBlockProtectListener crateBlockProtectListener;
    private DatabaseBootstrap databaseBootstrap;
    private SoulCratesApi soulCratesApi;
    private CratesCommand cratesCommand;

    public SoulCratesCore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.startupLog = new SoulCratesStartupLog(plugin);
    }

    public void enable() {
        startupLog.bannerStart(plugin.getPluginMeta().getVersion());
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            startupLog.stepFail("Data folder — failed to create");
        }
        startupLog.stepSchedulers();
        configurationLoader = new ConfigurationLoader(plugin);
        pluginConfig = configurationLoader.load();
        startupLog.stepOk("Config loaded — crates=" + pluginConfig.crateDefinitions().size());
        messageService = new MessageService(plugin);
        applyMessageLocaleSettings();
        startupLog.stepOk("Messages — lang: " + String.join(", ", messageService.loadedLocaleIds()));
        crateRegistry = new CrateRegistry();
        displayEngineRegistry = new DisplayEngineRegistry(plugin);
        sessionRegistry = new SessionRegistry(plugin, pluginConfig.cratesSettings().sessionTimeoutSeconds);
        rewardRollService = new RewardRollService();
        hookRegistry = new HookRegistry(plugin);
        hookRegistry.registerProvider(new VaultHookProvider());
        hookRegistry.registerProvider(new ModelEngineHookProvider());
        hookRegistry.registerProvider(new PlaceholderHookProvider(() -> crateService));
        hookRegistry.registerHooks();
        rewardDeliveryService = new RewardDeliveryService(plugin, hookRegistry);
        broadcastService = new BroadcastService(pluginConfig.cratesSettings().broadcast, messageService);
        phaseFactory = new PhaseFactory(plugin, messageService, broadcastService, pluginConfig.guiSpinnerSettings());
        rerollService = new RerollService(rewardRollService, hookRegistry);
        crateService = new CrateService(
                plugin,
                configurationLoader,
                pluginConfig,
                messageService,
                crateRegistry,
                displayEngineRegistry,
                sessionRegistry,
                rewardRollService,
                rewardDeliveryService,
                phaseFactory
        );
        soulCratesApi = new SoulCratesApi(crateService);
        plugin.getServer().getPluginManager().registerEvents(new SoulGuiListener(pluginConfig.guiGeneralSettings()), plugin);
        cratesCommand = new CratesCommand(this);
        registerCommands();
        databaseBootstrap = new DatabaseBootstrap(plugin, pluginConfig.cratesSettings().database);
        PluginSchedulers.runAsync(plugin, () -> databaseBootstrap.start().whenComplete((repository, error) -> {
            if (error != null) {
                startupLog.stepFail("Database — " + error.getMessage());
                return;
            }
            attachRepository(repository);
        }));
        hookRegistry.loadHooks();
    }

    private void attachRepository(CrateRepository repository) {
        boolean mysql = "MYSQL".equalsIgnoreCase(pluginConfig.cratesSettings().database.mode);
        redisMirror = new RedisPlayerMirror(plugin, mysql, pluginConfig.cratesSettings().redis);
        keyService = new KeyService(plugin, messageService, repository);
        keyService.attachMirror(redisMirror);
        locationService = new CrateLocationService(repository);
        playerDataService = new PlayerDataService(repository, keyService);
        playerDataService.attachMirror(redisMirror);
        npcService = new CrateNpcService(repository);
        claimService = new ClaimService(repository, rewardDeliveryService, pluginConfig.cratesSettings().claim);
        lastWinnerService = new LastWinnerService(repository, pluginConfig.cratesSettings().lastWinner);
        lootBoxService = new LootBoxService(plugin, messageService);
        physicalCrateService = new PhysicalCrateService(plugin, messageService, repository);
        physicalCrateService.attachCrateRegistry(crateRegistry);
        physicalCrateService.applySettings(pluginConfig.cratesSettings().physicalCrates);
        physicalCrateService.loadCache();
        bulkOpenService = new BulkOpenService(
                rewardRollService,
                new PityService(repository),
                rewardDeliveryService,
                broadcastService,
                playerDataService,
                repository
        );
        bulkOpenService.attachClaim(claimService, lastWinnerService, pluginConfig.cratesSettings().claim);
        keyShopService = new KeyShopService(
                pluginConfig.crateShopSettings(),
                hookRegistry,
                keyService,
                crateRegistry,
                messageService
        );
        hologramService = new CrateHologramService(
                plugin,
                messageService,
                crateRegistry,
                pluginConfig.cratesSettings().idleDisplay.hologram,
                lastWinnerService
        );
        IdleParticleService idleParticleService = new IdleParticleService(plugin, pluginConfig.cratesSettings().idleDisplay);
        idleCrateDisplayService = new IdleCrateDisplayService(
                plugin,
                pluginConfig.cratesSettings().idleDisplay,
                displayEngineRegistry,
                crateRegistry,
                locationService,
                hologramService,
                idleParticleService
        );
        var crateIds = crateRegistry.list().stream().map(crate -> crate.id()).toList();
        redisMirror.startSubscriber(
                (playerId, payload) -> {
                    String[] parts = payload.split("\\|", 2);
                    if (parts.length < 2) {
                        return;
                    }
                    keyService.applyRemoteKeys(playerId, parts[0], Integer.parseInt(parts[1]));
                },
                (playerId, payload) -> {
                    String[] parts = payload.split("\\|", 2);
                    if (parts.length < 2) {
                        return;
                    }
                    playerDataService.applyRemotePity(playerId, parts[0], Integer.parseInt(parts[1]));
                },
                playerId -> PluginSchedulers.runAsync(plugin, () -> playerDataService.reloadFromRemote(playerId, crateIds))
        );
        CompletableFuture.allOf(locationService.loadAll(), npcService.loadAll()).whenComplete((ignored, error) -> PluginSchedulers.runGlobal(plugin, () -> {
            if (error != null) {
                startupLog.stepFail("Locations — " + error.getMessage());
            }
            crateService.attachRepository(
                    repository,
                    keyService,
                    locationService,
                    rerollService,
                    playerDataService,
                    broadcastService,
                    idleCrateDisplayService,
                    bulkOpenService,
                    keyShopService,
                    npcService,
                    claimService,
                    lastWinnerService,
                    lootBoxService,
                    configurationLoader,
                    rewardDeliveryService,
                    displayEngineRegistry,
                    phaseFactory,
                    hookRegistry,
                    physicalCrateService
            );
            for (String crateId : crateIds) {
                lastWinnerService.preload(crateId);
            }
            idleCrateDisplayService.spawnAll();
            crateInteractListener = new CrateInteractListener(
                    crateService,
                    locationService,
                    physicalCrateService,
                    pluginConfig.cratesSettings().idleDisplay
            );
            crateBlockProtectListener = new CrateBlockProtectListener(
                    plugin,
                    messageService,
                    locationService,
                    idleCrateDisplayService,
                    pluginConfig.cratesSettings().idleDisplay
            );
            plugin.getServer().getPluginManager().registerEvents(crateInteractListener, plugin);
            plugin.getServer().getPluginManager().registerEvents(crateBlockProtectListener, plugin);
            plugin.getServer().getPluginManager().registerEvents(
                    new PhysicalCrateListener(plugin, messageService, crateRegistry, physicalCrateService),
                    plugin
            );
            plugin.getServer().getPluginManager().registerEvents(new ShulkerPickListener(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new MobPickListener(plugin), plugin);
            plugin.getServer().getPluginManager().registerEvents(new NpcInteractListener(crateService, npcService), plugin);
            plugin.getServer().getPluginManager().registerEvents(new LootBoxListener(crateService, lootBoxService), plugin);
            plugin.getServer().getPluginManager().registerEvents(new PlayerJoinListener(plugin, playerDataService, claimService, crateRegistry), plugin);
            plugin.getServer().getPluginManager().registerEvents(new CrateChunkListener(idleCrateDisplayService), plugin);
            for (org.bukkit.entity.Player online : plugin.getServer().getOnlinePlayers()) {
                PluginSchedulers.runAsync(plugin, () -> playerDataService.preload(online.getUniqueId(), crateIds));
            }
            startupLog.stepOk("Database ready — locations="
                    + locationService.allBindings().size()
                    + ", npcs="
                    + npcService.allBindings().size()
                    + (redisMirror.enabled() ? ", redis=on" : ""));
            startupLog.bannerReady();
        }));
    }

    public void reload() {
        pluginConfig = configurationLoader.load();
        messageService.reload();
        applyMessageLocaleSettings();
        phaseFactory = new PhaseFactory(plugin, messageService, broadcastService, pluginConfig.guiSpinnerSettings());
        broadcastService.applySettings(pluginConfig.cratesSettings().broadcast);
        if (claimService != null) {
            claimService.applySettings(pluginConfig.cratesSettings().claim);
        }
        if (lastWinnerService != null) {
            lastWinnerService.applySettings(pluginConfig.cratesSettings().lastWinner);
        }
        if (keyShopService != null) {
            keyShopService.applySettings(pluginConfig.crateShopSettings());
        }
        if (physicalCrateService != null) {
            physicalCrateService.applySettings(pluginConfig.cratesSettings().physicalCrates);
        }
        crateService.applyConfig(pluginConfig);
        if (crateInteractListener != null) {
            crateInteractListener.applySettings(pluginConfig.cratesSettings().idleDisplay);
        }
        if (crateBlockProtectListener != null) {
            crateBlockProtectListener.applySettings(pluginConfig.cratesSettings().idleDisplay);
        }
        startupLog.stepOk("Reload complete — crates=" + pluginConfig.crateDefinitions().size());
    }

    private void applyMessageLocaleSettings() {
        messageService.setForcedLocaleSupplier(() -> {
            MessagesSettings messages = pluginConfig.cratesSettings().messages;
            if (messages == null || !"SERVER".equalsIgnoreCase(messages.localeMode)) {
                return null;
            }
            return messages.serverLocale;
        });
    }

    public void disable() {
        if (hookRegistry != null) {
            hookRegistry.unloadHooks();
        }
        if (redisMirror != null) {
            redisMirror.close();
            redisMirror = null;
        }
        if (crateService != null) {
            crateService.shutdown();
        }
        if (npcService != null) {
            npcService.clear();
        }
        if (databaseBootstrap != null) {
            databaseBootstrap.shutdown();
        }
        startupLog.bannerShutdown();
    }

    private void registerCommands() {
        PluginCommand command = plugin.getCommand("soulcrates");
        command.setExecutor(cratesCommand);
        command.setTabCompleter(cratesCommand);
    }

    public ConfigurationLoader configurationLoader() {
        return configurationLoader;
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }

    public MessageService messageService() {
        return messageService;
    }

    public CrateService crateService() {
        return crateService;
    }

    public SoulCratesApi api() {
        return soulCratesApi;
    }
}
