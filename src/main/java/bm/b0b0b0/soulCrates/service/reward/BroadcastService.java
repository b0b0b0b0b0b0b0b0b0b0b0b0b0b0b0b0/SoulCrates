package bm.b0b0b0.soulCrates.service.reward;

import bm.b0b0b0.soulCrates.config.settings.BroadcastSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BroadcastService {

    private static final List<String> DRAMATIC_KEYS = List.of(
            "broadcast-win-line1",
            "broadcast-win-line2",
            "broadcast-win-line3",
            "broadcast-win-line4"
    );

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
        if (!shouldBroadcast(crate, reward)) {
            return;
        }
        if (settings.dramaticMultiLine) {
            dramaticWinBroadcast(player, crate, reward);
            return;
        }
        Bukkit.getServer().sendMessage(messageService.prefixed(
                null,
                "broadcast-win",
                placeholders(player, crate, reward)
        ));
    }

    public boolean revealBroadcastEnabled() {
        return settings.enabled && settings.dramaticMultiLine;
    }

    public void dramaticWinBroadcast(Player player, CrateDefinition crate, RewardDefinition reward) {
        if (!shouldBroadcast(crate, reward)) {
            return;
        }
        TagResolver[] resolvers = placeholders(player, crate, reward);
        for (String key : DRAMATIC_KEYS) {
            Bukkit.getServer().sendMessage(messageService.prefixed(null, key, resolvers));
        }
    }

    public boolean shouldBroadcast(CrateDefinition crate, RewardDefinition reward) {
        if (!settings.enabled) {
            return false;
        }
        if (crate.opening() != null && !crate.opening().broadcastOpens) {
            return false;
        }
        if (settings.broadcastAllOpens) {
            return true;
        }
        return reward.broadcast();
    }

    private TagResolver[] placeholders(Player player, CrateDefinition crate, RewardDefinition reward) {
        return new TagResolver[] {
                messageService.placeholder("player", player.getName()),
                messageService.placeholder("reward", reward.displayName()),
                messageService.placeholder("crate", crate.displayName())
        };
    }
}
