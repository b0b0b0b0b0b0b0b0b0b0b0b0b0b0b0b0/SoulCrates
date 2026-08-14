package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiPreviewSettings;
import bm.b0b0b0.soulCrates.config.settings.PremiumOpeningSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CratePreviewMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiPreviewSettings previewSettings;
    private final PremiumOpeningSettings premiumOpeningSettings;
    private final CrateDefinition crateDefinition;
    private final RewardRollService rewardRollService;
    private final BiConsumer<Player, Integer> openAction;
    private final Runnable backAction;
    private final Map<Integer, Integer> multiOpenSlots = new HashMap<>();

    public CratePreviewMenu(
            UUID viewerId,
            MessageService messageService,
            GuiPreviewSettings previewSettings,
            PremiumOpeningSettings premiumOpeningSettings,
            CrateDefinition crateDefinition,
            RewardRollService rewardRollService,
            BiConsumer<Player, Integer> openAction,
            Runnable backAction
    ) {
        super(
                viewerId,
                normalizeSize(previewSettings.size),
                messageService.component(viewerId, "preview-title", Placeholder.parsed("crate", crateDefinition.displayName()))
        );
        this.messageService = messageService;
        this.previewSettings = previewSettings;
        this.premiumOpeningSettings = premiumOpeningSettings;
        this.crateDefinition = crateDefinition;
        this.rewardRollService = rewardRollService;
        this.openAction = openAction;
        this.backAction = backAction;
        refresh();
    }

    @Override
    public void refresh() {
        getInventory().clear();
        multiOpenSlots.clear();
        Player player = Bukkit.getPlayer(viewerId());
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < getInventory().getSize(); slot++) {
            getInventory().setItem(slot, GuiItemFactory.filler(previewSettings.fillerMaterial));
        }
        List<Integer> rewardSlots = previewSettings.rewardSlots;
        List<RewardDefinition> rewards = crateDefinition.rewards();
        for (int index = 0; index < rewardSlots.size() && index < rewards.size(); index++) {
            RewardDefinition reward = rewards.get(index);
            getInventory().setItem(
                    rewardSlots.get(index),
                    GuiItemFactory.rewardPreview(
                            messageService,
                            player,
                            crateDefinition,
                            reward,
                            rewardRollService.chancePercent(crateDefinition, reward)
                    )
            );
        }
        getInventory().setItem(previewSettings.openSlot, GuiItemFactory.actionButton(messageService, player, "preview-open-title", "preview-open-lore"));
        if (previewSettings.multiOpenButtons
                && crateDefinition.opening().allowMultiOpen
                && crateDefinition.opening().massOpening.enabled
                && player.hasPermission(premiumOpeningSettings.multiOpenPermission)) {
            List<Integer> presets = crateDefinition.opening().massOpening.presets;
            List<Integer> slots = previewSettings.multiOpenSlots;
            for (int index = 0; index < presets.size() && index < slots.size(); index++) {
                int amount = presets.get(index);
                int slot = slots.get(index);
                multiOpenSlots.put(slot, amount);
                getInventory().setItem(slot, multiOpenButton(player, amount));
            }
        }
        getInventory().setItem(previewSettings.backSlot, GuiItemFactory.cancelButton(messageService, player));
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (click.slot() == previewSettings.openSlot) {
            player.closeInventory();
            openAction.accept(player, 1);
            return;
        }
        Integer amount = multiOpenSlots.get(click.slot());
        if (amount != null) {
            player.closeInventory();
            openAction.accept(player, amount);
            return;
        }
        if (click.slot() == previewSettings.backSlot) {
            player.closeInventory();
            if (backAction != null) {
                backAction.run();
            }
        }
    }

    private ItemStack multiOpenButton(Player player, int amount) {
        if (amount < 0) {
            return GuiItemFactory.actionButton(messageService, player, "preview-open-all-title", "preview-open-all-lore");
        }
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messageService.component(
                    player.getUniqueId(),
                    "preview-open-xn-title",
                    messageService.placeholder("amount", Integer.toString(amount))
            ));
            meta.lore(List.of(messageService.component(
                    player.getUniqueId(),
                    "preview-open-xn-lore",
                    messageService.placeholder("amount", Integer.toString(amount))
            )));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static int normalizeSize(int size) {
        if (size < 9 || size % 9 != 0) {
            return 54;
        }
        return size;
    }
}
