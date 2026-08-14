package bm.b0b0b0.soulCrates.service.open;

import bm.b0b0b0.soulCrates.animation.OpeningAnimationPipeline;
import bm.b0b0b0.soulCrates.animation.PhaseFactory;
import bm.b0b0b0.soulCrates.api.event.CrateOpenFinishEvent;
import bm.b0b0b0.soulCrates.api.event.CrateOpenStartEvent;
import bm.b0b0b0.soulCrates.config.PluginConfig;
import bm.b0b0b0.soulCrates.config.settings.OpenCostSettings;
import bm.b0b0b0.soulCrates.config.settings.PremiumOpeningSettings;
import bm.b0b0b0.soulCrates.config.settings.RerollSettings;
import bm.b0b0b0.soulCrates.engine.DisplayComponent;
import bm.b0b0b0.soulCrates.engine.DisplayEngineRegistry;
import bm.b0b0b0.soulCrates.gui.CrateConfirmMenu;
import bm.b0b0b0.soulCrates.gui.CrateRerollMenu;
import bm.b0b0b0.soulCrates.gui.CrateSelectRewardMenu;
import bm.b0b0b0.soulCrates.hook.HookRegistry;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.OpeningContext;
import bm.b0b0b0.soulCrates.model.OpeningSessionState;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.model.RewardRollResult;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.admin.CrateAdminService;
import bm.b0b0b0.soulCrates.service.idle.IdleCrateDisplayService;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import bm.b0b0b0.soulCrates.service.location.CrateLocationService;
import bm.b0b0b0.soulCrates.service.lootbox.LootBoxService;
import bm.b0b0b0.soulCrates.service.menu.CrateMenuService;
import bm.b0b0b0.soulCrates.service.reward.DeliveryResult;
import bm.b0b0b0.soulCrates.service.reward.PityService;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import bm.b0b0b0.soulCrates.service.reward.RewardSettlementService;
import bm.b0b0b0.soulCrates.service.reward.WinLimitService;
import bm.b0b0b0.soulCrates.service.reroll.RerollService;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import bm.b0b0b0.soulCrates.session.SessionRegistry;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateOpeningService implements CrateOpenCallbacks {

    private enum OpeningSkipMode {
        INSTANT,
        SKIP_ANIMATION,
        MULTI
    }

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final CrateRegistry crateRegistry;
    private final DisplayEngineRegistry displayEngineRegistry;
    private final SessionRegistry sessionRegistry;
    private final RewardRollService rewardRollService;
    private final PhaseFactory phaseFactory;
    private final KeyService keyService;
    private final BulkOpenService bulkOpenService;
    private final RerollService rerollService;
    private final PityService pityService;
    private final RewardSettlementService rewardSettlementService;
    private final WinLimitService winLimitService;
    private final HookRegistry hookRegistry;
    private final OpenCooldownTracker cooldownTracker;
    private final IdleCrateDisplayService idleCrateDisplayService;
    private final CrateLocationService locationService;
    private final LootBoxService lootBoxService;
    private final CrateAdminService.KeyCountResolver keyCountResolver;
    private PluginConfig pluginConfig;
    private CrateMenuService menuService;

    public CrateOpeningService(
            JavaPlugin plugin,
            PluginConfig pluginConfig,
            MessageService messageService,
            CrateRegistry crateRegistry,
            DisplayEngineRegistry displayEngineRegistry,
            SessionRegistry sessionRegistry,
            RewardRollService rewardRollService,
            PhaseFactory phaseFactory,
            KeyService keyService,
            BulkOpenService bulkOpenService,
            RerollService rerollService,
            PityService pityService,
            RewardSettlementService rewardSettlementService,
            WinLimitService winLimitService,
            HookRegistry hookRegistry,
            OpenCooldownTracker cooldownTracker,
            IdleCrateDisplayService idleCrateDisplayService,
            CrateLocationService locationService,
            LootBoxService lootBoxService,
            CrateAdminService.KeyCountResolver keyCountResolver
    ) {
        this.plugin = plugin;
        this.pluginConfig = pluginConfig;
        this.messageService = messageService;
        this.crateRegistry = crateRegistry;
        this.displayEngineRegistry = displayEngineRegistry;
        this.sessionRegistry = sessionRegistry;
        this.rewardRollService = rewardRollService;
        this.phaseFactory = phaseFactory;
        this.keyService = keyService;
        this.bulkOpenService = bulkOpenService;
        this.rerollService = rerollService;
        this.pityService = pityService;
        this.rewardSettlementService = rewardSettlementService;
        this.winLimitService = winLimitService;
        this.hookRegistry = hookRegistry;
        this.cooldownTracker = cooldownTracker;
        this.idleCrateDisplayService = idleCrateDisplayService;
        this.locationService = locationService;
        this.lootBoxService = lootBoxService;
        this.keyCountResolver = keyCountResolver;
    }

    public void attachMenuService(CrateMenuService menuService) {
        this.menuService = menuService;
    }

    public void applyConfig(PluginConfig config) {
        this.pluginConfig = config;
    }

    public void beginOpen(Player player, String crateId, Location location) {
        beginOpen(player, crateId, location, 1);
    }

    public void beginOpen(Player player, String crateId, Location location, int amount) {
        Optional<CrateDefinition> crateOptional = crateRegistry.find(crateId);
        if (crateOptional.isEmpty()) {
            messageService.send(player.getUniqueId(), "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        CrateDefinition crate = crateOptional.get();
        if (!crate.opening().permission.isBlank() && !player.hasPermission(crate.opening().permission)) {
            messageService.send(player.getUniqueId(), "no-permission");
            return;
        }
        if (!cooldownTracker.check(player, crate)) {
            return;
        }
        int safeAmount = normalizeOpenAmount(player, crate, amount);
        Location openLocation = location == null ? player.getLocation() : location;
        if (safeAmount > 1) {
            startMultiOpen(player, crate, openLocation, safeAmount);
            return;
        }
        if (crate.opening().previewEnabled) {
            menuService.openPreview(player, crate.id());
            return;
        }
        proceedOpenFlow(player, crate, openLocation, 1);
    }

    public void openLootBox(Player player, ItemStack item) {
        if (lootBoxService == null) {
            return;
        }
        String crateId = lootBoxService.readCrateId(item);
        if (crateId == null) {
            return;
        }
        Optional<CrateDefinition> crateOptional = crateRegistry.find(crateId);
        if (crateOptional.isEmpty()) {
            messageService.send(player.getUniqueId(), "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        CrateDefinition crate = crateOptional.get();
        if (!crate.lootBox().enabled) {
            messageService.send(player.getUniqueId(), "lootbox-disabled");
            return;
        }
        if (!lootBoxService.consumeOne(player, item)) {
            return;
        }
        String guaranteedRarity = lootBoxService.readGuaranteedRarity(item);
        if (guaranteedRarity == null || guaranteedRarity.isBlank()) {
            guaranteedRarity = crate.lootBox().guaranteedRarity;
        }
        instantReward(player, crate, player.getLocation(), guaranteedRarity);
    }

    public void claimReward(Player player, CrateOpeningSession session) {
        if (!session.tryBeginClaim()) {
            return;
        }
        RewardRollResult roll = session.rollResult();
        CrateDefinition crate = session.crateDefinition();
        rewardSettlementService.deliver(player, crate, roll).thenAccept(deliveryResult ->
                PluginSchedulers.run(plugin, player, () -> {
                    rewardSettlementService.applyRollStats(player, crate, roll, deliveryResult);
                    completeClaimReward(player, session, roll, deliveryResult);
                })
        );
    }

    @Override
    public void proceedOpenFlow(Player player, CrateDefinition crate, Location location, int amount) {
        if (amount > 1) {
            if (isSelectMode(crate)) {
                messageService.send(player.getUniqueId(), "select-multi-disabled");
                return;
            }
            startMultiOpen(player, crate, location, amount);
            return;
        }
        if (isSelectMode(crate)) {
            if (crate.opening().confirmEnabled) {
                CrateConfirmMenu menu = new CrateConfirmMenu(
                        player.getUniqueId(),
                        messageService,
                        pluginConfig.guiConfirmSettings(),
                        crate,
                        target -> openSelectMenu(target, crate, location),
                        null
                );
                PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
                return;
            }
            openSelectMenu(player, crate, location);
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

    @Override
    public void reloadCrates(Player player) {
        menuService.reloadCrates(player);
    }

    private void completeClaimReward(
            Player player,
            CrateOpeningSession session,
            RewardRollResult roll,
            DeliveryResult deliveryResult
    ) {
        if (deliveryResult.isFailed()) {
            sessionRegistry.unregister(session);
            resumeIdleIfBound(session.context().crateLocation());
            return;
        }
        resumeIdleIfBound(session.context().crateLocation());
        session.unload();
        Bukkit.getPluginManager().callEvent(new CrateOpenFinishEvent(session.context()));
        sessionRegistry.unregister(session);
    }

    public void openSelectMenu(Player player, CrateDefinition crate, Location location) {
        if (sessionRegistry.isBusy(player.getUniqueId())) {
            messageService.send(player.getUniqueId(), "open-already");
            return;
        }
        if (!cooldownTracker.check(player, crate)) {
            return;
        }
        CrateSelectRewardMenu menu = new CrateSelectRewardMenu(
                player.getUniqueId(),
                messageService,
                pluginConfig.guiSelectSettings(),
                crate,
                rewardRollService,
                winLimitService,
                keyService,
                location,
                (target, reward) -> redeemSelectedReward(target, crate, location, reward),
                null
        );
        PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
    }

    private void redeemSelectedReward(Player player, CrateDefinition crate, Location location, RewardDefinition reward) {
        if (sessionRegistry.isBusy(player.getUniqueId())) {
            messageService.send(player.getUniqueId(), "open-already");
            return;
        }
        int keysRequired = reward.requiredKeys(crate.opening().keysRequired);
        winLimitService.check(player, crate, reward).thenCompose(check -> {
            if (!check.allowed()) {
                PluginSchedulers.run(plugin, player, () -> winLimitService.sendBlockedMessage(player, check));
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
            return OpenCostHelper.consumeAccess(
                    player,
                    crate,
                    keysRequired,
                    keyService,
                    hookRegistry,
                    messageService
            ).thenCompose(consumed -> {
                if (!consumed) {
                    PluginSchedulers.run(plugin, player, () -> {
                        if (crate.opening().requireKey || keysRequired > 0) {
                            messageService.send(player.getUniqueId(), "open-no-keys", messageService.placeholder("crate", crate.displayName()));
                        } else {
                            messageService.send(player.getUniqueId(), "open-no-money");
                        }
                    });
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                }
                return winLimitService.resolveReward(player, crate, reward);
            });
        }).thenAccept(resolved -> {
            if (resolved == null) {
                return;
            }
            PluginSchedulers.run(plugin, player, () -> {
                cooldownTracker.apply(player, crate);
                launchSession(player, crate, location, new RewardRollResult(resolved, false));
            });
        });
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
        if (!cooldownTracker.check(player, crate)) {
            return;
        }
        int keysPerOpen = crate.opening().requireKey ? Math.max(1, crate.opening().keysRequired) : 0;
        int totalKeysNeeded = keysPerOpen * amount;
        OpenCostSettings openCost = crate.opening().openCost;
        boolean canPay = openCost != null && openCost.enabled && openCost.vaultPrice > 0.0;
        if (totalKeysNeeded > 0) {
            int owned = keyCountResolver.totalKeys(player, crate.id());
            if (owned < totalKeysNeeded && !canPay) {
                messageService.send(player.getUniqueId(), "open-no-keys", messageService.placeholder("crate", crate.displayName()));
                return;
            }
        }
        if (!sessionRegistry.tryBeginBulk(player.getUniqueId())) {
            messageService.send(player.getUniqueId(), "open-already");
            return;
        }
        if (keysPerOpen > 0) {
            keyService.consumeForMultiOpen(player, crate, amount, keysPerOpen).thenAccept(consumed ->
                    PluginSchedulers.run(plugin, player, () -> {
                        if (!consumed) {
                            sessionRegistry.endBulk(player.getUniqueId());
                            messageService.send(player.getUniqueId(), "open-no-keys", messageService.placeholder("crate", crate.displayName()));
                            return;
                        }
                        continueMultiOpen(player, crate, location, amount);
                    })
            );
            return;
        }
        continueMultiOpen(player, crate, location, amount);
    }

    private void continueMultiOpen(Player player, CrateDefinition crate, Location location, int amount) {
        cooldownTracker.apply(player, crate);
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
        bulkOpenService.deliverAll(player, crate, rolls).thenRun(() -> PluginSchedulers.run(plugin, player, () -> {
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
        }));
    }

    private int normalizeOpenAmount(Player player, CrateDefinition crate, int amount) {
        PremiumOpeningSettings premium = pluginConfig.cratesSettings().premiumOpening;
        if (amount == -1) {
            if (!crate.opening().massOpening.enabled
                    || !crate.opening().massOpening.allowOpenAll
                    || !player.hasPermission(premium.multiOpenPermission)
                    || !crate.opening().allowMultiOpen) {
                return 1;
            }
            int perOpen = Math.max(1, crate.opening().keysRequired);
            int keys = keyCountResolver.totalKeys(player, crate.id());
            int max = Math.min(premium.maxMultiOpen, Math.max(1, crate.opening().massOpening.maxAmount));
            return Math.max(1, Math.min(keys / perOpen, max));
        }
        int safeAmount = Math.max(1, amount);
        if (safeAmount <= 1) {
            return 1;
        }
        if (!player.hasPermission(premium.multiOpenPermission) || !crate.opening().allowMultiOpen || !crate.opening().massOpening.enabled) {
            return 1;
        }
        int max = Math.min(premium.maxMultiOpen, Math.max(1, crate.opening().massOpening.maxAmount));
        return Math.min(safeAmount, max);
    }

    private void startOpeningSession(Player player, CrateDefinition crate, Location location) {
        if (sessionRegistry.isBusy(player.getUniqueId())) {
            messageService.send(player.getUniqueId(), "open-already");
            return;
        }
        if (!cooldownTracker.check(player, crate)) {
            return;
        }
        if (isSelectMode(crate)) {
            openSelectMenu(player, crate, location);
            return;
        }
        int required = crate.opening().requireKey ? Math.max(1, crate.opening().keysRequired) : 0;
        if (required > 0 || (crate.opening().openCost != null && crate.opening().openCost.enabled)) {
            OpenCostHelper.consumeAccess(
                    player,
                    crate,
                    Math.max(1, required),
                    keyService,
                    hookRegistry,
                    messageService
            ).thenAccept(consumed -> PluginSchedulers.run(plugin, player, () -> {
                if (!consumed) {
                    if (crate.opening().requireKey) {
                        messageService.send(player.getUniqueId(), "open-no-keys", messageService.placeholder("crate", crate.displayName()));
                    } else {
                        messageService.send(player.getUniqueId(), "open-no-money");
                    }
                    return;
                }
                continueOpeningSession(player, crate, location);
            }));
            return;
        }
        continueOpeningSession(player, crate, location);
    }

    private void continueOpeningSession(Player player, CrateDefinition crate, Location location) {
        cooldownTracker.apply(player, crate);
        pityService.loadCounter(player.getUniqueId(), crate.id()).thenCompose(counter ->
                pityService.shouldForcePity(crate, counter).thenApply(forcePity -> {
                    RewardRollResult roll = rewardRollService.roll(crate, counter, forcePity, resolveGuaranteedRarity(crate));
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

    private void instantReward(Player player, CrateDefinition crate, Location location, String guaranteedRarity) {
        pityService.loadCounter(player.getUniqueId(), crate.id()).thenCompose(counter ->
                pityService.shouldForcePity(crate, counter).thenAccept(forcePity -> {
                    RewardRollResult roll = rewardRollService.roll(crate, counter, forcePity, guaranteedRarity);
                    PluginSchedulers.run(plugin, player, () ->
                            rewardSettlementService.deliver(player, crate, roll).thenAccept(deliveryResult ->
                                    PluginSchedulers.run(plugin, player, () ->
                                            rewardSettlementService.applyRollStats(player, crate, roll, deliveryResult)
                                    )
                            )
                    );
                })
        );
    }

    private static boolean isSelectMode(CrateDefinition crate) {
        return crate.opening().rewardsMode != null
                && "SELECT".equalsIgnoreCase(crate.opening().rewardsMode.trim());
    }

    private String resolveGuaranteedRarity(CrateDefinition crate) {
        if (crate.keys().guaranteedRarity != null && !crate.keys().guaranteedRarity.isBlank()) {
            return crate.keys().guaranteedRarity;
        }
        return null;
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
