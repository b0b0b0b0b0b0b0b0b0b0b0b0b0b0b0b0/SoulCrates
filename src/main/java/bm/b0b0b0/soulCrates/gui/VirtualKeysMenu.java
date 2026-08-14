package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiVirtualKeysSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.key.KeyService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class VirtualKeysMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiVirtualKeysSettings virtualKeysSettings;
    private final CrateRegistry crateRegistry;
    private final KeyService keyService;
    private final Runnable backAction;
    private final Map<Integer, String> entrySlots = new HashMap<>();

    public VirtualKeysMenu(
            UUID viewerId,
            MessageService messageService,
            GuiVirtualKeysSettings virtualKeysSettings,
            CrateRegistry crateRegistry,
            KeyService keyService,
            Runnable backAction
    ) {
        super(
                viewerId,
                normalizeSize(virtualKeysSettings.size),
                messageService.component(viewerId, "virtual-keys-title")
        );
        this.messageService = messageService;
        this.virtualKeysSettings = virtualKeysSettings;
        this.crateRegistry = crateRegistry;
        this.keyService = keyService;
        this.backAction = backAction;
        refresh();
    }

    @Override
    public void refresh() {
        getInventory().clear();
        entrySlots.clear();
        Player player = Bukkit.getPlayer(viewerId());
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < getInventory().getSize(); slot++) {
            getInventory().setItem(slot, GuiItemFactory.filler(virtualKeysSettings.fillerMaterial));
        }
        List<Integer> slots = virtualKeysSettings.entrySlots;
        List<CrateDefinition> crates = crateRegistry.list().stream()
                .filter(crate -> crate.keys().enabled && crate.keys().virtualKeys)
                .toList();
        for (int index = 0; index < slots.size() && index < crates.size(); index++) {
            CrateDefinition crate = crates.get(index);
            int slot = slots.get(index);
            entrySlots.put(slot, crate.id());
            getInventory().setItem(slot, entryItem(player, crate));
        }
        getInventory().setItem(virtualKeysSettings.backSlot, GuiItemFactory.cancelButton(messageService, player));
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (click.slot() == virtualKeysSettings.backSlot) {
            player.closeInventory();
            if (backAction != null) {
                backAction.run();
            }
        }
    }

    private ItemStack entryItem(Player player, CrateDefinition crate) {
        int virtual = keyService.virtualKeys(player.getUniqueId(), crate.id());
        int physical = keyService.countPhysicalKeys(player, crate.id());
        Material material = Material.matchMaterial(crate.keys().material);
        if (material == null || material.isAir()) {
            material = Material.TRIPWIRE_HOOK;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messageService.component(
                    player.getUniqueId(),
                    "virtual-keys-entry-name",
                    Placeholder.parsed("crate", crate.displayName())
            ));
            meta.lore(List.of(
                    messageService.component(
                            player.getUniqueId(),
                            "virtual-keys-entry-lore",
                            messageService.placeholder("virtual", Integer.toString(virtual)),
                            messageService.placeholder("physical", Integer.toString(physical))
                    )
            ));
            if (crate.keys().customModelData >= 0) {
                meta.setCustomModelData(crate.keys().customModelData);
            }
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
