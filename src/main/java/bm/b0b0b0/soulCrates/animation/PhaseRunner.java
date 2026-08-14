package bm.b0b0b0.soulCrates.animation;

import bm.b0b0b0.soulCrates.session.CrateOpeningSession;
import org.bukkit.entity.Player;

public interface PhaseRunner {

    OpeningPhaseKind kind();

    void load(Player player, CrateOpeningSession session);

    void tick(Player player, CrateOpeningSession session);

    void unload(Player player, CrateOpeningSession session);

    boolean finished();
}
