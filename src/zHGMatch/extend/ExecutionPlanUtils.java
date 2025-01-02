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

    // TODO 生成排列顺序？？
    public static List<List<List<Integer>>> all_matching_order(QueryGraph graph) {
        List<List<List<Integer>>> results = new ArrayList<>();
        List<List<Integer>> edges = new ArrayList<>(graph.to_graph().getEdges());

        // 生成边的排列组合并进行筛选
        List<List<List<Integer>>> permutations = generatePermutations(edges);
        for (List<List<Integer>> order : permutations) {
            if (is_connected(order))
                results.add(order);
        }
        return results;
    }

    /**
     * TODO 计算匹配顺序
     * 首先根据每条边所在的分区边的数量定一个基本的匹配顺序，然后从顺序内的第一条边开始往后顺序找连通的边以确定最终的匹配顺序
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

        // 解释：--- 得到每条edge对应的分区内边的数量，也即按边标签的频率排序
        queryEdges.sort(Comparator.comparingInt(edge -> {
            List<Integer> labels = query_graph.getEdgeLabels(edge);
            return edges.get_edge_label_frequency(labels);
        }));

        // 克隆第一条边并移除
        List<Integer> firstEdge = new ArrayList<>(queryEdges.get(0));
        queryEdges.remove(0);

        // 初始化 matchedEdges 和 matchedNodes
        List<List<Integer>> matchedEdges = new ArrayList<>();
        matchedEdges.add(firstEdge);

        Set<Integer> matchedNodes = new HashSet<>(firstEdge);

        // 解释：--- 迭代匹配剩余的边
        while (!queryEdges.isEmpty()) {
            boolean found = false;

            Iterator<List<Integer>> iterator = queryEdges.iterator();
            while (iterator.hasNext()) {
                List<Integer> edge = iterator.next();
                // 检查是否有任何节点已经匹配
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

    // 计算 matched_edges 中有哪些边包含点 adj，然后从 edge_pos 中取出这些边和adj_label对应的 indices
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

    // 计算顶点 adj 在所有 matched_edges 中不在当前边 cur_edge 中的标签对应的位置索引。
    public static List<Integer> cal_not_in_constraint(int adj, List<Integer> cur_edge, int adj_label,
                                                      List<List<Integer>> matched_edges, Map<List<Integer>, Map<Integer, List<Integer>>> edge_pos) {
        List<Integer> not_in_indices = new ArrayList<>();

        for (List<Integer> edge : matched_edges) {
            List<Integer> intersection = intersect(edge, cur_edge);

            if (!intersection.contains(adj)) {
                List<Integer> tempIndices = edge_pos.get(edge).get(adj_label);
                if (tempIndices != null) {
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

    public static NodeVerifier cal_node_filter(DynamicHyperGraph query_graph, List<List<Integer>> matched_edges,
                                               Map<List<Integer>, Map<Integer, List<Integer>>> edge_pos) {
        List<Integer> last_edge = matched_edges.get(matched_edges.size() - 1);
        Map<Integer, List<List<Integer>>> node_to_edge_map = new HashMap<>(); // 构建节点到边的映射

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

        // 过滤出在多个边中出现的节点，并关联标签  Vec<(u32, (Vec<Vec<u32>>, u32))>
//        List< Pair<Integer, Pair<List<List<Integer>>, Integer> >> node_to_edge_and_label = new ArrayList<>();
//        for (Map.Entry<Integer, List<List<Integer>>> entry : node_to_edge_map.entrySet()) {
//            int key = entry.getKey();
//            List<List<Integer>> value = entry.getValue();
//
//            if (value.size() > 1) {
//                int label = query_graph.getNodeLabel(key);
//                // 排序边（按某种顺序，假设按自然顺序）
//                value.sort(lexComparator);
//                node_to_edge_and_label.add(new Pair<>(key, new Pair<>(value, label)));
//            }
//
//        }
//        node_to_edge_and_label.sort((l1, l2) -> (l1.getKey() - l2.getKey()));

        // 分组节点 HashMap<(Vec<Vec<u32>>, u32), Vec<u32>
//        Map<Pair<Set<List<Integer>>, Integer>, List<Integer>> node_groups = new HashMap<>();
//        for (Pair<Integer, Pair<List<List<Integer>>, Integer>> pair : node_to_edge_and_label) {
//            int node = pair.getKey();
//            Pair<List<List<Integer>>, Integer> edgeAndLabel = pair.getValue();
//            Set<List<Integer>> edgeSet = new HashSet<>(edgeAndLabel.getKey());
//            int label = edgeAndLabel.getValue();
//
//            Pair<Set<List<Integer>>, Integer> groupKey = new Pair<>(edgeSet, label);
//            node_groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(node);
//        }

        // 【转换为列表并排序
//        List<Pair<Pair<Set<List<Integer>>, Integer>, List<Integer>>> node_groups_vec = new ArrayList<>();
//        for (Map.Entry<Pair<Set<List<Integer>>, Integer>, List<Integer>> entry : node_groups.entrySet()) {
//            node_groups_vec.add(new Pair<>(entry.getKey(), entry.getValue()));
//        }

        // 【新写的】
        // // 过滤并处理节点与边
        List<NodeEdgeLabel> node_to_edge_and_label = node_to_edge_map.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1) // 过滤出连接到多于一条边的节点
                .map(entry -> {
                    Integer nodeId = entry.getKey();
                    List<List<Integer>> edges = new ArrayList<>(entry.getValue());
                    // 对边列表进行排序，Java 中 List.sort() 默认是稳定排序
                    edges.sort(lexComparator);
                    Integer label = query_graph.getNodeLabel(nodeId);
                    return new NodeEdgeLabel(nodeId, edges, label);
                })
                .sorted() // 使用 NodeEdgeLabel 的 compareTo 方法
                .collect(Collectors.toList());

        // 根据边和标签对节点进行分组
        Map<EdgeLabelKey, List<Integer>> nodeGroups = new HashMap<>();
        for (NodeEdgeLabel nel : node_to_edge_and_label) {
            Set<List<Integer>> edgesSet = new HashSet<>(nel.getEdges());
            EdgeLabelKey key = new EdgeLabelKey(edgesSet, nel.getLabel());
            nodeGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(nel.getNodeId());
        }

        // 转换分组为包含 HashSet 的结构，并排序
        List<GroupEntry> nodeGroupsVec = nodeGroups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new GroupEntry(entry.getKey().getEdges(), entry.getKey().getLabel(), entry.getValue()))
                .collect(Collectors.toList());

        // List<Pair<Pair<Set<List<Id>>, String>, List<Id>>> nodeGroupsVec = nodeGroups.entrySet().stream()
        //         .sorted(Comparator.comparing(entry -> entry.getKey().getKey().toString()))
        //         .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
        //         .collect(Collectors.toList());

        // 构建 indices, degrees, len_s
        List<List<Integer>> indices = new ArrayList<>();
        List<Integer> degrees = new ArrayList<>();
        List<Integer> len_s = new ArrayList<>();
        for (GroupEntry group : nodeGroupsVec) {
            Set<List<Integer>> edges = group.getEdges();
            int label = group.getLabel();
            List<Integer> nodes = group.getNodes();

            List<Integer> index = edges.stream().flatMap(edge -> {
                Map<Integer, List<Integer>> posMap = edge_pos.get(edge);
                if (posMap != null && posMap.containsKey(label)) {
                    return posMap.get(label).stream();
                }
                return Stream.empty();
            }).sorted().collect(Collectors.toList());

            int degree = edges.size();
            int len = nodes.size();

            indices.add(index);
            degrees.add(degree);
            len_s.add(len);
        }
//        for (Pair<Pair<Set<List<Integer>>, Integer>, List<Integer>> group : node_groups_vec) {
//            Pair<Set<List<Integer>>, Integer> edgeAndLabel = group.getKey();
//            Set<List<Integer>> edges = edgeAndLabel.getKey();
//            int label = edgeAndLabel.getValue();
//            List<Integer> nodes = group.getValue();
//
//            List<Integer> index = edges.stream().flatMap(edge -> {
//                Map<Integer, List<Integer>> posMap = edge_pos.get(edge);
//                if (posMap != null && posMap.containsKey(label)) {
//                    return posMap.get(label).stream();
//                }
//                return Stream.empty();
//            }).sorted().collect(Collectors.toList());
//
//            int degree = edges.size();
//            int len = nodes.size();
//            indices.add(index);
//            degrees.add(degree);
//            len_s.add(len);
//        }

        // 构建 contains，会计算包含关系
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
//        for (int i = 0; i < node_groups_vec.size(); i++) {
//            Pair<Pair<Set<List<Integer>>, Integer>, List<Integer>> group1 = node_groups_vec.get(i);
//            Set<List<Integer>> edge1 = group1.getKey().getKey();
//            int label1 = group1.getKey().getValue();
//
//            List<Integer> contains_elem = new ArrayList<>();
//            for (int j = 0; j < node_groups_vec.size(); j++) {
//                if (i == j)
//                    continue;
//                Pair<Pair<Set<List<Integer>>, Integer>, List<Integer>> group2 = node_groups_vec.get(j);
//                Set<List<Integer>> edge2 = group2.getKey().getKey();
//                int label2 = group2.getKey().getValue();
//                if (label1 != label2)
//                    continue;
//
//                if (edge2.containsAll(edge1))
//                    contains_elem.add(j);
//            }
//
//            Collections.sort(contains_elem);
//            contains.add(contains_elem);
//        }
        return new NodeVerifier(indices, degrees, len_s, contains);
    }


}
