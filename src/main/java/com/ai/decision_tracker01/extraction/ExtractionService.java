package com.ai.decision_tracker01.extraction;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExtractionService {

    private static final String SYSTEM = """
            You extract a knowledge graph from a company document.
            Return a list of entities and a list of relations.

            Entity types: Person, Team, System, Decision, Incident.

            Relation types, written as FROM -> TO:
            MEMBER_OF: Person -> Team
            OWNS: Team -> System
            REVIEWS: Team -> System
            DEPENDS_ON: System -> System
            DECIDED: Person -> Decision
            TRIGGERED: Incident -> Decision
            AFFECTS: Decision or Incident -> System
            SUPERSEDES: Decision -> Decision

            Rules:
            - Use only these types. Never invent new ones.
            - Include only what the document states or clearly implies. If unsure, leave it out.
            - Name people by first name only, for example Priya, not "Priya (Platform team)".
            - Name teams without the word team, for example Platform.
            - Name systems as the document writes them, for example Auth Service.
            - Name an incident as a short event description plus its date, for example Auth session loss, 14 March 2026.
            - Name a decision with a short imperative title, for example Move session storage from Redis to Postgres.
            - A team OWNS a system only if the document says it owns it or is responsible for it. A team that only reviews a system uses REVIEWS.
            - In relations, from and to must exactly match a name in your entity list.
            - A System is a service or component that the company builds or runs, such as Auth Service or Session Store. Do not create a System for a third-party technology or product such as Redis, Postgres, or Kafka. Those are not entities.
            - If a decision or incident concerns a technology, link it to the company System that uses that technology. For example, a decision to move session storage away from Redis AFFECTS the Session Store system.
            - A Decision is a choice that the document says someone made or approved, such as "we will move", "decided", or "accepted". Action items, follow-ups, to-do lists, proposals, and plans for future work are NOT decisions. Do not create a Decision for them.
            - A document with no decision, such as a postmortem or an ownership list, should produce no Decision entities unless it records a past decision made earlier.
            - Name a decision by the choice itself, including key details, for example "Retry failed invoices up to 3 times, with a 5 minute wait", not by the task of carrying it out.
            """;

    private final ChatClient chatClient;

    public ExtractionService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public Extraction extract(String documentText, Map<String, List<String>> existing) {
        String known = existing.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> e.getKey() + ": " + String.join("; ", e.getValue()))
                .collect(Collectors.joining("\n"));

        String system = known.isBlank() ? SYSTEM : SYSTEM + """

            Entities already in the graph:
            """ + known + """

            If the document refers to one of these, use its exact name above, even if the
            document words it differently or leaves out a detail such as a year.
            Create a new entity only if it is truly a different thing.
            """;

        return chatClient.prompt().system(system).user(documentText).call().entity(Extraction.class);
    }
}
