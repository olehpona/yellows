package com.github.olehpona.yellows.core.graph;

import java.util.Map;
import java.util.Set;

public record Node(
        String name,
        String plugin,
        String routine,
        Map<String,String> input,
        Map<String, String> output,
        Set<String> next
) {}
