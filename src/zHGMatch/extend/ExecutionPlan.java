package zHGMatch.extend;

import zHGMatch.graph.DynamicHyperGraph;
import zHGMatch.graph.PartitionedEdges;
import zHGMatch.graph.QueryGraph;
import zHGMatch.graph.util.Pair;

import java.util.*;
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

    public List<ExecutionPlan> all_plans_from_query(QueryGraph queryGraph) {
        List<List<List<Integer>>> orders = ExecutionPlanUtils.all_matching_order(queryGraph);
        List<ExecutionPlan> results = new ArrayList<>();

        for (List<List<Integer>> order : orders)
            results.add(from_query_and_order(queryGraph, order));

        return results;
    }

    public ExecutionPlan from_query(QueryGraph queryGraph, PartitionedEdges edges) {
        List<List<Integer>> order = ExecutionPlanUtils.compute_matching_order(queryGraph, edges);
        return this.from_query_and_order(queryGraph, order);
    }

    /**
     * 该方法的主要目标是根据给定的查询图和匹配顺序生成一个执行计划 (ExecutionPlan)。执行计划可能用于执行复杂的图查询，优化查询的执行顺序和策略。
     * @param query
     * @param order
     * @return
     */
    public ExecutionPlan from_query_and_order(QueryGraph query, List<List<Integer>> order) {
        DynamicHyperGraph queryGraph = query.to_graph();

        System.out.println("%%%%%%%%%%%%%%% Computing execution plan %%%%%%%%%%%%%%% ");
        System.out.println(String.format("Query - #Nodes: %d, #Hyperedges: %d", queryGraph.nums_nodes(), queryGraph.num_edges()));

        List<List<Integer>> matchingOrder = new ArrayList<>(order);
        // partialQuery 保存顶点id 到标签的映射，以及相应的已经处理过的边，用于逐步构建和扩展查询图
        DynamicHyperGraph partialQuery = new DynamicHyperGraph();

        // 1. 首先扩展匹配顺序中的第一条边
        //    first_edge 中顶点 id 和 start_label 中标签的顺序不是严格匹配，因为在 getEdgeLabels 方法中进行了一层排序
        List<Integer> first_edge = matchingOrder.get(0);
        List<Integer> start_labels = queryGraph.getEdgeLabels(first_edge);

        List<Integer> result_labels = new ArrayList<>(start_labels);
        List<Integer> result_arity = new ArrayList<>();
        result_arity.add(first_edge.size());

        //     将每个顶点 ID 映射到其对应的标签，并将映射添加到 partialQuery.labels 中
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

        // edge_pos 用于存储每条边中每个标签对应的位置列表
        // map.key 表示 edge，
        //    map.value.key 表示 label，map.value.value 则表示 size+i，可以理解为位置
        Map<List<Integer>, Map<Integer, List<Integer>>> edge_pos = new HashMap<>();
        ExecutionPlanUtils.update_pos(0, first_edge, result_labels, edge_pos);

        for (int i = 1; i < matchingOrder.size(); i++) {
            List<Integer> cur_edge = matchingOrder.get(i);
            boolean unique = true;

            // 将当前边扩展到 partialQuery 当前查询图中
            partialQuery.extendNodes(cur_edge.stream()
                    .collect(Collectors.toMap(
                            v -> v,
                            v -> queryGraph.getNodeLabel(v)
                    )).entrySet().iterator());
            partialQuery.addEdge(cur_edge);

            int num_nodes = partialQuery.nums_nodes();      // 获取当前 partialQuery 中的节点总数
            List<Integer> extended_labels = queryGraph.getEdgeLabels(cur_edge);    // 获取当前边的标签列表
            int extended_arity = cur_edge.size();     // 获取当前边包含的节点总数

            // 用于选择和约束节点
            List<NodeSelector> node_selectors = new ArrayList<>();

            // 处理已匹配的边与当前的边
            for (List<Integer> matched_edge : matched_edges) {
                List<Integer> adjs = ExecutionPlanUtils.intersect(matched_edge, cur_edge); // 找出两条边共同的顶点

                for (int adj : adjs) {
                    int adj_label = queryGraph.getNodeLabel(adj);
                    List<Integer> adj_indices = new ArrayList<>(edge_pos.get(matched_edge).get(adj_label));
                    NodeSelector node_selector = new NodeSelector(new ArrayList<>(adj_indices));

                    // 第一部分：
                    //      计算 matched_edges 中与顶点 adj 相邻的边有几条，然后从 edge_pos 中取出这些边对应的标签的 indices
                    List<List<Integer>> adj_edges = new ArrayList<>();
                    Pair<Integer, List<Integer>> degreeAndIndices =
                            ExecutionPlanUtils.cal_degree_constraint(adj, adj_label, matched_edges, edge_pos, adj_edges);
                    int degree = degreeAndIndices.getKey();
                    List<Integer> degreeIndices = degreeAndIndices.getValue();

                    int num_adj_edges = adj_edges.size();

                    // 第二部分：
                    //      从 cur_edge 中筛选出与 adj 标签相同且不是 adj 本身的顶点
                    //      对于每个这样的顶点，计算它在 adj_edges 中出现的次数，存储在 other_degree 列表中
                    List<Integer> other_nodes = cur_edge.stream().filter(n -> n != adj)
                            .filter(n -> queryGraph.getNodeLabel(n) == adj_label).collect(Collectors.toList());

                    List<Integer> other_degree = new ArrayList<>();
                    for (int n : other_nodes) {
                        int d = 0;
                        for (List<Integer> e : adj_edges) {
                            if (e.contains(n))
                                d += 1;
                        }
                        other_degree.add(d);
                    }

                    // 如果 other_degree 不包含当前顶点 adj 的度，则设置为 true
                    boolean unique_by_degree = !other_degree.contains(degree);
                    boolean set_degree = num_adj_edges > 1 && adj_indices.size() > 1
                            && !other_degree.stream().allMatch(d -> d == degree);

                    // 设置度约束
                    if (set_degree) {
                        node_selector.setDegree(new Pair<>(degree, degreeIndices));
                    }

                    // 设置 not in 约束
                    List<Integer> not_in_indices = ExecutionPlanUtils.cal_not_in_constraint(adj, cur_edge,
                            adj_label, matched_edges, edge_pos);
                    List<List<Integer>> other_not_in = other_nodes.stream()
                            .map(n -> ExecutionPlanUtils.cal_not_in_constraint(n, cur_edge, adj_label, matched_edges, edge_pos))
                            .collect(Collectors.toList());

                    boolean set_not_in = not_in_indices.size() > 0
                            && adj_indices.size() > 1
                            && !unique_by_degree
                            && other_not_in.stream().allMatch(list -> list.equals(not_in_indices));
                    if (set_not_in)
                        node_selector.setNot_in(new ArrayList<>(not_in_indices));

                    // 检查唯一性
                    boolean unique_by_degree_not_in = IntStream.range(0, other_degree.size())
                            .noneMatch(idx -> other_degree.get(idx) == degree && other_not_in.get(idx).equals(not_in_indices));
                    if (!unique_by_degree_not_in)
                        unique = false;

                    node_selectors.add(node_selector);

                }
            }


            // 定义一个比较器，按字典序比较两条边
            node_selectors.sort((node1, node2) -> node1.getIndices().size() - node2.getIndices().size());
            node_selectors = node_selectors.stream().distinct().collect(Collectors.toList());
            Extender extender = new Extender(num_nodes, new ArrayList<>(extended_labels), node_selectors);
            matched_edges.add(new ArrayList<>(cur_edge));
            ExecutionPlanUtils.update_pos(result_labels.size(), cur_edge, extended_labels, edge_pos);

            result_labels.addAll(extended_labels);
            result_arity.add(extended_arity);

            if (!unique) {
                NodeVerifier nodeFilter = ExecutionPlanUtils.cal_node_filter(queryGraph, matched_edges, edge_pos);
                extender.setNode_filter(nodeFilter);
            }

            extenders.add(extender);
        }

        return new ExecutionPlan(start_labels, extenders, result_labels, result_arity);
    }
}
