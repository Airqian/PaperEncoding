package zNIWGraph.extend;

import zNIWGraph.graph.DynamicHyperGraph;
import zNIWGraph.graph.PartitionedEdges;
import zNIWGraph.graph.QueryGraph;
import zNIWGraph.graph.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExecutionPlan {
    private List<Integer> startLabels;
    private List<Extender> extenders;
    private List<Integer> resultLabels;
    private List<Integer> resultArity;

    // Logger for logging information
    private static final Logger logger = Logger.getLogger(ExecutionPlan.class.getName());

    public ExecutionPlan() {

    }

    public ExecutionPlan(List<Integer> startLabels, List<Extender> extenders, List<Integer> resultLabels, List<Integer> resultArity) {
        this.startLabels = startLabels;
        this.extenders = extenders;
        this.resultLabels = resultLabels;
        this.resultArity = resultArity;
    }

    public List<Integer> getStartLabels() {
        return startLabels;
    }

    public List<Extender> getExtenders() {
        return extenders;
    }

    public List<Integer> getResultLabels() {
        return resultLabels;
    }

    public List<Integer> getResultArity() {
        return resultArity;
    }

    // 生成所有可能的匹配顺序和查询计划，数量为(#edges)!，如有六条边的话会生成6✖️5✖️4✖️3✖️2✖️1=720种匹配顺序
    public List<ExecutionPlan> all_plans_from_query(QueryGraph queryGraph) {
        List<List<List<Integer>>> orders = ExecutionPlanUtils.all_matching_order(queryGraph);
        List<ExecutionPlan> results = new ArrayList<>();

        for (List<List<Integer>> order : orders)
            results.add(from_query_and_order(queryGraph, order));

        return results;
    }

    // 生成优化后的匹配顺序和查询计划，且数量都为1
    public ExecutionPlan from_query(QueryGraph queryGraph, PartitionedEdges edges) {
        // 生成匹配顺序
        List<List<Integer>> order = ExecutionPlanUtils.compute_matching_order(queryGraph, edges);

        // 生成查询计划
        return this.from_query_and_order(queryGraph, order);
    }

    /**
     * 该方法的主要目标是根据给定的查询图和匹配顺序生成一个执行计划 (ExecutionPlan)。
     * 执行计划可能用于执行复杂的图查询，优化查询的执行顺序和策略。
     * @param query
     * @param order
     * @return
     */
    public ExecutionPlan from_query_and_order(QueryGraph query, List<List<Integer>> order) {
        // queryGraph 为完整的查询图，partialQuery 为部分嵌入
        // partialQuery 保存顶点id 到标签的映射，以及相应的已经处理过的边，用于逐步构建和扩展查询图
        DynamicHyperGraph queryGraph = query.to_graph();
        DynamicHyperGraph partialQuery = new DynamicHyperGraph();
        List<List<Integer>> matchingOrder = new ArrayList<>(order);

        // System.out.println("%%%%%%%%%%%%%%% Computing execution plan %%%%%%%%%%%%%%% ");
        // System.out.println(String.format("Query - #Nodes: %d, #Hyperedges: %d", queryGraph.nums_nodes(), queryGraph.num_edges()));

        // 1. 首先扩展匹配顺序中的第一条边
        List<Integer> first_edge = matchingOrder.get(0);
        List<Integer> start_labels = queryGraph.getEdgeLabels(first_edge);

        List<Integer> result_labels = new ArrayList<>(start_labels); // 记录当前已加入mated_edges的超边的标签列表，需要的时候可以获取标签（顶点）的数量
        List<Integer> result_arity = new ArrayList<>();
        result_arity.add(first_edge.size());

        //     构建顶点 ID 映射到标签的映射，并对 partialQuery 扩展顶点和边
        Map<Integer, Integer> map = first_edge.stream()
                        .collect(Collectors.toMap(
                                v -> v,
                                v -> queryGraph.getNodeLabel(v)
                        ));
        partialQuery.extendNodes(map.entrySet().iterator());
        partialQuery.addEdge(first_edge);

        // 2. 生成扩展器 Extender
        List<Extender> extenders = new ArrayList<>(matchingOrder.size() - 1);
        List<List<Integer>> matched_edges = new ArrayList<>();  // 初始化已匹配的边集合
        matched_edges.add(new ArrayList<>(first_edge));

        /**
         * edge_pos 用于存储 edge 中 不同标签 顶点的索引位置（会对标签进行排序）
         * map.key 表示 edge，
         * map.value.key 表示 label，map.value.value 表示该条 edge 中标签为 label 的顶点的数量
         * 并且随着查询的扩展，map.value.value 的数值是递增的，总的来说，该数量是部分查询中某种标签的顶点的总数
         * ‼️‼️‼️‼️ edge_pos 记录顶点的索引，可以用来做查询顶点和候选顶点的映射
         */
        Map<List<Integer>, Map<Integer, List<Integer>>> edge_pos = new HashMap<>();
        ExecutionPlanUtils.update_pos(0, first_edge, result_labels, edge_pos);

        for (int i = 1; i < matchingOrder.size(); i++) {
            List<Integer> cur_edge = matchingOrder.get(i);
            boolean unique = true;

            // 将 cur_edge 扩展到 partialQuery 中
            partialQuery.extendNodes(cur_edge.stream()
                    .collect(Collectors.toMap(
                            v -> v,
                            v -> queryGraph.getNodeLabel(v)
                    )).entrySet().iterator());
            partialQuery.addEdge(cur_edge);

            int num_nodes = partialQuery.nums_nodes();      // 获取当前 partialQuery 中的节点总数
            List<Integer> extended_labels = queryGraph.getEdgeLabels(cur_edge);    // 获取当前边的标签列表（经过排序）
            int extended_arity = cur_edge.size();     // 获取当前边包含的节点总数

            // 用于选择和约束节点，对 cur_edge 的所有交点都会生成一个 NodeSelector
            List<NodeSelector> node_selectors = new ArrayList<>();

            // 3. 根据论文里提出的四条 observation，对所有已匹配的 cur_edge 的邻边进行操作
            for (List<Integer> matched_edge : matched_edges) {
                // 找出两条边共同的顶点
                List<Integer> adjs = ExecutionPlanUtils.intersect(matched_edge, cur_edge);

                for (int adj : adjs) {
                    int adj_label = queryGraph.getNodeLabel(adj);
                    List<Integer> matched_edge_adj_indices = new ArrayList<>(edge_pos.get(matched_edge).get(adj_label)); // 在当前邻边里，与公共顶点标签相同的顶点索引位置
                    NodeSelector node_selector = new NodeSelector(new ArrayList<>(matched_edge_adj_indices));

                    /**
                     * 第一部分：计算部分查询中包含 adj 的邻边数量，以及取出这些超边中标签为 adj_label 的索引。
                     *         eq 的候选超边 f(eq) 包含 f(adj) 的邻边也需要满足该度约束
                     *
                     */
                    List<List<Integer>> adj_edges = new ArrayList<>();
                    Pair<Integer, List<Integer>> degreeAndIndices =
                            ExecutionPlanUtils.cal_degree_constraint(adj, adj_label, matched_edges, edge_pos, adj_edges);

                    int degree = degreeAndIndices.getKey();
                    List<Integer> degreeIndices = degreeAndIndices.getValue();
                    int num_adj_edges = adj_edges.size();

                    // TODO 标签不相同的顶点管了吗
                    /**
                     * 第二部分：计算 cur_edge 中与 adj 标签相同的其他顶点 v 在 邻边 中的度数「判断 adj 是不是部分查询里「度」唯一的交点」
                     *      从 cur_edge 中筛选出与 adj 标签相同的其他顶点
                     *      对于每个这样的顶点，计算它在 adj_edges 中出现的次数，存储在 other_degree 列表中
                     */
                    List<Integer> cur_edge_other_nodes = cur_edge.stream().filter(n -> n != adj)
                            .filter(n -> queryGraph.getNodeLabel(n) == adj_label).collect(Collectors.toList());

                    List<Integer> cur_edge_other_degree = new ArrayList<>();
                    for (int n : cur_edge_other_nodes) {
                        int d = 0;
                        for (List<Integer> e : adj_edges) {
                            if (e.contains(n))
                                d += 1;
                        }
                        cur_edge_other_degree.add(d);
                    }

                    // unique_by_degree 判断 adj 是不是 cur_edge 中标签为 adj_label 的一众节点里，在部分查询中 度 唯一的顶点
                    boolean cur_edge_adj_unique_by_degree = !cur_edge_other_degree.contains(degree);

                    /**
                     * 设置度约束：
                     *      再看 set_degree 的判断方式，如果有两条及以上的邻边且 cur_edge 中有同 adj_label 标签的顶点出现的次数和 adj 不同，那就说明 degreeIndices 记录的索引所代表的顶点中，
                     *      有从出现次数上来看不可能是公共顶点的顶点，因此也不可能出现在 cur_edge 中，同样也不可能出现在候选超边中。所以需要设置 degree，在后续把这些顶点过滤掉，从而过滤掉一部分超边（NodeSelector#select 方法）。
                     *
                     *  num_adj_edges > 1：成立时表示 matched_edges 中有两条及以上的超边包含顶点 adj。当 num_adj_edges = 1 时说明也只有 matched_edges 中包含顶点 adj，暂不设置过滤条件？
                     *  matched_edge_adj_indices.size() > 1：成立时表示当前 matched_edge 中标签为 adj_label 的顶点不止一个，存在非公共顶点，
                     *          如果 matched_edge_adj_indices.size() = 1，那么就说明 matched_edge 中只有一个标签为 adj_label 的顶点，而且还是当前的公共顶点，不能被过滤掉
                     *  !cur_edge_other_degree.stream().allMatch(d -> d == degree);：allMatch 在所有条件判断都为 true 时返回 true，因此在 cur_edge 中只要有一个标签为 adj_label 的顶点出现的次数不和 adj 出现的次数相等，
                     *          整个表达式返回 true，而这个点一定不是当前的公共顶点，因此需要被过滤掉
                     */
                    boolean set_degree = num_adj_edges > 1 && matched_edge_adj_indices.size() > 1
                            && !cur_edge_other_degree.stream().allMatch(d -> d == degree);

                    if (set_degree) {
                        node_selector.setDegree(new Pair<>(degree, degreeIndices));
                    }

                    /**
                     * 设置标签约束：
                     *    not_in_indices 表示找出在 matched_edges 中不与 cur_edge 共享顶点 adj 的超边，并返回这些超边中标签为 adj_label 的顶点索引，这些顶点不可能出现在超边 cur_edge 里，后续会被 select 掉。
                     *    other_not_in 表示找出在 matched_edges 中不与 cur_edge 共享其他标签为 adj_label 的顶点的超边，并返回这些超边标签为 adj_label 的顶点索引
                     */
                    List<Integer> adj_not_in_indices = ExecutionPlanUtils.cal_not_in_constraint(adj, cur_edge,
                            adj_label, matched_edges, edge_pos);
                    List<List<Integer>> cur_edge_other_nodes_not_in_indices = cur_edge_other_nodes.stream()
                            .map(n -> ExecutionPlanUtils.cal_not_in_constraint(n, cur_edge, adj_label, matched_edges, edge_pos))
                            .collect(Collectors.toList());

                    /**
                     * adj_not_in_indices.size() > 0 表示 matched_edges 中有超边不包含顶点 adj
                     * matched_edge_adj_indices.size() > 1 说明 matched_edge 中标签为 adj_label 的顶点除了顶点 adj 还有其他点
                     * cur_edge_adj_unique_by_degree 判断 adj 是不是 cur_edge 在当前嵌入中度唯一的顶点，如果为 true 的话，就这一项就能把其他的节点过滤掉，再设置 set_not_in 就是冗余条件了
                     * cur_edge_other_nodes_not_in_indices.stream().allMatch(list -> list.equals(adj_not_in_indices)) 成立时说明 adj 和 cur_edge 中其他同 adj_label 的顶点的 not_in 索引相同，此时 adj 不是唯一
                     */
                    // TODO adj_not_in_indices 列表里的点一定不会出现在 cur_edge 中，但是有可能出现在 matched_edge 中，但是感觉最后一个条件是冗余的呢
                    boolean set_not_in = adj_not_in_indices.size() > 0
                            && matched_edge_adj_indices.size() > 1
                            && !cur_edge_adj_unique_by_degree
                            && cur_edge_other_nodes_not_in_indices.stream().allMatch(list -> list.equals(adj_not_in_indices));

                    if (set_not_in)
                        node_selector.setNot_in(new ArrayList<>(adj_not_in_indices));

                    /**
                     * - noneMatch：所有的条件判断都为 false，返回 true
                     * - 检查在 cur_edge 里，所有其他同 adj_label 的顶点与 adj 相比，如果有一个顶点的度约束和标签约束和adj相同，
                     *   那么unique_by_degree_not_in会返回false，进而将unique设置为false，进而后面会设置filter，filter用于嵌入验证（vertex profile），
                     *   这是因为有可能这两个顶点的 vertex profile 可能相同，因此 unique 为 false，要设置相关的 filter。
                     */
                    boolean unique_by_degree_not_in = IntStream.range(0, cur_edge_other_degree.size())
                            .noneMatch(idx -> cur_edge_other_degree.get(idx) == degree && cur_edge_other_nodes_not_in_indices.get(idx).equals(adj_not_in_indices));
                    if (!unique_by_degree_not_in)
                        unique = false;

                    node_selectors.add(node_selector);
                }
            }

            // cur_edge 和所有 matched_edge 的所有交点交互结束
            node_selectors.sort((node1, node2) -> node1.getIndices().size() - node2.getIndices().size());
            node_selectors = node_selectors.stream().distinct().collect(Collectors.toList());

            // 创建扩展器，分别表示当前嵌入的顶点数、当前需要嵌入的分区标签以及相关的约束
            Extender extender = new Extender(num_nodes, new ArrayList<>(extended_labels), node_selectors);
            matched_edges.add(new ArrayList<>(cur_edge));
            ExecutionPlanUtils.update_pos(result_labels.size(), cur_edge, extended_labels, edge_pos);

            result_labels.addAll(extended_labels);
            result_arity.add(extended_arity);

            /**
             * 表示在 cur_edge 里，有某个公共顶点和其他同标签的顶点的 度约束 和 标签约束 相同，这种情况下，
             * 顶点的 vertex profile 可能产生歧义，因此要设置 filter
             */
            //
            if (!unique) {
                NodeVerifier nodeFilter = ExecutionPlanUtils.cal_node_filter(queryGraph, matched_edges, edge_pos);
                extender.setNode_filter(nodeFilter);
            }

            extenders.add(extender);
        }

        return new ExecutionPlan(start_labels, extenders, result_labels, result_arity);
    }
}
