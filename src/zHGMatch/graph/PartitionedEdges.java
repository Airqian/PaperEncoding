package zHGMatch.graph;

import java.util.*;

// partitioned_edges.rs
public class PartitionedEdges {
    private List<Integer> nodeLabels;

    private HashMap<List<Integer>, EdgePartition> map; // map 就是边标签到边的分区，key相当于分区签名，详细信息保存在value里，value包括了该分区内的所有边

    public PartitionedEdges(List<Integer> nodeLabels) {
        this.nodeLabels = nodeLabels;
        this.map = new HashMap<>();
    }

    public PartitionedEdges(List<Integer> nodeLabels, HashMap<List<Integer>, EdgePartition> map) {
        this.nodeLabels = nodeLabels;
        this.map = map;
    }

    public PartitionedEdgesWrapper toPartitionedEdgesWrapper() {
        List<PartitionedEdgesWrapper.EdgePartitionMapEntry> newMap = new ArrayList<>();
        for (List<Integer> key : this.map.keySet())
            newMap.add(new PartitionedEdgesWrapper.EdgePartitionMapEntry(new ArrayList<>(key), map.get(key).toEdgePartitionWrapper()));
        return new PartitionedEdgesWrapper(new ArrayList<>(this.nodeLabels), newMap);
    }

    public int num_nodes() {
        return this.nodeLabels.size();
    }

    public int num_edges() {
        return this.map.values().stream().mapToInt(EdgePartition::num_edges).sum();
    }

    public List<EdgePartition> edge_indices() {
        return new ArrayList<>(this.map.values());
    }

    public boolean has_edge_label(List<Integer> labels) {
        return this.map.containsKey(labels); // list中的元素相同的话得到的结果会是true
    }

    public int get_edge_label_frequency(List<Integer> nodeLabels) {
        EdgePartition edgePartition = this.map.get(nodeLabels);
        return edgePartition == null ? 0 : edgePartition.num_edges();
    }

    public int get_node_label(int node) {
        return this.nodeLabels.get(node-1);
    }

    public EdgePartition get_partition(List<Integer> labels) {
        return map.get(labels);
    }

    // 添加一条边，将处理后的边添加到合适的EdgePartition分区中
    public void add_edge(List<Integer> edge) {
        // 1. 将 edge 变成一个去重后的有序状态
        // 去重的原因是因为超边中没有重复的顶点（顶点id不能重复），但是顶点的标签是可以重复的
        edge.sort(Integer::compareTo);
        List<Integer> uniqueEdge = new ArrayList<>(new HashSet<>(edge));

        // 2. labelsMap 用于构建节点标签到节点id列表的映射关系，并将节点id列表排序
        HashMap<Integer, List<Integer>> labelsMap = new HashMap<>();
        int arity = uniqueEdge.size(); // 去重后的边的长度

        for (int node : uniqueEdge) {
            int label = get_node_label(node); // 获取当前节点的标签
            List<Integer> nodes = labelsMap.getOrDefault(label, new ArrayList<>());
            nodes.add(node);
            labelsMap.put(label, nodes);
        }

        for (List<Integer> list : labelsMap.values())
            list.sort(Integer::compareTo);

        // 3. 这一步将 labelsMap 中的标签和顶点关系打平，经过整个循环后，labels 和 new_edge 就构建好了符合特定规则的新的边标签列表和边节点列表。
        List<Integer> labels = new ArrayList<>(arity);
        List<Integer> newEdge = new ArrayList<>(arity);

        labelsMap.entrySet().stream()
                .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                .forEach(entry -> {
                    int label = entry.getKey();
                    List<Integer> nodes = entry.getValue();
                    for (int i = 0; i < nodes.size(); i++)
                        labels.add(label);
                    newEdge.addAll(nodes);
                });

        // 4. 将新的边添加到分区中或者创建一个新的分区
        List<Integer> labelVec = new ArrayList<>(labels);
        EdgePartition edgePartition = this.map.getOrDefault(labelVec, new EdgePartition(labels));
        this.map.put(labelVec, edgePartition);
        edgePartition.addEdge(newEdge);
    }

    public void sort_index(boolean build_index) {
        this.map.entrySet().stream().forEach(entry -> entry.getValue().sortEdges(build_index));
    }

    public void build_index(){
        this.sort_index(true);
    }

    public void drop_index() {
        this.map.entrySet().forEach(entry -> entry.getValue().dropIndex());
    }

    public int graph_size() {
        int idSize = Integer.BYTES;
        int labelsSize = (nodeLabels.size() + 2) * idSize;
        int edgesSize = map.entrySet().stream()
                .mapToInt(entry -> entry.getValue().graphSize() + (entry.getKey().size() + 2) * idSize)
                .sum();
        return labelsSize + edgesSize;
    }

    public int index_size() {
        return map.entrySet().stream().mapToInt(entry -> entry.getValue().indexSize()).sum();
    }

    // 输出状态信息的方法，对应原Rust代码中的status方法
    public void status() {
        System.out.println("Number of nodes: " + num_nodes());
        System.out.println("Number of edges: " + num_edges());
        System.out.println("Distinct edge labels: " + this.map.size());

        int minArity = Integer.MAX_VALUE;
        int maxArity = Integer.MIN_VALUE;
        for (List<Integer> key : this.map.keySet()) {
            minArity = Math.min(key.size(), minArity);
            maxArity = Math.max(key.size(), maxArity);
        }

        System.out.println("Min/Max arity：" + minArity + "," + maxArity);

        System.out.println("Graph Size = " + graph_size() + ", Index Size = " + index_size());
    }

    public static void main(String[] args) {
        // 测试通过
        List<Integer> labels = new ArrayList<>(Arrays.asList(0,0,0,0,1));
        PartitionedEdges partitionedEdges = new PartitionedEdges(new ArrayList<>(labels));
        partitionedEdges.add_edge(new ArrayList<>(Arrays.asList(0,1)));
        partitionedEdges.add_edge(new ArrayList<>(Arrays.asList(1,2)));
        partitionedEdges.add_edge(new ArrayList<>(Arrays.asList(2,3)));

        partitionedEdges.add_edge(new ArrayList<>(Arrays.asList(0,1,2)));
        partitionedEdges.add_edge(new ArrayList<>(Arrays.asList(1,2,3)));

        partitionedEdges.add_edge(new ArrayList<>(Arrays.asList(0,1,2,3,4)));
        partitionedEdges.build_index();
        partitionedEdges.status();
    }
}
