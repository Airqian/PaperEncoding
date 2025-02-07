package zNIWGraph.graph;

import zHyperISO.WeightedGraph;

import java.util.*;

public class NIWHypergraph {
    private List<Integer> nodeLabels;     // 顶点标签map

    private Map<Integer, List<Integer>> idToEdge;         // 超边id到超边的映射map

    private Map<List<Integer>, Integer> edgeToId;         // 超边到超边id的映射map

    private Map<Integer, Set<Integer>> vertexToEdges;    // 顶点到超边的倒排索引

    public NIWHypergraph() {}

    public NIWHypergraph(Map<Integer, List<Integer>> idToEdge, Map<List<Integer>, Integer> edgeToId,
                         Map<Integer, Set<Integer>> vertexToEdges, List<Integer> nodeLabels) {
        this.idToEdge = idToEdge;
        this.edgeToId = edgeToId;
        this.vertexToEdges = vertexToEdges;
        this.nodeLabels = nodeLabels;
    }

    // 构建二元邻居交叉权重图
    public WeightedGraph build_weighted_graph_inverted() {
        long start = System.nanoTime();

        Map<Integer, Map<Integer, Integer>> vertexNumWGraph = new HashMap<>();
        Map<Integer, Map<Integer, Set<Integer>>> labelWGraph = new HashMap<>();

        // 遍历倒排索引计算公共顶点数
        for (Map.Entry<Integer, Set<Integer>> entry : vertexToEdges.entrySet()) {
            int vertex = entry.getKey(); // 公共顶点
            Set<Integer> hyperedges = entry.getValue();

            // 遍历超边集合中的所有超边对
            List<Integer> hyperedgeList = new ArrayList<>(hyperedges);
            for (int i = 0; i < hyperedgeList.size(); i++) {
                for (int j = i + 1; j < hyperedgeList.size(); j++) {
                    int hyperedge1 = hyperedgeList.get(i);
                    int hyperedge2 = hyperedgeList.get(j);

                    // 更新图中的公共顶点数
                    vertexNumWGraph.computeIfAbsent(hyperedge1, k -> new HashMap<>());
                    vertexNumWGraph.computeIfAbsent(hyperedge2, k -> new HashMap<>());
                    labelWGraph.putIfAbsent(hyperedge1, new HashMap<>());
                    labelWGraph.get(hyperedge1).putIfAbsent(hyperedge2, new HashSet<>());
                    labelWGraph.putIfAbsent(hyperedge2, new HashMap<>());
                    labelWGraph.get(hyperedge2).putIfAbsent(hyperedge1, new HashSet<>());

                    // 计算公共顶点数
                    int commonVerticesCount = vertexNumWGraph.get(hyperedge1).getOrDefault(hyperedge2, 0) + 1;

                    // 更新边权重图
                    vertexNumWGraph.get(hyperedge1).put(hyperedge2, commonVerticesCount);
                    vertexNumWGraph.get(hyperedge2).put(hyperedge1, commonVerticesCount);
                    labelWGraph.get(hyperedge1).get(hyperedge2).add(this.getNodeLabel(vertex));
                    labelWGraph.get(hyperedge2).get(hyperedge1).add(this.getNodeLabel(vertex));
                }
            }
        }
        long end = System.nanoTime();
        System.out.printf("--- build_weighted_graph_inverted: %.2fms\n", ((end - start) * 1.0 / 1000_1000));

        return new WeightedGraph(vertexNumWGraph, labelWGraph);
    }

    public int getNodeLabel(int vertexId) {
        return this.nodeLabels.get(vertexId);
    }

    public void setIdToEdge(Map<Integer, List<Integer>> idToEdge) {
        this.idToEdge = idToEdge;
    }

    public void setEdgeToId(Map<List<Integer>, Integer> edgeToId) {
        this.edgeToId = edgeToId;
    }

    public void setVertexToEdges(Map<Integer, Set<Integer>> vertexToEdges) {
        this.vertexToEdges = vertexToEdges;
    }
}
