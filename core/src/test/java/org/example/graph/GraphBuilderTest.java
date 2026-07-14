package org.example.graph;

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
            return new Node(name,"",null, in, out, next);
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

        Graph graph = GraphBuilder.buildGraph(List.of(a,b,c), Map.of());

        int idA = graph.nodeNames().getInt("a");
        int idB = graph.nodeNames().getInt("b");
        int idC = graph.nodeNames().getInt("c");

        assertThat(graph.subGraphs()).hasSize(1);
        assertThat(graph.nodes()).hasSize(3);

        var subGraph = graph.subGraphs().getFirst();

        assertThat(subGraph.inDegree()).hasSize(3);
        assertThat(subGraph.inDegree()[0]).isEqualTo(0);
        assertThat(subGraph.inDegree()[1]).isEqualTo(1);
        assertThat(subGraph.inDegree()[2]).isEqualTo(1);

        assertThat(subGraph.localToGlobal()[0]).isEqualTo(idA);
        assertThat(subGraph.localToGlobal()[1]).isEqualTo(idB);
        assertThat(subGraph.localToGlobal()[2]).isEqualTo(idC);
    }

    @Test
    void testDuplicateNodeNames(){
        Node a = node("a").build();
        Node a1 = node("a").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, a1), Map.of());
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_DUPLICATE_NODE);
    }

    @Test
    void testSimpleLoop(){
        Node a = node("a").pointsTo("b").build();
        Node b = node("b").pointsTo("c").build();
        Node c = node("c").pointsTo("a").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c), Map.of());
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
            GraphBuilder.buildGraph(List.of(a, b, d, e), Map.of());
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_LOOP);
    }

    @Test
    void testExactKeyNoCollision(){
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a").pointsTo("d").build();
        Node c = node("c").reads("a").pointsTo("d").build();
        Node d = node("d").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c,d), Map.of()));
    }

    @Test
    void testWriteWriteExactKeyConflict(){
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").writes("a").pointsTo("d").build();
        Node c = node("c").writes("a").pointsTo("d").build();
        Node d = node("d").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c,d), Map.of());
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
            GraphBuilder.buildGraph(List.of(a, b, c,d), Map.of());
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
            GraphBuilder.buildGraph(List.of(a, b, c,d), Map.of());
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
            GraphBuilder.buildGraph(List.of(a, b, c,d), Map.of());
        });
        System.err.println(exception.getMessage());
        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testWriteReadHierarchyChildConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").writes("a").pointsTo("d").build();
        Node c = node("c").reads("a.b").pointsTo("d").build();
        Node d = node("d").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c,d), Map.of());
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testSafeSiblingsNoConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").writes("a.d").pointsTo("d").build();
        Node c = node("c").reads("a.b").pointsTo("d").build();
        Node d = node("d").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c,d), Map.of()));
    }

    @Test
    void testPrefixTrapNoConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").writes("a.b").pointsTo("d").build();
        Node c = node("c").writes("a.bc").pointsTo("d").build();
        Node d = node("d").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c,d), Map.of()));
    }

    @Test
    void testSharedAncestorReadReadNoConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a.b").pointsTo("d").build();
        Node c = node("c").reads("a.b.c").pointsTo("d").build();
        Node d = node("d").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c,d), Map.of()));
    }

    @Test
    void testBrokenAncestorWriteReadConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a.b").pointsTo("d").build();
        Node c = node("c").writes("a.b.c").pointsTo("d").build();
        Node d = node("d").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c,d), Map.of());
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testImplicitRootReadReadNoConflict() {
        Node a = node("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a.b").pointsTo("d").build();
        Node c = node("c").reads("a.c").pointsTo("d").build();
        Node d = node("d").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c,d), Map.of()));
    }

    @Test
    void testUnmergedBranchesWriteWriteConflict() {
        Node a = node("a").writes("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a.b").build();
        Node c = node("c").writes("a.b.c").build();

        GraphException exception = assertThrows(GraphException.class, () -> {
            GraphBuilder.buildGraph(List.of(a, b, c), Map.of());
        });

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testUnmergedBranchesSafeNoConflict() {
        Node a = node("a").pointsTo("b", "c").build();
        Node b = node("b").reads("a.b").build();
        Node c = node("c").reads("a.c").build();

        assertDoesNotThrow(() -> GraphBuilder.buildGraph(List.of(a,b,c), Map.of()));
    }

    @Test
    void testThreeWayMergeConflictOnSecondPair() {
        Node a = node("a").pointsTo("b", "c", "e").build();
        Node b = node("b").writes("a.b").pointsTo("d").build();
        Node c = node("c").reads("a.b").pointsTo("d").build();   // ок з b
        Node e = node("e").writes("a.b.x").pointsTo("d").build(); // конфлікт, але третя гілка
        Node d = node("d").build();

        GraphException exception = assertThrows(GraphException.class, () ->
                GraphBuilder.buildGraph(List.of(a, b, c, e, d), Map.of()));

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testConflictAfterSuccessfulMergePropagates() {
        Node a = node("a").pointsTo("b", "c", "x").build();
        Node b = node("b").reads("a.b").pointsTo("merge").build();
        Node c = node("c").reads("a.c").pointsTo("merge").build();
        Node merge = node("merge").pointsTo("d").build();
        Node x = node("x").writes("a.b").pointsTo("d").build();
        Node d = node("d").build();

        GraphException exception = assertThrows(GraphException.class, () ->
                GraphBuilder.buildGraph(List.of(a, b, c, merge, x, d), Map.of()));

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }

    @Test
    void testIndependentRootsIsolation() {
        Node a1 = node("a1").writes("x").pointsTo("b1", "c1").build();
        Node b1 = node("b1").writes("x").build();
        Node c1 = node("c1").writes("x").build();

        Node a2 = node("a2").writes("y").pointsTo("b2", "c2").build();
        Node b2 = node("b2").reads("y").build();
        Node c2 = node("c2").reads("y").build();

        GraphException exception = assertThrows(GraphException.class, () ->
                GraphBuilder.buildGraph(List.of(a1, b1, c1, a2, b2, c2), Map.of(), 4));

        assertThat(exception.getExceptionCode()).isEqualTo(GraphExceptionCode.ERR_CONTEXT_COLLISION);
    }
}
