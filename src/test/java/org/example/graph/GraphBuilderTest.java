package org.example.graph;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.example.graph.exceptions.GraphException;
import org.example.graph.exceptions.GraphExceptionCode;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class GraphBuilderTest {
    private static class TestNode {
        private final String name;
        private final Map<String, String> in = new HashMap<>();
        private final Map<String, String> out = new HashMap<>();
        private final Set<String> next = new HashSet<>();

        TestNode(String name) { this.name = name; }

        TestNode reads(String... paths) {
            for (int i = 0; i < paths.length; i++) in.put("in" + i, paths[i]);
            return this;
        }

        TestNode writes(String... paths) {
            for (int i = 0; i < paths.length; i++) out.put("out" + i, paths[i]);
            return this;
        }

        TestNode pointsTo(String... nodes) {
            next.addAll(Arrays.asList(nodes));
            return this;
        }

        Node build() {
            return new Node(name, in, out, next);
        }
    }

    private static TestNode node(String name) {
        return new TestNode(name);
    }

    @Test
    void testValidLinearGraph() {
        Node a = node("a").writes("a", "b").pointsTo("b").build();
        Node b = node("b").reads("a").writes("b", "c").pointsTo("c").build();
        Node c = node("c").reads("a", "b", "c").build();

        Graph graph = GraphBuilder.buildGraph(List.of(a,b,c));

        int idA = graph.dict().register("a");
        int idB = graph.dict().register("b");
        int idC = graph.dict().register("c");

        assertThat(graph.inDegree()).hasSize(1);
        assertThat(idA).isEqualTo(0);

        Int2IntOpenHashMap inDegreeForA = graph.inDegree().get(idA);

        assertThat(inDegreeForA)
                .containsEntry(idB, 1)
                .containsEntry(idC, 1)
                .doesNotContainKey(idA);
    }

    @Test
    void testDuplicateNodeNames(){
        Node a = node("a").build();
        Node a1 = node("a").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, a1));
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_DUPLICATE_NODE);
    }

    @Test
    void testSimpleLoop(){
        Node a = node("a").pointsTo("b").build();
        Node b = node("b").pointsTo("c").build();
        Node c = node("c").pointsTo("a").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c));
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_LOOP);
    }

    @Test
    void testDisconnectedLoop() {
        Node a = node("a").pointsTo("b").build();
        Node b = node("b").build();

        Node d = node("d").pointsTo("e").build();
        Node e = node("e").pointsTo("d").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, d, e));
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_LOOP);
    }

    @Test
    void testExactKeyNoCollision(){
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a").pointsTo("d").build();
        Node c = node("c").reads("a").pointsTo("d").build();
        Node d = node("d").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c,d)));
    }

    @Test
    void testWriteWriteExactKeyConflict(){
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").writes("a").pointsTo("d").build();
        Node c = node("c").writes("a").pointsTo("d").build();
        Node d = node("d").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c,d));
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testReadWriteExactKeyConflict(){
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a").pointsTo("d").build();
        Node c = node("c").writes("a").pointsTo("d").build();
        Node d = node("d").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c,d));
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testWriteWriteHierarchyConflict(){
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").writes("a.b").pointsTo("d").build();
        Node c = node("c").writes("a.b.c").pointsTo("d").build();
        Node d = node("d").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c,d));
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testReadWriteHierarchyParentConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a").pointsTo("d").build();
        Node c = node("c").writes("a.b").pointsTo("d").build();
        Node d = node("d").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c,d));
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testWriteReadHierarchyChildConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").writes("a").pointsTo("d").build();
        Node c = node("c").reads("a.b").pointsTo("d").build();
        Node d = node("d").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c,d));
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testSafeSiblingsNoConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").writes("a.d").pointsTo("d").build();
        Node c = node("c").reads("a.b").pointsTo("d").build();
        Node d = node("d").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c,d)));
    }

    @Test
    void testPrefixTrapNoConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").writes("a.b").pointsTo("d").build();
        Node c = node("c").writes("a.bc").pointsTo("d").build();
        Node d = node("d").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c,d)));
    }

    @Test
    void testSharedAncestorReadReadNoConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a.b").pointsTo("d").build();
        Node c = node("c").reads("a.b.c").pointsTo("d").build();
        Node d = node("d").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c,d)));
    }

    @Test
    void testBrokenAncestorWriteReadConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a.b").pointsTo("d").build();
        Node c = node("c").writes("a.b.c").pointsTo("d").build();
        Node d = node("d").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c,d));
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testImplicitRootReadReadNoConflict() {
        Node a = node("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a.b").pointsTo("d").build();
        Node c = node("c").reads("a.c").pointsTo("d").build();
        Node d = node("d").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c,d)));
    }

    @Test
    void testUnmergedBranchesWriteWriteConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a.b").build();
        Node c = node("c").writes("a.b.c").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c));
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testUnmergedBranchesSafeNoConflict() {
        Node a = node("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a.b").build();
        Node c = node("c").reads("a.c").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c)));
    }
}
