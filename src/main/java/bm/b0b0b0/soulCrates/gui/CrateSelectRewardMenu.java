package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiSelectSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import bm.b0b0b0.soulCrates.service.reward.RewardRollService;
import bm.b0b0b0.soulCrates.service.reward.WinLimitService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CrateSelectRewardMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiSelectSettings selectSettings;
    private final CrateDefinition crateDefinition;
    private final RewardRollService rewardRollService;
    private final WinLimitService winLimitService;
    private final KeyService keyService;
    private final Location openLocation;
    private final BiConsumer<Player, RewardDefinition> redeemAction;
    private final Runnable backAction;
    private final Map<Integer, RewardDefinition> rewardSlots = new HashMap<>();

    public CrateSelectRewardMenu(
            UUID viewerId,
            MessageService messageService,
            GuiSelectSettings selectSettings,
            CrateDefinition crateDefinition,
            RewardRollService rewardRollService,
            WinLimitService winLimitService,
            KeyService keyService,
            Location openLocation,
            BiConsumer<Player, RewardDefinition> redeemAction,
            Runnable backAction
    ) {
        super(
                viewerId,
                normalizeSize(selectSettings.size),
                messageService.component(viewerId, "select-title", Placeholder.parsed("crate", crateDefinition.displayName()))
        );
        this.messageService = messageService;
        this.selectSettings = selectSettings;
        this.crateDefinition = crateDefinition;
        this.rewardRollService = rewardRollService;
        this.winLimitService = winLimitService;
        this.keyService = keyService;
        this.openLocation = openLocation;
        this.redeemAction = redeemAction;
        this.backAction = backAction;
        refresh();
    }

    public Location openLocation() {
        return openLocation;
    }

    @Override
    public void refresh() {
        getInventory().clear();
        rewardSlots.clear();
        Player player = Bukkit.getPlayer(viewerId());
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < getInventory().getSize(); slot++) {
            getInventory().setItem(slot, GuiItemFactory.filler(selectSettings.fillerMaterial));
        }
        List<Integer> slots = selectSettings.rewardSlots;
        List<RewardDefinition> rewards = crateDefinition.rewards();
        for (int index = 0; index < slots.size() && index < rewards.size(); index++) {
            RewardDefinition reward = rewards.get(index);
            int slot = slots.get(index);
            rewardSlots.put(slot, reward);
            if (!winLimitService.canSelectByKeyRarity(crateDefinition, reward)) {
                getInventory().setItem(slot, lockedReward(player, reward, "select-locked-rarity"));
                continue;
            }
            getInventory().setItem(
                    slot,
                    selectableReward(player, reward)
            );
        }
        int keysRequired = Math.max(1, crateDefinition.opening().keysRequired);
        int keysOwned = keyService.totalKeys(player, crateDefinition.id());
        getInventory().setItem(
                selectSettings.keysInfoSlot,
                GuiItemFactory.actionButton(
                        messageService,
                        player,
                        "select-keys-title",
                        null
                )
        );
        ItemStack keysInfo = getInventory().getItem(selectSettings.keysInfoSlot);
        if (keysInfo != null && keysInfo.hasItemMeta()) {
            var meta = keysInfo.getItemMeta();
            meta.lore(List.of(
                    messageService.component(
                            player.getUniqueId(),
                            "select-keys-lore",
                            messageService.placeholder("keys", Integer.toString(keysOwned)),
                            messageService.placeholder("required", Integer.toString(keysRequired))
                    )
            ));
            keysInfo.setItemMeta(meta);
        }
        getInventory().setItem(selectSettings.backSlot, GuiItemFactory.cancelButton(messageService, player));
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (click.slot() == selectSettings.backSlot) {
            player.closeInventory();
            if (backAction != null) {
                backAction.run();
            }
            return;
        }
        RewardDefinition reward = rewardSlots.get(click.slot());
        if (reward == null) {
            return;
        }
        if (!winLimitService.canSelectByKeyRarity(crateDefinition, reward)) {
            messageService.send(player.getUniqueId(), "select-locked-rarity");
            return;
        }
        player.closeInventory();
        redeemAction.accept(player, reward);
    }

    private ItemStack selectableReward(Player player, RewardDefinition reward) {
        int required = reward.requiredKeys(crateDefinition.opening().keysRequired);
        ItemStack item = GuiItemFactory.rewardPreview(
                messageService,
                player,
                crateDefinition,
                reward,
                rewardRollService.chancePercent(crateDefinition, reward)
        );
        if (item.hasItemMeta()) {
            var meta = item.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = meta.lore() == null
                    ? new java.util.ArrayList<>()
                    : new java.util.ArrayList<>(meta.lore());
            lore.add(messageService.component(
                    player.getUniqueId(),
                    "select-reward-keys",
                    messageService.placeholder("required", Integer.toString(required))
            ));
            lore.add(messageService.component(player.getUniqueId(), "select-reward-click"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack lockedReward(Player player, RewardDefinition reward, String loreKey) {
        ItemStack item = GuiItemFactory.filler(selectSettings.lockedMaterial);
        if (item.hasItemMeta()) {
            var meta = item.getItemMeta();
            meta.displayName(messageService.component(
                    player.getUniqueId(),
                    "reward-preview-name",
                    messageService.placeholder("reward", reward.displayName())
            ));
            meta.lore(List.of(messageService.component(player.getUniqueId(), loreKey)));
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
