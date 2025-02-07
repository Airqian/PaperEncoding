package zHyperISO;

import zHyperISO.util.Pair;

import java.util.*;

/**
 * 主回溯过程用于获得 HQ 中匹配的顶点，而次回溯过程用于识别 HQ 中匹配的超边。
 * - 顶点的匹配顺序：根据超边的匹配顺序来生成顶点的匹配顺序，如果在超边匹配顺序中 hq1 在 hq2 之前，那么在顶点匹配顺序里 hq1 中的所有顶点也会出现在 hq2 中的顶点之前
 * - 识别顶点：每条超边需要选一个识别顶点，用来标识该超边的匹配位置。例如，如果超边中的顶点有未被匹配的，选其中一个作为识别顶点，否则随机选
 * <p>
 * 算法的主要逻辑：
 * - 在回溯过程中，当遇到识别顶点时，触发对应的超边匹配。这时候需要从候选超边中找到符合标签条件的，并验证顶点是否匹配。
 * 非识别顶点的候选集可能来自全局所有候选超边，或者局部于当前超边的候选。
 */
public class DualEnumMatcher {
    public static List<Map<Integer, Integer>> match(
            Hypergraph queryGraph, Hypergraph dataGraph,
            Map<Integer, Set<Integer>> candidates, List<Integer> matchingOrder) {
        // 1 基于超边顺序生成顶点的匹配顺序
        List<Integer> vertexOrder = generateVertexOrder(queryGraph, matchingOrder); // 顶点匹配顺序

        // 2 identifiedPositions 中保存识别顶点和超边的映射关系
        Pair<Map<Integer, Integer>, List<Integer>> vertexMappingPair = getIdentifiedPositions(queryGraph, matchingOrder);// 得到识别顶点的位置
        Map<Integer, Integer> identifiedPositions = vertexMappingPair.getKey();
        List<Integer> allVertexMapping = vertexMappingPair.getValue();

        // 3 执行回溯搜索
        List<Map<Integer, Integer>> embeddings = new ArrayList<>();
        backtrack(embeddings, new HashMap<>(), new HashMap<>(), vertexOrder, 0,
                queryGraph, dataGraph, candidates, identifiedPositions, allVertexMapping);

        return embeddings;
    }

    private static void backtrack(List<Map<Integer, Integer>> embeddings,
                                  Map<Integer, Integer> currentVertexMap,
                                  Map<Integer, Integer> currentHyperEdgeMap,
                                  List<Integer> vertexOrder,
                                  int depth,
                                  Hypergraph queryGraph,
                                  Hypergraph dataGraph,
                                  Map<Integer, Set<Integer>> candidates,
                                  Map<Integer, Integer> identifiedPositions,
                                  List<Integer> allVertexMapping) {
        if (depth == vertexOrder.size()) {
            embeddings.add(new HashMap<>(currentHyperEdgeMap));
            return;
        }

        int u = vertexOrder.get(depth); // 当前顶点，需要判断这个顶点处于哪条边中，然后从该边匹配上的候选超边中生成顶点候选集
        int queryLabel = queryGraph.getNodeLabel(u);
        boolean isIdentified = identifiedPositions.containsKey(depth); // 判断当前顶点是不是识别顶点

        // 如果顶点u已经被映射过，那么应该前进到下一个顶点
        if (currentVertexMap.containsKey(u))
            backtrack(embeddings, currentVertexMap, currentHyperEdgeMap, vertexOrder, depth + 1,
                    queryGraph, dataGraph, candidates, identifiedPositions, allVertexMapping);

        /**
         * 识别顶点会开启超边的映射，而后非识别且为匹配的顶点的候选集则局限在特定的候选超边中
         */
        Set<Integer> vertexCandidates = new HashSet<>();
        if (isIdentified) {
            vertexCandidates = getIdentifiedCandidates(u, depth, isIdentified, currentVertexMap, queryGraph,
                    dataGraph, identifiedPositions, candidates);      // 获得当前顶点的候选集
        } else if (!currentVertexMap.containsKey(u)){
            vertexCandidates = getNonIdentifiedCandidates(u, depth, currentVertexMap, currentHyperEdgeMap,
                    allVertexMapping, dataGraph, queryGraph);
        }


        for (int v : vertexCandidates) {   // 将查询顶点到候选顶点进行逐一映射
            if (isValidMapping(u, v, currentVertexMap, queryGraph, dataGraph)) {
                currentVertexMap.put(u, v); // 如果能形成合理的映射 那么能将该查询顶点映射到候选顶点上

                if (isIdentified) {
                    int queryEdgeId = identifiedPositions.get(depth);
                    Set<Integer> candidateEdges = candidates.get(queryEdgeId);

                    for (int candidateEdgeId : candidateEdges) { // 选定当前候选超边
                        if (validateHyperEdgeMapping(queryEdgeId, queryGraph, candidateEdgeId, dataGraph, currentVertexMap)) {
                            currentHyperEdgeMap.put(queryEdgeId, candidateEdgeId);
                            backtrack(embeddings, currentVertexMap, currentHyperEdgeMap, vertexOrder, depth + 1,
                                    queryGraph, dataGraph, candidates, identifiedPositions, allVertexMapping);
                            currentHyperEdgeMap.remove(queryEdgeId);
                        }
                    }
                } else {
                    backtrack(embeddings, currentVertexMap, currentHyperEdgeMap, vertexOrder, depth + 1,
                            queryGraph, dataGraph, candidates, identifiedPositions, allVertexMapping);
                }

                currentVertexMap.remove(u);
            }
        }
    }

