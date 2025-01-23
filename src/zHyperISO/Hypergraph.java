package zHyperISO;

import jdk.nashorn.internal.ir.IfNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

// 定义超图类
class Hypergraph {
    private Map<Integer, Integer> nodeLabels;
    private Map<Integer, List<Integer>> hyperedges; // key 是超边id
    private Map<Integer, Set<Integer>> invertedIndex;  // 顶点到超边的倒排索引
    private Map<Integer, Integer> edgeLabels;   // 超边标签（暂时没用）


    public Hypergraph() {

    }

    public Hypergraph(Map<Integer, Integer> nodeLabels, Map<Integer, List<Integer>> hyperedges, Map<Integer, Set<Integer>> invertedIndex) {
        this.nodeLabels = nodeLabels;
        this.hyperedges = hyperedges;
        this.invertedIndex = invertedIndex;
    }


    // 获取超边的邻居超边集合
    public List<List<Integer>> getHyperedgeNeighbors(List<Integer> he) {
        List<List<Integer>> neighbors = new ArrayList<>();

        for (List<Integer> otherHe : hyperedges.values()) {
            Set<Integer> intersection = new HashSet<>(he);
            intersection.retainAll(otherHe);

            if (!intersection.isEmpty()) {
                neighbors.add(otherHe);
            }
        }
        return neighbors;
    }

    // 获取顶点所在的超边集合
    public List<List<Integer>> getVertexHyperedges(int v) {
        List<List<Integer>> result = new ArrayList<>();
        for (List<Integer> he : hyperedges.values()) {
            if (he.contains(v)) {
                result.add(he);
            }
        }
        return result;
    }

    public int getNodeLabel(int vertexId) {
        return this.nodeLabels.get(vertexId);
    }


    /**
     * 权重图的构建方式（都实现）
     * 1 对每一条超边都用遍历图的方式找出邻边然后构建（时间复杂度最高，O(n^2)）
     * 2 构建顶点到超边的倒排索引，通过顶点倒查超边（时间复杂度较低，空间占用高）
     */
    public WeightedGraph build_weighted_graph_normal() {
        long start = System.nanoTime();

        Map<Integer, Map<Integer, Integer>> vertexNumWGraph = new HashMap<>();
        Map<Integer, Map<Integer ,Set<Integer>>> labelWGraph = new HashMap<>();

        for (Map.Entry<Integer, List<Integer>> entry1 : hyperedges.entrySet()) {
            int edgeId = entry1.getKey();
            vertexNumWGraph.putIfAbsent(edgeId, new HashMap<>());

            for (Map.Entry<Integer, List<Integer>> entry2 : hyperedges.entrySet()) {
                int neighborEdgeId = entry2.getKey();
                List<Integer> neighborEdge = entry2.getValue();

                if (edgeId == neighborEdgeId)
                    continue;

                Set<Integer> edge1 = new HashSet<>(entry1.getValue());
                Set<Integer> edge2 = new HashSet<>(neighborEdge);
                edge1.retainAll(edge2);

                if (edge1.size() != 0) {
                    vertexNumWGraph.get(edgeId).put(neighborEdgeId, edge1.size());

                    for (int vertex : edge1) {
                        labelWGraph.putIfAbsent(edgeId, new HashMap<>());
                        labelWGraph.get(edgeId).putIfAbsent(neighborEdgeId, new HashSet<>());
                        labelWGraph.get(edgeId).get(neighborEdgeId).add(this.getNodeLabel(vertex));
                    }
                }

            }
        }

        long end = System.nanoTime();
        System.out.printf("--- build_weighted_graph_normal: %.2fms\n", ((end - start) * 1.0 / 1000_1000));

        return new WeightedGraph(vertexNumWGraph, labelWGraph);
    }

