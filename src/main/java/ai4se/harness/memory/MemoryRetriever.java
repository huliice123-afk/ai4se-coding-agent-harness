package ai4se.harness.memory;

import java.util.*;
import java.util.stream.Collectors;

public class MemoryRetriever {
    private static final String SESSION_LATEST = "session_latest";

    private final MemoryStore store;

    public MemoryRetriever(MemoryStore store) {
        this.store = store;
    }

    public void saveSessionSummary(String summary) {
        store.save(SESSION_LATEST, summary);
    }

    public String retrieve(String query) {
        Optional<String> latest = store.load(SESSION_LATEST);
        if (latest.isPresent()) {
            return latest.get();
        }
        List<String> results = search(query, 1);
        return results.isEmpty() ? "" : results.get(0);
    }

    public List<String> search(String query, int topK) {
        List<String> results = new ArrayList<>();
        String queryLower = query.toLowerCase();
        String[] keywords = queryLower.split("\\s+");

        for (String key : List.of("decisions_1", "decisions_2", SESSION_LATEST)) {
            store.load(key).ifPresent(content -> {
                String contentLower = content.toLowerCase();
                for (String kw : keywords) {
                    if (contentLower.contains(kw)) {
                        results.add(content);
                        break;
                    }
                }
            });
        }
        return results.stream().limit(topK).collect(Collectors.toList());
    }
}
