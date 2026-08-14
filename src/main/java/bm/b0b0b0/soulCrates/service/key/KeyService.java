package bm.b0b0b0.soulCrates.service.key;

import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.repository.CrateRepository;
import bm.b0b0b0.soulCrates.util.SoulCratesKeys;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class KeyService {

    private final Plugin plugin;
    private final MessageService messageService;
    private final CrateRepository repository;
    private final Map<UUID, Map<String, Integer>> virtualCache = new ConcurrentHashMap<>();

    public KeyService(Plugin plugin, MessageService messageService, CrateRepository repository) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.repository = repository;
    }

    public void preload(UUID playerId) {
        virtualCache.remove(playerId);
    }

    public CompletableFuture<Void> preloadPlayer(UUID playerId, Collection<String> crateIds) {
        if (crateIds == null || crateIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<?>[] futures = crateIds.stream()
                .map(crateId -> loadVirtualKeys(playerId, crateId))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    public void unloadPlayer(UUID playerId) {
        virtualCache.remove(playerId);
    }

    public int virtualKeys(UUID playerId, String crateId) {
        Map<String, Integer> keys = virtualCache.get(playerId);
        if (keys == null) {
            return 0;
        }
        return keys.getOrDefault(crateId.toLowerCase(Locale.ROOT), 0);
    }

    public CompletableFuture<Integer> loadVirtualKeys(UUID playerId, String crateId) {
        return repository.loadVirtualKeys(playerId, crateId).thenApply(amount -> {
            virtualCache.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                    .put(crateId.toLowerCase(Locale.ROOT), amount);
            return amount;
        });
    }

    public CompletableFuture<Void> giveVirtualKeys(UUID playerId, String crateId, int amount) {
        int next = Math.max(0, virtualKeys(playerId, crateId) + amount);
        virtualCache.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(crateId.toLowerCase(Locale.ROOT), next);
        return repository.saveVirtualKeys(playerId, crateId, next);
    }

    public int countPhysicalKeys(Player player, String crateId) {
        int total = 0;
        String normalized = crateId.toLowerCase(Locale.ROOT);
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String keyCrate = readKeyCrate(stack);
            if (normalized.equals(keyCrate)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public boolean consumeForOpen(Player player, CrateDefinition crate, int required) {
        if (!crate.keys().enabled && !crate.opening().requireKey) {
            return true;
        }
        if (!crate.opening().requireKey) {
            return true;
        }
        int remaining = Math.max(1, required);
        if (crate.keys().virtualKeys) {
            int virtual = virtualKeys(player.getUniqueId(), crate.id());
            if (virtual >= remaining) {
                int left = virtual - remaining;
                repository.saveVirtualKeys(player.getUniqueId(), crate.id(), left);
                virtualCache.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                        .put(crate.id(), left);
                return true;
            }
            remaining -= virtual;
            if (virtual > 0) {
                repository.saveVirtualKeys(player.getUniqueId(), crate.id(), 0);
                virtualCache.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                        .put(crate.id(), 0);
            }
        }
        if (remaining <= 0) {
            return true;
        }
        if (!crate.keys().physicalKeys) {
            return false;
        }
        return consumePhysical(player, crate.id(), remaining);
    }

    public ItemStack createKeyItem(CrateDefinition crate, int amount) {
        Material material = Material.matchMaterial(crate.keys().material);
        if (material == null || material.isAir()) {
            material = Material.TRIPWIRE_HOOK;
        }
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messageService.component(null, "key-item-name", messageService.placeholder("crate", crate.displayName())));
            meta.lore(java.util.List.of(messageService.component(null, "key-item-lore", messageService.placeholder("crate", crate.id()))));
            if (crate.keys().customModelData >= 0) {
                meta.setCustomModelData(crate.keys().customModelData);
            }
            meta.getPersistentDataContainer().set(SoulCratesKeys.keyType(plugin), PersistentDataType.STRING, crate.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    public void givePhysicalKey(Player player, CrateDefinition crate, int amount) {
        ItemStack key = createKeyItem(crate, amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(key);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private boolean consumePhysical(Player player, String crateId, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (!crateId.equalsIgnoreCase(readKeyCrate(stack))) {
                continue;
            }
            int remove = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - remove);
            if (stack.getAmount() <= 0) {
                contents[slot] = null;
            }
            remaining -= remove;
        }
        player.getInventory().setContents(contents);
        return remaining <= 0;
    }

    private String readKeyCrate(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(SoulCratesKeys.keyType(plugin), PersistentDataType.STRING);
    }

    public void clearCache() {
        virtualCache.clear();
    }
}
