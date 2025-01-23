package zHyperISO;

import java.util.*;

// 实现 HFilter 的连接和权重过滤技术
class HFilter {
    /**
     * 超边的初始候补集：由顶点数和邻边数均大于等于对应查询超边的数据超边组成
     * 1. Weight Filtering. 如果两条查询超边能在查询加权图里组成一条超边，那么对应的候补超边也能在数据加权图里组成一条超边，并且候选超边之间的权重要大于等于查询超边之间的权重。
     * 2. Neighboring Filtering. 首先，候选超边hd所有邻边的顶点数大于等于查询超边hq所有邻边的顶点数；
     *                           此外，如果(hd1,hd2)是(hq1,hq2)的映射，那么对于hq1来说，除了hq2，剩下的邻边的所有顶点数不能大于hd1除去hd2所有邻边的顶点数。
     * 3. Index Filtering. 公共顶点约束。假设(hd1,hd2)是(hq1,hq2)的映射，那么(hd1,hd2)的公共标签集合需要包含(hq1,hq2)的公共标签集合。
     *                                比如hq1和hq2有公共顶点 A 和 B，那么合适的hq2的候选超边和hd1也需要有公共顶点 A 和 B
     */
    public static Map<Integer, Set<Integer>> generateCandidates(Hypergraph dataGraph, Hypergraph queryGraph, boolean ifUseInverted) {
        // 初始化候选超边集合，key是查询超边id，value是数据超边id列表
        Map<Integer, Set<Integer>>  candidates = new HashMap<>();

        // 生成数据权重图和查询权重图
        WeightedGraph dataWgraph = ifUseInverted ? dataGraph.build_weighted_graph_inverted() : dataGraph.build_weighted_graph_normal();
        WeightedGraph queryWgraph = ifUseInverted ? queryGraph.build_weighted_graph_inverted() : queryGraph.build_weighted_graph_normal();

        // 1 通过顶点数和邻超边数计算初始候选集，时间复杂度为O(|E|✖️|Eq|)，该过程里融合了 Neighboring Filtering 的第一部分
        for (int i = 0; i < queryGraph.num_edges(); i++)
            candidates.put(i+1, new HashSet<>());

        for (Map.Entry<Integer, Map<Integer, Integer>> queryWeight : queryWgraph.getvertexNumWGraph().entrySet()) {
            int queryEdgeId = queryWeight.getKey();
            int queryEdgeVertexNum = queryGraph.getVertexNum(queryEdgeId);

            // 获得该查询超边的邻超边数以及邻超边所包含的顶点总数
            int queryNeighborEdgeNum = queryWeight.getValue().size();
            int queryNeighborEdgeVertexNum = queryWeight.getValue().entrySet().stream().mapToInt
                    (entry -> queryGraph.getVertexNum(entry.getKey())).sum();

            for (Map.Entry<Integer, Map<Integer, Integer>> dataWeight : dataWgraph.getvertexNumWGraph().entrySet()) {
                int dataEdgeId = dataWeight.getKey();
                int dataEdgeVertexNum = dataGraph.getVertexNum(dataWeight.getKey());
                int dataNeighborEdgeNum = dataWeight.getValue().size();
                int dataNeighborEdgeVertexNum = dataWeight.getValue().entrySet().stream().mapToInt
                        (entry -> dataGraph.getVertexNum(entry.getKey())).sum();

                if (dataEdgeVertexNum >= queryEdgeVertexNum && dataNeighborEdgeNum >= queryNeighborEdgeNum
                        && dataNeighborEdgeVertexNum >= queryNeighborEdgeVertexNum)
                    candidates.get(queryEdgeId).add(dataEdgeId);
            }
        }

        /**
         * 2 Weight Filtering 如果两条查询超边能在查询加权图里组成一条超边，那么对应的候补超边也能在数据加权图里组成一条超边，并且候选超边之间的权重要大于等于查询超边之间的权重。
         *   超边的相邻性可以从权重图中获取，如果如果候选超边 a 和候选超边 b 相邻，那么对应的候选超边 a 和 b 都会被保留
         *
         * 3 Index Filtering. 公共顶点约束。假设(hd1,hd2)是(hq1,hq2)的映射，那么(hd1,hd2)的公共标签集合需要包含(hq1,hq2)的公共标签集合。
         *   比如 hq1 和 hq2 有公共顶点 A 和 B，那么合适的 hq2 的候选超边和 hd1 也需要有公共顶点 A 和 B
         */
        Map<Integer, Set<Integer>> weightFilterCandidates = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Integer>> queryEntry : queryWgraph.getvertexNumWGraph().entrySet()) {
            int queryEdgeId = queryEntry.getKey(); // 查询超边id

            for (Map.Entry<Integer, Integer> neighbor : queryEntry.getValue().entrySet()) {
                int neighborEdgeId = neighbor.getKey(); // 邻查询边id
                int weight = neighbor.getValue();   // 查询超边之间的权重

                for (int queryEdgeCanId : candidates.get(queryEdgeId)) {  // 当前查询超边的候选超边id
                    for (int neighborEdgeCanId : candidates.get(neighborEdgeId)) { // 邻查询超边的候选超边id
                        if (queryEdgeCanId == neighborEdgeCanId)
                            continue;

                        weightFilterCandidates.putIfAbsent(queryEdgeId, new HashSet<>());
                        weightFilterCandidates.putIfAbsent(neighborEdgeId, new HashSet<>());

                        boolean isNeighbor = dataWgraph.isNeighbor(queryEdgeCanId, neighborEdgeCanId); // 判断两条候选超边是否相邻
                        int neighborWeight = dataGraph.intersectionNum(queryEdgeCanId, neighborEdgeCanId); // 计算候选超边之间的权重值
                        Set<Integer> dataWgraphIntersectionLabel = dataWgraph.getIntersectionLabel(queryEdgeCanId, neighborEdgeCanId); // 计算候选超边之间的公共标签
                        Set<Integer> queryWgraphIntersectionLabel = queryWgraph.getIntersectionLabel(queryEdgeId, neighborEdgeId);     // 计算查询超边之间的公共标签

                        boolean labelEquals = isNeighbor == false ? false : dataWgraphIntersectionLabel.equals(queryWgraphIntersectionLabel);

                        // 只要能找到一条邻边候选超边就 break
                        if (isNeighbor && neighborWeight >= weight && labelEquals) {
                            weightFilterCandidates.get(queryEdgeId).add(queryEdgeCanId);
                            break;
                        }
                    }
                }
            }
        }

        // 合并过滤结果
        for (int key : weightFilterCandidates.keySet()) {
            candidates.get(key).retainAll(weightFilterCandidates.get(key));
        }

        return candidates;
    }


}
