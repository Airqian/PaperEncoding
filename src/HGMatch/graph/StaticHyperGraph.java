package HGMatch.graph;

import HGMatch.graph.util.Canonize;

import java.util.*;
import java.util.stream.Collectors;

/**
 * labels: [1, 1, 1, 2, 1, 1, 1]
 * edges：[[0,6], [1,2,3,4,5,6]]
 * 如果换成标签，那么 edges 可以表示为：[[1, 1], [1, 1, 2, 1, 1, 1]]，超图中是有重复的标签的
 */
public class StaticHyperGraph extends DynamicToStaticConverter implements Canonize {
    private List<Integer> labels;      // 顶点列表，list.size表示不同的顶点个数，顶点i的标签为labels.get(i)，i表示下标
    private List<List<Integer>> edges; // 静态超图中的所有边，edges 中的数字表示顶点，对应labels中的下标
    private List<List<Integer>> adj;   // 邻接表

    public StaticHyperGraph(List<Integer> labels, List<List<Integer>> edges) {
        this.labels = labels;
        this.edges = edges;
        this.adj = new ArrayList<>();

        // 初始化邻接表adj，长度和labels一样，初始每个元素为一个空的ArrayList
        for (int i = 0; i < labels.size(); i++) {
            this.adj.add(new ArrayList<>());
        }

        // 遍历每条边中的每个顶点v，将当前边edge中除了当前节点v之外的其他节点添加到以v为索引对应的邻接表列表中
        for (List<Integer> edge : edges) {
            for (int i = 0; i < edge.size(); i++) {
                int v = edge.get(i);
                adj.get(v).addAll(edge.subList(0, i));
                adj.get(v).addAll(edge.subList(i + 1, edge.size()));
            }
            // 对当前边进行排序，这里使用稳定排序（Collections.sort是稳定排序）
            Collections.sort(edge);
        }

        // 对所有的边进行排序（类似字符串的比较规则）
        Collections.sort(edges, Comparator.comparingInt(a -> a.get(0))); // Sort edges

        // 处理邻接表中的元素并进行去重
        for (List<Integer> list : adj) {
            Collections.sort(list);
            removeDuplicates(list);
        }

    }
    // 辅助方法用于去除列表中的重复元素，这里简单实现一种方式
    private void removeDuplicates(List<Integer> list) {
        List<Integer> result = new ArrayList<>();
        for (int num : list) {
            if (!result.contains(num)) {
                result.add(num);
            }
        }
        list.clear();
        list.addAll(result);
    }

    // 这里重新实现rust提供的现成库canonical_form里的方法
    // 实现Canonize接口中的size方法，返回点的个数
    public int size() {
        return labels.size();
    }

    // 实现Canonize接口中的applyMorphism方法，这是一个置换方法（能理解为同构映射吗）
    public StaticHyperGraph applyMorphism(int[] p) {
        List<Integer> newLabels = new ArrayList<>();
        for (int i = 0; i < size(); i++) {
            newLabels.add(0);
        }
        for (int i = 0; i < labels.size(); i++) {
            newLabels.set(p[i], labels.get(i));
        }

        List<List<Integer>> newEdges = new ArrayList<>();
        for (List<Integer> e : edges) {
            List<Integer> edge = e.stream().map(v -> p[v]).collect(Collectors.toList());
            newEdges.add(edge);
        }

        return new StaticHyperGraph(newLabels, newEdges);
    }

    // 实现Canonize接口中的invariantColoring方法
    public Optional<List<Integer>> invariantColoring() {
        return Optional.of(labels.stream().map(l -> l).collect(Collectors.toList()));
    }

    // 实现Canonize接口中的invariantNeighborhood方法
    // 获取 u 的邻接顶点列表，将其中的元素添加到 neighborhood 中，然后返回 result
    public List<List<Integer>> invariantNeighborhood(int u) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> neighborhood = adj.get(u).stream().map(v -> v).collect(Collectors.toList());
        result.add(neighborhood);
        return result;
    }

    // TODO：返回一个组合对象的规范形式
    public StaticHyperGraph canonical() {

        return this;
    }

    public int numsNodes() {
        return this.labels.size();
    }

    public int numsEdges() {
        return this.edges.size();
    }
}
