package HGMatch.graph;

import java.util.*;
import java.util.stream.Collectors;

// edge_partition.rs
public class EdgePartition {
    private int arity;  // 表示该分区边的度数（超边中的顶点个数）
    private List<Integer> labels;  // 超边签名
    private List<Integer> edges;
    private HashMap<Integer, List<Integer>> index;

    public EdgePartition(List<Integer> labels) {
        this.arity = labels.size();
        this.labels = new ArrayList<>(labels);
        this.edges = new ArrayList<>();
        index = new HashMap<>();
    }

    public EdgePartition (int arity, List<Integer> labels, List<Integer> edges, HashMap<Integer, List<Integer>> index) {
        this.arity = arity;
        this.labels = labels;
        this.edges = edges;
        this.index = index;
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

    public int numNodes() {
        if (index.size() != 0)
            return index.size();

        return (int) edges.stream().distinct().count();
    }

    public int numEdges() {
        return this.edges.size() / this.arity;
    }

    public List<Integer> getRowsOfNode(int node) {
        if (index.size() != 0) {
            List<Integer> rows = index.get(node);
            return rows != null ? rows : Collections.emptyList();
        } else {
            List<Integer> result = new ArrayList<>();
            for (int row = 0; row < this.numEdges(); row++) {
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

//        Set<Integer> resultSet = new TreeSet<>();
//        for (int node : nodes) {
//            List<Integer> rows = getRowsOfNode(node);
//            for (int row : rows) {
//                resultSet.add(row);
//            }
//        }
//
//        return resultSet.stream().map(i->i).collect(Collectors.toList());

        List<Integer> result = nodes.stream().map(this::getRowsOfNode).flatMap(List::stream).distinct().collect(Collectors.toList());
        return result;
    }

    public List<Integer> getEdge(int row) {
        int start = row * arity;
        int end = Math.min(start + arity, edges.size());
        return edges.subList(start, end);
    }

    public void addEdge(List<Integer> edge) {
        if (edge.size() != arity)
            throw new IllegalArgumentException("Edge size does not match arity.");
        edges.addAll(edge);
    }
    // 从 EdgePartition 转换成 EdgePartitionWapper

}
