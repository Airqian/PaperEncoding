package zNIWGraph.graph;

import zNIWGraph.graph.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// serde.rs
public class EdgePartitionWrapper {
    private int arity;
    private List<Integer> labels;
    private List<Integer> edges;
    private List<Pair<Integer, List<Integer>>> index;  // 表示点到边的倒排索引，不用 HashMap 的原因是可能有重复的 key

    public EdgePartitionWrapper(int arity, List<Integer> labels, List<Integer> edges, List<Pair<Integer, List<Integer>>> index) {
        this.arity = arity;
        this.labels = labels;
        this.edges = edges;
        this.index = index;
    }

    public EdgePartition toEdgePartition() {
        // 这里能起到一个去重的作用？
        HashMap<Integer, List<Integer>> newIndex = new HashMap<>();
        for (Pair<Integer, List<Integer>> pair : index) {
            newIndex.put(pair.getKey(), new ArrayList<>(pair.getValue()));
        }
        return new EdgePartition(arity, new ArrayList<>(labels), new ArrayList<>(edges), newIndex);
    }

    // 获取arity属性的方法
    public int getArity() {
        return arity;
    }

    // 获取labels属性的方法
    public List<Integer> getLabels() {
        return labels;
    }

    // 获取edges属性的方法
    public List<Integer> getEdges() {
        return edges;
    }

    // 获取index属性的方法
    public List<Pair<Integer, List<Integer>>> getIndex() {
        return index;
    }
}
