package bm.b0b0b0.soulCrates.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PluginSchedulers {

    private PluginSchedulers() {
    }

    public static void runGlobal(Plugin plugin, Runnable runnable) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, runnable);
    }

    public static void runGlobalLater(Plugin plugin, long delayTicks, Runnable runnable) {
        long delay = Math.max(1L, delayTicks);
        plugin.getServer().getGlobalRegionScheduler().runDelayed(
                plugin,
                (ScheduledTask task) -> runnable.run(),
                delay
        );
    }

    public static ScheduledTask runGlobalRepeating(Plugin plugin, long initialDelayTicks, long periodTicks, Runnable runnable) {
        return runGlobalTimerReturning(plugin, initialDelayTicks, periodTicks, runnable);
    }

    public static void runGlobalTimer(Plugin plugin, long initialDelayTicks, long periodTicks, Runnable runnable) {
        long initial = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                (ScheduledTask task) -> runnable.run(),
                initial,
                period
        );
    }

    public static void run(Plugin plugin, Player player, Runnable runnable) {
        run(plugin, (Entity) player, runnable);
    }

    public static void run(Plugin plugin, Entity entity, Runnable runnable) {
        if (entity != null && Bukkit.isOwnedByCurrentRegion(entity)) {
            runnable.run();
            return;
        }
        if (entity == null) {
            runGlobal(plugin, runnable);
            return;
        }
        entity.getScheduler().execute(plugin, runnable, null, 0L);
    }

    public static void runLater(Plugin plugin, Player player, long delayTicks, Runnable runnable) {
        runLater(plugin, (Entity) player, delayTicks, runnable);
    }

    public static void runLater(Plugin plugin, Entity entity, long delayTicks, Runnable runnable) {
        long delay = Math.max(1L, delayTicks);
        if (entity == null) {
            runGlobalLater(plugin, delay, runnable);
            return;
        }
        entity.getScheduler().runDelayed(plugin, (ScheduledTask task) -> runnable.run(), null, delay);
    }

    public static ScheduledTask runTimer(Plugin plugin, Player player, long initialDelayTicks, long periodTicks, Runnable runnable) {
        return runTimer(plugin, (Entity) player, initialDelayTicks, periodTicks, runnable);
    }

    public static ScheduledTask runTimer(Plugin plugin, Entity entity, long initialDelayTicks, long periodTicks, Runnable runnable) {
        long initial = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        if (entity == null) {
            return runGlobalTimerReturning(plugin, initial, period, runnable);
        }
        return entity.getScheduler().runAtFixedRate(plugin, (ScheduledTask task) -> runnable.run(), null, initial, period);
    }

    private static ScheduledTask runGlobalTimerReturning(Plugin plugin, long initialDelayTicks, long periodTicks, Runnable runnable) {
        long initial = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        ScheduledTask[] holder = new ScheduledTask[1];
        holder[0] = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                (ScheduledTask task) -> runnable.run(),
                initial,
                period
        );
        return holder[0];
    }

    public static void runAt(Plugin plugin, Location location, Runnable runnable) {
        if (location == null || location.getWorld() == null) {
            runGlobal(plugin, runnable);
            return;
        }
        if (Bukkit.isOwnedByCurrentRegion(location)) {
            runnable.run();
            return;
        }
        plugin.getServer().getRegionScheduler().execute(plugin, location, runnable);
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run());
    }

    public static ScheduledTask runAsyncLater(Plugin plugin, long delayTicks, Runnable runnable) {
        long delayMillis = Math.max(1L, delayTicks) * 50L;
        return plugin.getServer().getAsyncScheduler().runDelayed(
                plugin,
                scheduledTask -> runnable.run(),
                delayMillis,
                TimeUnit.MILLISECONDS
        );
    }
}
