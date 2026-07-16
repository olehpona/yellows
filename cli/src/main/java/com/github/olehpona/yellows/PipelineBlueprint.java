package com.github.olehpona.yellows;

import com.github.olehpona.yellows.core.graph.Node;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public record PipelineBlueprint(List<Node> nodes, JsonNode constants, Map<String, List<Node>> routines) {}
