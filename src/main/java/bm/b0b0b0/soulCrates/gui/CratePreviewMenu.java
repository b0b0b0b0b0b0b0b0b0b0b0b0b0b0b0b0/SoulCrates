package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiPreviewSettings;
import bm.b0b0b0.soulCrates.config.settings.PremiumOpeningSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CratePreviewMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiPreviewSettings previewSettings;
    private final PremiumOpeningSettings premiumOpeningSettings;
    private final CrateDefinition crateDefinition;
    private final RewardRollService rewardRollService;
    private final BiConsumer<Player, Integer> openAction;
    private final Runnable backAction;

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
                    GuiItemFactory.rewardPreview(messageService, player, reward, rewardRollService.chancePercent(crateDefinition, reward))
            );
        }
        getInventory().setItem(previewSettings.openSlot, GuiItemFactory.actionButton(messageService, player, "preview-open-title", "preview-open-lore"));
        if (previewSettings.multiOpenButtons
                && crateDefinition.opening().allowMultiOpen
                && player.hasPermission(premiumOpeningSettings.multiOpenPermission)) {
            getInventory().setItem(
                    previewSettings.multiOpenSlot5,
                    GuiItemFactory.actionButton(messageService, player, "preview-open-x5-title", "preview-open-x5-lore")
            );
            getInventory().setItem(
                    previewSettings.multiOpenSlot10,
                    GuiItemFactory.actionButton(messageService, player, "preview-open-x10-title", "preview-open-x10-lore")
            );
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
        if (click.slot() == previewSettings.multiOpenSlot5) {
            player.closeInventory();
            openAction.accept(player, 5);
            return;
        }
        if (click.slot() == previewSettings.multiOpenSlot10) {
            player.closeInventory();
            openAction.accept(player, 10);
            return;
        }
        if (click.slot() == previewSettings.backSlot) {
            player.closeInventory();
            if (backAction != null) {
                backAction.run();
            }
        }
    }

    private static int normalizeSize(int size) {
        if (size < 9 || size % 9 != 0) {
            return 54;
        }
        return size;
    }
}