    public WeightedGraph build_weighted_graph_inverted() {
        long start = System.nanoTime();

        Map<Integer, Map<Integer, Integer>> vertexNumWGraph = new HashMap<>();
        Map<Integer, Map<Integer, Set<Integer>>> labelWGraph = new HashMap<>();

        // 遍历倒排索引计算公共顶点数
        for (Map.Entry<Integer, Set<Integer>> entry : invertedIndex.entrySet()) {
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
        System.out.printf("--- build_weighted_graph_inverted: %.2fms", ((end - start) * 1.0 / 1000_1000));

        return new WeightedGraph(vertexNumWGraph, labelWGraph);
    }

    public List<Integer> getHyperEdgeById(int edgeId) {
        return this.hyperedges.get(edgeId);
    }

    public int getVertexNum(int hyperedgeId) {
        return this.hyperedges.get(hyperedgeId).size();
    }

    public int num_edges() {
        return this.hyperedges.size();
    }

    public int intersectionNum(int edgeId1, int edgeId2) {
        return intersectionSize(this.getHyperEdgeById(edgeId1), this.getHyperEdgeById(edgeId2));
    }

    private static int intersectionSize(List<Integer> list1, List<Integer> list2) {
        Set<Integer> set1 = new HashSet<>(list1);
        Set<Integer> set2 = new HashSet<>(list2);
        set1.retainAll(set2);
        return set1.size();
    }

    public void status() {
        System.out.println("Number of nodes: " + this.nodeLabels.size());
        System.out.println("Number of edges: " + this.hyperedges.size());
        System.out.printf("Graph Size: %.1fKB, Inverted Index Size: %.1fKB\n", (graph_size() * 1.0 / 1000), (invertedIndex_size() * 1.0 / 1000));
    }

    public int graph_size() {
        int idSize = Integer.BYTES;

        int labels_size = (this.nodeLabels.size() * 2 + 4) * idSize;
        int edges_size = this.hyperedges.entrySet().stream().mapToInt(entry -> entry.getKey() + entry.getValue().size() + 4).sum() * idSize;

        return labels_size + edges_size;
    }

    public int invertedIndex_size() {
        int intSize = Integer.BYTES;
        return invertedIndex.size() != 0 ? invertedIndex.values().stream().mapToInt(v -> intSize * (v.size() + 3)).sum() : 0;
    }

    public void setHyperedges(Map<Integer, List<Integer>> hyperedges) {
        this.hyperedges = hyperedges;
    }

    public void setInvertedIndex(Map<Integer, Set<Integer>> invertedIndex) {
        this.invertedIndex = invertedIndex;
    }

    public Map<Integer, List<Integer>> getHyperedges() {
        return hyperedges;
    }

    public void setNodeLabels(Map<Integer, Integer> nodeLabels) {
        this.nodeLabels = nodeLabels;
    }

    public Map<Integer, Integer> getNodeLabels() {
        return nodeLabels;
    }

    // 静态方法，用于从 JSONObject 创建 GraphData 实例
    public static Hypergraph fromJSONObject(JSONObject obj) {
        JSONArray labelsArray = obj.getJSONArray("labels"); // 读取顶点标签
        Map<Integer, Integer> nodeLabels = new HashMap<>();

        for (int i = 0; i < labelsArray.length(); i++) {
            nodeLabels.put(i+1, labelsArray.getInt(i));
        }

        JSONArray edgesArray = obj.getJSONArray("edges");  // 读取各条超边并构建顶点到超边的倒排索引
        Map<Integer, List<Integer>> hyperedges = new HashMap<>();
        Map<Integer, Set<Integer>> invertedIndex = new HashMap<>();

        for (int i = 0; i < edgesArray.length(); i++) {
            JSONArray edge = edgesArray.getJSONArray(i);
            List<Integer> edgeList = new ArrayList<>();

            for (int j = 0; j < edge.length(); j++) {
                int vertex = edge.getInt(j);
                edgeList.add(vertex);

                invertedIndex.putIfAbsent(vertex, new HashSet<>());
                invertedIndex.get(vertex).add(i+1);
            }
            hyperedges.put(i+1, edgeList);
        }

        return new Hypergraph(nodeLabels, hyperedges, invertedIndex);
    }
}
