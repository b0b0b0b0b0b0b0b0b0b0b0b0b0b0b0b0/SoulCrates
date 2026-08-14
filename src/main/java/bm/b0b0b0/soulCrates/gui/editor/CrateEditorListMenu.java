package bm.b0b0b0.soulCrates.gui.editor;

import bm.b0b0b0.soulCrates.config.settings.GuiEditorSettings;
import bm.b0b0b0.soulCrates.gui.GuiItemFactory;
import bm.b0b0b0.soulCrates.gui.SoulMenu;
import bm.b0b0b0.soulCrates.gui.SoulMenuClick;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

public final class CrateEditorListMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiEditorSettings editorSettings;
    private final List<CrateDefinition> crates;
    private final BiConsumer<Player, CrateDefinition> editAction;
    private final Consumer<Player> reloadAction;

    public CrateEditorListMenu(
            UUID viewerId,
            MessageService messageService,
            GuiEditorSettings editorSettings,
            List<CrateDefinition> crates,
            BiConsumer<Player, CrateDefinition> editAction,
            Consumer<Player> reloadAction
    ) {
        super(viewerId, normalizeSize(editorSettings.size), messageService.component(viewerId, "editor-list-title"));
        this.messageService = messageService;
        this.editorSettings = editorSettings;
        this.crates = crates;
        this.editAction = editAction;
        this.reloadAction = reloadAction;
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
            getInventory().setItem(slot, GuiItemFactory.filler(editorSettings.fillerMaterial));
        }
        List<Integer> slots = editorSettings.crateSlots;
        for (int index = 0; index < slots.size() && index < crates.size(); index++) {
            CrateDefinition crate = crates.get(index);
            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(crate.displayName()));
                meta.lore(List.of(
                        Component.text("ID: " + crate.id()),
                        Component.text("Rewards: " + crate.rewards().size()),
                        Component.text("Engine: " + crate.engineKind().name())
                ));
                item.setItemMeta(meta);
            }
            getInventory().setItem(slots.get(index), item);
        }
        getInventory().setItem(editorSettings.reloadSlot, GuiItemFactory.actionButton(messageService, player, "editor-reload-title", "editor-reload-lore"));
        getInventory().setItem(editorSettings.backSlot, GuiItemFactory.cancelButton(messageService, player));
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (click.slot() == editorSettings.reloadSlot) {
            reloadAction.accept(player);
            refresh();
            return;
        }
        if (click.slot() == editorSettings.backSlot) {
            player.closeInventory();
            return;
        }
        List<Integer> slots = editorSettings.crateSlots;
        for (int index = 0; index < slots.size() && index < crates.size(); index++) {
            if (click.slot() == slots.get(index)) {
                editAction.accept(player, crates.get(index));
                return;
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
