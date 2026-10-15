package com.ai.decision_tracker01.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private final IngestionService ingestionService;

    public DocumentService(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public List<Document> read(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No content was provided.");
        }

        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        Resource resource = file.getResource();

        List<Document> documents = switch (ext) {
            case "pdf" -> new PagePdfDocumentReader(resource).get();
            case "md" -> new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig.defaultConfig()).get();
            case "txt" -> new TextReader(resource).get();
            default -> throw new IllegalArgumentException("Only PDF, Markdown (.md) and text (.txt) files are supported.");
        };

        log.info("Read '{}' as {} document(s)", name, documents.size());
        return documents;
    }

    public void upload(MultipartFile file) {
        List<Document> documents = read(file);
        ingestionService.ingest(file.getOriginalFilename(), documents);
    }
}
