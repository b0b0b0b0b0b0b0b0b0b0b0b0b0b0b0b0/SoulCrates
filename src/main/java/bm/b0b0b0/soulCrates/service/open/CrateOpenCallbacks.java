package bm.b0b0b0.soulCrates.service.open;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface CrateOpenCallbacks {

    void proceedOpenFlow(Player player, CrateDefinition crate, Location location, int amount);

    void reloadCrates(Player player);
}
