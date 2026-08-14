package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiRewardGridSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class PagedRewardGuiRenderer {

    public record PageView(int page, int totalPages, int pageStartIndex, int slotsPerPage) {
    }

    private PagedRewardGuiRenderer() {
    }

    public static PageView normalizePage(int page, int itemCount, int slotsPerPage) {
        int safeSlots = Math.max(1, slotsPerPage);
        int totalPages = Math.max(1, (itemCount + safeSlots - 1) / safeSlots);
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        return new PageView(clampedPage, totalPages, clampedPage * safeSlots, safeSlots);
    }

    public static void applyGrid(
            Inventory inventory,
            GuiRewardGridSettings grid,
            MessageService messages,
            Player player,
            PageView pageView
    ) {
        ItemStack border = GuiItemFactory.filler(grid.borderFillerMaterial);
        ItemStack content = GuiItemFactory.filler(grid.contentFillerMaterial);
        for (int slot : grid.borderSlots) {
            setIfInBounds(inventory, slot, border);
        }
        for (int slot : grid.rewardSlots) {
            setIfInBounds(inventory, slot, content);
        }
        boolean showPrevious = grid.paginationEnabled && pageView.page() > 0;
        boolean showNext = grid.paginationEnabled && pageView.page() < pageView.totalPages() - 1;
        if (grid.previousPageSlot >= 0) {
            if (showPrevious) {
                inventory.setItem(
                        grid.previousPageSlot,
                        GuiItemFactory.pageButton(
                                messages,
                                player,
                                grid.previousPageMaterial,
                                true,
                                pageView.page() + 1,
                                pageView.totalPages()
                        )
                );
            } else {
                setIfInBounds(inventory, grid.previousPageSlot, border);
            }
        }
        if (grid.nextPageSlot >= 0) {
            if (showNext) {
                inventory.setItem(
                        grid.nextPageSlot,
                        GuiItemFactory.pageButton(
                                messages,
                                player,
                                grid.nextPageMaterial,
                                false,
                                pageView.page() + 1,
                                pageView.totalPages()
                        )
                );
            } else {
                setIfInBounds(inventory, grid.nextPageSlot, border);
            }
        }
    }

    public static boolean isPreviousPageSlot(GuiRewardGridSettings grid, int slot) {
        return grid.paginationEnabled && grid.previousPageSlot >= 0 && slot == grid.previousPageSlot;
    }

    public static boolean isNextPageSlot(GuiRewardGridSettings grid, int slot) {
        return grid.paginationEnabled && grid.nextPageSlot >= 0 && slot == grid.nextPageSlot;
    }

    public static int rewardSlotIndex(GuiRewardGridSettings grid, int slot) {
        return grid.rewardSlots.indexOf(slot);
    }

    private static void setIfInBounds(Inventory inventory, int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }
}
