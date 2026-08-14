package bm.b0b0b0.soulCrates.service;

import bm.b0b0b0.soulCrates.model.CrateDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CrateRegistry {

    private final Map<String, CrateDefinition> crates = new ConcurrentHashMap<>();

    public void replaceAll(List<CrateDefinition> definitions) {
        crates.clear();
        for (CrateDefinition definition : definitions) {
            register(definition);
        }
    }

    public void register(CrateDefinition crateDefinition) {
        crates.put(crateDefinition.id().toLowerCase(), crateDefinition);
    }

    public Optional<CrateDefinition> find(String crateId) {
        if (crateId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(crates.get(crateId.toLowerCase()));
    }

    public Collection<CrateDefinition> all() {
        return crates.values();
    }

    public List<CrateDefinition> list() {
        return new ArrayList<>(crates.values());
    }
}
