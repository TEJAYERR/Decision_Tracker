package com.ai.decision_tracker01.graph;

import com.ai.decision_tracker.extraction.Extraction;
import com.ai.decision_tracker.extraction.GraphSchema;
import com.falkordb.Record;
import com.falkordb.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphWriter {

    private static final String GRAPH = "company";
    private static final Logger log = LoggerFactory.getLogger(GraphWriter.class);
    private final GraphService graph;

    public GraphWriter(GraphService graph) { this.graph = graph; }

    public void write(String source, Extraction ex) {
        if (ex == null || ex.entities() == null) return;

        graph.query(GRAPH, "MERGE (:Document {name:$name})", Map.of("name", source));

        Map<String, String> typeByKey = new HashMap<>();
        for (Extraction.Entity e : ex.entities()) {
            if (!GraphSchema.LABELS.contains(e.type())) {
                log.warn("Skipped entity with unknown type: {}", e);
                continue;
            }
            String key = key(e.name());
            typeByKey.put(key, e.type());
            graph.query(GRAPH, "MERGE (n:" + e.type() + " {key:$key}) SET n.name=$name",
                    Map.of("key", key, "name", e.name()));
            graph.query(GRAPH, "MATCH (n:" + e.type() + " {key:$key}), (d:Document {name:$doc}) MERGE (n)-[:MENTIONED_IN]->(d)",
                    Map.of("key", key, "doc", source));
        }

        if (ex.relations() == null) return;
        for (Extraction.Relation r : ex.relations()) {
            String fromType = typeByKey.get(key(r.from()));
            String toType = typeByKey.get(key(r.to()));
            if (fromType == null || toType == null || !GraphSchema.isValid(fromType, r.type(), toType)) {
                log.warn("Skipped relation: {}", r);
                continue;
            }
            graph.query(GRAPH, "MATCH (a:" + fromType + " {key:$from}), (b:" + toType + " {key:$to}) MERGE (a)-[:" + r.type() + "]->(b)",
                    Map.of("from", key(r.from()), "to", key(r.to())));
        }
    }

    public void linkChunks(String source) {
        graph.query(GRAPH, "MATCH (c:Chunk {source:$s}), (d:Document {name:$s}) MERGE (c)-[:PART_OF]->(d)",
                Map.of("s", source));
    }

    private static String key(String name) { return name.trim().toLowerCase(); }

    public Map<String, List<String>> existingNames() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (String label : GraphSchema.LABELS) {
            ResultSet rs = graph.query(GRAPH, "MATCH (n:" + label + ") RETURN n.name AS name", Map.of());
            List<String> names = new ArrayList<>();
            for (Record r : rs) {
                Object name = r.getValue("name");
                names.add(String.valueOf(name));
            }
            out.put(label, names);
        }
        return out;
    }
}
