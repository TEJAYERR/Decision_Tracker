package com.ai.decision_tracker01.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentService {

    public record AgentAnswer(String answer, List<PathRecorder.Step> path) {}

    private static final String SYSTEM = """
            You answer questions about a company's decisions, systems, teams and incidents,
            using the tools provided. Rules:
            - Always use the tools to find facts. Never answer from your own knowledge.
            - Use more than one tool when a question needs it.
            - If the tools return nothing relevant, say you could not find it. Do not guess.
            - Mention the document names that support your answer.
            - When the tools provide who made a decision and what triggered it, always include both in the answer.
            - Keep the answer short and plain.
            """;

    private final ChatClient chatClient;
    private final GraphTools tools;
    private final PathRecorder pathRecorder;

    public AgentService(ChatClient.Builder builder, GraphTools tools, PathRecorder pathRecorder) {
        this.chatClient = builder.build();
        this.tools = tools;
        this.pathRecorder = pathRecorder;
    }

    public AgentAnswer ask(String question) {
        pathRecorder.start();
        try {
            String answer = chatClient.prompt()
                    .system(SYSTEM)
                    .user(question)
                    .tools(tools)
                    .call()
                    .content();
            return new AgentAnswer(answer, pathRecorder.steps());
        } finally {
            pathRecorder.clear();
        }
    }
}