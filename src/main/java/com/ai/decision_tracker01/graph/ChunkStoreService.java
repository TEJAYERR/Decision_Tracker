package com.ai.decision_tracker01.graph;

import com.falkordb.Record;
import com.falkordb.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChunkStoreService {

    private static final String GRAPH = "company";
    private static final Logger log = LoggerFactory.getLogger(ChunkStoreService.class);

    private final GraphService graph;
    private final int dimension;
    private final EmbeddingModel embeddingModel;

    public ChunkStoreService(GraphService graph,
                             @Value("${chunks.embedding-dimension}") int dimension, EmbeddingModel embeddingModel) {
        this.graph = graph;
        this.dimension = dimension;
        this.embeddingModel = embeddingModel;
    }

    @EventListener(ApplicationReadyEvent.class)
    void ensureIndex() {
        try {
            graph.query(GRAPH,
                    "CREATE VECTOR INDEX FOR (c:Chunk) ON (c.embedding) " +
                            "OPTIONS {dimension:" + dimension + ", similarityFunction:'cosine'}", Map.of());
            log.info("Created vector index on Chunk.embedding");
        } catch (Exception e) {
            log.info("Vector index not created (probably exists already): {}", e.getMessage());
        }
    }

    public void add(String source, List<Document> chunks){

        List<String> texts = chunks.stream().map(Document::getText).toList();
        List<float[]> embeddings = embeddingModel.embed(texts);

        for(int i = 0; i < chunks.size(); i++){

            String id = UUID.nameUUIDFromBytes(
                    (source + "::" + texts.get(i)).getBytes(StandardCharsets.UTF_8)).toString();

            List<Float> vector = new ArrayList<>(embeddings.get(i).length);
            for (float f : embeddings.get(i)) vector.add(f);

            graph.query(GRAPH,
                    "MERGE (c:Chunk {id:$id}) SET c.text=$text, c.source=$source, c.embedding=vecf32($embedding)",
                    Map.of("id", id, "text", texts.get(i), "source", source, "embedding", vector));
        }
    }

    public record Hit(String id, String text, String source, double score) {}

    public List<Hit> search(String query, int k) {
        float[] raw = embeddingModel.embed(query);
        List<Float> vector = new ArrayList<>(raw.length);
        for (float f : raw) vector.add(f);

        ResultSet rs = graph.query(GRAPH,
                "CALL db.idx.vector.queryNodes('Chunk', 'embedding', " + k + ", vecf32($vector)) " +
                        "YIELD node, score " +
                        "RETURN node.id AS id, node.text AS text, node.source AS source, score",
                Map.of("vector", vector));

        List<Hit> hits = new ArrayList<>();

        for (Record r : rs) {
            Object id = r.getValue("id");
            Object text = r.getValue("text");
            Object source = r.getValue("source");
            Object score = r.getValue("score");

            hits.add(new Hit(String.valueOf(id), String.valueOf(text),
                    String.valueOf(source), ((Number) score).doubleValue()));
        }

        return hits;
    }
}
