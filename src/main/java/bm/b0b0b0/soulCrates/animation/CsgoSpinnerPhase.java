package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.config.settings.GuiSpinnerSettings;
import bm.b0b0b0.soulCrates.gui.CsgoSpinnerMenu;
import bm.b0b0b0.soulCrates.lang.MessageService;
import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.RewardDefinition;
import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import bm.b0b0b0.soulCrates.util.PluginSchedulers;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CsgoSpinnerPhase implements PhaseRunner {

    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final GuiSpinnerSettings spinnerSettings;
    private final CrateDefinition crateDefinition;
    private final RewardDefinition rolledReward;
    private final int durationTicks;
    private CsgoSpinnerMenu menu;
    private ScheduledTask spinTask;
    private int ticksRemaining;
    private int spinCursor;

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
        this.durationTicks = Math.max(20, durationTicks);
    }

    @Override
    public OpeningPhaseKind kind() {
        return OpeningPhaseKind.SECOND;
    }

    @Override
    public void load(Player player, CrateOpeningSession session) {
        ticksRemaining = durationTicks;
        menu = new CsgoSpinnerMenu(
                player.getUniqueId(),
                messageService,
                spinnerSettings,
                crateDefinition,
                rolledReward
        );
        PluginSchedulers.run(plugin, player, () -> player.openInventory(menu.getInventory()));
        spinTask = PluginSchedulers.runTimer(
                plugin,
                player,
                spinnerSettings.spinIntervalTicks,
                spinnerSettings.spinIntervalTicks,
                () -> {
                    spinCursor++;
                    menu.spin(spinCursor);
                }
        );
    }

    @Override
    public void tick(Player player, CrateOpeningSession session) {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
        if (ticksRemaining <= 10 && menu != null) {
            menu.lockWinner(rolledReward);
        }
    }

    @Override
    public void unload(Player player, CrateOpeningSession session) {
        if (spinTask != null) {
            spinTask.cancel();
            spinTask = null;
        }
        PluginSchedulers.run(plugin, player, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof CsgoSpinnerMenu) {
                player.closeInventory();
            }
        });
        menu = null;
    }

    @Override
    public boolean finished() {
        return ticksRemaining <= 0;
    }
}
