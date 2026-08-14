package bm.b0b0b0.soulCrates.command;

import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.service.CrateService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandGuard {

    private CommandGuard() {
    }

    public static boolean requirePlayer(CommandSender sender, MessageService messages) {
        if (sender instanceof Player) {
            return true;
        }
        messages.send(sender, "player-only");
        return false;
    }

    public static boolean requireLoaded(CrateService crateService, CommandSender sender, MessageService messages) {
        if (crateService.isLoaded()) {
            return true;
        }
        messages.send(sender, "startup-not-ready");
        return false;
    }

    public static boolean requirePermission(Player player, String permission, MessageService messages) {
        if (player.hasPermission(permission)) {
            return true;
        }
        messages.send(player.getUniqueId(), "no-permission");
        return false;
    }
}
