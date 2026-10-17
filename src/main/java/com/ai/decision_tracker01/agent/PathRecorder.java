package com.ai.decision_tracker01.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PathRecorder {

    public record Step(String tool, String input, List<String> edges) {}

    private final ThreadLocal<List<Step>> steps = ThreadLocal.withInitial(ArrayList::new);

    public void start() { steps.set(new ArrayList<>()); }
    public void record(String tool, String input, List<String> edges) { steps.get().add(new Step(tool, input, edges)); }
    public List<Step> steps() { return new ArrayList<>(steps.get()); }
    public void clear() { steps.remove(); }
}
