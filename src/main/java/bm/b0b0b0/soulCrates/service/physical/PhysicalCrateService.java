package bm.b0b0b0.soulCrates.service.physical;

import bm.b0b0b0.soulCrates.config.settings.PhysicalCrateSettings;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.CrateInstance;
import bm.b0b0b0.soulCrates.model.CrateInstanceState;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import bm.b0b0b0.soulCrates.util.SoulCratesKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class PhysicalCrateService {

    private final Plugin plugin;
    private final MessageService messageService;
    private final CrateRepository repository;
    private final ConcurrentHashMap<UUID, CrateInstance> cacheById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> cacheByLocation = new ConcurrentHashMap<>();
    private PhysicalCrateSettings settings = new PhysicalCrateSettings();

    public PhysicalCrateService(Plugin plugin, MessageService messageService, CrateRepository repository) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.repository = repository;
    }

    public void applySettings(PhysicalCrateSettings settings) {
        this.settings = settings == null ? new PhysicalCrateSettings() : settings;
    }

    public void loadCache() {
        repository.loadPlacedInstances().thenAccept(instances -> {
            cacheById.clear();
            cacheByLocation.clear();
            for (CrateInstance instance : instances) {
                remember(instance);
            }
        });
    }

    public void clearCache() {
        cacheById.clear();
        cacheByLocation.clear();
    }

    public boolean enabled() {
        return settings.enabled;
    }

    public PhysicalCrateSettings settings() {
        return settings;
    }

    public boolean isPhysicalCrateItem(ItemStack stack) {
        return readInstanceId(stack) != null;
    }

    public UUID readInstanceId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = meta.getPersistentDataContainer().get(SoulCratesKeys.crateInstanceId(plugin), PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public String readCrateId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(SoulCratesKeys.crateBlock(plugin), PersistentDataType.STRING);
    }

    public Optional<CrateInstance> findCached(UUID instanceId) {
        return Optional.ofNullable(cacheById.get(instanceId));
    }

    public Optional<CrateInstance> findAt(Location location) {
        String key = locationKey(location);
        if (key == null) {
            return Optional.empty();
        }
        UUID instanceId = cacheByLocation.get(key);
        if (instanceId == null) {
            return Optional.empty();
        }
        CrateInstance instance = cacheById.get(instanceId);
        if (instance == null || instance.state() == CrateInstanceState.CONSUMED || instance.state() == CrateInstanceState.UNPLACED) {
            return Optional.empty();
        }
        return Optional.of(instance);
    }

    public UUID readBlockInstanceId(Block block) {
        if (!(block.getState() instanceof TileState tileState)) {
            return null;
        }
        String raw = tileState.getPersistentDataContainer().get(SoulCratesKeys.crateInstanceId(plugin), PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public void writeBlockTag(Block block, UUID instanceId, String crateId) {
        if (!(block.getState() instanceof TileState tileState)) {
            return;
        }
        tileState.getPersistentDataContainer().set(SoulCratesKeys.crateInstanceId(plugin), PersistentDataType.STRING, instanceId.toString());
        tileState.getPersistentDataContainer().set(SoulCratesKeys.crateBlock(plugin), PersistentDataType.STRING, crateId.toLowerCase());
        tileState.update(true, false);
    }

    public void clearBlockTag(Block block) {
        if (!(block.getState() instanceof TileState tileState)) {
            return;
        }
        tileState.getPersistentDataContainer().remove(SoulCratesKeys.crateInstanceId(plugin));
        tileState.getPersistentDataContainer().remove(SoulCratesKeys.crateBlock(plugin));
        tileState.update(true, false);
    }

    public ItemStack createItem(CrateDefinition crate, UUID instanceId, UUID ownerId) {
        Material material = resolveMaterial(crate);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            CrateInstance preview = new CrateInstance(
                    instanceId,
                    crate.id(),
                    ownerId,
                    CrateInstanceState.UNPLACED,
                    null,
                    0,
                    0,
                    0,
                    System.currentTimeMillis()
            );
            meta.displayName(messageService.component(
                    ownerId,
                    "physical-crate-item-name",
                    messageService.placeholder("crate", crate.displayName())
            ));
            meta.lore(buildItemLore(ownerId, crate, preview));
            int modelData = crate.lootBox().customModelData >= 0 ? crate.lootBox().customModelData : settings.customModelData;
            if (modelData >= 0) {
                meta.setCustomModelData(modelData);
            }
            meta.getPersistentDataContainer().set(SoulCratesKeys.crateInstanceId(plugin), PersistentDataType.STRING, instanceId.toString());
            meta.getPersistentDataContainer().set(SoulCratesKeys.crateBlock(plugin), PersistentDataType.STRING, crate.id().toLowerCase());
            item.setItemMeta(meta);
        }
        return item;
    }

    public void giveItems(Player target, CrateDefinition crate, List<UUID> instanceIds) {
        List<ItemStack> items = new ArrayList<>(instanceIds.size());
        for (UUID instanceId : instanceIds) {
            items.add(createItem(crate, instanceId, target.getUniqueId()));
        }
        for (ItemStack item : items) {
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(item);
            for (ItemStack leftover : overflow.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), leftover);
            }
        }
    }

    public void remember(CrateInstance instance) {
        cacheById.put(instance.instanceId(), instance);
        String key = locationKey(instance.world(), instance.x(), instance.y(), instance.z());
        if (key != null && (instance.state() == CrateInstanceState.PLACED || instance.state() == CrateInstanceState.OPENING)) {
            cacheByLocation.put(key, instance.instanceId());
        }
    }

    public void forgetLocation(CrateInstance instance) {
        String key = locationKey(instance.world(), instance.x(), instance.y(), instance.z());
        if (key != null) {
            cacheByLocation.remove(key, instance.instanceId());
        }
    }

    public void updateCache(CrateInstance instance) {
        CrateInstance previous = cacheById.put(instance.instanceId(), instance);
        if (previous != null) {
            forgetLocation(previous);
        }
        remember(instance);
    }

    public void removeFromCache(UUID instanceId) {
        CrateInstance previous = cacheById.remove(instanceId);
        if (previous != null) {
            forgetLocation(previous);
        }
    }

    public java.util.concurrent.CompletableFuture<Boolean> tryPlace(UUID instanceId, UUID ownerId, Location location, String crateId) {
        return repository.tryPlaceInstance(instanceId, ownerId, location).thenApply(success -> {
            if (!success) {
                return false;
            }
            CrateInstance placed = new CrateInstance(
                    instanceId,
                    crateId.toLowerCase(),
                    ownerId,
                    CrateInstanceState.PLACED,
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    System.currentTimeMillis()
            );
            updateCache(placed);
            return true;
        });
    }

    public java.util.concurrent.CompletableFuture<Boolean> tryBeginOpen(UUID instanceId, UUID playerId) {
        return repository.tryBeginInstanceOpen(instanceId, playerId).thenApply(success -> {
            if (!success) {
                return false;
            }
            CrateInstance current = cacheById.get(instanceId);
            if (current != null) {
                updateCache(new CrateInstance(
                        current.instanceId(),
                        current.crateId(),
                        current.ownerId(),
                        CrateInstanceState.OPENING,
                        current.world(),
                        current.x(),
                        current.y(),
                        current.z(),
                        current.createdAt()
                ));
            }
            return true;
        });
    }

    public java.util.concurrent.CompletableFuture<Boolean> tryFinishOpen(UUID instanceId) {
        return repository.tryFinishInstanceOpen(instanceId).thenApply(success -> {
            if (success) {
                removeFromCache(instanceId);
            }
            return success;
        });
    }

    public java.util.concurrent.CompletableFuture<Boolean> tryCancelOpen(UUID instanceId) {
        return repository.tryCancelInstanceOpen(instanceId).thenApply(success -> {
            if (!success) {
                return false;
            }
            CrateInstance current = cacheById.get(instanceId);
            if (current != null) {
                updateCache(new CrateInstance(
                        current.instanceId(),
                        current.crateId(),
                        current.ownerId(),
                        CrateInstanceState.PLACED,
                        current.world(),
                        current.x(),
                        current.y(),
                        current.z(),
                        current.createdAt()
                ));
            }
            return true;
        });
    }

    public java.util.concurrent.CompletableFuture<Boolean> tryUnplace(UUID instanceId, UUID ownerId, Location location) {
        return repository.tryUnplaceInstance(instanceId, ownerId, location).thenApply(success -> {
            if (!success) {
                return false;
            }
            CrateInstance current = cacheById.get(instanceId);
            if (current != null) {
                forgetLocation(current);
                cacheById.put(instanceId, new CrateInstance(
                        current.instanceId(),
                        current.crateId(),
                        current.ownerId(),
                        CrateInstanceState.UNPLACED,
                        null,
                        0,
                        0,
                        0,
                        current.createdAt()
                ));
            }
            return true;
        });
    }

    public java.util.concurrent.CompletableFuture<List<UUID>> registerInstances(String crateId, UUID ownerId, int amount) {
        long now = System.currentTimeMillis();
        List<UUID> ids = new ArrayList<>(amount);
        List<java.util.concurrent.CompletableFuture<Void>> tasks = new ArrayList<>(amount);
        for (int index = 0; index < amount; index++) {
            UUID instanceId = UUID.randomUUID();
            ids.add(instanceId);
            tasks.add(repository.createInstance(instanceId, crateId, ownerId, now));
        }
        return java.util.concurrent.CompletableFuture.allOf(tasks.toArray(java.util.concurrent.CompletableFuture[]::new))
                .thenApply(ignored -> ids);
    }

    public boolean canOpen(Player player, CrateInstance instance) {
        if (instance.state() != CrateInstanceState.PLACED) {
            return false;
        }
        if (settings.ownerOnlyOpen && !player.getUniqueId().equals(instance.ownerId())) {
            return false;
        }
        return true;
    }

    public boolean canBreak(Player player, CrateInstance instance) {
        if (instance.state() != CrateInstanceState.PLACED && instance.state() != CrateInstanceState.OPENING) {
            return false;
        }
        if (settings.ownerOnlyBreak && !player.getUniqueId().equals(instance.ownerId())) {
            return false;
        }
        return true;
    }

    public void consumeHandItem(Player player, ItemStack template, EquipmentSlot hand) {
        ItemStack item = hand == EquipmentSlot.OFF_HAND ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();
        if (item == null || item.isEmpty()) {
            return;
        }
        if (item.getAmount() <= 1) {
            if (hand == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            return;
        }
        item.setAmount(item.getAmount() - 1);
    }

    public void removePlacedBlock(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        Block block = location.getBlock();
        clearBlockTag(block);
        block.setType(Material.AIR);
    }

    public boolean hasInstanceItem(Player player, UUID instanceId) {
        if (player == null || instanceId == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            UUID found = readInstanceId(stack);
            if (instanceId.equals(found)) {
                return true;
            }
        }
        return false;
    }

    public void returnPlacedCrate(Player player, CrateDefinition crate, CrateInstance instance, Location location) {
        if (player == null || crate == null || instance == null || location == null || location.getWorld() == null) {
            return;
        }
        tryUnplace(instance.instanceId(), instance.ownerId(), location).thenAccept(success ->
                PluginSchedulers.run(plugin, player, () -> {
                    if (!success) {
                        return;
                    }
                    PluginSchedulers.runAt(plugin, location, () -> removePlacedBlock(location));
                    if (!hasInstanceItem(player, instance.instanceId())) {
                        ItemStack restored = createItem(crate, instance.instanceId(), instance.ownerId());
                        Map<Integer, ItemStack> overflow = player.getInventory().addItem(restored);
                        for (ItemStack leftover : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                        }
                    }
                    messageService.send(
                            player.getUniqueId(),
                            "physical-crate-returned",
                            messageService.placeholder("crate", crate.displayName())
                    );
                })
        );
    }

    private List<net.kyori.adventure.text.Component> buildItemLore(UUID ownerId, CrateDefinition crate, CrateInstance instance) {
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(messageService.component(ownerId, "physical-crate-item-lore-line1"));
        lore.add(messageService.component(ownerId, "physical-crate-item-lore-line2"));
        lore.add(messageService.component(ownerId, "physical-crate-item-lore-line3"));
        lore.add(messageService.component(
                ownerId,
                "physical-crate-item-lore-serial",
                messageService.placeholder("serial", instance.serial())
        ));
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(messageService.component(
                ownerId,
                "physical-crate-item-lore-preset",
                messageService.placeholder("preset", crate.animations().preset == null ? "SHOWCASE" : crate.animations().preset)
        ));
        return lore;
    }

    private Material resolveMaterial(CrateDefinition crate) {
        Material fromCrate = Material.matchMaterial(crate.lootBox().material);
        if (fromCrate != null && fromCrate.isBlock() && !fromCrate.isAir()) {
            return fromCrate;
        }
        Material configured = Material.matchMaterial(settings.material);
        if (configured != null && configured.isBlock() && !configured.isAir()) {
            return configured;
        }
        return Material.CHEST;
    }

    public static String locationKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return locationKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static String locationKey(String world, int x, int y, int z) {
        if (world == null) {
            return null;
        }
        return world + ":" + x + ":" + y + ":" + z;
    }
}
