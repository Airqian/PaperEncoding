package zHyperISO;

import zHGMatch.graph.PartitionedEdges;

import java.util.*;
import java.util.stream.Collectors;

// 实现 HOrder 的顶点优先排序策略
class HOrder {
    public static List<Integer> candidatePriorityOrder(Hypergraph queryGraph, Map<Integer, Set<Integer>> candidates) {
        List<Integer> matchingOrder = candidates.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(Set::size)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return matchingOrder;
    }

    public static List<Integer> vertexPriorityOrder(Hypergraph queryGraph, Map<Integer, Set<Integer>> candidates) {
        List<Integer> matchingOrder = new ArrayList<>();

        // 1 选择邻居超边最多的查询超边作为起始超边，如果多条超边具有相同的邻居数量，则随机选择候选集最小的超边
        int maxNeighbors = -1;
        int startEdge = -1;
        Map<Integer, Map<Integer, Integer>> wGraph = queryGraph.getWeightedGraph().getvertexNumWGraph();

        for (Map.Entry<Integer, Map<Integer, Integer>> entry : wGraph.entrySet()) {
            int edgeId = entry.getKey();
            int neighborCount = entry.getValue().size();

            if (neighborCount > maxNeighbors) {
                maxNeighbors = neighborCount;
                startEdge = edgeId;
            }
        }

        // 2 初始化相关数据结构：已访问超边、优先队列、最大生成树
        Set<Integer> visited = new HashSet<>();
        PriorityQueue<Edge> edgeQueue = new PriorityQueue<>(Comparator.comparingInt(e -> -e.weight));
        List<Edge> mstEdges = new ArrayList<>();

        // 3 将起始超边加入 MST
        visited.add(startEdge);
        addEdgesToQueue(wGraph, startEdge, visited, edgeQueue);

        // 4 构建最大生成树
        while (!edgeQueue.isEmpty() && visited.size() < wGraph.size()) {
            Edge maxEdge = edgeQueue.poll();
            int toVertex = maxEdge.to;

            if (!visited.contains(toVertex)) {
                mstEdges.add(maxEdge);
                visited.add(toVertex);
                addEdgesToQueue(wGraph, toVertex, visited, edgeQueue);
            }
        }

        // 5 整理最大生成树生成匹配序列
        for (Edge e : mstEdges) {
            int from = e.from;
            int to = e.to;

            if (!matchingOrder.contains(from))
                matchingOrder.add(from);
            if (!matchingOrder.contains(to))
                matchingOrder.add(to);
        }

        return matchingOrder;
    }

    private static void addEdgesToQueue(Map<Integer, Map<Integer, Integer>> vertexNumWGraph, int edgeId, Set<Integer> visited, PriorityQueue<Edge> edgeQueue) {
        Map<Integer, Integer> neighbors = vertexNumWGraph.get(edgeId);

        for (Map.Entry<Integer, Integer> neighborEntry : neighbors.entrySet()) {
            int neighbor = neighborEntry.getKey();
            int weight = neighborEntry.getValue();

            if (!visited.contains(neighbor)) {
                edgeQueue.offer(new Edge(edgeId, neighbor, weight));
            }
        }
    }

    static class Edge {
        int from;
        int to;
        int weight;

        Edge(int from, int to, int weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return from + " -> " + to + " (" + weight + ")";
        }
    }
}
