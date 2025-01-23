package zHyperISO;

import java.util.*;

// 实现 HOrder 的顶点优先排序策略
class HOrder {
    public static List<List<Integer>> vertexPriorityOrder(Hypergraph queryHg, Map<List<Integer>, List<List<Integer>>> candidates) {
        // 选择具有最大邻居超边的超边作为起始超边
        int maxNeighbors = -1;
        List<Integer> startHe = null;
        for (List<Integer> he : queryHg.getHyperedges().values()) {
            List<List<Integer>> neighbors = queryHg.getHyperedgeNeighbors(he);
            if (neighbors.size() > maxNeighbors) {
                maxNeighbors = neighbors.size();
                startHe = he;
            }
        }

        // 构建超边排序顺序（这里简化，实际可能需要更完善的算法，如扩展 Prim 算法）
        List<List<Integer>> order = new ArrayList<>();
        order.add(startHe);
        Set<List<Integer>> visited = new HashSet<>();
        visited.add(startHe);
        while (visited.size() < queryHg.getHyperedges().size()) {
            List<Integer> currentHe = order.get(order.size() - 1);
            List<List<Integer>> neighbors = queryHg.getHyperedgeNeighbors(currentHe);
            List<List<Integer>> unvisitedNeighbors = new ArrayList<>();
            for (List<Integer> n : neighbors) {
                if (!visited.contains(n)) {
                    unvisitedNeighbors.add(n);
                }
            }
            if (!unvisitedNeighbors.isEmpty()) {
                List<Integer> nextHe = null;
                int minSize = Integer.MAX_VALUE;
                for (List<Integer> unvisited : unvisitedNeighbors) {
                    if (candidates.get(unvisited).size() < minSize) {
                        minSize = candidates.get(unvisited).size();
                        nextHe = unvisited;
                    }
                }
                order.add(nextHe);
                visited.add(nextHe);
            }
        }
        return order;
    }
}
