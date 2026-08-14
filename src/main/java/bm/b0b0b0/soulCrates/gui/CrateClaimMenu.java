package bm.b0b0b0.soulCrates.gui;

import bm.b0b0b0.soulCrates.config.settings.GuiClaimSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.PendingClaim;
import bm.b0b0b0.soulCrates.service.claim.ClaimService;
import bm.b0b0b0.soulCrates.service.reward.RewardDisplayService;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.ArrayList;
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
import org.bukkit.plugin.java.JavaPlugin;

public final class CrateClaimMenu extends SoulMenu {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final GuiClaimSettings claimSettings;
    private final ClaimService claimService;
    private List<PendingClaim> claims;
    private final Map<Integer, PendingClaim> slotClaims = new HashMap<>();

    public CrateClaimMenu(
            JavaPlugin plugin,
            UUID viewerId,
            MessageService messageService,
            GuiClaimSettings claimSettings,
            ClaimService claimService,
            List<PendingClaim> claims
    ) {
        super(
                viewerId,
                normalizeSize(claimSettings.size),
                messageService.component(viewerId, "claim-title")
        );
        this.plugin = plugin;
        this.messageService = messageService;
        this.claimSettings = claimSettings;
        this.claimService = claimService;
        this.claims = new ArrayList<>(claims);
        refresh();
    }

    @Override
    public void refresh() {
        getInventory().clear();
        slotClaims.clear();
        Player player = Bukkit.getPlayer(viewerId());
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < getInventory().getSize(); slot++) {
            getInventory().setItem(slot, GuiItemFactory.filler(claimSettings.fillerMaterial));
        }
        List<Integer> rewardSlots = claimSettings.rewardSlots;
        for (int index = 0; index < rewardSlots.size() && index < claims.size(); index++) {
            PendingClaim claim = claims.get(index);
            if (claim.reward() == null) {
                continue;
            }
            int slot = rewardSlots.get(index);
            slotClaims.put(slot, claim);
            getInventory().setItem(slot, claimIcon(player, claim));
        }
        getInventory().setItem(
                claimSettings.claimAllSlot,
                GuiItemFactory.actionButton(messageService, player, "claim-all-title", "claim-all-lore")
        );
        getInventory().setItem(claimSettings.closeSlot, GuiItemFactory.cancelButton(messageService, player));
    }

    @Override
    public void handleClick(SoulMenuClick click) {
        if (!click.clickedTop()) {
            return;
        }
        Player player = click.player();
        if (click.slot() == claimSettings.claimAllSlot) {
            claimService.claimAll(player).thenAccept(count -> PluginSchedulers.run(plugin, player, () -> {
                if (count <= 0) {
                    messageService.send(player.getUniqueId(), "claim-empty");
                } else {
                    messageService.send(
                            player.getUniqueId(),
                            "claim-all-success",
                            messageService.placeholder("amount", Integer.toString(count))
                    );
                }
                player.closeInventory();
            }));
            return;
        }
        if (click.slot() == claimSettings.closeSlot) {
            player.closeInventory();
            return;
        }
        PendingClaim claim = slotClaims.get(click.slot());
        if (claim == null) {
            return;
        }
        claimService.claimOne(player, claim).thenAccept(success -> PluginSchedulers.run(plugin, player, () -> {
            if (success) {
                messageService.send(
                        player.getUniqueId(),
                        "claim-success",
                        Placeholder.component(
                                "reward",
                                RewardDisplayService.displayName(
                                        messageService,
                                        player.getUniqueId(),
                                        claim.crateId(),
                                        claim.reward()
                                )
                        )
                );
            }
            claimService.loadPending(player.getUniqueId()).thenAccept(updated -> PluginSchedulers.run(plugin, player, () -> {
                if (updated.isEmpty()) {
                    player.closeInventory();
                    return;
                }
                claims = updated;
                refresh();
            }));
        }));
    }

    private ItemStack claimIcon(Player player, PendingClaim claim) {
        Material material = Material.matchMaterial(claim.reward().material());
        if (material == null || material.isAir()) {
            material = Material.CHEST;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messageService.component(
                    player.getUniqueId(),
                    "claim-entry-name",
                    Placeholder.component(
                            "reward",
                            RewardDisplayService.displayName(
                                    messageService,
                                    player.getUniqueId(),
                                    claim.crateId(),
                                    claim.reward()
                            )
                    )
            ));
            meta.lore(List.of(messageService.component(
                    player.getUniqueId(),
                    "claim-entry-lore",
                    Placeholder.parsed("crate", claim.crateId())
            )));
            if (claim.reward().customModelData() >= 0) {
                meta.setCustomModelData(claim.reward().customModelData());
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
