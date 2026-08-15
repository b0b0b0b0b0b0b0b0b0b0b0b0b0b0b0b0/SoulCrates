package bm.b0b0b0.soulCrates.hook.worldguard;

import bm.b0b0b0.soulCrates.config.settings.WorldGuardPhysicalCrateSettings;
import bm.b0b0b0.soulCrates.hook.PluginHook;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldGuardHook implements PluginHook {

    private static final String WORLDGUARD_PLUGIN = "WorldGuard";
    private static final String WORLDGUARD_CLASS = "com.sk89q.worldguard.WorldGuard";
    private static final String BUKKIT_ADAPTER_CLASS = "com.sk89q.worldedit.bukkit.BukkitAdapter";
    private static final String USE_BLOCK_EVENT = "com.sk89q.worldguard.bukkit.event.block.UseBlockEvent";
    private static final String USE_ENTITY_EVENT = "com.sk89q.worldguard.bukkit.event.entity.UseEntityEvent";
    private static final String DAMAGE_ENTITY_EVENT = "com.sk89q.worldguard.bukkit.event.entity.DamageEntityEvent";
    private static final String USE_ITEM_EVENT = "com.sk89q.worldguard.bukkit.event.inventory.UseItemEvent";

    private final Logger logger;
    private final Listener bypassListenerToken = new Listener() {
    };

    private Object regionContainer;
    private Method createQueryMethod;
    private Method getApplicableRegionsMethod;
    private Method adaptBukkitLocationMethod;
    private Method regionGetIdMethod;
    private Method delegateGetResultMethod;
    private Method delegateSetResultMethod;
    private Method delegateSetSilentMethod;
    private Method delegateGetCauseMethod;
    private Method delegateGetEntityMethod;
    private Method delegateGetTargetMethod;
    private Method delegateGetBlocksMethod;
    private Class<?> useBlockEventClass;
    private Method causeGetFirstPlayerMethod;
    private Object eventResultAllow;
    private boolean enabled;
    private boolean initFailed;
    private final List<HandlerRegistration> bypassRegistrations = new ArrayList<>();

    private static final class HandlerRegistration {
        private final HandlerList handlerList;
        private final RegisteredListener registration;

        private HandlerRegistration(HandlerList handlerList, RegisteredListener registration) {
            this.handlerList = handlerList;
            this.registration = registration;
        }
    }

    public WorldGuardHook(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
    }

    @Override
    public String id() {
        return "worldguard";
    }

    @Override
    public boolean enabled() {
        return enabled && regionContainer != null;
    }

    @Override
    public void load(JavaPlugin plugin) {
        enabled = false;
        initFailed = false;
        regionContainer = null;
        unregisterBypassListeners();
        ensureInitialized(plugin);
    }

    @Override
    public void unload() {
        unregisterBypassListeners();
        enabled = false;
        initFailed = false;
        regionContainer = null;
        delegateGetResultMethod = null;
        delegateSetResultMethod = null;
        delegateSetSilentMethod = null;
        delegateGetCauseMethod = null;
        delegateGetEntityMethod = null;
        delegateGetTargetMethod = null;
        delegateGetBlocksMethod = null;
        useBlockEventClass = null;
        causeGetFirstPlayerMethod = null;
        eventResultAllow = null;
    }

    public void ensureInitialized(JavaPlugin plugin) {
        if (enabled()) {
            return;
        }
        if (initFailed) {
            return;
        }
        Plugin worldGuard = plugin.getServer().getPluginManager().getPlugin(WORLDGUARD_PLUGIN);
        if (worldGuard == null || !worldGuard.isEnabled()) {
            return;
        }
        initialize(plugin, worldGuard);
    }

    public void registerBypassListener(JavaPlugin plugin, WorldGuardBypassListener listener) {
        ensureInitialized(plugin);
        unregisterBypassListeners();
        if (!enabled() || listener == null) {
            return;
        }
        registerDelegateHandler(plugin, USE_BLOCK_EVENT, listener::handleLow, EventPriority.LOWEST);
        registerDelegateHandler(plugin, USE_ENTITY_EVENT, listener::handleLow, EventPriority.LOWEST);
        registerDelegateHandler(plugin, DAMAGE_ENTITY_EVENT, listener::handleLow, EventPriority.LOWEST);
        registerDelegateHandler(plugin, USE_ITEM_EVENT, listener::handleLow, EventPriority.LOWEST);
        registerDelegateHandler(plugin, USE_BLOCK_EVENT, listener::handleHigh, EventPriority.HIGH);
        registerDelegateHandler(plugin, USE_ENTITY_EVENT, listener::handleHigh, EventPriority.HIGH);
        registerDelegateHandler(plugin, DAMAGE_ENTITY_EVENT, listener::handleHigh, EventPriority.HIGH);
        registerDelegateHandler(plugin, USE_ITEM_EVENT, listener::handleHigh, EventPriority.HIGH);
        if (!bypassRegistrations.isEmpty()) {
            logger.info("WorldGuard deny bypass active (" + (bypassRegistrations.size() / 2) + " delegate events).");
        }
    }

    public boolean isDelegateAllowed(Object delegateEvent) {
        if (delegateEvent == null || delegateGetResultMethod == null || eventResultAllow == null) {
            return false;
        }
        try {
            Object result = delegateGetResultMethod.invoke(delegateEvent);
            return eventResultAllow.equals(result);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    public void allowDelegateEvent(Object delegateEvent) {
        if (delegateEvent == null) {
            return;
        }
        try {
            if (delegateSetResultMethod != null && eventResultAllow != null) {
                delegateSetResultMethod.invoke(delegateEvent, eventResultAllow);
            }
            if (delegateSetSilentMethod != null) {
                delegateSetSilentMethod.invoke(delegateEvent, true);
            }
        } catch (ReflectiveOperationException exception) {
            logger.warning("WorldGuard bypass failed: " + exception.getMessage());
        }
    }

    public Location resolveDelegateLocation(Object delegateEvent) {
        if (delegateEvent == null) {
            return null;
        }
        try {
            if (delegateGetTargetMethod != null) {
                Object target = delegateGetTargetMethod.invoke(delegateEvent);
                if (target instanceof Location location) {
                    return location;
                }
            }
            if (delegateGetEntityMethod != null) {
                Object entity = delegateGetEntityMethod.invoke(delegateEvent);
                if (entity instanceof Entity typed) {
                    return typed.getLocation();
                }
            }
            if (delegateGetBlocksMethod != null
                    && useBlockEventClass != null
                    && useBlockEventClass.isInstance(delegateEvent)) {
                Object blocks = delegateGetBlocksMethod.invoke(delegateEvent);
                if (blocks instanceof List<?> blockList && !blockList.isEmpty()) {
                    Object first = blockList.get(0);
                    if (first instanceof Block block) {
                        return block.getLocation();
                    }
                }
            }
            Player player = resolveDelegatePlayer(delegateEvent);
            if (player != null) {
                return player.getLocation();
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    public Player resolveDelegatePlayer(Object delegateEvent) {
        if (delegateEvent == null || delegateGetCauseMethod == null || causeGetFirstPlayerMethod == null) {
            return null;
        }
        try {
            Object cause = delegateGetCauseMethod.invoke(delegateEvent);
            if (cause == null) {
                return null;
            }
            Object player = causeGetFirstPlayerMethod.invoke(cause);
            if (player instanceof Player typed) {
                return typed;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    public Entity resolveDelegateEntity(Object delegateEvent) {
        if (delegateEvent == null || delegateGetEntityMethod == null) {
            return null;
        }
        try {
            Object entity = delegateGetEntityMethod.invoke(delegateEvent);
            if (entity instanceof Entity typed) {
                return typed;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private void initialize(JavaPlugin plugin, Plugin worldGuard) {
        try {
            ClassLoader loader = worldGuard.getClass().getClassLoader();
            Class<?> worldGuardClass = Class.forName(WORLDGUARD_CLASS, true, loader);
            Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
            Object platform = worldGuardInstance.getClass().getMethod("getPlatform").invoke(worldGuardInstance);
            regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Class<?> bukkitAdapterClass = Class.forName(BUKKIT_ADAPTER_CLASS, true, loader);
            adaptBukkitLocationMethod = bukkitAdapterClass.getMethod("adapt", Location.class);
            Class<?> weLocationClass = adaptBukkitLocationMethod.getReturnType();

            createQueryMethod = regionContainer.getClass().getMethod("createQuery");
            Class<?> regionQueryClass = createQueryMethod.getReturnType();
            getApplicableRegionsMethod = regionQueryClass.getMethod("getApplicableRegions", weLocationClass);

            Class<?> protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion", true, loader);
            regionGetIdMethod = protectedRegionClass.getMethod("getId");

            Class<?> delegateEventClass = Class.forName("com.sk89q.worldguard.bukkit.event.DelegateEvent", true, loader);
            delegateGetResultMethod = delegateEventClass.getMethod("getResult");
            delegateSetResultMethod = delegateEventClass.getMethod("setResult", Event.Result.class);
            delegateSetSilentMethod = delegateEventClass.getMethod("setSilent", boolean.class);
            delegateGetCauseMethod = delegateEventClass.getMethod("getCause");
            Class<?> abstractEntityEventClass = Class.forName(
                    "com.sk89q.worldguard.bukkit.event.entity.AbstractEntityEvent",
                    true,
                    loader
            );
            delegateGetEntityMethod = abstractEntityEventClass.getMethod("getEntity");
            delegateGetTargetMethod = abstractEntityEventClass.getMethod("getTarget");
            useBlockEventClass = Class.forName(USE_BLOCK_EVENT, true, loader);
            delegateGetBlocksMethod = useBlockEventClass.getMethod("getBlocks");
            Class<?> causeClass = Class.forName("com.sk89q.worldguard.bukkit.cause.Cause", true, loader);
            causeGetFirstPlayerMethod = causeClass.getMethod("getFirstPlayer");
            eventResultAllow = Event.Result.ALLOW;

            enabled = true;
            initFailed = false;
            logger.info("WorldGuard hook ready for physical crates (" + worldGuard.getDescription().getVersion() + ").");
        } catch (ReflectiveOperationException exception) {
            enabled = false;
            initFailed = true;
            regionContainer = null;
            logger.warning("WorldGuard hook failed to initialize: " + exception.getMessage());
        }
    }

    private void registerDelegateHandler(JavaPlugin plugin, String eventClassName, Consumer<Object> handler, EventPriority priority) {
        try {
            ClassLoader loader = plugin.getServer().getPluginManager().getPlugin(WORLDGUARD_PLUGIN).getClass().getClassLoader();
            Class<? extends Event> eventClass = Class.forName(eventClassName, true, loader).asSubclass(Event.class);
            HandlerList handlerList = (HandlerList) eventClass.getMethod("getHandlerList").invoke(null);
            RegisteredListener registration = new RegisteredListener(
                    bypassListenerToken,
                    (listener, event) -> handler.accept(event),
                    priority,
                    plugin,
                    false
            );
            handlerList.register(registration);
            bypassRegistrations.add(new HandlerRegistration(handlerList, registration));
        } catch (ReflectiveOperationException exception) {
            logger.warning("WorldGuard bypass listener failed for " + eventClassName + " (" + priority + "): " + exception.getMessage());
        }
    }

    private void unregisterBypassListeners() {
        for (HandlerRegistration registration : bypassRegistrations) {
            registration.handlerList.unregister(registration.registration);
        }
        bypassRegistrations.clear();
    }

    public boolean integrationActive(WorldGuardPhysicalCrateSettings settings) {
        return enabled() && settings != null && settings.enabled;
    }

    public boolean isBypassRegion(Location location, WorldGuardPhysicalCrateSettings settings) {
        if (!integrationActive(settings)) {
            return false;
        }
        for (Object region : applicableRegions(location)) {
            if (region == null) {
                continue;
            }
            try {
                Object regionId = regionGetIdMethod.invoke(region);
                if (regionId instanceof String id && settings.matchesBypassRegion(id)) {
                    return true;
                }
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        }
        return false;
    }

    private Iterable<Object> applicableRegions(Location location) {
        if (location == null || location.getWorld() == null) {
            return List.of();
        }
        try {
            Object query = createQueryMethod.invoke(regionContainer);
            Object weLocation = adaptBukkitLocationMethod.invoke(null, location);
            Object regionSet = getApplicableRegionsMethod.invoke(query, weLocation);
            if (regionSet == null) {
                return List.of();
            }
            if (regionSet instanceof Iterable<?> iterable) {
                List<Object> regions = new ArrayList<>();
                for (Object region : iterable) {
                    regions.add(region);
                }
                return regions;
            }
            Method regionsMethod = regionSet.getClass().getMethod("getRegions");
            Object regions = regionsMethod.invoke(regionSet);
            if (regions instanceof Iterable<?> iterable) {
                List<Object> list = new ArrayList<>();
                for (Object region : iterable) {
                    list.add(region);
                }
                return list;
            }
        } catch (ReflectiveOperationException ignored) {
            return List.of();
        }
        return List.of();
    }
}
