package zHGMatch.extend;

import zHGMatch.graph.DynamicHyperGraph;
import zHGMatch.graph.PartitionedEdges;
import zHGMatch.graph.QueryGraph;
import zHGMatch.graph.util.Pair;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.swap;


public class ExecutionPlanUtils {
    private static final Logger logger = Logger.getLogger(ExecutionPlanUtils.class.getName());


    public static void main(String[] args) {
        List<List<Integer>> res = new ArrayList<>();
        res.add(new ArrayList<>(Arrays.asList(1, 12, 3)));
        res.add(new ArrayList<>(Arrays.asList(4, 5, 6)));
        res.add(new ArrayList<>(Arrays.asList(89, 2, 3)));
        res.add(new ArrayList<>(Arrays.asList(89, 2, 3)));
        List<List<List<Integer>>> lists = generatePermutations(res);

        System.out.println(lists.size());
        for (List<List<Integer>> list : lists)
            System.out.println(list);
    }

    // 排列组合生成所有可能的匹配顺序
    public static List<List<List<Integer>>> all_matching_order(QueryGraph graph) {
        List<List<List<Integer>>> results = new ArrayList<>();
        List<List<Integer>> edges = new ArrayList<>(graph.to_graph().getEdges());

        // 生成边的排列组合并进行连通性筛选
        List<List<List<Integer>>> permutations = generatePermutations(edges);
        for (List<List<Integer>> order : permutations) {
            if (is_connected(order))
                results.add(order);
        }
        return results;
    }

    /**
     * 优化后的匹配顺序计算
     * 计算超边所在分区的超边数与超边连通度的比值，选择最小的
     *
     * @param query
     * @param edges
     * @return
     */
    public static List<List<Integer>> compute_matching_order(QueryGraph query, PartitionedEdges edges) {
        System.out.println("%%%%%%%%%%%%%%% Computing matching order %%%%%%%%%%%%%%%");
        DynamicHyperGraph query_graph = query.to_graph();

        // 获取所有查询边并按基数排序
        List<List<Integer>> queryEdges = new ArrayList<>(query_graph.getEdges());

        // 检查是否有边存在
        if (queryEdges.isEmpty()) {
            System.out.println("Query graph has no edges.");
            return Collections.emptyList();
        }

        // 第一步：按不同标签分区内边的数量，也即按边标签的频率进行排序
        queryEdges.sort(Comparator.comparingInt(edge -> {
            List<Integer> labels = query_graph.getEdgeLabels(edge);
            return edges.get_edge_label_frequency(labels);
        }));

        /**
         * 第二步：从第一条边开始判断，按照 queryEdges 中 edge 的顺序，是否能连通成一个子图
         *       ‼️‼️‼️‼️ 这个地方和论文不一样！！没有计算分区基数值与连通度的比值，只是判断是否能成为一个连通的子图
         */
        // 克隆第一条边并移除
        List<Integer> firstEdge = new ArrayList<>(queryEdges.get(0));
        queryEdges.remove(0);

        // 初始化 matchedEdges 和 matchedNodes
        List<List<Integer>> matchedEdges = new ArrayList<>();
        matchedEdges.add(firstEdge);

        Set<Integer> matchedNodes = new HashSet<>(firstEdge);

        while (!queryEdges.isEmpty()) {
            boolean found = false;

            Iterator<List<Integer>> iterator = queryEdges.iterator();
            while (iterator.hasNext()) {
                List<Integer> edge = iterator.next();
                // 检查是否已经选定的边是否与当前待入选的边连通
                boolean hasMatchedNode = edge.stream().anyMatch(matchedNodes::contains);

                if (hasMatchedNode) {
                    // 添加到 matchedEdges
                    List<Integer> edgeCopy = new ArrayList<>(edge);
                    matchedEdges.add(edgeCopy);
                    // 添加所有节点到 matchedNodes
                    matchedNodes.addAll(edge);
                    // 从 queryEdges 中移除
                    iterator.remove();
                    found = true;
                    break; // 跳出当前循环，继续下一个边
                }
            }

            if (!found) {
                logger.severe("Query is not connected.");
                logger.severe(query.toString());
                logger.severe("Isolated hyperedges: " + queryEdges);
                throw new RuntimeException("Query is not connected.");
            }
        }

        return matchedEdges;
    }

