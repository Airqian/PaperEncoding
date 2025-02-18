package zNIWGraph.graph;

import zNIWGraph.index.IntersectionLabelGraph;

import java.util.*;
import java.util.stream.Collectors;

public class NIWHypergraph {
    private boolean ifQueryGraph;

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
    public IntersectionLabelGraph build_weighted_graph_inverted() {
        long start = System.nanoTime();

        Map<Integer, Map<Integer, Integer>> vertexNumWGraph = new HashMap<>();
        Map<Integer, Map<Integer, Map<Integer, Integer>>> labelWGraph = new HashMap<>();

        // 遍历倒排索引计算公共顶点数
        for (Map.Entry<Integer, Set<Integer>> entry : vertexToEdges.entrySet()) {
            int vertex = entry.getKey(); // 公共顶点id
            Set<Integer> hyperedges = entry.getValue(); // 公共顶点所在超边id列表

            if (hyperedges.size() == 1)
                continue;

            // 遍历超边集合中的所有超边对
            List<Integer> hyperedgeList = new ArrayList<>(hyperedges);
            for (int i = 0; i < hyperedgeList.size(); i++) {
                for (int j = i + 1; j < hyperedgeList.size(); j++) {
                    int hyperedge1 = hyperedgeList.get(i);
                    int hyperedge2 = hyperedgeList.get(j);

                    // 更新图中的公共顶点数
                    vertexNumWGraph.computeIfAbsent(hyperedge1, k -> new HashMap<>());
                    vertexNumWGraph.computeIfAbsent(hyperedge2, k -> new HashMap<>());


                    // 计算公共顶点数
                    int commonVerticesCount = vertexNumWGraph.get(hyperedge1).getOrDefault(hyperedge2, 0) + 1;

                    // 更新二元邻居交叉权重图
                    vertexNumWGraph.get(hyperedge1).put(hyperedge2, commonVerticesCount);
                    vertexNumWGraph.get(hyperedge2).put(hyperedge1, commonVerticesCount);

                    if (!labelWGraph.containsKey(hyperedge1) || !labelWGraph.get(hyperedge1).containsKey(hyperedge2)) {
                        // 计算两条超边的公共标签
                        labelWGraph.putIfAbsent(hyperedge1, new HashMap<>());
                        labelWGraph.get(hyperedge1).putIfAbsent(hyperedge2, new HashMap<>());
                        labelWGraph.putIfAbsent(hyperedge2, new HashMap<>());
                        labelWGraph.get(hyperedge2).putIfAbsent(hyperedge1, new HashMap<>());

                        Map<Integer, Integer> neighborLabelMap = computeNeighborLabel(hyperedge1, hyperedge2);
                        labelWGraph.get(hyperedge1).put(hyperedge2, neighborLabelMap);
                        labelWGraph.get(hyperedge2).put(hyperedge1, neighborLabelMap);
                    }
                }
            }
        }
        long end = System.nanoTime();
        System.out.printf("%.2fms\n", ((end - start) * 1.0 / 1000_1000));

        return new IntersectionLabelGraph(vertexNumWGraph, labelWGraph);
    }

    private Map<Integer, Integer> computeNeighborLabel(int hyperedge1, int hyperedge2) {
        Map<Integer, Integer> map = new HashMap<>();
        List<Integer> edge1 = this.idToEdge.get(hyperedge1);
        List<Integer> edge2 = this.idToEdge.get(hyperedge2);

        Set<Integer> set = new HashSet<>(edge1);
        Set<Integer> commonVertex = new HashSet<>();
        for (int num : edge2) {
            if (set.contains(num) &&!commonVertex.contains(num)) {
                commonVertex.add(num);
            }
        }

        List<Integer> labelList = this.ifQueryGraph == true ? commonVertex.stream().map(i -> this.nodeLabels.get(i)).collect(Collectors.toList()) :
                commonVertex.stream().map(i -> this.nodeLabels.get(i - 1)).collect(Collectors.toList());

        for (int label : labelList) {
            map.put(label, map.getOrDefault(label, 0) + 1);
        }

        return map;
    }

    public List<Integer> getEdge(int id) {
        return this.idToEdge.get(id);
    }

    // edgeToId在中间有变化
    public int getEdgeId(List<Integer> edge) {
        int edgeId = 0;
        if (this.edgeToId != null)
            edgeId = this.edgeToId.get(edge);
        return edgeId;
    }

    public int getDataNodeLabel(int vertexId) {
        return this.nodeLabels.get(vertexId - 1);
    }

    public int getQueryNodeLabel(int vertexId) {
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

    public void setIfQueryGraph(boolean ifQueryGraph) {
        this.ifQueryGraph = ifQueryGraph;
    }

    public Map<List<Integer>, Integer> getEdgeToId() {
        return edgeToId;
    }

    public Map<Integer, List<Integer>> getIdToEdge() {
        return idToEdge;
    }
}
