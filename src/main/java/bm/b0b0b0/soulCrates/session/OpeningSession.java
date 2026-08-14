package bm.b0b0b0.soulCrates.session;

import bm.b0b0b0.soulCrates.model.OpeningContext;
import bm.b0b0b0.soulCrates.model.OpeningSessionState;
import java.util.UUID;

public interface OpeningSession {

    UUID sessionId();

    OpeningContext context();

    OpeningSessionState state();

    boolean isActive();

    void cancel();

    void finish();
}
