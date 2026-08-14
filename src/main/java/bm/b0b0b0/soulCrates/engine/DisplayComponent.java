package bm.b0b0b0.soulCrates.engine;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import bm.b0b0b0.soulCrates.model.DisplayEngineKind;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface DisplayComponent {

    void create();

    void destroy();

    void show(Player player);

    void hide(Player player);

    void playAnimation(String animationId);

    Location anchor();
}
