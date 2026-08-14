package bm.b0b0b0.soulCrates.service;

import bm.b0b0b0.soulCrates.animation.OpeningAnimationPipeline;
import bm.b0b0b0.soulCrates.animation.PhaseFactory;
import bm.b0b0b0.soulCrates.api.event.CrateOpenFinishEvent;
import bm.b0b0b0.soulCrates.api.event.CrateOpenStartEvent;
import bm.b0b0b0.soulCrates.config.ConfigurationLoader;
import bm.b0b0b0.soulCrates.config.PluginConfig;
import bm.b0b0b0.soulCrates.config.settings.CrateDefinitionSettings;
import bm.b0b0b0.soulCrates.config.settings.IdleDisplaySettings;
import bm.b0b0b0.soulCrates.config.settings.PremiumOpeningSettings;
import bm.b0b0b0.soulCrates.config.settings.RerollSettings;
import bm.b0b0b0.soulCrates.engine.DisplayComponent;
import bm.b0b0b0.soulCrates.engine.DisplayEngineRegistry;
import bm.b0b0b0.soulCrates.gui.CrateConfirmMenu;
import bm.b0b0b0.soulCrates.gui.CratePreviewMenu;
import bm.b0b0b0.soulCrates.gui.CrateRerollMenu;
import bm.b0b0b0.soulCrates.gui.KeyShopMenu;
import bm.b0b0b0.soulCrates.gui.editor.CrateEditorListMenu;
import bm.b0b0b0.soulCrates.gui.editor.CrateEditorMenu;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.OpeningContext;
import bm.b0b0b0.soulCrates.model.OpeningSessionState;
import bm.b0b0b0.soulCrates.model.RewardRollResult;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import bm.b0b0b0.soulCrates.service.idle.IdleCrateDisplayService;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import bm.b0b0b0.soulCrates.service.location.CrateLocationService;
import bm.b0b0b0.soulCrates.service.npc.CrateNpcService;
import bm.b0b0b0.soulCrates.service.player.PlayerDataService;
import bm.b0b0b0.soulCrates.service.reward.BroadcastService;
import bm.b0b0b0.soulCrates.service.reward.PityService;
import bm.b0b0b0.soulCrates.service.reward.RewardDeliveryService;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import bm.b0b0b0.soulCrates.service.open.BulkOpenService;
import bm.b0b0b0.soulCrates.service.reroll.RerollService;
import bm.b0b0b0.soulCrates.service.shop.KeyShopService;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import bm.b0b0b0.soulCrates.session.SessionRegistry;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateService {

    private enum OpeningSkipMode {
        INSTANT,
        SKIP_ANIMATION,
        MULTI
    }

    private final JavaPlugin plugin;
    private final ConfigurationLoader configurationLoader;
    private PluginConfig pluginConfig;
    private final MessageService messageService;
    private final CrateRegistry crateRegistry;
    private final DisplayEngineRegistry displayEngineRegistry;
    private final SessionRegistry sessionRegistry;
    private final RewardRollService rewardRollService;
    private final RewardDeliveryService rewardDeliveryService;
    private final PhaseFactory phaseFactory;
    private PityService pityService;
    private CrateRepository repository;
    private KeyService keyService;
    private CrateLocationService locationService;
    private RerollService rerollService;
    private PlayerDataService playerDataService;
    private BroadcastService broadcastService;
    private IdleCrateDisplayService idleCrateDisplayService;
    private BulkOpenService bulkOpenService;
    private KeyShopService keyShopService;
    private CrateNpcService npcService;
    private volatile boolean loaded;
    private final Map<UUID, Long> openCooldownUntil = new ConcurrentHashMap<>();

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
        this.configurationLoader = configurationLoader;
        this.pluginConfig = pluginConfig;
        this.messageService = messageService;
        this.crateRegistry = crateRegistry;
        this.displayEngineRegistry = displayEngineRegistry;
        this.sessionRegistry = sessionRegistry;
        this.rewardRollService = rewardRollService;
        this.rewardDeliveryService = rewardDeliveryService;
        this.phaseFactory = phaseFactory;
        this.crateRegistry.replaceAll(pluginConfig.crateDefinitions());
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
            keyShopService.applySettings(config.cratesSettings().shop);
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
            CrateNpcService npcService
    ) {
        this.repository = repository;
        this.pityService = new PityService(repository);
        this.keyService = keyService;
        this.locationService = locationService;
        this.rerollService = rerollService;
        this.playerDataService = playerDataService;
        this.broadcastService = broadcastService;
        this.idleCrateDisplayService = idleCrateDisplayService;
        this.bulkOpenService = bulkOpenService;
        this.keyShopService = keyShopService;
        this.npcService = npcService;
        this.loaded = true;
    }

    public void shutdown() {
        loaded = false;
        sessionRegistry.cancelAll();
        openCooldownUntil.clear();
        if (keyService != null) {
            keyService.clearCache();
        }
        if (locationService != null) {
            locationService.clear();
        }
        if (playerDataService != null) {
            playerDataService.clearAll();
        }
        if (idleCrateDisplayService != null) {
            idleCrateDisplayService.shutdown();
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }

    public CrateRegistry crateRegistry() {
        return crateRegistry;
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
        if (!ready(player)) {
            return;
        }
        Optional<CrateDefinition> crateOptional = findCrate(crateId);
        if (crateOptional.isEmpty()) {
            messageService.send(player.getUniqueId(), "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        CrateDefinition crate = crateOptional.get();
        CratePreviewMenu menu = new CratePreviewMenu(
                player.getUniqueId(),
                messageService,
                pluginConfig.guiPreviewSettings(),
                pluginConfig.cratesSettings().premiumOpening,
                crate,
                rewardRollService,
                (target, amount) -> proceedOpenFlow(target, crate, target.getLocation(), amount),
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
                target -> reloadCrates(target)
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
                mutable -> {
                    configurationLoader.saveCrateSettings(mutable);
                    reloadCrates(player);
                },
                () -> openEditor(player)
        );
        PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
    }

    public void reloadCrates(Player player) {
        PluginConfig reloaded = configurationLoader.load();
        applyConfig(reloaded);
        if (player != null) {
            messageService.send(player.getUniqueId(), "reload-success");
        }
    }

    public void beginOpen(Player player, String crateId, Location location) {
        beginOpen(player, crateId, location, 1);
    }

    public void beginOpen(Player player, String crateId, Location location, int amount) {
        if (!ready(player)) {
            return;
        }
        Optional<CrateDefinition> crateOptional = findCrate(crateId);
        if (crateOptional.isEmpty()) {
            messageService.send(player.getUniqueId(), "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        CrateDefinition crate = crateOptional.get();
        if (!crate.opening().permission.isBlank() && !player.hasPermission(crate.opening().permission)) {
            messageService.send(player.getUniqueId(), "no-permission");
            return;
        }
        if (!checkCooldown(player, crate)) {
            return;
        }
        int safeAmount = normalizeOpenAmount(player, crate, amount);
        if (safeAmount > 1) {
            startMultiOpen(player, crate, location == null ? player.getLocation() : location, safeAmount);
            return;
        }
        if (crate.opening().previewEnabled) {
            openPreview(player, crate.id());
            return;
        }
        proceedOpenFlow(player, crate, location == null ? player.getLocation() : location, 1);
    }

    public void openShop(Player player) {
        if (!ready(player)) {
            return;
        }
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

    public void bindCrate(Player player, String crateId, Location location) {
        if (!player.hasPermission("soulcrates.command.admin")) {
            messageService.send(player.getUniqueId(), "no-permission");
            return;
        }
        if (locationService == null) {
            messageService.send(player.getUniqueId(), "startup-not-ready");
            return;
        }
        Optional<CrateDefinition> crateOptional = findCrate(crateId);
        if (crateOptional.isEmpty()) {
            messageService.send(player.getUniqueId(), "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        locationService.bind(location, crateId).thenRun(() -> PluginSchedulers.run(plugin, player, () -> {
            if (idleCrateDisplayService != null) {
                idleCrateDisplayService.onBind(location, crateId);
            }
            messageService.send(
                    player.getUniqueId(),
                    "setcrate-success",
                    messageService.placeholder("crate", crateOptional.get().displayName())
            );
        }));
    }

    public void unbindCrate(Player player, Location location) {
        if (!player.hasPermission("soulcrates.command.admin")) {
            messageService.send(player.getUniqueId(), "no-permission");
            return;
        }
        if (locationService == null) {
            messageService.send(player.getUniqueId(), "startup-not-ready");
            return;
        }
        locationService.unbind(location).thenRun(() -> PluginSchedulers.run(plugin, player, () -> {
            if (idleCrateDisplayService != null) {
                idleCrateDisplayService.onUnbind(location);
            }
            messageService.send(player.getUniqueId(), "unsetcrate-success");
        }));
    }

    public void bindNpc(Player player, String crateId, int npcId) {
        if (!player.hasPermission("soulcrates.command.admin")) {
            messageService.send(player.getUniqueId(), "no-permission");
            return;
        }
        if (npcService == null) {
            messageService.send(player.getUniqueId(), "startup-not-ready");
            return;
        }
        Optional<CrateDefinition> crateOptional = findCrate(crateId);
        if (crateOptional.isEmpty()) {
            messageService.send(player.getUniqueId(), "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        npcService.bind(npcId, crateId).thenRun(() -> PluginSchedulers.run(plugin, player, () -> messageService.send(
                player.getUniqueId(),
                "setnpc-success",
                messageService.placeholder("crate", crateOptional.get().displayName()),
                messageService.placeholder("npc", String.valueOf(npcId))
        )));
    }

    public void unbindNpc(Player player, int npcId) {
        if (!player.hasPermission("soulcrates.command.admin")) {
            messageService.send(player.getUniqueId(), "no-permission");
            return;
        }
        if (npcService == null) {
            messageService.send(player.getUniqueId(), "startup-not-ready");
            return;
        }
        npcService.unbind(npcId).thenRun(() -> PluginSchedulers.run(plugin, player, () -> messageService.send(
                player.getUniqueId(),
                "unsetnpc-success",
                messageService.placeholder("npc", String.valueOf(npcId))
        )));
    }

    public void giveKeys(CommandSender sender, Player target, String crateId, int amount, boolean physical) {
        if (!sender.hasPermission("soulcrates.command.givekey")) {
            notifySender(sender, "no-permission");
            return;
        }
        if (keyService == null) {
            notifySender(sender, "startup-not-ready");
            return;
        }
        Optional<CrateDefinition> crateOptional = findCrate(crateId);
        if (crateOptional.isEmpty()) {
            notifySender(sender, "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        CrateDefinition crate = crateOptional.get();
        int safeAmount = Math.max(1, amount);
        if (physical) {
            PluginSchedulers.run(plugin, target, () -> keyService.givePhysicalKey(target, crate, safeAmount));
        } else {
            keyService.giveVirtualKeys(target.getUniqueId(), crate.id(), safeAmount);
        }
        messageService.send(
                target.getUniqueId(),
                "givekey-received",
                messageService.placeholder("amount", Integer.toString(safeAmount)),
                messageService.placeholder("crate", crate.displayName())
        );
        if (sender instanceof Player player) {
            messageService.send(
                    player.getUniqueId(),
                    "givekey-success",
                    messageService.placeholder("player", target.getName()),
                    messageService.placeholder("amount", Integer.toString(safeAmount)),
                    messageService.placeholder("crate", crate.displayName())
            );
        } else {
            sender.sendMessage(messageService.prefixed(
                    null,
                    "givekey-success",
                    messageService.placeholder("player", target.getName()),
                    messageService.placeholder("amount", Integer.toString(safeAmount)),
                    messageService.placeholder("crate", crate.displayName())
            ));
        }
    }

    public void showKeys(Player player, String crateId) {
        if (keyService == null) {
            messageService.send(player.getUniqueId(), "startup-not-ready");
            return;
        }
        if (crateId == null || crateId.isBlank()) {
            for (CrateDefinition crate : crateRegistry.list()) {
                sendKeyLine(player, crate);
            }
            return;
        }
        Optional<CrateDefinition> crateOptional = findCrate(crateId);
        if (crateOptional.isEmpty()) {
            messageService.send(player.getUniqueId(), "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        sendKeyLine(player, crateOptional.get());
    }

    public void showStats(Player viewer, Player target) {
        if (playerDataService == null) {
            messageService.send(viewer.getUniqueId(), "startup-not-ready");
            return;
        }
        messageService.send(
                viewer.getUniqueId(),
                "stats-header",
                messageService.placeholder("player", target.getName())
        );
        for (CrateDefinition crate : crateRegistry.list()) {
            messageService.send(
                    viewer.getUniqueId(),
                    "stats-line",
                    messageService.placeholder("crate", crate.displayName()),
                    messageService.placeholder("opens", Integer.toString(playerDataService.opens(target.getUniqueId(), crate.id()))),
                    messageService.placeholder("pity", Integer.toString(playerDataService.pity(target.getUniqueId(), crate.id()))),
                    messageService.placeholder("keys", Integer.toString(totalKeys(target, crate.id()))),
                    messageService.placeholder("last", playerDataService.lastReward(target.getUniqueId(), crate.id()))
            );
        }
    }

    public void listLocations(Player player) {
        if (locationService == null) {
            messageService.send(player.getUniqueId(), "startup-not-ready");
            return;
        }
        Map<String, String> bindings = locationService.allBindings();
        if (bindings.isEmpty()) {
            messageService.send(player.getUniqueId(), "locations-empty");
            return;
        }
        messageService.send(player.getUniqueId(), "locations-header");
        List<String> keys = new ArrayList<>(bindings.keySet());
        keys.sort(String::compareTo);
        for (String key : keys) {
            String crateId = bindings.get(key);
            findCrate(crateId).ifPresentOrElse(
                    crate -> messageService.send(
                            player.getUniqueId(),
                            "locations-line",
                            messageService.placeholder("location", key),
                            messageService.placeholder("crate", crate.displayName())
                    ),
                    () -> messageService.send(
                            player.getUniqueId(),
                            "locations-line",
                            messageService.placeholder("location", key),
                            messageService.placeholder("crate", crateId)
                    )
            );
        }
    }

    private void sendKeyLine(Player player, CrateDefinition crate) {
        int virtual = virtualKeys(player.getUniqueId(), crate.id());
        int physical = keyService.countPhysicalKeys(player, crate.id());
        messageService.send(
                player.getUniqueId(),
                "keys-line",
                messageService.placeholder("crate", crate.displayName()),
                messageService.placeholder("virtual", Integer.toString(virtual)),
                messageService.placeholder("physical", Integer.toString(physical))
        );
    }

    private void proceedOpenFlow(Player player, CrateDefinition crate, Location location, int amount) {
        if (amount > 1) {
            startMultiOpen(player, crate, location, amount);
            return;
        }
        if (crate.opening().confirmEnabled) {
            CrateConfirmMenu menu = new CrateConfirmMenu(
                    player.getUniqueId(),
                    messageService,
                    pluginConfig.guiConfirmSettings(),
                    crate,
                    target -> startOpeningSession(target, crate, location),
                    null
            );
            PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
            return;
        }
        startOpeningSession(player, crate, location);
    }

    private void startMultiOpen(Player player, CrateDefinition crate, Location location, int amount) {
        PremiumOpeningSettings premium = pluginConfig.cratesSettings().premiumOpening;
        if (!player.hasPermission(premium.multiOpenPermission)) {
            messageService.send(player.getUniqueId(), "no-permission");
            return;
        }
        if (!crate.opening().allowMultiOpen) {
            messageService.send(player.getUniqueId(), "multi-open-disabled");
            return;
        }
        if (sessionRegistry.isBusy(player.getUniqueId())) {
            messageService.send(player.getUniqueId(), "open-already");
            return;
        }
        if (!checkCooldown(player, crate)) {
            return;
        }
        int keysPerOpen = crate.opening().requireKey ? Math.max(1, crate.opening().keysRequired) : 0;
        int totalKeysNeeded = keysPerOpen * amount;
        if (totalKeysNeeded > 0 && totalKeys(player, crate.id()) < totalKeysNeeded) {
            messageService.send(player.getUniqueId(), "open-no-keys", messageService.placeholder("crate", crate.displayName()));
            return;
        }
        if (!sessionRegistry.tryBeginBulk(player.getUniqueId())) {
            messageService.send(player.getUniqueId(), "open-already");
            return;
        }
        if (keysPerOpen > 0) {
            for (int index = 0; index < amount; index++) {
                if (!keyService.consumeForOpen(player, crate, keysPerOpen)) {
                    sessionRegistry.endBulk(player.getUniqueId());
                    messageService.send(player.getUniqueId(), "open-no-keys", messageService.placeholder("crate", crate.displayName()));
                    return;
                }
            }
        }
        applyCooldown(player, crate);
        pauseIdleIfBound(location);
        messageService.send(
                player.getUniqueId(),
                "multi-open-started",
                messageService.placeholder("amount", Integer.toString(amount)),
                messageService.placeholder("crate", crate.displayName())
        );
        bulkOpenService.rollSequential(player.getUniqueId(), crate, amount).thenAccept(rolls ->
                PluginSchedulers.run(plugin, player, () -> finishMultiOpen(player, crate, location, amount, rolls))
        );
    }

    private void finishMultiOpen(Player player, CrateDefinition crate, Location location, int amount, List<RewardRollResult> rolls) {
        bulkOpenService.deliverAll(player, crate, rolls);
        resumeIdleIfBound(location);
        sessionRegistry.endBulk(player.getUniqueId());
        Bukkit.getPluginManager().callEvent(new CrateOpenFinishEvent(new OpeningContext(
                player.getUniqueId(),
                crate.id(),
                location,
                crate.opening().keysRequired * amount,
                false
        )));
        messageService.send(
                player.getUniqueId(),
                "multi-open-finished",
                messageService.placeholder("amount", Integer.toString(amount)),
                messageService.placeholder("crate", crate.displayName()),
                messageService.placeholder("summary", bulkOpenService.formatSummary(bulkOpenService.summarize(rolls)))
        );
    }

    private int normalizeOpenAmount(Player player, CrateDefinition crate, int amount) {
        int safeAmount = Math.max(1, amount);
        PremiumOpeningSettings premium = pluginConfig.cratesSettings().premiumOpening;
        if (safeAmount <= 1) {
            return 1;
        }
        if (!player.hasPermission(premium.multiOpenPermission) || !crate.opening().allowMultiOpen) {
            return 1;
        }
        return Math.min(safeAmount, Math.max(1, premium.maxMultiOpen));
    }

    private void startOpeningSession(Player player, CrateDefinition crate, Location location) {
        if (sessionRegistry.isBusy(player.getUniqueId())) {
            messageService.send(player.getUniqueId(), "open-already");
            return;
        }
        if (!checkCooldown(player, crate)) {
            return;
        }
        if (crate.opening().requireKey) {
            int required = Math.max(1, crate.opening().keysRequired);
            if (totalKeys(player, crate.id()) < required) {
                messageService.send(player.getUniqueId(), "open-no-keys", messageService.placeholder("crate", crate.displayName()));
                return;
            }
            if (!keyService.consumeForOpen(player, crate, required)) {
                messageService.send(player.getUniqueId(), "open-no-keys", messageService.placeholder("crate", crate.displayName()));
                return;
            }
        }
        applyCooldown(player, crate);
        pityService.loadCounter(player.getUniqueId(), crate.id()).thenCompose(counter ->
                pityService.shouldForcePity(crate, counter).thenApply(forcePity -> {
                    RewardRollResult roll = rewardRollService.roll(crate, counter, forcePity);
                    PluginSchedulers.run(plugin, player, () -> launchSession(player, crate, location, roll));
                    return roll;
                })
        );
    }

    private void launchSession(Player player, CrateDefinition crate, Location location, RewardRollResult roll) {
        pauseIdleIfBound(location);
        OpeningContext context = new OpeningContext(
                player.getUniqueId(),
                crate.id(),
                location,
                crate.opening().keysRequired,
                false
        );
        Bukkit.getPluginManager().callEvent(new CrateOpenStartEvent(context));
        UUID sessionId = UUID.randomUUID();
        CrateOpeningSession session = new CrateOpeningSession(sessionId, context, crate, roll, plugin);
        PremiumOpeningSettings premium = pluginConfig.cratesSettings().premiumOpening;
        session.setOnCancel(() -> PluginSchedulers.run(plugin, player, () -> {
            resumeIdleIfBound(session.context().crateLocation());
            sessionRegistry.unregister(session);
            messageService.send(player.getUniqueId(), "open-cancelled");
        }));
        session.setOnFinish(() -> sessionRegistry.unregister(session));
        try {
            sessionRegistry.register(session);
        } catch (IllegalStateException exception) {
            messageService.send(player.getUniqueId(), "open-already");
            return;
        }
        messageService.send(
                player.getUniqueId(),
                "open-started",
                messageService.placeholder("crate", crate.displayName())
        );
        if (player.hasPermission(premium.instantOpenPermission)) {
            finishWithoutAnimation(player, session, OpeningSkipMode.INSTANT);
            return;
        }
        if (player.hasPermission(premium.skipAnimationPermission)) {
            finishWithoutAnimation(player, session, OpeningSkipMode.SKIP_ANIMATION);
            return;
        }
        DisplayComponent displayComponent = displayEngineRegistry.createComponent(crate, context.crateLocation(), player);
        displayComponent.create();
        session.setDisplayComponent(displayComponent);
        OpeningAnimationPipeline pipeline = new OpeningAnimationPipeline(plugin, phaseFactory, crate, roll.reward());
        pipeline.setCompletionCallback(() -> PluginSchedulers.run(plugin, player, () -> onAnimationComplete(player, session)));
        session.setAnimationPipeline(pipeline);
        session.start(player);
    }

    private void finishWithoutAnimation(Player player, CrateOpeningSession session, OpeningSkipMode mode) {
        RerollSettings reroll = session.crateDefinition().reroll();
        PremiumOpeningSettings premium = pluginConfig.cratesSettings().premiumOpening;
        boolean skipReroll = switch (mode) {
            case INSTANT -> premium.instantSkipsReroll || reroll.skipOnInstantOpen;
            case SKIP_ANIMATION -> reroll.skipOnSkipAnimation;
            case MULTI -> reroll.skipOnMultiOpen;
        };
        if (skipReroll || !reroll.enabled || !rerollService.canReroll(player, session)) {
            claimReward(player, session);
            return;
        }
        session.markAwaitingReroll();
        openRerollMenu(player, session);
    }

    private void onAnimationComplete(Player player, CrateOpeningSession session) {
        if (session.displayComponent() != null) {
            session.displayComponent().destroy();
            session.setDisplayComponent(null);
        }
        if (session.animationPipeline() != null) {
            session.animationPipeline().unload();
        }
        if (session.crateDefinition().reroll().enabled && rerollService.canReroll(player, session)) {
            session.markAwaitingReroll();
            openRerollMenu(player, session);
            return;
        }
        claimReward(player, session);
    }

    private void openRerollMenu(Player player, CrateOpeningSession session) {
        CrateRerollMenu menu = new CrateRerollMenu(
                player.getUniqueId(),
                messageService,
                pluginConfig.guiRerollSettings(),
                session,
                rewardRollService,
                rerollService,
                target -> claimReward(target, session),
                target -> performReroll(target, session)
        );
        PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
    }

    private void performReroll(Player player, CrateOpeningSession session) {
        if (session.state() != OpeningSessionState.AWAITING_REROLL) {
            return;
        }
        if (!rerollService.canReroll(player, session)) {
            messageService.send(player.getUniqueId(), "reroll-no-remaining");
            return;
        }
        if (!rerollService.chargeForReroll(player, session)) {
            messageService.send(player.getUniqueId(), "reroll-no-money");
            return;
        }
        RewardRollResult newRoll = rerollService.reroll(session);
        session.markAnimating();
        player.closeInventory();
        messageService.send(
                player.getUniqueId(),
                "reroll-success",
                messageService.placeholder("reward", newRoll.reward().displayName())
        );
        restartAnimation(player, session, newRoll);
    }

    private void restartAnimation(Player player, CrateOpeningSession session, RewardRollResult roll) {
        Location location = session.context().crateLocation();
        DisplayComponent displayComponent = displayEngineRegistry.createComponent(session.crateDefinition(), location, player);
        displayComponent.create();
        session.setDisplayComponent(displayComponent);
        OpeningAnimationPipeline pipeline = new OpeningAnimationPipeline(plugin, phaseFactory, session.crateDefinition(), roll.reward());
        pipeline.setCompletionCallback(() -> PluginSchedulers.run(plugin, player, () -> onAnimationComplete(player, session)));
        session.setAnimationPipeline(pipeline);
        session.markAnimating();
        pipeline.restartFromSecondPhase(player, session, roll.reward());
    }

    public void claimReward(Player player, CrateOpeningSession session) {
        if (!session.tryBeginClaim()) {
            return;
        }
        RewardRollResult roll = session.rollResult();
        rewardDeliveryService.deliver(player, session.crateDefinition().id(), roll.reward());
        broadcastService.maybeBroadcast(player, session.crateDefinition(), roll.reward());
        UUID playerId = player.getUniqueId();
        String crateId = session.crateDefinition().id();
        pityService.afterRoll(playerId, session.crateDefinition(), roll.pityTriggered(), roll.reward().id())
                .thenCompose(ignored -> repository.loadPityCounter(playerId, crateId))
                .thenAccept(counter -> playerDataService.onPityUpdated(playerId, crateId, counter));
        repository.recordLastReward(playerId, crateId, roll.reward().id())
                .thenRun(() -> playerDataService.onRewardRecorded(playerId, crateId, roll.reward().id()));
        playerDataService.incrementOpens(playerId, crateId);
        resumeIdleIfBound(session.context().crateLocation());
        session.unload();
        Bukkit.getPluginManager().callEvent(new CrateOpenFinishEvent(session.context()));
        messageService.send(
                player.getUniqueId(),
                "open-finished",
                messageService.placeholder("reward", roll.reward().displayName()),
                messageService.placeholder("crate", session.crateDefinition().displayName())
        );
        if (roll.pityTriggered()) {
            messageService.send(player.getUniqueId(), "open-pity-triggered");
        }
        sessionRegistry.unregister(session);
    }

    private boolean checkCooldown(Player player, CrateDefinition crate) {
        if (crate.opening().cooldownSeconds <= 0) {
            return true;
        }
        Long until = openCooldownUntil.get(player.getUniqueId());
        if (until == null || System.currentTimeMillis() >= until) {
            return true;
        }
        long remaining = Math.max(1L, (until - System.currentTimeMillis() + 999L) / 1000L);
        messageService.send(
                player.getUniqueId(),
                "open-cooldown",
                messageService.placeholder("seconds", Long.toString(remaining))
        );
        return false;
    }

    private void applyCooldown(Player player, CrateDefinition crate) {
        if (crate.opening().cooldownSeconds > 0) {
            openCooldownUntil.put(
                    player.getUniqueId(),
                    System.currentTimeMillis() + crate.opening().cooldownSeconds * 1000L
            );
        }
    }

    private boolean ready(Player player) {
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

    private void notifySender(CommandSender sender, String key, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        if (sender instanceof Player player) {
            messageService.send(player.getUniqueId(), key, resolvers);
            return;
        }
        sender.sendMessage(messageService.prefixed(null, key, resolvers));
    }

    private void pauseIdleIfBound(Location location) {
        if (idleCrateDisplayService == null || location == null || location.getWorld() == null) {
            return;
        }
        if (locationService.findCrateId(location).isPresent()) {
            idleCrateDisplayService.pause(location);
        }
    }

    private void resumeIdleIfBound(Location location) {
        if (idleCrateDisplayService == null || location == null || location.getWorld() == null) {
            return;
        }
        if (locationService.findCrateId(location).isPresent()) {
            idleCrateDisplayService.resume(location);
        }
    }
}
