package com.ai.decision_tracker01.extraction;

import java.util.List;

public record Extraction(List<Entity> entities, List<Relation> relations) {
    public record Entity(String type, String name) {}
    public record Relation(String from, String type, String to) {}
}
