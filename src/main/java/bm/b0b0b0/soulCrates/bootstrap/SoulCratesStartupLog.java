package bm.b0b0b0.soulCrates.bootstrap;

import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulCratesStartupLog {

    private final Logger logger;

    public SoulCratesStartupLog(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
    }

    public void bannerStart(String version) {
        logger.info("SoulCrates v" + version + " — loading");
    }

    public void stepOk(String message) {
        logger.info("[OK] " + message);
    }

    public void stepFail(String message) {
        logger.warning("[FAIL] " + message);
    }

    public void stepSchedulers() {
        stepOk("Schedulers — Folia region/global/async");
    }

    public void bannerReady() {
        logger.info("SoulCrates ready");
    }

    public void bannerShutdown() {
        logger.info("SoulCrates disabled");
    }
}
