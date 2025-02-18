package zNIWGraph.index;


import zNIWGraph.graph.*;
import zNIWGraph.graph.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DualFilter {
    // 数据图与查询图的二元邻居交叉权重图
    private IntersectionLabelGraph dataInterLabelGraph;
    private IntersectionLabelGraph queryInterLabelGraph;

    // 包含超边ID到超边对象的映射（数据图和查询图）
    private NIWHypergraph dataNIWGraph;
    private NIWHypergraph queryNIWGraph;

    // HGMatch 构建的数据分区以及查询图
    private PartitionedEdges edges;
    private QueryGraph queryGraph;

    // 匹配顺序
    private List<List<Integer>> matchingOrder;

    private List<Integer> queryEdgeIds;

    private List<List<Integer>> candidateEdgeIds;

    // 新增反向索引：数据超边ID -> 所属查询超边ID集合
    private Map<Integer, Set<Integer>> candidateReverseIndex;

    public DualFilter(QueryGraph queryGraph, PartitionedEdges edges, List<List<Integer>> matchingOrder, IntersectionLabelGraph dataInterLabelGraph, IntersectionLabelGraph queryInterLabelGraph,
                      NIWHypergraph dataNIWGraph, NIWHypergraph queryNIWGraph) {
        this.queryGraph = queryGraph;
        this.edges = edges;
        this.matchingOrder = matchingOrder;
        this.dataInterLabelGraph = dataInterLabelGraph;
        this.queryInterLabelGraph = queryInterLabelGraph;
        this.dataNIWGraph = dataNIWGraph;
        this.queryNIWGraph = queryNIWGraph;
        this.queryEdgeIds = new ArrayList<>();
        this.candidateEdgeIds = new ArrayList<>();
        this.candidateReverseIndex = new HashMap<>();
    }


    // 主过滤入口：执行双重约束过滤
    public List<List<Integer>> filterCandidates() {
        // 1. 过滤前准备：得到查询超边id列表及其对应的候选超边id列表
        this.getEdgeIds();

        // 2. 第一重过滤：一跳邻居全量约束

        List<List<Integer>> filteredByOneHop = filterByOneHopConstraintsParallel();

        // 3. 第二重过滤：二跳传播消息验证
        List<List<Integer>> finalCandidates = filteredByOneHop;
        if (queryGraph.getEdges().size() > 2)
            finalCandidates = filterByTwoHopPropagation(filteredByOneHop);

        return finalCandidates;
    }

    /**
     * 对查询超边的全量邻居约束进行检测，找到符合条件的候选超边
     * 实现上分为了并行版本和单线程版本
     * @return
     */
    private List<List<Integer>> filterByOneHopConstraintsParallel() {
        return IntStream.range(0, candidateEdgeIds.size())
                .parallel()
                .mapToObj(i -> {
                    int qId = queryEdgeIds.get(i);
                    List<Integer> qCandidates = candidateEdgeIds.get(i);
                    // 使用内部流过滤候选超边
                    return qCandidates.stream()
                            .filter(eId -> {
                                Map<Integer, Map<Integer, Integer>> qNeighbors = queryInterLabelGraph.getLabelNeighbors(qId);
                                if (qNeighbors == null) return true;

                                // 检查所有邻边约束
                                for (Map.Entry<Integer, Map<Integer, Integer>> entry : qNeighbors.entrySet()) {
                                    int qPrime = entry.getKey();
                                    Map<Integer, Integer> constraints = entry.getValue();
                                    List<Integer> qPrimeCandidates = candidateEdgeIds.get(queryEdgeIds.indexOf(qPrime));
                                    boolean hasValid = qPrimeCandidates.stream()
                                            .anyMatch(ePrime -> satisfyEdgePairConstraints(eId, ePrime, constraints));
                                    if (!hasValid) return false;
                                }
                                return true;
                            })
                            .collect(Collectors.toList());
                })
                .collect(Collectors.toList());
    }


    private List<List<Integer>> filterByOneHopConstraintsSingleThread() {
        List<List<Integer>> filtered = new ArrayList<>();

        // candidateEdgeIds 按照匹配顺序排列
        for (int i = 0; i < candidateEdgeIds.size(); i++) {
            // 得到查询超边q的候选集
            int qId = queryEdgeIds.get(i);
            List<Integer> qCandidates = candidateEdgeIds.get(i);
            List<Integer> validForQ = new ArrayList<>();

            for (int eId : qCandidates) { // 遍历当前查询超边的候选超边
                if (satisfyOneHopConstraints(qId, eId)) {
                    validForQ.add(eId);
                }
            }
            filtered.add(validForQ);
        }
        return filtered;
    }

    // 检查
    private boolean satisfyOneHopConstraints(int q, int e) {
        // 获取查询超边q的所有邻边
        Map<Integer, Map<Integer, Integer>> qNeighbors = queryInterLabelGraph.getLabelNeighbors(q);
        if (qNeighbors == null) return true;  // 表示无邻边约束

        // 对全量邻边约束进行遍历
        for (Map.Entry<Integer, Map<Integer, Integer>> qNeighborEntry : qNeighbors.entrySet()) {
            int qPrime = qNeighborEntry.getKey(); // 查询超边q的邻边q'
            // 查询超边q与邻边q'的约束（标签 -> 最小公共顶点数）
            Map<Integer, Integer> queryConstraints = qNeighborEntry.getValue();


            // 获取q'的候选集
            List<Integer> qPrimeCandidates = candidateEdgeIds.get(queryEdgeIds.indexOf(qPrime));
            boolean hasValidEPrime = false;

            // 遍历q'的候选，寻找满足约束的e'
            for (int ePrime : qPrimeCandidates) {
                if (satisfyEdgePairConstraints(e, ePrime, queryConstraints)) {
                    hasValidEPrime = true;
                    break;
                }
            }
            if (!hasValidEPrime) return false; // 存在不满足的邻边
        }

        return true;
    }

    // 检查数据超边e和e'是否满足查询约束（标签对应的公共顶点数≥查询值）
    private boolean satisfyEdgePairConstraints(int e, int ePrime, Map<Integer, Integer> queryConstraints) {
        // 获取数据超边e和e'的交叉权重（从dataLabelWGraph）
        Map<Integer, Map<Integer, Integer>> eNeighbors = dataInterLabelGraph.getLabelNeighbors(e);
        if (eNeighbors == null) return false;

        Map<Integer, Integer> eToEPrime = eNeighbors.get(ePrime);
        if (eToEPrime == null) return false;

        // 检查查询超边的全量邻居
        for (Map.Entry<Integer, Integer> constraint : queryConstraints.entrySet()) {
            int label = constraint.getKey();
            int requiredCount = constraint.getValue();
            int actualCount = eToEPrime.getOrDefault(label, 0);

            if (actualCount < requiredCount) {
                return false;
            }
        }
        return true;
    }

    /**
     * 第二重过滤：二跳传播消息验证
     * @param oneHopFiltered
     * @return
     */
    private List<List<Integer>> filterByTwoHopPropagation(List<List<Integer>> oneHopFiltered) {
        List<List<Integer>> twoHopFiltered = new ArrayList<>();

        for (int i = 0; i < oneHopFiltered.size(); i++) {
            int qId = queryEdgeIds.get(i);  // 查询超边id
            List<Integer> qCandidates = oneHopFiltered.get(i);
            List<Integer> validForQ = new ArrayList<>();

            // 预计算查询超边q的二跳邻居结构
            Set<Integer> qTwoHopNeighbors = getQueryTwoHopNeighbors(qId);
            for (int e : qCandidates) {
                if (checkTwoHopReachability(qId, e, qTwoHopNeighbors, oneHopFiltered)) {
                    validForQ.add(e);
                }
            }
            twoHopFiltered.add(validForQ);
        }
        return twoHopFiltered;
    }



    private boolean checkTwoHopReachability(int qId, int e, Set<Integer> qTwoHopNeighbors, List<List<Integer>> candidates) {
        // 步骤1：获取数据超边e的一跳邻居（在数据权重图中）
        Map<Integer, Map<Integer, Integer>> eNeighbors = dataInterLabelGraph.getLabelNeighbors(e);
        if (eNeighbors == null) return qTwoHopNeighbors.isEmpty();

        // 步骤2：遍历e的一跳邻居e'，验证其是否属于q的某个一跳邻居q'的候选集
        for (int ePrime : eNeighbors.keySet()) {
            // -------------------- 优化前代码 --------------------
            boolean ePrimeIsInQPrimeCandidates = false;
            int qPrime = 0;
            for (int qPrime2 : queryInterLabelGraph.getLabelWGraph().getOrDefault(qId, Collections.emptyMap()).keySet()) {
                qPrime = qPrime2;
                List<Integer> qPrimeCandidates = candidates.get(queryEdgeIds.indexOf(qPrime));
                if (qPrimeCandidates.contains(ePrime)) {
                    ePrimeIsInQPrimeCandidates = true;
                    break;
                }
            }
            if (!ePrimeIsInQPrimeCandidates) continue;

            // 步骤3：检查从e'出发的二跳邻居e''是否能覆盖q的二跳邻居q''
            Map<Integer, Map<Integer, Integer>> ePrimeNeighbors = dataInterLabelGraph.getLabelNeighbors(ePrime);
            if (ePrimeNeighbors == null) continue;

            for (int eDoublePrime : ePrimeNeighbors.keySet()) {
                // 遍历q的二跳邻居q''，检查e''是否在q''的候选集中
                for (int qDoublePrime : qTwoHopNeighbors) {
                    List<Integer> qDoublePrimeCandidates = candidates.get(queryEdgeIds.indexOf(qDoublePrime));
                    if (qDoublePrimeCandidates.contains(eDoublePrime)) {
                        // 验证路径约束：q -> q' -> q'' 的标签和数量约束需在数据图中满足
                        if (validateTwoHopConstraints(qId, qPrime, qDoublePrime, e, ePrime, eDoublePrime)) {
                            return true; // 至少存在一条有效二跳路径
                        }
                    }
                }
            }
        }
        return false; // 无有效二跳路径满足约束
    }

    // 验证数据超边路径 e -> e' -> e'' 是否满足查询路径 q -> q' -> q'' 的约束
    private boolean validateTwoHopConstraints(int q, int qPrime, int qDoublePrime,
                                              int e, int ePrime, int eDoublePrime) {
        // 获取查询中 q -> q' 的约束
        Map<Integer, Integer> qToQPrimeConstraints = queryInterLabelGraph.getLabelNeighbors(q).get(qPrime);
        // 获取数据中 e -> e' 的实际值
        Map<Integer, Integer> eToEPrimeValues = dataInterLabelGraph.getLabelNeighbors(e).get(ePrime);
        if (!satisfyConstraints(qToQPrimeConstraints, eToEPrimeValues)) {
            return false;
        }

        // 获取查询中 q' -> q'' 的约束
        Map<Integer, Integer> qPrimeToQDoublePrimeConstraints = queryInterLabelGraph.getLabelNeighbors(qPrime).get(qDoublePrime);
        // 获取数据中 e' -> e'' 的实际值
        Map<Integer, Integer> ePrimeToEDoublePrimeValues = dataInterLabelGraph.getLabelNeighbors(ePrime).get(eDoublePrime);
        return satisfyConstraints(qPrimeToQDoublePrimeConstraints, ePrimeToEDoublePrimeValues);
    }

    // 通用约束检查：实际值是否≥查询要求
    private boolean satisfyConstraints(Map<Integer, Integer> constraints, Map<Integer, Integer> actualValues) {
        if (constraints == null || actualValues == null) return false;
        for (Map.Entry<Integer, Integer> entry : constraints.entrySet()) {
            int label = entry.getKey();
            int required = entry.getValue();
            int actual = actualValues.getOrDefault(label, 0);
            if (actual < required) {
                return false;
            }
        }
        return true;
    }


    private void getEdgeIds() {
        // 1. 按照【匹配顺序】获取每条查询超边的原始候选超边分区
        int num_edges = matchingOrder.size();
        DynamicHyperGraph dynamicHyperGraph = queryGraph.to_graph();

        List<EdgePartition> rawPartitions = new ArrayList<>();
        for (int i = 0; i < num_edges; i++) {
            List<Integer> edge = matchingOrder.get(i);
            List<Integer> edgeLabels = dynamicHyperGraph.getEdgeLabels(edge);
            rawPartitions.add(edges.get_partition(edgeLabels));
        }

        // 2. 按照【匹配顺序】提前获取每条候选超边和查询超边的id
        List<Integer> queryEdgeIds = new ArrayList<>(); // [2,3,1]
        for (int i = 0; i < matchingOrder.size(); i++) {
            int id = queryNIWGraph.getEdgeId(matchingOrder.get(i));
            queryEdgeIds.add(id);
        }

        List<List<Integer>> candidateEdgeIds = new ArrayList<>();
        for (EdgePartition edgePartition : rawPartitions) {
            List<Integer> ids = new ArrayList<>();

            for (int i = 0; i < edgePartition.num_edges(); i++) {
                List<Integer> edge = new ArrayList<>(edgePartition.getEdge(i));
//                Collections.sort(edge);
                // niwDataGraph.edgeToId里，key是超边列表，其中会按照顶点id的大小自动排序，因此这里获取edge后要进行排序
                ids.add(dataNIWGraph.getEdgeId(edge));
            }

            candidateEdgeIds.add(ids);
        }

        setQueryEdgeIds(queryEdgeIds);
        setCandidateEdgeIds(candidateEdgeIds);
        setCandidateReverseIndex(buildCandidateReverseIndex(candidateEdgeIds));
    }

    private Map<Integer, Set<Integer>> buildCandidateReverseIndex(List<List<Integer>> candidateEdgeIds) {
        Map<Integer, Set<Integer>> index = new HashMap<>();

        for (int i = 0; i < candidateEdgeIds.size(); i++) {
            List<Integer> qCandidates = candidateEdgeIds.get(i);
            for (int eId : qCandidates) {
                index.computeIfAbsent(eId, k -> new HashSet<>()).add(queryEdgeIds.get(i));
            }
        }
        return index;
    }

    // 获取查询超边q的二跳邻居集合（通过中间超边q'）
    private Set<Integer> getQueryTwoHopNeighbors(int q) {
        Set<Integer> twoHopNeighbors = new HashSet<>();
        Map<Integer, Map<Integer, Integer>> qNeighbors = queryInterLabelGraph.getLabelNeighbors(q);
        if (qNeighbors == null) return twoHopNeighbors;

        // 遍历q的一跳邻居q'
        for (int qPrime : qNeighbors.keySet()) {
            Map<Integer, Map<Integer, Integer>> qPrimeNeighbors = queryInterLabelGraph.getLabelNeighbors(qPrime);
            if (qPrimeNeighbors == null) continue;

            // 收集q'的邻居作为q的二跳邻居
            twoHopNeighbors.addAll(qPrimeNeighbors.keySet());
        }

        if (twoHopNeighbors.contains(q)) twoHopNeighbors.remove(q);
        return twoHopNeighbors;
    }

    public void setQueryEdgeIds(List<Integer> queryEdgeIds) {
        this.queryEdgeIds = queryEdgeIds;
    }

    public void setCandidateEdgeIds(List<List<Integer>> candidateEdgeIds) {
        this.candidateEdgeIds = candidateEdgeIds;
    }

    public void setCandidateReverseIndex(Map<Integer, Set<Integer>> candidateReverseIndex) {
        this.candidateReverseIndex = candidateReverseIndex;
    }
}
