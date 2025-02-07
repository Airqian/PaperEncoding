package zNIWGraph.graph;

import java.util.*;

public class DynamicHyperGraph {
    // 从querygraph的labels构建而来，key是labels的下标，value是其上对应的值
    private HashMap<Integer, Integer> labels;  // key 是顶点id，value 是顶点对应的标签
    private List<List<Integer>> edges;         // 其中的每一个元素是一条边。如果和 StaticHyperGraph 一致的话，edges 中的数字保存的是点的 id，通过 labels 拿到标签

    // 构造函数，用于创建默认的DynamicHyperGraph实例，初始化labels和edges
    public DynamicHyperGraph() {
        this.labels = new HashMap<>();
        this.edges = new ArrayList<>();
    }

    public static DynamicHyperGraph withCapacity(int n, int m) {
        DynamicHyperGraph graph = new DynamicHyperGraph();
        graph.labels = new HashMap<>(n);  // labels: IndexMap::with_capacity_and_hasher(n, BuildHasher::default()),
        graph.edges = new ArrayList<>(m);
        return graph;
    }

    public HashMap<Integer, Integer> getLabels() {
        return labels;
    }

    // 模拟edges方法，返回edges字段的不可变引用（在Java中返回List的只读视图）
    public List<List<Integer>> getEdges() {
        return edges;  // 原文返回的是一个不可变引用
    }

    public void addNode(int id, int label) {
        labels.put(id, label);
    }

    // 扩展节点集合，接受实现了Iterator接口的迭代器，将迭代器中的元素（键值对形式）添加到labels中
    public void extendNodes(Iterator<Map.Entry<Integer, Integer>> iter) {
        while (iter.hasNext()) {
            Map.Entry<Integer, Integer> entry = iter.next();
            labels.put(entry.getKey(), entry.getValue());
        }
    }

    public void addEdge(List<Integer> edge) {
        edges.add(edge);
    }

    // 模拟into_static方法，将DynamicHyperGraph实例转换为StaticHyperGraph实例（这里简单返回一个模拟的StaticHyperGraph实例，具体转换逻辑根据实际完善）
    // TODO 转的是key还是value
    // 将可变的DynamicHyperGraph实例自身转换为StaticHyperGraph实例（消耗自身），对应Rust中的into_static方法
    public StaticHyperGraph intoStatic() {
        return StaticHyperGraph.from(this);
    }

    // 先克隆当前的DynamicHyperGraph实例，然后调用into_static方法将克隆后的实例转换为StaticHyperGraph实例，对应Rust中的to_static方法
    public StaticHyperGraph toStatic() {
        DynamicHyperGraph cloned = new DynamicHyperGraph();
        cloned.labels = new HashMap<>(this.labels);
        cloned.edges = new ArrayList<>(this.edges);
        return cloned.intoStatic();
    }

    // 扩展边的集合，对应Rust中的extend_edges方法
    public void extendEdges(Iterator<List<Integer>> iter) {
        while (iter.hasNext()) {
            edges.add(iter.next());
        }
    }

    // 获取节点数量的方法，对应Rust中的num_nodes方法
    public int nums_nodes() {
        return labels.size();
    }

    // 模拟num_edges方法，返回边的数量，即edges列表的大小
    public int num_edges() {
        return edges.size();
    }

    // 模拟get_node_label方法，根据节点标识获取对应的节点标签，从labels映射中获取
    public int getNodeLabel(int node) {
        return labels.get(node);
    }

    // 模拟get_edge_labels方法，根据给定的边（节点标识列表形式）获取边中所有节点对应的标签，进行排序后返回
    public List<Integer> getEdgeLabels(List<Integer> edge) {
        List<Integer> labelsList = new ArrayList<>();
        for (int nodeId : edge) {
            labelsList.add(labels.get(nodeId));
        }
        labelsList.sort(Integer::compareTo);
        return labelsList;
    }

    // 返回图的规范形式，对应Rust中的canonical_form方法
    public StaticHyperGraph canonicalForm() {
        return toStatic().canonical();
    }
}
