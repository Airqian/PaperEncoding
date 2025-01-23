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

    public List<List<Integer>> extend(List<Integer> partial, PartitionedEdges data_graph) {
        // 获取对应的 Partition
        EdgePartition partition = data_graph.get_partition(this.extended_labels);

        // ‼️‼️‼️‼️ 返回的是部分嵌入中的顶点，这些顶点里可能存在与当前待匹配超边的公共顶点
        // node_selectors 中的 node_selector 是待匹配超边与 partial 中的超边的所有公共顶点相关的约束
        List<List<Integer>> nodes = this.node_selectors.stream()
                .map(selector -> selector.select(partial)).collect(Collectors.toList());

        // 接着通过倒排索引找出nodes中每组node共同出现的 f(eq)
        List<List<Integer>> rows = nodes.stream().map(n -> partition.getRowsOfNodes(n))
                .collect(Collectors.toList());

        // 对找到的f(eq)取交集，能理解
        List<Integer> intersected = rows.stream()
                .reduce((set1, set2) -> set1.stream()
                        .filter(set2::contains)
                        .collect(Collectors.toList())).orElse(new ArrayList<>());

        // 计算新节点数量（要扩展的新节点个数）
        Set<Integer> embeddingSet = new HashSet<>(partial);
        int num_new_nodes = num_nodes - embeddingSet.size();

        // 获取交集行对应的边，并过滤不满足新节点数量的边
        List<List<Integer>> extended = intersected.stream()
                .map(r -> partition.getEdge(r)) // 根据边的下标得到边的顶点列表
                .filter(e -> e.stream()
                        .filter(n -> !embeddingSet.contains(n))  // 过滤掉公共顶点，剩下的顶点个数是否等于需要扩展的新节点个数
                        .count() == num_new_nodes)
                .collect(Collectors.toList()); // 如果满足的话，就把这条边留下来

        // 合并 partial 和找到的新的候选超边
        List<List<Integer>> results = extended.stream()
                .map(ext -> {
                    List<Integer> combined = new ArrayList<>();
                    combined.addAll(partial);
                    combined.addAll(ext);
                    return combined;
                })
                .collect(Collectors.toList());

        // ‼️‼️‼️‼️ 这里应该是嵌入验证了，如果 filter 返回 false 就过滤，返回 true 则保留
        if (this.node_filter != null)
            results = results.stream().filter(node_filter::filter).collect(Collectors.toList());

        return results;
    }
}
