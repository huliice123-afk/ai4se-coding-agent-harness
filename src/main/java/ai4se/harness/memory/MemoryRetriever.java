package ai4se.harness.memory;

import java.util.*;
import java.util.stream.Collectors;

public class MemoryRetriever {
    private final MemoryStore store;

    public MemoryRetriever(MemoryStore store) {
        this.store = store;
    }

    public List<String> search(String query, int topK) {
        List<String> results = new ArrayList<>();
        String queryLower = query.toLowerCase();
        String[] keywords = queryLower.split("\\s+");

        for (String key : List.of("decisions_1", "decisions_2", "session_latest")) {
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