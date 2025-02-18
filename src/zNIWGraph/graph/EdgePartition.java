package zNIWGraph.graph;

import zNIWGraph.graph.util.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//

/**
 * edge_partition.rs 该文件应该就是建立的超边分区以及包含的倒排索引
 * arity = 3
 * labels = [1,1,2]
 * edges = [[0,2,1], [3,5,6]]，有顶点 0、2、3、5 的标签为 1，顶点 1、6 的标签为 2
 * getRowsOfNode(0) = 0 （第一条超边中包含顶点0）
 */
public class EdgePartition {
    private int arity;  // 表示该分区边的度数（超边中的顶点个数）
    private List<Integer> labels;  // 该分区的标签 [1,1,2]
    private List<Integer> edges;   // 该分区包含的超边（包含每条边的顶点id）
    private HashMap<Integer, List<Integer>> index;  // 顶点到超边的倒排索引

    public EdgePartition(List<Integer> labels) {
        this.arity = labels.size();
        this.labels = new ArrayList<>(labels);
        this.edges = new ArrayList<>();
        index = new HashMap<>();
    }

    public EdgePartition(List<Integer> labels, List<Integer> edges) {
        this.arity = labels.size();
        this.labels = labels;
        this.edges = edges;
        this.index = new HashMap<>();
    }

    public EdgePartition (int arity, List<Integer> labels, List<Integer> edges, HashMap<Integer, List<Integer>> index) {
        this.arity = arity;
        this.labels = labels;
        this.edges = edges;
        this.index = index;
    }

    // 从 EdgePartition 转换成 EdgePartitionWrapper
    public EdgePartitionWrapper toEdgePartitionWrapper() {
        List<Pair<Integer, List<Integer>>> newIndex = new ArrayList<>();
        for (int key : index.keySet())
            newIndex.add(new Pair<>(key, new ArrayList<>(index.get(key))));
        return new EdgePartitionWrapper(arity, new ArrayList<>(labels), new ArrayList<>(edges), newIndex);
    }

    public int getArity() {
        return arity;
    }

    public List<Integer> getLabels() {
        return labels;
    }

    public List<Integer> getEdges() {
        return edges;
    }

    public int num_nodes() {
        if (index.size() != 0)
            return index.size();

        // 如果还没建立倒排索引则返回edges中的顶点
        return (int) edges.stream().distinct().count();
    }

    // 获取边的数量需要用超图中所有顶点的数量除以每条超边包含的顶点数
    public int num_edges() {
        return this.edges.size() / this.arity;
    }

    // 判断哪几条超边中包含node
    public List<Integer> getRowsOfNode(int node) {
        if (index.size() != 0) { // 判断index是不是null的方式是判断其size
            List<Integer> rows = index.get(node);
            return rows != null ? rows : Collections.emptyList();
        } else {
            List<Integer> result = new ArrayList<>();
            for (int row = 0; row < this.num_edges(); row++) {
                int start = row * arity;
                int end = start + arity;
                List<Integer> edge = edges.subList(start, end);
                if (edge.contains(node))
                    result.add(row);
            }
            return result;
        }
    }

    public List<Integer> getRowsOfNodes(List<Integer> nodes) {
        if (nodes.size() == 1)
            return getRowsOfNode(nodes.get(0));

        List<Integer> result = nodes.stream().map(this::getRowsOfNode)
                .flatMap(List::stream).distinct().collect(Collectors.toList());
        return result;
    }

    // 获取第row条超边
    public List<Integer> getEdge(int row) {
        int start = row * arity;
        int end = Math.min(start + arity, edges.size());
        return edges.subList(start, end);
    }

    // 添加超边，edge 包含该条超边中的所有顶点 id
    public void addEdge(List<Integer> edge) {
        if (edge.size() != arity)
            throw new IllegalArgumentException("Edge size does not match arity.");
        edges.addAll(edge);
    }

    // 对 edges 进行排序并选择是否构建索引
    public void sortEdges(boolean build_index) {
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

        // 将边按顶点数进行分组
        List<List<Integer>> edgeChunks = IntStream.range(0, edges.size() / arity)
                .mapToObj(i -> edges.subList(i * arity, (i + 1) * arity))
                .sorted(lexComparator)
                .distinct()
                .collect(Collectors.toList());

        // 展平成一个列表
        List<Integer> sortedEdges = edgeChunks.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // 更新 edges
        this.edges = new ArrayList<>(sortedEdges);

        if (build_index) {
            HashMap<Integer, List<Integer>> newIndex = new HashMap<>();

            for (int row = 0; row < num_edges(); row++) {
                int start = row * arity;
                List<Integer> edge = edges.subList(start, start + arity);
                for (int node : edge) {
                    newIndex.computeIfAbsent(node, k -> new ArrayList<>()).add(row);
                }
            }

            this.index = newIndex;
        }
    }

    /**
     * 构建索引。
     */
    public void buildIndex() {
        sortEdges(true);
    }

    /**
     * 删除索引。
     */
    public void dropIndex() {
        this.index = new HashMap<>();
    }

    public int graphSize() {
        int intSize = Integer.BYTES;
        return (labels.size() + edges.size() + 4) * intSize;
    }

    public int indexSize() {
        int intSize = Integer.BYTES;
        return index.size() != 0 ? index.values().stream().mapToInt(v -> intSize * (v.size() + 3))
                .sum() : 0;
    }

    public static void main(String[] args) {
        // 下面部分测试通过
        List<Integer> labels = new ArrayList<>(Arrays.asList(0,0,0));
        List<Integer> edges = new ArrayList<>(Arrays.asList(2,3,4,1,2,3,2,3,5,3,4,5));
        EdgePartition edgePartition = new EdgePartition(labels,edges);
        edgePartition.sortEdges(true);
        List<Integer> rowsOfNode = edgePartition.getRowsOfNode(1);
        System.out.println(rowsOfNode.toString());
        List<Integer> rowsOfNode1 = edgePartition.getRowsOfNode(2);
        System.out.println(rowsOfNode1.toString());
        List<Integer> rowsOfNode2 = edgePartition.getRowsOfNode(3);
        System.out.println(rowsOfNode2.toString());
    }
}