    private static boolean validateHyperEdgeMapping(int queryEdgeId, Hypergraph queryGraph, int candidateEdgeId, Hypergraph dataGraph, Map<Integer, Integer> currentVertexMap) {
        // 1 两条超边的标签必须匹配
        if (!queryGraph.getEdgeLabels(queryEdgeId).equals(dataGraph.getEdgeLabels(candidateEdgeId)))
            return false;

        // 2 已映射顶点必须包含在候选超边中
        List<Integer> queryEdge = queryGraph.getHyperEdgeById(queryEdgeId);

        for (Map.Entry<Integer, Integer> entry : currentVertexMap.entrySet()) {
            boolean query = queryGraph.containsVertex(queryEdgeId, entry.getKey());
            boolean data = !dataGraph.containsVertex(candidateEdgeId, entry.getValue());

            if ( query && data)
                return false;
        }
        return true;
    }

    // 验证顶点映射是否有效：检查标签是否相同以及该查询顶点和候选顶点是否已映射过
    private static boolean isValidMapping(int u, int v,
                                          Map<Integer, Integer> currentVertexMap,
                                          Hypergraph queryGraph, Hypergraph dataGraph) {
        // 检查标签一致性和已有映射一致性
        return queryGraph.getNodeLabel(u) == dataGraph.getNodeLabel(v) &&
                !currentVertexMap.containsKey(u) && !currentVertexMap.containsValue(v);
    }

    private static Pair<Map<Integer, Integer>, List<Integer>> getIdentifiedPositions(Hypergraph queryGraph, List<Integer> matchingOrder) {
        Map<Integer, Integer> identifiedPositions = new HashMap<>(); // 指示顶点顺序中某个位置是某条超边的开始
        List<Integer> allVertexMapping = new ArrayList<>();
        int curVertexNum = 0;
        identifiedPositions.put(curVertexNum, matchingOrder.get(0));

        for (int i = 1; i < matchingOrder.size(); i++) {
            curVertexNum += queryGraph.getHyperEdgeById(matchingOrder.get(i - 1)).size();
            identifiedPositions.put(curVertexNum, matchingOrder.get(i));
        }

        for (int i = 0; i < matchingOrder.size(); i++) {
            int edgeId = matchingOrder.get(i);
            for (int v : queryGraph.getHyperEdgeById(edgeId)) {
                allVertexMapping.add(edgeId);
            }
        }
        return new Pair<>(identifiedPositions, allVertexMapping);
    }

    // 识别顶点的候选集由对应超边候选超边中的顶点组成
    private static Set<Integer> getIdentifiedCandidates(int u, int depth, boolean isIdentified, Map<Integer, Integer> currentVertexMap,
                                                        Hypergraph queryGraph, Hypergraph dataGraph,
                                                        Map<Integer, Integer> identifiedPositions, Map<Integer, Set<Integer>> candidates) {
        Set<Integer> vertexCandidates = new HashSet<>();
        int edgeId = identifiedPositions.get(depth);

        for (int candidateEdgeId : candidates.get(edgeId)) {
            for (int v : dataGraph.getHyperEdgeById(candidateEdgeId)) {
                if (queryGraph.getNodeLabel(u) == dataGraph.getNodeLabel(v))
                    vertexCandidates.add(v);
            }
        }

        return vertexCandidates;
    }

    // 非识别顶点的候选集从对应候选超边中产生，也相当于以顶点优先的过滤了
    private static Set<Integer> getNonIdentifiedCandidates(int u, int depth, Map<Integer, Integer> currentVertexMap,
                                                           Map<Integer, Integer> currentHyperEdgeMap, List<Integer> allVertexMapping,
                                                           Hypergraph dataGraph, Hypergraph queryGraph) {
        int queryEdgeId = allVertexMapping.get(depth);
        int candidateEdgeId = currentHyperEdgeMap.getOrDefault(queryEdgeId, -1);
        Set<Integer> res = new HashSet<>();

        if (candidateEdgeId != -1) {
            int queryLabel = queryGraph.getNodeLabel(u);
            for (int v : dataGraph.getHyperEdgeById(candidateEdgeId)) {
                int candidateLabel = dataGraph.getNodeLabel(v);
                if (queryLabel == candidateLabel && !currentVertexMap.containsKey(u) && !currentVertexMap.containsValue(v))
                    res.add(v);
            }
        }

        return res;
    }

    // 根据超边的匹配顺序生成顶点的匹配顺序
    private static List<Integer> generateVertexOrder(Hypergraph queryGraph, List<Integer> matchingOrder) {
        List<Integer> vertexOrder = new ArrayList<>();
        Set<Integer> added = new HashSet<>();
        int vertexNum = 0;

        for (int i = 0; i < matchingOrder.size(); i++) {
            int edgeId = matchingOrder.get(i);
            List<Integer> edge = queryGraph.getHyperEdgeById(edgeId);
            List<Integer> contains = new ArrayList<>(); // 暂时保存前面已经被记录的顶点

            for (int v : edge) {
                if (!added.contains(v)) {
                    vertexOrder.add(v);
                    added.add(v);
                } else
                    contains.add(v);
            }
            vertexOrder.addAll(vertexNum + 1, contains);
            vertexNum += edge.size();
        }

        return vertexOrder;
    }
}
