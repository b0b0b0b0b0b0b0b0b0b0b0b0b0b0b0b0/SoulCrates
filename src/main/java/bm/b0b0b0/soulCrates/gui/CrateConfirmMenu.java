package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiConfirmSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CrateConfirmMenu extends SoulMenu {

    private final MessageService messageService;
    private final GuiConfirmSettings confirmSettings;
    private final Consumer<Player> confirmAction;
    private final Runnable cancelAction;

    public CrateConfirmMenu(
            UUID viewerId,
            MessageService messageService,
            GuiConfirmSettings confirmSettings,
            CrateDefinition crateDefinition,
            Consumer<Player> confirmAction,
            Runnable cancelAction
    ) {
        super(viewerId, normalizeSize(confirmSettings.size), messageService.component(viewerId, "confirm-title"));
        this.messageService = messageService;
        this.confirmSettings = confirmSettings;
        this.confirmAction = confirmAction;
        this.cancelAction = cancelAction;
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
            getInventory().setItem(slot, GuiItemFactory.filler(confirmSettings.fillerMaterial));
        }
        getInventory().setItem(confirmSettings.confirmSlot, GuiItemFactory.actionButton(messageService, player, "confirm-open-title", "confirm-open-lore"));
        getInventory().setItem(confirmSettings.cancelSlot, GuiItemFactory.cancelButton(messageService, player));
        ItemStack preview = new ItemStack(Material.ENDER_CHEST);
        getInventory().setItem(confirmSettings.previewSlot, preview);
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (click.slot() == confirmSettings.confirmSlot) {
            confirmAction.accept(player);
            return;
        }
        if (click.slot() == confirmSettings.cancelSlot) {
            player.closeInventory();
            if (cancelAction != null) {
                cancelAction.run();
            }
        }
    }

    private static int normalizeSize(int size) {
        if (size < 9 || size % 9 != 0) {
            return 27;
        }
        return size;
    }
}