    // 假设 edges 中不存在重复的边，生成 edges 的全排列
    private static List<List<List<Integer>>> generatePermutations(List<List<Integer>> edges) {
        List<List<List<Integer>>> results = new ArrayList<>();
        permute(edges, 0, results);
        return results;
    }

    private static void permute(List<List<Integer>> edges, int index, List<List<List<Integer>>> results) {
        if (index == edges.size()) {
            List<List<Integer>> res = new ArrayList<>();
            for (List<Integer> edge : edges)
                res.add(edge);
            results.add(res);
        } else {
            for (int j = index; j < edges.size(); j++) {
                swap(edges, j, index);
                permute(edges, index + 1, results);
                swap(edges, j, index);
            }
        }
    }

    /**
     * 检查给定的边顺序是否保持连接性。
     *
     * @param edges 边的顺序
     * @return 如果保持连接性，返回 true；否则返回 false
     */
    public static boolean is_connected(List<List<Integer>> edges) {
        if (edges.isEmpty()) {
            return false; // 根据需求调整：是否空图视为连通
        }

        // 初始化 vertices，包含第一条边的所有节点
        Set<Integer> vertices = new HashSet<>(edges.get(0));
        for (int i = 1; i < edges.size(); i++) {
            List<Integer> edge = edges.get(i);
            boolean connected = false;

            // 检查当前边是否与已存在的 vertices 有重叠
            for (int node : edge) {
                if (vertices.contains(node)) {
                    connected = true;
                    break;
                }
            }

            // 如果没有重叠，图不连通
            if (!connected)
                return false;

            vertices.addAll(edge);
        }

        return true;
    }

    // map.key 表示 edge，
    // map.value.key 表示 label，map.value.value 则表示 size+i，可以理解为位置
    public static void update_pos(int size, List<Integer> edge, List<Integer> labels, Map<List<Integer>, Map<Integer, List<Integer>>> map) {
        // 获取对应edge的内层Map，如果不存在则创建默认的（空的）内层Map
        Map<Integer, List<Integer>> inner = map.computeIfAbsent(new ArrayList<>(edge), k -> new HashMap<>());

        for (int i = 0; i < labels.size(); i++) {
            int l = labels.get(i);
            inner.computeIfAbsent(l, k -> new ArrayList<>()).add(size + i);
        }
    }

    // 计算 matched_edges 中包含点 adj 的数量，然后从 edge_pos 中取出这些边和adj_label对应的 indices
    public static Pair<Integer, List<Integer>> cal_degree_constraint(int adj, int adj_label, List<List<Integer>> matched_edges,
                                                                     Map<List<Integer>, Map<Integer, List<Integer>>> edge_pos,
                                                                     List<List<Integer>> adj_edges) {
        int degree = 0;
        List<Integer> degree_indices = new ArrayList<>();

        for (List<Integer> matched_edge : matched_edges) {
            if (matched_edge.contains(adj)) {
                adj_edges.add(matched_edge);
                degree++;
                List<Integer> degree_index = edge_pos.get(matched_edge).get(adj_label);
                degree_indices.addAll(degree_index);
            }
        }
        degree_indices.sort(Integer::compare);
        return new Pair<>(degree, degree_indices);
    }

