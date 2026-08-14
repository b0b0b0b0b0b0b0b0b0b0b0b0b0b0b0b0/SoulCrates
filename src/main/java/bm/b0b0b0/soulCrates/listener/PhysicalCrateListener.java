package bm.b0b0b0.soulCrates.listener;

import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateInstance;
import bm.b0b0b0.soulCrates.service.CrateRegistry;
import bm.b0b0b0.soulCrates.service.physical.PhysicalCrateService;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class PhysicalCrateListener implements Listener {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final CrateRegistry crateRegistry;
    private final PhysicalCrateService physicalCrateService;

    public PhysicalCrateListener(
            JavaPlugin plugin,
            MessageService messageService,
            CrateRegistry crateRegistry,
            PhysicalCrateService physicalCrateService
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.crateRegistry = crateRegistry;
        this.physicalCrateService = physicalCrateService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!physicalCrateService.enabled()) {
            return;
        }
        ItemStack item = event.getItemInHand();
        UUID instanceId = physicalCrateService.readInstanceId(item);
        if (instanceId == null) {
            return;
        }
        String crateId = physicalCrateService.readCrateId(item);
        if (crateId == null || crateRegistry.find(crateId).isEmpty()) {
            event.setCancelled(true);
            messageService.send(event.getPlayer().getUniqueId(), "physical-crate-invalid-item");
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Location location = event.getBlockPlaced().getLocation();
        BlockData blockData = event.getBlockPlaced().getBlockData().clone();
        EquipmentSlot hand = event.getHand();
        physicalCrateService.tryPlace(instanceId, player.getUniqueId(), location, crateId).thenAccept(success ->
                PluginSchedulers.runAt(plugin, location, () -> {
                    if (!success) {
                        messageService.send(player.getUniqueId(), "physical-crate-place-denied");
                        return;
                    }
                    Block block = location.getBlock();
                    block.setBlockData(blockData);
                    physicalCrateService.writeBlockTag(block, instanceId, crateId);
                    physicalCrateService.consumeHandItem(player, item, hand);
                    messageService.send(
                            player.getUniqueId(),
                            "physical-crate-placed",
                            messageService.placeholder("crate", crateRegistry.find(crateId).orElseThrow().displayName())
                    );
                })
        );
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!physicalCrateService.enabled()) {
            return;
        }
        Block block = event.getBlock();
        Optional<CrateInstance> instanceOptional = resolveInstance(block);
        if (instanceOptional.isEmpty()) {
            return;
        }
        CrateInstance instance = instanceOptional.get();
        Player player = event.getPlayer();
        if (!physicalCrateService.canBreak(player, instance)) {
            event.setCancelled(true);
            messageService.send(player.getUniqueId(), "physical-crate-break-denied");
            return;
        }
        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);
        Location location = block.getLocation();
        physicalCrateService.tryUnplace(instance.instanceId(), player.getUniqueId(), location).thenAccept(success ->
                PluginSchedulers.runAt(plugin, location, () -> {
                    if (!success) {
                        messageService.send(player.getUniqueId(), "physical-crate-break-denied");
                        return;
                    }
                    physicalCrateService.clearBlockTag(block);
                    block.setType(Material.AIR);
                    if (physicalCrateService.settings().returnItemOnBreak) {
                        crateRegistry.find(instance.crateId()).ifPresent(crate -> {
                            ItemStack restored = physicalCrateService.createItem(crate, instance.instanceId(), instance.ownerId());
                            var overflow = player.getInventory().addItem(restored);
                            for (ItemStack leftover : overflow.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                            }
                        });
                    }
                    messageService.send(player.getUniqueId(), "physical-crate-picked-up");
                })
        );
    }

    private Optional<CrateInstance> resolveInstance(Block block) {
        UUID blockId = physicalCrateService.readBlockInstanceId(block);
        if (blockId != null) {
            Optional<CrateInstance> cached = physicalCrateService.findCached(blockId);
            if (cached.isPresent()) {
                return cached;
            }
        }
        return physicalCrateService.findAt(block.getLocation());
    }
}
