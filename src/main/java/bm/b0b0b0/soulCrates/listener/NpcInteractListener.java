package bm.b0b0b0.soulCrates.listener;

import bm.b0b0b0.soulCrates.hook.citizens.CitizensBridge;
import bm.b0b0b0.soulCrates.service.CrateService;
import bm.b0b0b0.soulCrates.service.npc.CrateNpcService;
import java.util.Optional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class NpcInteractListener implements Listener {

    private final CrateService crateService;
    private final CrateNpcService npcService;

    public NpcInteractListener(CrateService crateService, CrateNpcService npcService) {
        this.crateService = crateService;
        this.npcService = npcService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!CitizensBridge.isAvailable()) {
            return;
        }
        Entity clicked = event.getRightClicked();
        Optional<Integer> npcIdOptional = CitizensBridge.npcId(clicked);
        if (npcIdOptional.isEmpty()) {
            return;
        }
        Optional<String> crateIdOptional = npcService.findCrateId(npcIdOptional.get());
        if (crateIdOptional.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        String crateId = crateIdOptional.get();
        if (player.isSneaking()) {
            crateService.openPreview(player, crateId);
            return;
        }
        crateService.beginOpen(player, crateId, player.getLocation());
    }
}
