package org.example;

import org.example.graph.Graph;
import org.example.graph.GraphBuilder;
import org.example.graph.Node;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        parse();
    }

    static void create() {
        int LAYERS = 1;
        int NODES_PER_LAYER = 1000000;

        Random random = new Random();
        List<Map<String, Object>> graph = new ArrayList<>();

        System.out.println("Generating graph...");

        for (int layer = 0; layer < LAYERS; layer++) {
            for (int i = 0; i < NODES_PER_LAYER; i++) {
                String nodeName = "L" + layer + "_N" + i;

                // Читання та Записи (щоб алгоритм працював)
                Map<String, String> inputs = Map.of("in1", "global.config");
                Map<String, String> outputs = Map.of("out1", "layer" + layer + "." + nodeName + ".data");

                // З'єднання: кожна нода кидає 2 стрілки в НАСТУПНИЙ шар
                List<String> nextNodes = new ArrayList<>();
                if (layer < LAYERS - 1) { // Останній шар - це листки (SINK)
                    int target1 = random.nextInt(NODES_PER_LAYER);
                    int target2 = random.nextInt(NODES_PER_LAYER);
                    nextNodes.add("L" + (layer + 1) + "_N" + target1);
                    if (target1 != target2) {
                        nextNodes.add("L" + (layer + 1) + "_N" + target2);
                    }
                }

                // Пакуємо в мапу (яка ідеально ляже в JSON)
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("name", nodeName);
                node.put("input", inputs);
                node.put("output", outputs);
                node.put("next", nextNodes);

                graph.add(node);
            }
        }

        // Пишемо у файл через Jackson
        ObjectMapper mapper = new ObjectMapper();
        File file = new File("benchmark_graph.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, graph);

        System.out.println("Done! Generated " + graph.size() + " nodes.");
        System.out.println("File size: " + file.length() / (1024 * 1024) + " MB");
    }

    static void parse() {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File("benchmark_graph.json");

        System.out.println("1. Parsing JSON...");
        long parseStart = System.currentTimeMillis();

        // Jackson автоматично збере твій JSON в List<Node>
        List<Node> nodes = mapper.readValue(file, new TypeReference<List<Node>>() {});

        long parseTime = System.currentTimeMillis() - parseStart;
        System.out.println("Parsed " + nodes.size() + " nodes in " + parseTime + " ms.");

        System.out.println("2. Running GraphBuilder Algorithm...");
        // Для прогріву JVM (JIT-компілятора) краще запустити алгоритм кілька разів
        for (int i = 0; i < 3; i++) {
            System.gc(); // Трохи чистимо пам'ять перед тестом
            long buildStart = System.currentTimeMillis();

            try {
                Graph graph = GraphBuilder.buildGraph(nodes, 5);
                long buildTime = System.currentTimeMillis() - buildStart;
                System.out.println("Run " + (i + 1) + ": SUCCESS in " + buildTime + " ms.");
            } catch (Exception e) {
                System.err.println("FAILED: " + e.getMessage());
                break;
            }
        }
    }
}
