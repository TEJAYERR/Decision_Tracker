package com.ai.decision_tracker01.agent;

import com.ai.decision_tracker01.graph.ChunkStoreService;
import com.ai.decision_tracker01.graph.GraphService;
import com.falkordb.Record;
import com.falkordb.ResultSet;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GraphTools {

    private static final String GRAPH = "company";
    private final GraphService graph;
    private final ChunkStoreService chunkStore;
    private final double maxDistance;
    private final PathRecorder pathRecorder;

    private static String edge(String a, String rel, String b) { return a + " —" + rel + "→ " + b; }

    public GraphTools(GraphService graph, ChunkStoreService chunkStore,
                      @Value("${chunks.max-distance:0.40}") double maxDistance, PathRecorder pathRecorder) {
        this.graph = graph;
        this.chunkStore = chunkStore;
        this.maxDistance = maxDistance;
        this.pathRecorder = pathRecorder;
    }

    public record DecisionTrace(String decision, List<String> triggers, List<String> decidedBy,
                                List<String> affects, List<String> mentionedIn) {}
    public record DecisionStatus(String decision, List<String> decidedBy, List<String> supersededBy, List<String> supersedes) {}
    public record SystemPeople(String system, List<String> ownedBy, List<String> members, List<String> reviewedBy, List<String> decisionMakers) {}
    public record Passage(String source, String text) {}

    @Tool(description = "List every decision that affects a system, who made it, and whether another decision superseded it (a decision with a non-empty supersededBy is outdated). Use for questions about decisions affecting a system or whether something is outdated.")
    public List<DecisionStatus> decisionsAffecting(
            @ToolParam(description = "A keyword from the system name, for example 'auth' or 'billing'") String system) {
        ResultSet rs = graph.query(GRAPH,
                "MATCH (d:Decision)-[:AFFECTS]->(s:System) WHERE toLower(s.name) CONTAINS $system " +
                        "OPTIONAL MATCH (newer:Decision)-[:SUPERSEDES]->(d) " +
                        "OPTIONAL MATCH (d)-[:SUPERSEDES]->(older:Decision) " +
                        "OPTIONAL MATCH (p:Person)-[:DECIDED]->(d) " +
                        "RETURN d.name AS decision, collect(DISTINCT p.name) AS decidedBy, " +
                        "collect(DISTINCT newer.name) AS supersededBy, collect(DISTINCT older.name) AS supersedes",
                Map.of("system", system.toLowerCase().trim()));
        List<DecisionStatus> out = new ArrayList<>();
        for (Record r : rs) {
            Object decision = r.getValue("decision");
            out.add(new DecisionStatus(String.valueOf(decision), strings(r.getValue("decidedBy")),
                    strings(r.getValue("supersededBy")), strings(r.getValue("supersedes"))));
        }

        List<String> edges = new ArrayList<>();
        for (DecisionStatus d : out) {
            d.decidedBy().forEach(x -> edges.add(edge(x, "DECIDED", d.decision())));
            d.supersededBy().forEach(x -> edges.add(edge(x, "SUPERSEDES", d.decision())));
        }
        pathRecorder.record("decisionsAffecting", system, edges);

        return out;
    }

    @Tool(description = "Find who to talk to about a system: the team that owns it, that team's members, teams that only review it, and people who made decisions about it. Use for 'who should I ask' questions.")
    public List<SystemPeople> whoKnowsAbout(
            @ToolParam(description = "A keyword from the system name, for example 'billing'") String system) {
        ResultSet rs = graph.query(GRAPH,
                "MATCH (s:System) WHERE toLower(s.name) CONTAINS $system " +
                        "OPTIONAL MATCH (t:Team)-[:OWNS]->(s) " +
                        "OPTIONAL MATCH (m:Person)-[:MEMBER_OF]->(t) " +
                        "OPTIONAL MATCH (r:Team)-[:REVIEWS]->(s) " +
                        "OPTIONAL MATCH (p:Person)-[:DECIDED]->(:Decision)-[:AFFECTS]->(s) " +
                        "RETURN s.name AS system, collect(DISTINCT t.name) AS ownedBy, collect(DISTINCT m.name) AS members, " +
                        "collect(DISTINCT r.name) AS reviewedBy, collect(DISTINCT p.name) AS decisionMakers",
                Map.of("system", system.toLowerCase().trim()));
        List<SystemPeople> out = new ArrayList<>();
        for (Record r : rs) {
            Object name = r.getValue("system");
            out.add(new SystemPeople(String.valueOf(name), strings(r.getValue("ownedBy")), strings(r.getValue("members")),
                    strings(r.getValue("reviewedBy")), strings(r.getValue("decisionMakers"))));
        }

        // whoKnowsAbout
        List<String> edges = new ArrayList<>();
        for (SystemPeople p : out) {
            p.ownedBy().forEach(x -> edges.add(edge(x, "OWNS", p.system())));
            p.reviewedBy().forEach(x -> edges.add(edge(x, "REVIEWS", p.system())));
            p.ownedBy().stream().findFirst().ifPresent(team -> p.members().forEach(m -> edges.add(edge(m, "MEMBER_OF", team))));
        }
        pathRecorder.record("whoKnowsAbout", system, edges);

        return out;
    }

    @Tool(description = "Search the text of the uploaded documents for passages relevant to a question. Use when you need the exact wording or details that the graph does not hold.")
    public List<Passage> searchDocuments(@ToolParam(description = "What to look for") String query) {
        List<Passage> result = chunkStore.search(query, 5).stream()
                .filter(h -> h.score() < maxDistance).map(h -> new Passage(h.source(), h.text())).toList();
        pathRecorder.record("searchDocuments", query, result.stream().map(p -> "text from " + p.source()).distinct().toList());
        return result;
    }

    @Tool(description = "Find decisions by a keyword from their title and explain them: what incident triggered each decision, who decided it, which systems it affects, and which documents mention it. Use this for 'why' questions about a decision.")
    public List<DecisionTrace> traceDecision(
            @ToolParam(description = "A keyword from the decision title, for example 'session' or 'retry'") String topic) {

        ResultSet rs = graph.query(GRAPH,
                "MATCH (d:Decision) WHERE toLower(d.name) CONTAINS $topic " +
                        "OPTIONAL MATCH (i:Incident)-[:TRIGGERED]->(d) " +
                        "OPTIONAL MATCH (p:Person)-[:DECIDED]->(d) " +
                        "OPTIONAL MATCH (d)-[:AFFECTS]->(s:System) " +
                        "OPTIONAL MATCH (d)-[:MENTIONED_IN]->(doc:Document) " +
                        "RETURN d.name AS decision, collect(DISTINCT i.name) AS triggers, collect(DISTINCT p.name) AS decidedBy, " +
                        "collect(DISTINCT s.name) AS affects, collect(DISTINCT doc.name) AS docs",
                Map.of("topic", topic.toLowerCase().trim()));

        List<DecisionTrace> out = new ArrayList<>();
        for (Record r : rs) {
            Object decision = r.getValue("decision");
            out.add(new DecisionTrace(String.valueOf(decision), strings(r.getValue("triggers")),
                    strings(r.getValue("decidedBy")), strings(r.getValue("affects")), strings(r.getValue("docs"))));
        }

        // traceDecision
        List<String> edges = new ArrayList<>();
        for (DecisionTrace t : out) {
            t.triggers().forEach(x -> edges.add(edge(x, "TRIGGERED", t.decision())));
            t.decidedBy().forEach(x -> edges.add(edge(x, "DECIDED", t.decision())));
            t.affects().forEach(x -> edges.add(edge(t.decision(), "AFFECTS", x)));
            t.mentionedIn().forEach(x -> edges.add(edge(t.decision(), "MENTIONED_IN", x)));
        }
        pathRecorder.record("traceDecision", topic, edges);

        return out;
    }

    private static List<String> strings(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof List<?> list) for (Object o : list) out.add(String.valueOf(o));
        return out;
    }
}
