package com.ai.decision_tracker01.extraction;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GraphSchema {

    public static final Set<String> LABELS =
            Set.of("Person", "Team", "System", "Decision", "Incident");

    // relationship type -> {allowed "from" labels, allowed "to" labels}
    public static final Map<String, List<Set<String>>> RELATIONS = Map.of(
            "MEMBER_OF",  List.of(Set.of("Person"), Set.of("Team")),
            "OWNS",       List.of(Set.of("Team"), Set.of("System")),
            "REVIEWS",    List.of(Set.of("Team"), Set.of("System")),
            "DEPENDS_ON", List.of(Set.of("System"), Set.of("System")),
            "DECIDED",    List.of(Set.of("Person"), Set.of("Decision")),
            "TRIGGERED",  List.of(Set.of("Incident"), Set.of("Decision")),
            "AFFECTS",    List.of(Set.of("Decision", "Incident"), Set.of("System")),
            "SUPERSEDES", List.of(Set.of("Decision"), Set.of("Decision"))
    );

    public static boolean isValid(String fromLabel, String rel, String toLabel) {
        List<Set<String>> rule = RELATIONS.get(rel);
        return rule != null && rule.get(0).contains(fromLabel) && rule.get(1).contains(toLabel);
    }

    private GraphSchema() {}
}
