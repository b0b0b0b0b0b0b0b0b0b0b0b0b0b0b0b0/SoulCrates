package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.config.settings.GuiSpinnerSettings;
import bm.b0b0b0.soulCrates.gui.CsgoSpinnerMenu;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CsgoSpinnerPhase implements PhaseRunner {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final GuiSpinnerSettings spinnerSettings;
    private final CrateDefinition crateDefinition;
    private final RewardDefinition rolledReward;
    private CsgoSpinnerMenu menu;
    private boolean scrolling;
    private int scrollOffset;
    private int spinWaitTicks;
    private int pauseTicksRemaining;
    private boolean winLockSoundPlayed;

    public CsgoSpinnerPhase(
            JavaPlugin plugin,
            MessageService messageService,
            GuiSpinnerSettings spinnerSettings,
            CrateDefinition crateDefinition,
            RewardDefinition rolledReward,
            int durationTicks
    ) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.spinnerSettings = spinnerSettings;
        this.crateDefinition = crateDefinition;
        this.rolledReward = rolledReward;
    }

    @Override
    public OpeningPhaseKind kind() {
        return OpeningPhaseKind.SECOND;
    }

    @Override
    public void load(Player player, CrateOpeningSession session) {
        scrolling = true;
        scrollOffset = 0;
        spinWaitTicks = 0;
        pauseTicksRemaining = 0;
        winLockSoundPlayed = false;
        menu = session.csgoSpinnerMenu();
        if (menu == null) {
            menu = new CsgoSpinnerMenu(
                    player.getUniqueId(),
                    messageService,
                    spinnerSettings,
                    crateDefinition,
                    rolledReward
            );
            session.setCsgoSpinnerMenu(menu);
        } else {
            menu.prepareRoulette();
        }
        if (!isViewingMenu(player, menu)) {
            player.openInventory(menu.getInventory());
        } else {
            menu.renderOffset(0, true);
        }
    }

    @Override
    public void tick(Player player, CrateOpeningSession session) {
        if (menu == null) {
            return;
        }
        if (scrolling) {
            spinWaitTicks--;
            if (spinWaitTicks > 0) {
                return;
            }
            scrollOffset++;
            boolean centerChanged = menu.renderOffset(scrollOffset);
            if (centerChanged) {
                playSpinTick(player, scrollProgress(), menu.isNearMissStep());
            }
            if (menu.isStopped()) {
                scrolling = false;
                pauseTicksRemaining = spinnerSettings.finishPauseTicks;
                if (!winLockSoundPlayed) {
                    winLockSoundPlayed = true;
                    playWinLockSound(player);
                }
                return;
            }
            spinWaitTicks = nextSpinInterval();
            return;
        }
        if (pauseTicksRemaining > 0) {
            pauseTicksRemaining--;
        }
    }

    @Override
    public void unload(Player player, CrateOpeningSession session) {
        PluginSchedulers.run(plugin, player, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof CsgoSpinnerMenu) {
                player.closeInventory();
            }
        });
        menu = null;
    }

    @Override
    public boolean finished() {
        return !scrolling && pauseTicksRemaining <= 0;
    }

    private float scrollProgress() {
        if (menu == null || menu.stopOffset() <= 0) {
            return 0.0f;
        }
        return Math.min(1.0f, scrollOffset / (float) menu.stopOffset());
    }

    private int nextSpinInterval() {
        float progress = scrollProgress();
        float eased = progress * progress * progress;
        int min = Math.max(1, spinnerSettings.minSpinIntervalTicks);
        int max = Math.max(min, spinnerSettings.maxSpinIntervalTicks);
        int interval = min + Math.round((max - min) * eased);
        if (progress >= 0.78f) {
            interval += 2;
        }
        if (progress >= 0.9f) {
            interval += 2;
        }
        return interval;
    }

    private void playSpinTick(Player player, float progress, boolean nearMiss) {
        if (!spinnerSettings.spinTickSound) {
            return;
        }
        Sound sound = parseSound(spinnerSettings.spinTickSoundName, Sound.BLOCK_NOTE_BLOCK_HAT);
        float pitch = spinnerSettings.spinTickSoundPitch + progress * 0.45f;
        if (nearMiss) {
            pitch += 0.35f;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.35f, pitch + 0.15f);
        }
        player.playSound(player.getLocation(), sound, spinnerSettings.spinTickSoundVolume, pitch);
    }

    private void playWinLockSound(Player player) {
        if (!spinnerSettings.winLockSound) {
            return;
        }
        Sound sound = parseSound(spinnerSettings.winLockSoundName, Sound.BLOCK_NOTE_BLOCK_PLING);
        player.playSound(
                player.getLocation(),
                sound,
                spinnerSettings.winLockSoundVolume,
                spinnerSettings.winLockSoundPitch
        );
    }

    private static Sound parseSound(String raw, Sound fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Sound.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static boolean isViewingMenu(Player player, CsgoSpinnerMenu menu) {
        return player.getOpenInventory().getTopInventory().getHolder(false) == menu;
    }
}
