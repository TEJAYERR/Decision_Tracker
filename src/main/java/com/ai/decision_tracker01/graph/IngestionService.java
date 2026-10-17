package com.ai.decision_tracker01.graph;

import com.ai.decision_tracker01.extraction.ExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private final ChunkStoreService chunkStore;
    private final GraphWriter graphWriter;
    private final ExtractionService extractionService;

    public IngestionService(ChunkStoreService chunkStore, GraphWriter graphWriter, ExtractionService extractionService) {
        this.chunkStore = chunkStore;
        this.graphWriter = graphWriter;
        this.extractionService = extractionService;
    }

    @Async
    public void ingest(String source, List<Document> documents) {
        log.info("Ingesting '{}' in the background", source);
        try {
            TextSplitter splitter = TokenTextSplitter.builder().withChunkSize(900).build();
            List<Document> batch = new ArrayList<>();

            for (Document document : documents) {
                String header = "Document: " + source + "\n";

                List<Document> chunks = splitter.split(List.of(document)).stream()
                        .map(c -> new Document(header + c.getText(), c.getMetadata()))
                        .toList();
                batch.addAll(chunks);

                if (batch.size() >= 30) {
                    chunkStore.add(source, batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                chunkStore.add(source, batch);
            }
            log.info("Finished ingesting '{}'", source);

            graphWriter.linkChunks(source);

            String fullText = documents.stream().map(Document::getText).collect(Collectors.joining("\n\n"));
            graphWriter.write(source, extractionService.extract(fullText, graphWriter.existingNames()));

        } catch (Exception e) {
            log.error("Ingestion failed for '{}'", source, e);
        }
    }
}
