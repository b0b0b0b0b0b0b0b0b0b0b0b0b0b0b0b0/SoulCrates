package bm.b0b0b0.soulCrates.hook.citizens;

import java.lang.reflect.Method;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class CitizensBridge {

    private static volatile boolean available;

    private CitizensBridge() {
    }

    public static boolean isAvailable() {
        if (available) {
            return true;
        }
        Plugin citizens = Bukkit.getPluginManager().getPlugin("Citizens");
        if (citizens == null || !citizens.isEnabled()) {
            return false;
        }
        try {
            Class.forName("net.citizensnpcs.api.CitizensAPI", true, citizens.getClass().getClassLoader());
            available = true;
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public static Optional<Integer> npcId(Entity entity) {
        if (entity == null || !isAvailable()) {
            return Optional.empty();
        }
        try {
            Plugin citizens = Bukkit.getPluginManager().getPlugin("Citizens");
            Class<?> apiClass = Class.forName("net.citizensnpcs.api.CitizensAPI", true, citizens.getClass().getClassLoader());
            Method registryMethod = apiClass.getMethod("getNPCRegistry");
            Object registry = registryMethod.invoke(null);
            Method getNpcMethod = registry.getClass().getMethod("getNPC", Entity.class);
            Object npc = getNpcMethod.invoke(registry, entity);
            if (npc == null) {
                return Optional.empty();
            }
            Method getIdMethod = npc.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(npc);
            if (id instanceof Integer npcId) {
                return Optional.of(npcId);
            }
            return Optional.empty();
        } catch (ReflectiveOperationException exception) {
            return Optional.empty();
        }
    }
}
