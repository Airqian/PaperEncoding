package zHyperISO;

import java.util.*;

// 权重图中的节点为超边id，边为两条超边之间的公共顶点数
public class WeightedGraph {
    // key 是超边id，Pair.key 是超边id，Pair.value 是两条超边之间的公共节点
    // TODO 可以修改数据存储结构才能进行排序以降低时间复杂度
    private Map<Integer, Map<Integer, Integer>> vertexNumWGraph;

    private Map<Integer, Map<Integer, Set<Integer>>> labelWGraph;

    public WeightedGraph() {
        vertexNumWGraph = new HashMap<>();
        labelWGraph = new HashMap<>();
    }

    public WeightedGraph(Map<Integer, Map<Integer, Integer>> vertexNumWGraph, Map<Integer, Map<Integer, Set<Integer>>> labelWGraph) {
        this.vertexNumWGraph = vertexNumWGraph;
        this.labelWGraph = labelWGraph;
    }

    // 给定查询超边获得其初始候选集，目前是用的最野蛮的方法，即遍历权重图
    public List<Integer> getInitialCandidates(List<Integer> hyperedge) {



        return new ArrayList<>();
    }

    public Map<Integer, Map<Integer, Integer>> getvertexNumWGraph() {
        return this.vertexNumWGraph;
    }

    public Set<Integer> getIntersectionLabel(int edge1, int edge2) {
        return labelWGraph.get(edge1).get(edge2);
    }

    public void print() {
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : vertexNumWGraph.entrySet()) {
            int hyperedgeId = entry.getKey();
            for (Map.Entry<Integer, Integer> neighbor : entry.getValue().entrySet()) {
                int neighborId = neighbor.getKey();
                int weight = neighbor.getValue();
                System.out.println("超边 " + hyperedgeId + " 与超边 " + neighborId + " 之间的公共顶点数为: " + weight);
            }
        }
    }

    public boolean isNeighbor(int queryEdgeId, int neighborEdgeId) {
        return vertexNumWGraph.get(queryEdgeId).containsKey(neighborEdgeId);
    }
}
