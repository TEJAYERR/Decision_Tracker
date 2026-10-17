package com.ai.decision_tracker01.controller;

import com.ai.decision_tracker01.agent.AgentService;
import com.ai.decision_tracker01.agent.GraphTools;
import com.ai.decision_tracker01.extraction.Extraction;
import com.ai.decision_tracker01.extraction.ExtractionService;
import com.ai.decision_tracker01.graph.DocumentService;
import com.ai.decision_tracker01.graph.GraphWriter;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class DocumentController {

    private final DocumentService documentService;
    private final ExtractionService extractionService;
    private final GraphWriter graphWriter;
    private final GraphTools graphTools;
    private final AgentService agentService;

    public DocumentController(DocumentService documentService, ExtractionService extractionService, GraphWriter graphWriter, GraphTools graphTools, AgentService agentService) {
        this.documentService = documentService;
        this.extractionService = extractionService;
        this.graphWriter = graphWriter;
        this.graphTools = graphTools;
        this.agentService = agentService;
    }

    @PostMapping("/documents")
    public ResponseEntity<?> upload(@RequestParam MultipartFile multipartFile) {
        documentService.upload(multipartFile);
        return ResponseEntity.ok("success");
    }

    @PostMapping("/extract-test")
    public Extraction extractTest(@RequestParam MultipartFile multipartFile) {
        String text = documentService.read(multipartFile).stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
        return extractionService.extract(text, graphWriter.existingNames());
    }

    @GetMapping("/tool-test")
    public List<GraphTools.DecisionTrace> toolTest(@RequestParam String topic) {
        return graphTools.traceDecision(topic);
    }

    @GetMapping("/ask")
    public AgentService.AgentAnswer ask(@RequestParam String question) {
        return agentService.ask(question);
    }
}