    /**
     * 该方法为在 matched_edges 中找到非 adj 邻边包含的同标签顶点索引，也就是说在 adj 邻边中不会出现这些索引
     * @param adj
     * @param cur_edge
     * @param adj_label
     * @param matched_edges
     * @param edge_pos
     * @return
     */
    public static List<Integer> cal_not_in_constraint(int adj, List<Integer> cur_edge, int adj_label,
                                                      List<List<Integer>> matched_edges, Map<List<Integer>, Map<Integer, List<Integer>>> edge_pos) {
        List<Integer> not_in_indices = new ArrayList<>();

        /**
         * 遍历所有已匹配的超边，找出那些与当前超边 cur_edge 不相邻（即不共享公共顶点 adj）的超边，记录标签为 adj_label 的顶点索引
         * ‼️‼️‼️ 需要知道的是，满足 !intersection.contains(adj) 条件的 edge 与 cur_edge 可能有交点也可能没有交点
         *    - 对于没有交点的情况，cur_edge 的候选超边一定没有与 edge 相同标签排列的邻边，可以作为标签约束
         *    - 而对于存在其他交点的情况，cur_edge 的候选超边的邻边也可以拥有与 edge 相同标签排列的超边，不一定要作为标签约束，只能说在交点为 adj 的情况下标签约束才成立
         */
        for (List<Integer> edge : matched_edges) {
            List<Integer> intersection = intersect(edge, cur_edge);

            // TODO 我觉得要改成 intersection.size() != 0
            if (!intersection.contains(adj)) {
                List<Integer> tempIndices = edge_pos.get(edge).get(adj_label);
                if (tempIndices != null) { // 得到 edge 中与公共顶点 adj 标签相同的索引位置
                    not_in_indices.addAll(tempIndices);
                }
            }
        }
        not_in_indices.sort(Integer::compareTo);

        return not_in_indices;
    }

    public static List<Integer> intersect(List<Integer> list1, List<Integer> list2) {
        return list1.stream()
                .filter(list2::contains)
                .collect(Collectors.toList());
    }

    private int compareEdgeLists(List<Integer> edge1, List<Integer> edge2) {
        for (int i = 0; i < Math.min(edge1.size(), edge2.size()); i++) {
            int cmp = Integer.compare(edge1.get(i), edge2.get(i));
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(edge1.size(), edge2.size());
    }

    // ‼️‼️‼️‼️ 作用的对象是部分查询当中的 last edge，计算其顶点在部分查询中的 标签、度数、以及相关联的边中的同标签顶点索引，其实就是在变相的计算 vertex profile
    public static NodeVerifier cal_node_filter(DynamicHyperGraph query_graph, List<List<Integer>> matched_edges,
                                               Map<List<Integer>, Map<Integer, List<Integer>>> edge_pos) {
        List<Integer> last_edge = matched_edges.get(matched_edges.size() - 1);
        Map<Integer, List<List<Integer>>> node_to_edge_map = new HashMap<>();

        // 构建 last_edge 中顶点到查询超边的映射
        for (int n : last_edge) {
            for (List<Integer> matched_edge : matched_edges) {
                if (matched_edge.contains(n)) {
                    node_to_edge_map.computeIfAbsent(n, k -> new ArrayList<>()).add(new ArrayList<>(matched_edge));
                }
            }
        }

        Comparator<List<Integer>> lexComparator = (list1, list2) -> {
            if (list1 == null && list2 == null) return 0;
            if (list1 == null) return -1;
            if (list2 == null) return 1;

            int size1 = list1.size();
            int size2 = list2.size();
            int minSize = Math.min(size1, size2);

            for (int i = 0; i < minSize; i++) {
                Integer elem1 = list1.get(i);
                Integer elem2 = list2.get(i);

                if (elem1 == null && elem2 == null) continue;
                if (elem1 == null) return -1;
                if (elem2 == null) return 1;

                int cmp = elem1.compareTo(elem2);
                if (cmp != 0) {
                    return cmp;
                }
            }
            // 如果前 minSize 个元素相同，较短的列表排前面
            return Integer.compare(size1, size2);
        };

        // 组织 last_edge 中是公共顶点的 顶点id、关联的边、顶点标签
        List<NodeEdgeLabel> node_to_edge_and_label = node_to_edge_map.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)  // 该顶点至少存在于两条边中
                .map(entry -> {
                    Integer nodeId = entry.getKey();
                    List<List<Integer>> edges = new ArrayList<>(entry.getValue());
                    // 对边列表进行排序，Java 中 List.sort() 默认是稳定排序
                    edges.sort(lexComparator);
                    int label = query_graph.getNodeLabel(nodeId);
                    return new NodeEdgeLabel(nodeId, edges, label);
                })
                .sorted() // 使用 NodeEdgeLabel 的 compareTo 方法
                .collect(Collectors.toList());

        // 根据边和标签对顶点进行分组。nodeGroups 的 key 是边和顶点标签，value 是顶点id
        Map<EdgeLabelKey, List<Integer>> nodeGroups = new HashMap<>();
        for (NodeEdgeLabel nel : node_to_edge_and_label) {
            Set<List<Integer>> edgesSet = new HashSet<>(nel.getEdges());
            EdgeLabelKey key = new EdgeLabelKey(edgesSet, nel.getLabel());
            nodeGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(nel.getNodeId());
        }

        // 将 nodeGroups 转换为列表，其中的每个元素是一条 entry 的组合对象
        List<GroupEntry> nodeGroupsVec = nodeGroups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new GroupEntry(entry.getKey().getEdges(), entry.getKey().getLabel(), entry.getValue()))
                .collect(Collectors.toList());

