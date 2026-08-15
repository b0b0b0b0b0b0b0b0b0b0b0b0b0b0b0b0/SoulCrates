package bm.b0b0b0.soulCrates.service.admin;

import bm.b0b0b0.soulCrates.config.AnimationPresetRegistry;
import bm.b0b0b0.soulCrates.config.ConfigurationLoader;
import bm.b0b0b0.soulCrates.config.CrateDefinitionLoader;
import bm.b0b0b0.soulCrates.config.settings.CrateDefinitionSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.claim.ClaimService;
import bm.b0b0b0.soulCrates.service.idle.IdleCrateDisplayService;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import bm.b0b0b0.soulCrates.service.location.CrateLocationService;
import bm.b0b0b0.soulCrates.service.lootbox.LootBoxService;
import bm.b0b0b0.soulCrates.service.physical.PhysicalCrateService;
import bm.b0b0b0.soulCrates.service.npc.CrateNpcService;
import bm.b0b0b0.soulCrates.service.player.PlayerDataService;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateAdminService {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final CrateRegistry crateRegistry;
    private final KeyService keyService;
    private final CrateLocationService locationService;
    private final CrateNpcService npcService;
    private final PlayerDataService playerDataService;
    private final IdleCrateDisplayService idleCrateDisplayService;
    private final LootBoxService lootBoxService;
    private final ClaimService claimService;
    private final KeyCountResolver keyCountResolver;
    private final PhysicalCrateService physicalCrateService;
    private ConfigurationLoader configurationLoader;

    public CrateAdminService(
            JavaPlugin plugin,
            MessageService messageService,
            CrateRegistry crateRegistry,
            KeyService keyService,
            CrateLocationService locationService,
            CrateNpcService npcService,
            PlayerDataService playerDataService,
            IdleCrateDisplayService idleCrateDisplayService,
            LootBoxService lootBoxService,
            ClaimService claimService,
            KeyCountResolver keyCountResolver,
            PhysicalCrateService physicalCrateService
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.crateRegistry = crateRegistry;
        this.keyService = keyService;
        this.locationService = locationService;
        this.npcService = npcService;
        this.playerDataService = playerDataService;
        this.idleCrateDisplayService = idleCrateDisplayService;
        this.lootBoxService = lootBoxService;
        this.claimService = claimService;
        this.keyCountResolver = keyCountResolver;
        this.physicalCrateService = physicalCrateService;
    }

    public void attachConfigurationLoader(ConfigurationLoader configurationLoader) {
        this.configurationLoader = configurationLoader;
    }

    public void giveLootBox(CommandSender sender, Player target, String crateId, int amount) {
        if (!sender.hasPermission("soulcrates.command.givelootbox")) {
            notifySender(sender, "no-permission");
            return;
        }
        if (lootBoxService == null) {
            notifySender(sender, "startup-not-ready");
            return;
        }
        Optional<CrateDefinition> crateOptional = crateRegistry.find(crateId);
        if (crateOptional.isEmpty()) {
            notifySender(sender, "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        CrateDefinition crate = crateOptional.get();
        if (!crate.lootBox().enabled) {
            notifySender(sender, "lootbox-disabled");
            return;
        }
        lootBoxService.giveLootBox(target, crate, amount);
        notifySender(
                sender,
                "givelootbox-success",
                messageService.placeholder("player", target.getName()),
                messageService.placeholder("crate", crate.displayName()),
                messageService.placeholder("amount", Integer.toString(amount))
        );
    }

    public void givePhysicalCrate(CommandSender sender, Player target, String crateId, int amount) {
        givePhysicalCrate(sender, target, crateId, amount, null);
    }

    public void givePhysicalCrate(CommandSender sender, Player target, String crateId, int amount, String presetId) {
        if (!sender.hasPermission("soulcrates.command.givecrate")) {
            notifySender(sender, "no-permission");
            return;
        }
        if (physicalCrateService == null || !physicalCrateService.enabled()) {
            notifySender(sender, "physical-crate-disabled");
            return;
        }
        String normalizedCrateId = crateId.toLowerCase(Locale.ROOT);
        if (presetId != null && !presetId.isBlank() && !AnimationPresetRegistry.isKnownPreset(presetId)) {
            notifySender(sender, "setcrate-invalid-preset", messageService.placeholder("preset", presetId));
            return;
        }
        Optional<CrateDefinition> crateOptional = ensureCrateDefinition(normalizedCrateId, null, presetId);
        if (crateOptional.isEmpty()) {
            crateOptional = crateRegistry.find(normalizedCrateId);
        }
        if (crateOptional.isEmpty()) {
            notifySender(sender, "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        CrateDefinition crate = crateOptional.get();
        int safeAmount = Math.max(1, Math.min(amount, 64));
        physicalCrateService.registerInstances(crate.id(), target.getUniqueId(), safeAmount).thenAccept(instanceIds ->
                PluginSchedulers.run(plugin, target, () -> {
                    physicalCrateService.giveItems(target, crate, instanceIds);
                    notifySender(
                            sender,
                            "givecrate-success",
                            messageService.placeholder("player", target.getName()),
                            messageService.placeholder("crate", crate.displayName()),
                            messageService.placeholder("amount", Integer.toString(safeAmount))
                    );
                    messageService.send(
                            target.getUniqueId(),
                            "givecrate-received",
                            messageService.placeholder("crate", crate.displayName()),
                            messageService.placeholder("amount", Integer.toString(safeAmount))
                    );
                })
        );
    }

    public void bindCrate(Player player, String crateId, Location location) {
        bindCrate(player, crateId, location, null);
    }

    public void bindCrate(Player player, String crateId, Location location, String presetId) {
        if (!player.hasPermission("soulcrates.command.admin")) {
            messageService.send(player.getUniqueId(), "no-permission");
            return;
        }
        if (locationService == null) {
            messageService.send(player.getUniqueId(), "startup-not-ready");
            return;
        }
        String normalizedCrateId = crateId.toLowerCase(Locale.ROOT);
        String locationSummary = locationSummary(location);
        Optional<String> existingBinding = locationService.findCrateId(location);
        if (existingBinding.isPresent()) {
            String boundCrateId = existingBinding.get();
            String boundCrateName = crateRegistry.find(boundCrateId)
                    .map(CrateDefinition::displayName)
                    .orElse(boundCrateId);
            messageService.send(
                    player.getUniqueId(),
                    "setcrate-already-bound",
                    messageService.placeholder("crate", boundCrateName)
            );
            plugin.getLogger().info(
                    "Setcrate skipped at " + locationSummary
                            + " for " + player.getName()
                            + ": already bound to " + boundCrateId
            );
            return;
        }
        if (presetId != null && !presetId.isBlank() && !AnimationPresetRegistry.isKnownPreset(presetId)) {
            messageService.send(
                    player.getUniqueId(),
                    "setcrate-invalid-preset",
                    messageService.placeholder("preset", presetId)
            );
            return;
        }
        Optional<CrateDefinition> crateOptional = ensureCrateDefinition(normalizedCrateId, location, presetId);
        if (crateOptional.isEmpty()) {
            messageService.send(player.getUniqueId(), "crate-not-found", messageService.placeholder("crate", crateId));
            return;
        }
        CrateDefinition crate = crateOptional.get();
        locationService.bind(location, normalizedCrateId)
                .thenRun(() -> PluginSchedulers.run(plugin, player, () -> {
                    if (idleCrateDisplayService != null) {
                        idleCrateDisplayService.onUnbind(location);
                        idleCrateDisplayService.onBind(location, normalizedCrateId);
                    }
                    if (presetId != null && !presetId.isBlank()) {
                        messageService.send(
                                player.getUniqueId(),
                                "setcrate-success-preset",
                                messageService.placeholder("crate", crate.displayName()),
                                messageService.placeholder("preset", presetId.trim().toUpperCase(Locale.ROOT))
                        );
                    } else {
                        messageService.send(
                                player.getUniqueId(),
                                "setcrate-success",
                                messageService.placeholder("crate", crate.displayName())
                        );
                    }
                    plugin.getLogger().info(
                            "Setcrate bound " + normalizedCrateId
                                    + " at " + locationSummary
                                    + " by " + player.getName()
                    );
                }))
                .exceptionally(throwable -> {
                    plugin.getLogger().warning(
                            "Setcrate failed for " + normalizedCrateId
                                    + " at " + locationSummary
                                    + ": " + throwable.getMessage()
                    );
                    PluginSchedulers.run(plugin, player, () -> messageService.send(player.getUniqueId(), "setcrate-bind-failed"));
                    return null;
                });
    }

    private Optional<CrateDefinition> ensureCrateDefinition(String crateId, Location location, String presetId) {
        if (configurationLoader == null) {
            return crateRegistry.find(crateId);
        }
        Optional<CrateDefinition> existing = crateRegistry.find(crateId);
        CrateDefinitionSettings settings = configurationLoader.loadCrateSettings(crateId);
        settings.id = crateId;
        if (existing.isEmpty()) {
            settings.displayName = titleCase(crateId);
        }
        applyBoundBlockEngine(settings, location);
        if (presetId != null && !presetId.isBlank()) {
            AnimationPresetRegistry.applyPreset(settings.animations, presetId);
        }
        configurationLoader.saveCrateSettings(settings);
        CrateDefinition definition = CrateDefinitionLoader.toDefinition(settings);
        crateRegistry.register(definition);
        return Optional.of(definition);
    }

    private static void applyBoundBlockEngine(CrateDefinitionSettings settings, Location location) {
        if (settings == null || location == null || location.getWorld() == null) {
            return;
        }
        Material material = location.getBlock().getType();
        if (material.isAir()) {
            return;
        }
        settings.engine.type = "VANILLA_BLOCK";
        settings.engine.blockMaterial = material.name();
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
        Optional<CrateDefinition> crateOptional = crateRegistry.find(crateId);
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
        Optional<CrateDefinition> crateOptional = crateRegistry.find(crateId);
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
        Optional<CrateDefinition> crateOptional = crateRegistry.find(crateId);
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
                    messageService.placeholder("keys", Integer.toString(keyCountResolver.totalKeys(target, crate.id()))),
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
            String boundCrateId = bindings.get(key);
            crateRegistry.find(boundCrateId).ifPresentOrElse(
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
                            messageService.placeholder("crate", boundCrateId)
                    )
            );
        }
    }

    public void claimAll(Player player) {
        if (claimService == null || !claimService.enabled()) {
            messageService.send(player.getUniqueId(), "claim-disabled");
            return;
        }
        claimService.claimAll(player).thenAccept(count -> PluginSchedulers.run(plugin, player, () -> {
            if (count <= 0) {
                messageService.send(player.getUniqueId(), "claim-empty");
            } else {
                messageService.send(
                        player.getUniqueId(),
                        "claim-all-success",
                        messageService.placeholder("amount", Integer.toString(count))
                );
            }
        }));
    }

    private void sendKeyLine(Player player, CrateDefinition crate) {
        int virtual = keyCountResolver.virtualKeys(player.getUniqueId(), crate.id());
        int physical = keyService.countPhysicalKeys(player, crate.id());
        messageService.send(
                player.getUniqueId(),
                "keys-line",
                messageService.placeholder("crate", crate.displayName()),
                messageService.placeholder("virtual", Integer.toString(virtual)),
                messageService.placeholder("physical", Integer.toString(physical))
        );
    }

    public interface KeyCountResolver {
        int totalKeys(Player player, String crateId);

        int virtualKeys(UUID playerId, String crateId);
    }

    private void notifySender(CommandSender sender, String key, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... resolvers) {
        if (sender instanceof Player player) {
            messageService.send(player.getUniqueId(), key, resolvers);
            return;
        }
        sender.sendMessage(messageService.prefixed(null, key, resolvers));
    }

    private static String titleCase(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Crate";
        }
        String[] parts = raw.toLowerCase(Locale.ROOT).split("[\\s_-]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? "Crate" : builder.toString();
    }

    private static String locationSummary(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown";
        }
        return location.getWorld().getName()
                + ":" + location.getBlockX()
                + ":" + location.getBlockY()
                + ":" + location.getBlockZ();
    }
}
