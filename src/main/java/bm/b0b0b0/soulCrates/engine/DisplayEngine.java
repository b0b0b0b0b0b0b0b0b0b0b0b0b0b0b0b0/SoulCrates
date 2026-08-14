package bm.b0b0b0.soulCrates.engine;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.DisplayEngineKind;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface DisplayEngine {

    DisplayEngineKind kind();

    boolean available();

    DisplayComponent createComponent(CrateDefinition crateDefinition, Location location, Player viewer);
}
