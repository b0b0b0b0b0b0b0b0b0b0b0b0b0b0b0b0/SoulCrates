package bm.b0b0b0.soulCrates.command;

import bm.b0b0b0.soulCrates.bootstrap.SoulCratesCore;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.service.CrateService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class CratesCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "open", "preview", "editor", "reload", "givekey", "setcrate", "keys", "stats", "locations", "shop"
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
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(sub)) {
            if (!sender.hasPermission("soulcrates.command.reload")) {
                messages.send(sender, "no-permission");
                return true;
            }
            core.reload();
            messages.send(sender, "reload-success");
            return true;
        }
        if ("editor".equals(sub)) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "player-only");
                return true;
            }
            core.crateService().openEditor(player);
            return true;
        }
        if ("preview".equals(sub)) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "player-only");
                return true;
            }
            if (!player.hasPermission("soulcrates.command.preview")) {
                messages.send(player.getUniqueId(), "no-permission");
                return true;
            }
            CrateService crateService = core.crateService();
            if (!crateService.isLoaded()) {
                messages.send(player.getUniqueId(), "startup-not-ready");
                return true;
            }
            String crateId = args.length >= 2 ? args[1] : core.pluginConfig().cratesSettings().defaultCrateId;
            crateService.openPreview(player, crateId);
            return true;
        }
        if ("open".equals(sub)) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "player-only");
                return true;
            }
            if (!player.hasPermission("soulcrates.command.open")) {
                messages.send(player.getUniqueId(), "no-permission");
                return true;
            }
            CrateService crateService = core.crateService();
            if (!crateService.isLoaded()) {
                messages.send(player.getUniqueId(), "startup-not-ready");
                return true;
            }
            String crateId = args.length >= 2 ? args[1] : core.pluginConfig().cratesSettings().defaultCrateId;
            int amount = 1;
            if (args.length >= 3) {
                amount = parseAmount(args[2]);
            }
            crateService.beginOpen(player, crateId, player.getLocation(), amount);
            return true;
        }
        if ("shop".equals(sub)) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "player-only");
                return true;
            }
            if (!player.hasPermission("soulcrates.command.shop")) {
                messages.send(player.getUniqueId(), "no-permission");
                return true;
            }
            CrateService crateService = core.crateService();
            if (!crateService.isLoaded()) {
                messages.send(player.getUniqueId(), "startup-not-ready");
                return true;
            }
            crateService.openShop(player);
            return true;
        }
        if ("givekey".equals(sub)) {
            if (!sender.hasPermission("soulcrates.command.givekey")) {
                messages.send(sender, "no-permission");
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
            if (!(sender instanceof Player playerSender)) {
                core.crateService().giveKeys(sender, target, crateId, amount, physical);
                return true;
            }
            core.crateService().giveKeys(playerSender, target, crateId, amount, physical);
            return true;
        }
        if ("setcrate".equals(sub)) {
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
            core.crateService().bindCrate(player, args[1], block.getLocation());
            return true;
        }
        if ("keys".equals(sub)) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "player-only");
                return true;
            }
            if (!player.hasPermission("soulcrates.command.keys")) {
                messages.send(player.getUniqueId(), "no-permission");
                return true;
            }
            String crateId = args.length >= 2 ? args[1] : null;
            core.crateService().showKeys(player, crateId);
            return true;
        }
        if ("stats".equals(sub)) {
            if (!(sender instanceof Player viewer)) {
                messages.send(sender, "player-only");
                return true;
            }
            Player target = viewer;
            if (args.length >= 2) {
                if (!viewer.hasPermission("soulcrates.command.stats.others")) {
                    messages.send(viewer.getUniqueId(), "no-permission");
                    return true;
                }
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    messages.send(viewer.getUniqueId(), "player-not-found");
                    return true;
                }
            }
            if (!core.crateService().isLoaded()) {
                messages.send(viewer.getUniqueId(), "startup-not-ready");
                return true;
            }
            core.crateService().showStats(viewer, target);
            return true;
        }
        if ("locations".equals(sub)) {
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
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return ROOT_SUBCOMMANDS.stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("givekey".equals(sub)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .toList();
            }
            if ("stats".equals(sub) && sender.hasPermission("soulcrates.command.stats.others")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .toList();
            }
            if ("open".equals(sub) || "preview".equals(sub) || "keys".equals(sub) || "setcrate".equals(sub)) {
                if ("setcrate".equals(sub)) {
                    List<String> options = new ArrayList<>(crateIds(args[1]));
                    options.add("remove");
                    return options;
                }
                return crateIds(args[1]);
            }
        }
        if (args.length == 3 && "givekey".equalsIgnoreCase(args[0])) {
            return crateIds(args[2]);
        }
        if (args.length == 3 && "open".equalsIgnoreCase(args[0])) {
            return List.of("1", "5", "10").stream()
                    .filter(value -> value.startsWith(args[2]))
                    .toList();
        }
        if (args.length == 4 && "givekey".equalsIgnoreCase(args[0])) {
            List<String> options = new ArrayList<>();
            options.add("physical");
            options.addAll(List.of("1", "5", "10", "64"));
            return options.stream()
                    .filter(value -> value.startsWith(args[3].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }

    private List<String> crateIds(String prefix) {
        return core.crateService().crateRegistry().list().stream()
                .map(crate -> crate.id())
                .filter(id -> id.startsWith(prefix.toLowerCase(Locale.ROOT)))
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
