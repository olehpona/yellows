package org.example.context;

import java.util.List;
import java.util.Map;

public record OutputContext(Map<String, Object> changes, List<String> nextHint) {
}
