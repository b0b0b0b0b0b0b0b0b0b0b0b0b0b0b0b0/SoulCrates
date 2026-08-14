package bm.b0b0b0.soulCrates.service.hologram;

import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.reward.RewardDisplayService;
import bm.b0b0b0.soulCrates.service.winner.LastWinnerService;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HologramLineFormatter {

    private static final Pattern REWARD_TOKEN = Pattern.compile("\\{reward:([^}]+)}", Pattern.CASE_INSENSITIVE);

    private HologramLineFormatter() {
    }

    static String forViewer(
            MessageService messageService,
            UUID viewerId,
            CrateDefinition crate,
            String template,
            LastWinnerService lastWinnerService,
            CrateRegistry crateRegistry
    ) {
        return RewardDisplayService.resolveLineTemplate(
                messageService,
                viewerId,
                crate,
                template,
                lastWinnerService,
                crateRegistry
        );
    }

    static String forExternalPlugin(String template, CrateDefinition crate) {
        if (template == null) {
            return "";
        }
        String line = template
                .replace("{crate}", crate.displayName())
                .replace("{crate_id}", crate.id());
        line = replaceRewardTokensWithPapi(line, crate.id());
        if (line.contains("{last_winner_reward}")) {
            line = line.replace(
                    "{last_winner_reward}",
                    "%soulcrates_last_winner_" + crate.id().toLowerCase(Locale.ROOT) + "_reward%"
            );
        }
        return line;
    }

    private static String replaceRewardTokensWithPapi(String line, String crateId) {
        Matcher matcher = REWARD_TOKEN.matcher(line);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String rewardId = matcher.group(1);
            String replacement = "%soulcrates_reward_"
                    + crateId.toLowerCase(Locale.ROOT)
                    + "_"
                    + rewardId.toLowerCase(Locale.ROOT)
                    + "%";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
