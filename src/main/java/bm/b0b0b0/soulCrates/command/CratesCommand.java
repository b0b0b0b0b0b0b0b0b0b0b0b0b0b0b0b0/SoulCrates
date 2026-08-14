package bm.b0b0b0.soulCrates.command;

import bm.b0b0b0.soulCrates.bootstrap.SoulCratesCore;
import bm.b0b0b0.soulCrates.config.AnimationPresetRegistry;
import bm.b0b0b0.soulCrates.hook.citizens.CitizensBridge;
import bm.b0b0b0.soulCrates.lang.MessageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class CratesCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "open", "preview", "claim", "editor", "reload", "givekey", "givecrate", "givelootbox", "setcrate", "setnpc", "keys", "stats", "locations", "shop", "virtualkeys", "paykey"
    );

    private final SoulCratesCore core;

    public CratesCommand(SoulCratesCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = core.messageService();
        if (args.length == 0) {
            sender.sendMessage(messages.prefixed(null, "startup-ready"));
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender, messages);
            case "editor" -> handleEditor(sender, messages);
            case "preview" -> handlePreview(sender, messages, args);
            case "open" -> handleOpen(sender, messages, args);
            case "shop" -> handleShop(sender, messages);
            case "claim" -> handleClaim(sender, messages, args);
            case "givelootbox" -> handleGiveLootBox(sender, messages, args);
            case "givecrate" -> handleGiveCrate(sender, messages, args);
            case "givekey" -> handleGiveKey(sender, messages, args);
            case "setcrate" -> handleSetCrate(sender, messages, args);
            case "setnpc" -> handleSetNpc(sender, messages, args);
            case "keys" -> handleKeys(sender, messages, args);
            case "stats" -> handleStats(sender, messages, args);
            case "locations" -> handleLocations(sender, messages);
            case "virtualkeys" -> handleVirtualKeys(sender, messages);
            case "paykey" -> handlePayKey(sender, messages, args);
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return ROOT_SUBCOMMANDS.stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "givekey", "givecrate", "givelootbox", "paykey" -> onlinePlayerNames(args[1]);
                case "stats" -> sender.hasPermission("soulcrates.command.stats.others")
                        ? onlinePlayerNames(args[1])
                        : List.of();
                case "open", "preview", "keys" -> crateIds(args[1]);
                case "setcrate", "setnpc" -> {
                    List<String> options = new ArrayList<>(crateIds(args[1]));
                    options.add("remove");
                    yield options;
                }
                case "claim" -> List.of("all").stream()
                        .filter(value -> value.startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .toList();
                default -> List.of();
            };
        }
        if (args.length == 3) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "givekey", "givecrate", "givelootbox", "paykey" -> crateIds(args[2]);
                case "open" -> amountSuggestions(args[2]);
                case "setcrate" -> {
                    if ("remove".equalsIgnoreCase(args[1])) {
                        yield List.of();
                    }
                    yield presetIds(args[2]);
                }
                default -> List.of();
            };
        }
        if (args.length == 4) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "givekey" -> giveKeyAmountSuggestions(args[3]);
                case "givecrate" -> giveCrateFourthArgSuggestions(args[3]);
                case "givelootbox" -> amountSuggestions(args[3]);
                case "paykey" -> amountSuggestions(args[3]);
                default -> List.of();
            };
        }
        if (args.length == 5) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "givekey" -> giveKeyAmountSuggestions(args[4]);
                case "givecrate" -> giveCrateFifthArgSuggestions(args[3], args[4]);
                default -> List.of();
            };
        }
        return List.of();
    }

    private boolean handleReload(CommandSender sender, MessageService messages) {
        if (!sender.hasPermission("soulcrates.command.reload")) {
            messages.send(sender, "no-permission");
            return true;
        }
        core.reload();
        messages.send(sender, "reload-success");
        return true;
    }

    private boolean handleEditor(CommandSender sender, MessageService messages) {
        if (!CommandGuard.requirePlayer(sender, messages)) {
            return true;
        }
        core.crateService().openEditor((Player) sender);
        return true;
    }

    private boolean handlePreview(CommandSender sender, MessageService messages, String[] args) {
        if (!CommandGuard.requirePlayer(sender, messages)) {
            return true;
        }
        Player player = (Player) sender;
        if (!CommandGuard.requirePermission(player, "soulcrates.command.preview", messages)) {
            return true;
        }
        if (!CommandGuard.requireLoaded(core.crateService(), sender, messages)) {
            return true;
        }
        String crateId = args.length >= 2 ? args[1] : core.pluginConfig().cratesSettings().defaultCrateId;
        core.crateService().openPreview(player, crateId);
        return true;
    }

    private boolean handleOpen(CommandSender sender, MessageService messages, String[] args) {
        if (!CommandGuard.requirePlayer(sender, messages)) {
            return true;
        }
        Player player = (Player) sender;
        if (!CommandGuard.requirePermission(player, "soulcrates.command.open", messages)) {
            return true;
        }
        if (!CommandGuard.requireLoaded(core.crateService(), sender, messages)) {
            return true;
        }
        String crateId = args.length >= 2 ? args[1] : core.pluginConfig().cratesSettings().defaultCrateId;
        int amount = args.length >= 3 ? parseAmount(args[2]) : 1;
        core.crateService().beginOpen(player, crateId, player.getLocation(), amount);
        return true;
    }

    private boolean handleShop(CommandSender sender, MessageService messages) {
        if (!CommandGuard.requirePlayer(sender, messages)) {
            return true;
        }
        Player player = (Player) sender;
        if (!CommandGuard.requirePermission(player, "soulcrates.command.shop", messages)) {
            return true;
        }
        if (!CommandGuard.requireLoaded(core.crateService(), sender, messages)) {
            return true;
        }
        core.crateService().openShop(player);
        return true;
    }

    private boolean handleClaim(CommandSender sender, MessageService messages, String[] args) {
        if (!CommandGuard.requirePlayer(sender, messages)) {
            return true;
        }
        Player player = (Player) sender;
        if (!CommandGuard.requirePermission(player, "soulcrates.command.claim", messages)) {
            return true;
        }
        if (!CommandGuard.requireLoaded(core.crateService(), sender, messages)) {
            return true;
        }
        if (args.length >= 2 && "all".equalsIgnoreCase(args[1])) {
            core.crateService().claimAll(player);
            return true;
        }
        core.crateService().openClaim(player);
        return true;
    }

    private boolean handleGiveLootBox(CommandSender sender, MessageService messages, String[] args) {
        if (!sender.hasPermission("soulcrates.command.givelootbox")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (!CommandGuard.requireLoaded(core.crateService(), sender, messages)) {
            return true;
        }
        if (args.length < 3) {
            return false;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messages.send(sender, "player-not-found");
            return true;
        }
        int amount = args.length >= 4 ? parseAmount(args[3]) : 1;
        core.crateService().giveLootBox(sender, target, args[2], amount);
        return true;
    }

    private boolean handleGiveCrate(CommandSender sender, MessageService messages, String[] args) {
        if (!sender.hasPermission("soulcrates.command.givecrate")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (!CommandGuard.requireLoaded(core.crateService(), sender, messages)) {
            return true;
        }
        if (args.length < 3) {
            return false;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messages.send(sender, "player-not-found");
            return true;
        }
        int amount = 1;
        String preset = null;
        if (args.length >= 4) {
            if (looksLikeAmount(args[3])) {
                amount = parseAmount(args[3]);
                if (args.length >= 5) {
                    preset = args[4];
                }
            } else {
                preset = args[3];
                if (args.length >= 5) {
                    amount = parseAmount(args[4]);
                }
            }
        }
        core.crateService().givePhysicalCrate(sender, target, args[2], amount, preset);
        return true;
    }

    private static boolean looksLikeAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        for (int index = 0; index < raw.length(); index++) {
            if (!Character.isDigit(raw.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private List<String> giveCrateFourthArgSuggestions(String partial) {
        List<String> options = new ArrayList<>(amountSuggestions(partial));
        options.addAll(AnimationPresetRegistry.presetIds().stream()
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(partial.toLowerCase(Locale.ROOT)))
                .toList());
        return options;
    }

    private List<String> giveCrateFifthArgSuggestions(String arg3, String partial) {
        if (looksLikeAmount(arg3)) {
            return AnimationPresetRegistry.presetIds().stream()
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(partial.toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return amountSuggestions(partial);
    }

    private boolean handleGiveKey(CommandSender sender, MessageService messages, String[] args) {
        if (!sender.hasPermission("soulcrates.command.givekey")) {
            messages.send(sender, "no-permission");
            return true;
        }
        if (!CommandGuard.requireLoaded(core.crateService(), sender, messages)) {
            return true;
        }
        if (args.length < 3) {
            return false;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messages.send(sender, "player-not-found");
            return true;
        }
        String crateId = args[2];
        int amount = 1;
        boolean physical = false;
        if (args.length >= 4) {
            if ("physical".equalsIgnoreCase(args[3])) {
                physical = true;
                if (args.length >= 5) {
                    amount = parseAmount(args[4]);
                }
            } else {
                amount = parseAmount(args[3]);
                if (args.length >= 5 && "physical".equalsIgnoreCase(args[4])) {
                    physical = true;
                }
            }
        }
        if (sender instanceof Player playerSender) {
            core.crateService().giveKeys(playerSender, target, crateId, amount, physical);
        } else {
            core.crateService().giveKeys(sender, target, crateId, amount, physical);
        }
        return true;
    }

    private boolean handleSetCrate(CommandSender sender, MessageService messages, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            messages.send(player.getUniqueId(), "setcrate-no-block");
            return true;
        }
        if (args.length >= 2 && "remove".equalsIgnoreCase(args[1])) {
            core.crateService().unbindCrate(player, block.getLocation());
            return true;
        }
        if (args.length < 2) {
            return false;
        }
        String preset = args.length >= 3 ? args[2] : null;
        core.crateService().bindCrate(player, args[1], block.getLocation(), preset);
        return true;
    }

    private boolean handleSetNpc(CommandSender sender, MessageService messages, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("soulcrates.command.admin")) {
            messages.send(player.getUniqueId(), "no-permission");
            return true;
        }
        if (!CitizensBridge.isAvailable()) {
            messages.send(player.getUniqueId(), "setnpc-citizens-missing");
            return true;
        }
        Entity target = player.getTargetEntity(5);
        if (target == null) {
            messages.send(player.getUniqueId(), "setnpc-no-entity");
            return true;
        }
        var npcIdOptional = CitizensBridge.npcId(target);
        if (npcIdOptional.isEmpty()) {
            messages.send(player.getUniqueId(), "setnpc-not-npc");
            return true;
        }
        if (args.length >= 2 && "remove".equalsIgnoreCase(args[1])) {
            core.crateService().unbindNpc(player, npcIdOptional.get());
            return true;
        }
        if (args.length < 2) {
            return false;
        }
        core.crateService().bindNpc(player, args[1], npcIdOptional.get());
        return true;
    }

    private boolean handleKeys(CommandSender sender, MessageService messages, String[] args) {
        if (!CommandGuard.requirePlayer(sender, messages)) {
            return true;
        }
        Player player = (Player) sender;
        if (!CommandGuard.requirePermission(player, "soulcrates.command.keys", messages)) {
            return true;
        }
        if (!CommandGuard.requireLoaded(core.crateService(), sender, messages)) {
            return true;
        }
        String crateId = args.length >= 2 ? args[1] : null;
        core.crateService().showKeys(player, crateId);
        return true;
    }

    private boolean handleStats(CommandSender sender, MessageService messages, String[] args) {
        if (!CommandGuard.requirePlayer(sender, messages)) {
            return true;
        }
        Player viewer = (Player) sender;
        Player target = viewer;
        if (args.length >= 2) {
            if (!CommandGuard.requirePermission(viewer, "soulcrates.command.stats.others", messages)) {
                return true;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                messages.send(viewer.getUniqueId(), "player-not-found");
                return true;
            }
        }
        if (!CommandGuard.requireLoaded(core.crateService(), sender, messages)) {
            return true;
        }
        core.crateService().showStats(viewer, target);
        return true;
    }

    private boolean handleLocations(CommandSender sender, MessageService messages) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("soulcrates.command.admin")) {
            messages.send(player.getUniqueId(), "no-permission");
            return true;
        }
        core.crateService().listLocations(player);
        return true;
    }

    private boolean handleVirtualKeys(CommandSender sender, MessageService messages) {
        if (!CommandGuard.requirePlayer(sender, messages)) {
            return true;
        }
        Player player = (Player) sender;
        if (!CommandGuard.requirePermission(player, "soulcrates.command.virtualkeys", messages)) {
            return true;
        }
        if (!CommandGuard.requireLoaded(core.crateService(), sender, messages)) {
            return true;
        }
        core.crateService().openVirtualKeys(player);
        return true;
    }

    private boolean handlePayKey(CommandSender sender, MessageService messages, String[] args) {
        if (!CommandGuard.requirePlayer(sender, messages)) {
            return true;
        }
        Player player = (Player) sender;
        if (!CommandGuard.requirePermission(player, "soulcrates.command.paykey", messages)) {
            return true;
        }
        if (!CommandGuard.requireLoaded(core.crateService(), sender, messages)) {
            return true;
        }
        if (args.length < 4) {
            return false;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messages.send(player.getUniqueId(), "player-not-found");
            return true;
        }
        int amount = parseAmount(args[3]);
        core.crateService().payVirtualKeys(player, target, args[2], amount);
        return true;
    }

    private List<String> onlinePlayerNames(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private List<String> amountSuggestions(String prefix) {
        return List.of("1", "5", "10").stream()
                .filter(value -> value.startsWith(prefix))
                .toList();
    }

    private List<String> giveKeyAmountSuggestions(String prefix) {
        List<String> options = new ArrayList<>();
        options.add("physical");
        options.addAll(List.of("1", "5", "10", "64"));
        return options.stream()
                .filter(value -> value.startsWith(prefix.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private List<String> crateIds(String prefix) {
        return core.crateService().crateRegistry().list().stream()
                .map(crate -> crate.id())
                .filter(id -> id.startsWith(prefix.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private List<String> presetIds(String prefix) {
        return AnimationPresetRegistry.presetIds().stream()
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private int parseAmount(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }
}
