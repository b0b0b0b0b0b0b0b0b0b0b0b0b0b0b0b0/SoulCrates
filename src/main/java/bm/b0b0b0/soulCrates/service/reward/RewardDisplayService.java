package bm.b0b0b0.soulCrates.service.reward;

import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.model.WinnerEntry;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.winner.LastWinnerService;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class RewardDisplayService {

    public static final String LANG_KEY_PREFIX = "reward-name.";
    private static final Pattern REWARD_TOKEN = Pattern.compile("\\{reward:([^}]+)}", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINIMESSAGE_HINT = Pattern.compile("<[^>]+>");

    private RewardDisplayService() {
    }

    public static String langKey(String crateId, String rewardId) {
        return LANG_KEY_PREFIX
                + normalizeId(crateId)
                + "."
                + normalizeId(rewardId);
    }

    public static Optional<RewardDefinition> findReward(CrateRegistry registry, String crateId, String rewardId) {
        if (registry == null || crateId == null || rewardId == null) {
            return Optional.empty();
        }
        return registry.find(crateId).flatMap(crate -> crate.rewards().stream()
                .filter(reward -> reward.id().equalsIgnoreCase(rewardId))
                .findFirst());
    }

    public static Component displayName(MessageService messages, UUID playerId, String crateId, RewardDefinition reward) {
        if (reward == null) {
            return Component.empty();
        }
        String key = langKey(crateId, reward.id());
        if (messages.hasKey(playerId, key)) {
            return messages.component(playerId, key);
        }
        Material material = material(reward);
        if (shouldUseMaterialName(reward, material)) {
            return Component.translatable(material.translationKey()).decoration(TextDecoration.ITALIC, false);
        }
        if (looksLikeMiniMessage(reward.displayName())) {
            return messages.parse(reward.displayName());
        }
        return messages.component(
                playerId,
                "reward-preview-name",
                messages.placeholder("reward", reward.displayName())
        );
    }

    public static Component displayName(MessageService messages, UUID playerId, CrateDefinition crate, RewardDefinition reward) {
        return displayName(messages, playerId, crate == null ? "" : crate.id(), reward);
    }

    public static String plainText(MessageService messages, UUID playerId, String crateId, RewardDefinition reward) {
        Material material = material(reward);
        if (shouldUseMaterialName(reward, material)) {
            int amount = primaryGrantAmount(reward, material);
            Component itemName = Component.translatable(material.translationKey()).decoration(TextDecoration.ITALIC, false);
            if (amount > 1) {
                return PlainTextComponentSerializer.plainText().serialize(messages.component(
                        playerId,
                        "reward-item-quantity",
                        Placeholder.component("item", itemName),
                        Placeholder.parsed("amount", Integer.toString(amount))
                ));
            }
            return PlainTextComponentSerializer.plainText().serialize(itemName);
        }
        return PlainTextComponentSerializer.plainText().serialize(displayName(messages, playerId, crateId, reward));
    }

    public static void applyPreviewStackSize(ItemStack item, RewardDefinition reward, Material material) {
        if (item == null || reward == null || material == null) {
            return;
        }
        item.setAmount(clampStackSize(material, primaryGrantAmount(reward, material)));
    }

    public static int clampStackSize(Material material, int amount) {
        if (material == null) {
            return Math.max(1, amount);
        }
        return Math.max(1, Math.min(amount, material.getMaxStackSize()));
    }

    public static String plainText(MessageService messages, UUID playerId, CrateRegistry registry, String crateId, String rewardId) {
        return findReward(registry, crateId, rewardId)
                .map(reward -> plainText(messages, playerId, crateId, reward))
                .orElse(rewardId == null ? "" : rewardId);
    }

    public static int primaryGrantAmount(RewardDefinition reward, Material previewMaterial) {
        if (reward.grants() == null || reward.grants().isEmpty()) {
            return 1;
        }
        int matchedAmount = 0;
        int fallbackAmount = 0;
        for (String grant : reward.grants()) {
            GrantParts parts = parseMaterialGrant(grant);
            if (parts == null) {
                continue;
            }
            if (fallbackAmount == 0) {
                fallbackAmount = parts.amount();
            }
            if (parts.material() == previewMaterial) {
                matchedAmount += parts.amount();
            }
        }
        if (matchedAmount > 0) {
            return matchedAmount;
        }
        return fallbackAmount > 0 ? fallbackAmount : 1;
    }

    public static String resolveLineTemplate(
            MessageService messages,
            UUID playerId,
            CrateDefinition crate,
            String template,
            LastWinnerService lastWinnerService,
            CrateRegistry crateRegistry
    ) {
        if (template == null) {
            return "";
        }
        String crateId = crate == null ? "" : crate.id();
        String line = template
                .replace("{crate}", crate == null ? "" : crate.displayName())
                .replace("{crate_id}", crateId);
        line = replaceRewardTokens(messages, playerId, crateId, crate, line);
        line = replaceLastWinnerRewardToken(messages, playerId, crateId, line, lastWinnerService, crateRegistry);
        return line;
    }

    private static String replaceRewardTokens(
            MessageService messages,
            UUID playerId,
            String crateId,
            CrateDefinition crate,
            String line
    ) {
        Matcher matcher = REWARD_TOKEN.matcher(line);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String rewardId = matcher.group(1);
            String resolved = "";
            if (crate != null) {
                resolved = crate.rewards().stream()
                        .filter(reward -> reward.id().equalsIgnoreCase(rewardId))
                        .findFirst()
                        .map(reward -> plainText(messages, playerId, crateId, reward))
                        .orElse(rewardId);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String replaceLastWinnerRewardToken(
            MessageService messages,
            UUID playerId,
            String crateId,
            String line,
            LastWinnerService lastWinnerService,
            CrateRegistry crateRegistry
    ) {
        if (!line.contains("{last_winner_reward}")) {
            return line;
        }
        String resolved = "";
        if (lastWinnerService != null && lastWinnerService.enabled()) {
            WinnerEntry entry = lastWinnerService.winner(crateId, 1);
            if (entry != null) {
                resolved = findReward(crateRegistry, crateId, entry.rewardId())
                        .map(reward -> plainText(messages, playerId, crateId, reward))
                        .orElse(entry.rewardDisplay() == null ? "" : entry.rewardDisplay());
            }
        }
        return line.replace("{last_winner_reward}", resolved);
    }

    public static Material material(RewardDefinition reward) {
        Material material = Material.matchMaterial(reward.material());
        if (material == null || material.isAir()) {
            material = Material.PAPER;
        }
        return material;
    }

    public static boolean shouldUseMaterialName(RewardDefinition reward, Material material) {
        if (isCommandOnlyReward(reward)) {
            return false;
        }
        if (hasItemGrants(reward)) {
            if (looksLikeMiniMessage(reward.displayName())) {
                return false;
            }
            if (hasCommands(reward)) {
                return reward.displayName() == null || reward.displayName().isBlank();
            }
            return true;
        }
        if (reward.displayName() == null || reward.displayName().isBlank()) {
            return true;
        }
        String display = reward.displayName().trim();
        String materialKey = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return display.equalsIgnoreCase(material.name())
                || display.equalsIgnoreCase(materialKey);
    }

    public static boolean isCommandOnlyReward(RewardDefinition reward) {
        if (!hasCommands(reward)) {
            return false;
        }
        return !hasItemGrants(reward);
    }

    private static boolean hasItemGrants(RewardDefinition reward) {
        if (reward.grants() == null) {
            return false;
        }
        for (String grant : reward.grants()) {
            if (parseMaterialGrant(grant) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCommands(RewardDefinition reward) {
        return reward.commands() != null && !reward.commands().isEmpty();
    }

    private static GrantParts parseMaterialGrant(String grant) {
        if (grant == null || grant.isBlank()) {
            return null;
        }
        String normalized = grant.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("vault:") || normalized.startsWith("money:")) {
            return null;
        }
        int separator = grant.indexOf(':');
        if (separator <= 0) {
            return null;
        }
        Material material = Material.matchMaterial(grant.substring(0, separator).trim());
        if (material == null || material.isAir()) {
            return null;
        }
        int amount = 1;
        try {
            amount = Math.max(1, Integer.parseInt(grant.substring(separator + 1).trim()));
        } catch (NumberFormatException exception) {
            double parsed = Double.parseDouble(grant.substring(separator + 1).trim());
            amount = Math.max(1, (int) Math.round(parsed));
        }
        return new GrantParts(material, amount);
    }

    private static boolean looksLikeMiniMessage(String raw) {
        return raw != null && MINIMESSAGE_HINT.matcher(raw).find();
    }

    private static String normalizeId(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private record GrantParts(Material material, int amount) {
    }
}
