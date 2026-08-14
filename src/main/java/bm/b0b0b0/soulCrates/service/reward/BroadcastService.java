package bm.b0b0b0.soulCrates.service.reward;

import bm.b0b0b0.soulCrates.config.settings.BroadcastSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BroadcastService {

    private BroadcastSettings settings;
    private final MessageService messageService;

    public BroadcastService(BroadcastSettings settings, MessageService messageService) {
        this.settings = settings;
        this.messageService = messageService;
    }

    public void applySettings(BroadcastSettings settings) {
        this.settings = settings;
    }

    public void maybeBroadcast(Player player, CrateDefinition crate, RewardDefinition reward) {
        if (!settings.enabled || !reward.broadcast()) {
            return;
        }
        Bukkit.getServer().sendMessage(messageService.prefixed(
                null,
                "broadcast-win",
                messageService.placeholder("player", player.getName()),
                messageService.placeholder("reward", reward.displayName()),
                messageService.placeholder("crate", crate.displayName())
        ));
    }
}
