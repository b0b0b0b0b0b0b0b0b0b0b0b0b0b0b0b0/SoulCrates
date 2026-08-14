package bm.b0b0b0.soulCrates.config.settings;

import net.elytrium.serializer.language.object.YamlSerializable;

public final class AnimationPhaseSettings extends YamlSerializable {

    public String type = "default";

    public int durationTicks = 20;

    public AnimationPhaseProperties properties = new AnimationPhaseProperties();
}
