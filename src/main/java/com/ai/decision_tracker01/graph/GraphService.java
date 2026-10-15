package com.ai.decision_tracker01.graph;

import com.falkordb.Driver;
import com.falkordb.ResultSet;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GraphService {

    private final Driver driver;

    public GraphService(Driver driver) {
        this.driver = driver;
    }

    public ResultSet query(String graph, String cypher, Map<String, Object> params) {
        return driver.graph(graph).query(cypher, params);
    }
}
