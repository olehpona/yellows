package org.example.executor;

import org.example.context.WriteContextValue;
import org.example.context.path.IntPath;
import org.example.context.path.StringPath;
import org.example.context.path.utils.PathCompiler;
import org.example.context.path.utils.SymbolTable;
import org.example.context.values.MissingValue;
import org.example.graph.NodeData;
import org.example.graph.SubGraph;
import org.example.graph.internal.CompiledNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RunContextTest {
    public static class TestSubGraphBuilder {
        private int maxLocalId = -1;
        private final Map<Integer, List<Integer>> edges = new HashMap<>();
        private final Map<Integer, Integer> globalIds = new HashMap<>();

        public static TestSubGraphBuilder graph() {
            return new TestSubGraphBuilder();
        }

        public NodeBuilder node(int localId) {
            maxLocalId = Math.max(maxLocalId, localId);
            return new NodeBuilder(this, localId);
        }

        public static class NodeBuilder {
            private final TestSubGraphBuilder parent;
            private final int localId;

            NodeBuilder(TestSubGraphBuilder parent, int localId) {
                this.parent = parent;
                this.localId = localId;
                parent.edges.putIfAbsent(localId, new ArrayList<>());
            }

            public NodeBuilder pointsTo(int... childIds) {
                for (int child : childIds) {
                    parent.edges.get(localId).add(child);
                    parent.maxLocalId = Math.max(parent.maxLocalId, child);
                }
                return this;
            }

            public NodeBuilder globalId(int globalId) {
                parent.globalIds.put(localId, globalId);
                return this;
            }

            public NodeBuilder node(int nextLocalId) {
                return parent.node(nextLocalId);
            }

            public SubGraph build() {
                return parent.build();
            }
        }

        public SubGraph build() {
            int nodeCount = maxLocalId + 1;

            int[] inDegree = new int[nodeCount];
            int[] childrenStart = new int[nodeCount + 1];
            List<Integer> flatList = new ArrayList<>();
            int[] localToGlobal = new int[nodeCount];

            for (int i = 0; i < nodeCount; i++) {
                childrenStart[i] = flatList.size();

                if (edges.containsKey(i)) {
                    for (int child : edges.get(i)) {
                        flatList.add(child);
                        inDegree[child]++;
                    }
                }
            }
            childrenStart[nodeCount] = flatList.size();

            int[] childrenFlat = flatList.stream().mapToInt(Integer::intValue).toArray();

            for (int i = 0; i < nodeCount; i++) {
                localToGlobal[i] = globalIds.getOrDefault(i, i);
            }

            return new SubGraph(inDegree, childrenStart, childrenFlat, localToGlobal);
        }
    }

    @Test
    void mergeOutput_decrementInDegree_returnReadyNodes() {
        SymbolTable nodeNames = new SymbolTable();
        SymbolTable dict = new SymbolTable();

        int idA = nodeNames.register("a");
        int idB = nodeNames.register("b");

        SubGraph graph = TestSubGraphBuilder.graph()
                .node(0).pointsTo(1).globalId(idA)
                .node(1).globalId(idB).build();

        IntPath globalPath = PathCompiler.compileGlobal("a.b", dict);
        StringPath remotePath = PathCompiler.compileRemote("a");

        NodeData aData = new NodeData(List.of(),
                List.of(
                        new CompiledNode.PathPair(globalPath, remotePath)
                )
        );

        NodeData bData = new NodeData(List.of(
                new CompiledNode.PathPair(globalPath, remotePath)
        ), List.of());

        WriteContextValue root = Mockito.mock(WriteContextValue.class);

        RunContext runContext = new RunContext(root, graph, List.of(aData, bData), dict, nodeNames);

        WriteContextValue outputValue = Mockito.mock(WriteContextValue.class);
        when(outputValue.resolvePath(anyIterable(), dict)).thenReturn(MissingValue.INSTANCE);

        int[] targeted = runContext.mergeOutput(0, outputValue, List.of() );

        assertThat(targeted).containsExactly(1);

        verify(root).putPath(globalPath, dict, MissingValue.INSTANCE);
        verify(outputValue).resolvePath(remotePath, dict);
    }

    @Test
    @DisplayName("Test branch canceling when we first trigger cancellation and then wanting node")
    void mergeOutput_nodeCancelledThenWanted_returnReadyNodes() {
        SymbolTable nodeNames = new SymbolTable();
        SymbolTable dict = new SymbolTable();

        int idA = nodeNames.register("a");
        int idB = nodeNames.register("b");
        int idC = nodeNames.register("c");
        int idD = nodeNames.register("d");
        int idE = nodeNames.register("e");


        SubGraph graph = TestSubGraphBuilder.graph()
                .node(0).pointsTo(1,2).globalId(idA)
                .node(1).pointsTo(4).globalId(idB)
                .node(2).pointsTo(3).globalId(idC)
                .node(3).pointsTo(4).globalId(idD)
                .node(4).globalId(idE)
                .build();


        NodeData data = new NodeData(List.of(),List.of());

        WriteContextValue root = Mockito.mock(WriteContextValue.class);
        RunContext runContext = new RunContext(root, graph, List.of(data, data, data, data, data), dict, nodeNames);

        WriteContextValue outputValue = Mockito.mock(WriteContextValue.class);

        var firstNextNodes = runContext.mergeOutput(0, outputValue, List.of("b"));

        assertThat(firstNextNodes).containsExactlyInAnyOrder(1);

        var secondNextNodes = runContext.mergeOutput(1, outputValue, List.of());

        assertThat(secondNextNodes).containsExactlyInAnyOrder(4);
    }

    @Test
    @DisplayName("Test wanting node that marking it ready after branch canceling")
    void mergeOutput_nodeWantedThenCanceled_returnReadyNodes() {
        SymbolTable nodeNames = new SymbolTable();
        SymbolTable dict = new SymbolTable();

        int idA = nodeNames.register("a");
        int idB = nodeNames.register("b");
        int idC = nodeNames.register("c");
        int idD = nodeNames.register("d");
        int idE = nodeNames.register("e");
        int idF = nodeNames.register("f");


        SubGraph graph = TestSubGraphBuilder.graph()
                .node(0).pointsTo(1,2).globalId(idA)
                .node(1).pointsTo(5).globalId(idB)
                .node(2).pointsTo(3, 4).globalId(idC)
                .node(3).pointsTo(5).globalId(idD)
                .node(4).globalId(idE)
                .node(5).globalId(idF)
                .build();


        NodeData data = new NodeData(List.of(),List.of());

        WriteContextValue root = Mockito.mock(WriteContextValue.class);
        RunContext runContext = new RunContext(root, graph, List.of(data, data, data, data, data), dict, nodeNames);

        WriteContextValue outputValue = Mockito.mock(WriteContextValue.class);

        assertThat(runContext.mergeOutput(0, outputValue, List.of())).containsExactlyInAnyOrder(1, 2);
        assertThat(runContext.mergeOutput(1, outputValue, List.of())).isEmpty();
        assertThat(runContext.mergeOutput(2, outputValue, List.of("e"))).containsExactlyInAnyOrder(4, 5);
    }

    @Test
    void mergeOutput_wantedIllegalTransaction_throwIllegalTransactionError() {
        SymbolTable nodeNames = new SymbolTable();
        SymbolTable dict = new SymbolTable();

        int idA = nodeNames.register("a");
        int idB = nodeNames.register("b");

        SubGraph graph = TestSubGraphBuilder.graph()
                .node(0).globalId(idA)
                .node(1).globalId(idB).build();

        NodeData data = new NodeData(List.of(),List.of());

        WriteContextValue root = Mockito.mock(WriteContextValue.class);
        RunContext runContext = new RunContext(root, graph, List.of(data, data), dict, nodeNames);

        WriteContextValue outputValue = Mockito.mock(WriteContextValue.class);
        ExecutorException exception = assertThrows(ExecutorException.class, () -> runContext.mergeOutput(0, outputValue, List.of("b")));
        assertThat(exception.getExceptionCode()).isEqualTo(ExecutorExceptionCode.ERR_ILLEGAL_TRANSITION);
    }

    @Test
    void mergeOutput_moreThanEdges_throwStateCorruption() {
        SymbolTable nodeNames = new SymbolTable();
        SymbolTable dict = new SymbolTable();

        int idA = nodeNames.register("a");
        int idB = nodeNames.register("b");

        SubGraph graph = TestSubGraphBuilder.graph()
                .node(0).pointsTo(1).globalId(idA)
                .node(1).globalId(idB).build();

        NodeData data = new NodeData(List.of(),List.of());

        WriteContextValue root = Mockito.mock(WriteContextValue.class);
        RunContext runContext = new RunContext(root, graph, List.of(data, data), dict, nodeNames);

        WriteContextValue outputValue = Mockito.mock(WriteContextValue.class);

        runContext.mergeOutput(0, outputValue, List.of());
        ExecutorException exception = assertThrows(ExecutorException.class, () -> runContext.mergeOutput(0, outputValue, List.of()));
        assertThat(exception.getExceptionCode()).isEqualTo(ExecutorExceptionCode.ERR_INTERNAL_STATE_CORRUPTION);
    }

    @Test
    void buildInputContext_correctOrder_returnCorrectContext() {
        SymbolTable nodeNames = new SymbolTable();
        SymbolTable dict = new SymbolTable();

        int idA = nodeNames.register("a");
        int idB = nodeNames.register("b");

        SubGraph graph = TestSubGraphBuilder.graph()
                .node(0).pointsTo(1).globalId(idA)
                .node(1).globalId(idB).build();

        IntPath globalPath = PathCompiler.compileGlobal("a.b", dict);
        StringPath remotePath = PathCompiler.compileRemote("a");

        NodeData aData = new NodeData(List.of(
                new CompiledNode.PathPair(globalPath, remotePath)
        ), List.of());

        WriteContextValue root = Mockito.mock(WriteContextValue.class);

        RunContext runContext = new RunContext(root, graph, List.of(aData), dict, nodeNames);

        WriteContextValue inputValue = Mockito.mock(WriteContextValue.class);
        when(root.resolvePath(anyIterable(), dict)).thenReturn(MissingValue.INSTANCE);

        runContext.buildInputContext(0, inputValue);
        verify(root).resolvePath(globalPath, dict);
        verify(inputValue).putPath(remotePath, dict,MissingValue.INSTANCE);
    }

    @Test
    void buildInputContext_incorrectOrder_throwNodeNotReadyErr() {
        SymbolTable nodeNames = new SymbolTable();
        SymbolTable dict = new SymbolTable();

        int idA = nodeNames.register("a");
        int idB = nodeNames.register("b");

        SubGraph graph = TestSubGraphBuilder.graph()
                .node(0).pointsTo(1).globalId(idA)
                .node(1).globalId(idB).build();

        IntPath globalPath = PathCompiler.compileGlobal("a.b", dict);
        StringPath remotePath = PathCompiler.compileRemote("a");

        NodeData data = new NodeData(List.of(
                new CompiledNode.PathPair(globalPath, remotePath)
        ), List.of());

        WriteContextValue root = Mockito.mock(WriteContextValue.class);

        RunContext runContext = new RunContext(root, graph, List.of(data, data), dict, nodeNames);

        WriteContextValue inputValue = Mockito.mock(WriteContextValue.class);
        when(root.resolvePath(anyIterable(), dict)).thenReturn(MissingValue.INSTANCE);

        ExecutorException exception = assertThrows(ExecutorException.class, () ->runContext.buildInputContext(1, inputValue));
        assertThat(exception.getExceptionCode()).isEqualTo(ExecutorExceptionCode.ERR_NODE_NOT_READY);
    }
}
