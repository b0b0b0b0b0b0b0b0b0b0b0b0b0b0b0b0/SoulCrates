package bm.b0b0b0.soulCrates.service.npc;

import bm.b0b0b0.soulCrates.repository.CrateRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CrateNpcService {

    private final CrateRepository repository;
    private final Map<Integer, String> bindings = new ConcurrentHashMap<>();

    public CrateNpcService(CrateRepository repository) {
        this.repository = repository;
    }

    public CompletableFuture<Void> loadAll() {
        return repository.loadAllNpcBindings().thenAccept(values -> {
            bindings.clear();
            bindings.putAll(values);
        });
    }

    public Optional<String> findCrateId(int npcId) {
        String crateId = bindings.get(npcId);
        if (crateId == null || crateId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(crateId);
    }

    public CompletableFuture<Void> bind(int npcId, String crateId) {
        bindings.put(npcId, crateId.toLowerCase());
        return repository.saveNpcBinding(npcId, crateId);
    }

    public CompletableFuture<Void> unbind(int npcId) {
        bindings.remove(npcId);
        return repository.deleteNpcBinding(npcId);
    }

    public Map<Integer, String> allBindings() {
        return Map.copyOf(bindings);
    }

    public void clear() {
        bindings.clear();
    }
}
