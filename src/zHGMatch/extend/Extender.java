package zHGMatch.extend;

import zHGMatch.graph.EdgePartition;
import zHGMatch.graph.PartitionedEdges;

import java.util.*;
import java.util.stream.Collectors;

public class Extender {
    private int num_nodes;
    private List<Integer> extended_labels;
    private List<NodeSelector> node_selectors;
    private NodeVerifier node_filter;

    public Extender(int num_nodes, List<Integer> extended_labels, List<NodeSelector> node_selectors) {
        this.num_nodes = num_nodes;
        this.extended_labels = extended_labels;
        this.node_selectors = node_selectors;
        this.node_filter = null;
    }

    public void setNode_filter(NodeVerifier node_filter) {
        this.node_filter = node_filter;
    }

    public List<List<Integer>> extend(List<Integer> partial, PartitionedEdges edges) {
        // 获取对应的 Partition
        EdgePartition partition = edges.get_partition(extended_labels);

        // 使用节点选择器选择节点
        List<List<Integer>> nodes = this.node_selectors.stream()
                .map(selector -> selector.select(partial)).collect(Collectors.toList());

        // 获取这些节点对应的行索引
        List<List<Integer>> rows = nodes.stream().map(n -> partition.getRowsOfNodes(n))
                .collect(Collectors.toList());

        // 对 rows 中的集合求交集
        List<Integer> intersected = rows.stream()
                .reduce((set1, set2) -> set1.stream()
                        .filter(set2::contains)
                        .collect(Collectors.toList())).orElse(new ArrayList<>());

        // 创建嵌入集合，并计算新节点数量
        Set<Integer> embeddingSet = new HashSet<>(partial);
        int num_new_nodes = num_nodes - embeddingSet.size();

        // 获取交集行对应的边，并过滤满足新节点数量的边
//        List<List<Integer>> extended = intersected.stream().map(r -> partition.getEdge(r))
//                .filter(e -> {
//                    long count = e.stream().filter(n -> embeddingSet.contains(n)).count();
//                    return count == num_new_nodes;
//                }).collect(Collectors.toList());

        List<List<Integer>> extended = intersected.stream()
                .map(r -> partition.getEdge(r)) // 将边的下标映射成边的顶点集合
                .filter(e -> e.stream()
                        .filter(n -> !embeddingSet.contains(n))  // // 过滤出不在 embedding_set 中的节点
                        .count() == num_new_nodes)  // // 保留那些不在 embedding_set 中的节点数量等于 num_new_nodes 的边
                .collect(Collectors.toList());

        // 合并结果，看起来像是为 extended 的每个元素都扩展 partial
        List<List<Integer>> results = extended.stream()
                .map(ext -> {
                    List<Integer> combined = new ArrayList<>();
                    combined.addAll(partial);
                    combined.addAll(ext);
                    return combined;
                })
                .collect(Collectors.toList());

        // 如果设置了节点过滤器，保留通过过滤器的结果
        if (this.node_filter != null)
            results = results.stream().filter(node_filter::filter).collect(Collectors.toList());

        return results;
    }
}
