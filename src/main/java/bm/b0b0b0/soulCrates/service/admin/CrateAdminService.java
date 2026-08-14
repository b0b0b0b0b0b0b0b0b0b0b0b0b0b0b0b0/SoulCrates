package bm.b0b0b0.soulCrates.service.admin;

import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.claim.ClaimService;
import bm.b0b0b0.soulCrates.service.idle.IdleCrateDisplayService;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import bm.b0b0b0.soulCrates.service.location.CrateLocationService;
import bm.b0b0b0.soulCrates.service.lootbox.LootBoxService;
import bm.b0b0b0.soulCrates.service.npc.CrateNpcService;
import bm.b0b0b0.soulCrates.service.player.PlayerDataService;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
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
            KeyCountResolver keyCountResolver
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

    public void bindCrate(Player player, String crateId, Location location) {
        if (!player.hasPermission("soulcrates.command.admin")) {
            messageService.send(player.getUniqueId(), "no-permission");
            return;
        }
        if (locationService == null) {
            messageService.send(player.getUniqueId(), "startup-not-ready");
            return;
        }
        Optional<CrateDefinition> crateOptional = crateRegistry.find(crateId);
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
}