        // ‼️‼️‼️‼️ 同下标 i 下，表示有 nodeNum[i] 个顶点同时出现在 edgeNum[i] 条边里，indices[i] 则记录这些边里标签为 label 的顶点索引位置
        List<List<Integer>> indices = new ArrayList<>();  // edge 中标签为 label 的索引位置
        List<Integer> edgeNums = new ArrayList<>(); // edge 的数量
        List<Integer> nodeNums = new ArrayList<>();   // node 的数量

        for (GroupEntry group : nodeGroupsVec) {
            Set<List<Integer>> edges = group.getEdges(); // edges 为顶点所被包含的边
            int label = group.getLabel(); // label 是 group 中顶点的标签
            List<Integer> nodes = group.getNodes(); // nodes 中的顶点同时存在于 edges 中且顶点标签为 label

            List<Integer> index = edges.stream().flatMap(edge -> {  // index 表示 group 里每条 edge 中标签为 label 的索引位置
                Map<Integer, List<Integer>> posMap = edge_pos.get(edge);
                if (posMap != null && posMap.containsKey(label)) {
                    return posMap.get(label).stream();
                }
                return Stream.empty();
            }).sorted().collect(Collectors.toList());

            int edgeNum = edges.size();
            int nodeNum = nodes.size();

            indices.add(index);
            edgeNums.add(edgeNum);
            nodeNums.add(nodeNum);
        }

        // 构建 GroupEntry 之间标签相同，且 edges 为子集的包含关系
        // contains 的长度和 nodeGroupsVec 的长度一样，idx=0 的值为当前 group 的 edgeSet 被其他索引位置的 group 的 edgeSet 包含，且顶点的标签相同
        List<List<Integer>> contains = new ArrayList<>();
        for (int i = 0; i < nodeGroupsVec.size(); i++) {
            GroupEntry groupEntry1 = nodeGroupsVec.get(i);
            Set<List<Integer>> edges1 = groupEntry1.getEdges();
            int label1 = groupEntry1.getLabel();

            List<Integer> contains_elem = new ArrayList<>();
            for (int j = 0; j < nodeGroupsVec.size(); j++) {
                if (i == j) continue;

                GroupEntry group2 = nodeGroupsVec.get(j);
                int label2 = group2.getLabel();

                if (label1 != label2) continue;

                Set<List<Integer>> edges2 = group2.getEdges();

                if (edges2.containsAll(edges1)) { // 判断 edges1 是否是 edges2 的子集
                    contains_elem.add(j);
                }
            }

            Collections.sort(contains_elem);
            contains.add(contains_elem);
        }

        return new NodeVerifier(indices, edgeNums, nodeNums, contains);
    }


}
