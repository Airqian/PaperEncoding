package zNIWGraph.extend;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// 辅助类：表示 HashMap 的键 (Vec<Vec<u32>>, u32)
public class EdgeLabelKey implements Comparable<EdgeLabelKey> {
    private final Set<List<Integer>> edges;
    private final int label;

    public EdgeLabelKey(Set<List<Integer>> edges, int label) {
        // 深拷贝边列表，以防外部修改
        this.edges = edges.stream()
                .map(ArrayList::new)
                .collect(Collectors.toSet());
        this.label = label;
    }

    public Set<List<Integer>> getEdges() {
        return edges;
    }

    public int getLabel() {
        return label;
    }

    @Override
    public int compareTo(EdgeLabelKey other) {
        // 先比较标签
        int labelCompare = Integer.compare(this.label, other.label);
        if (labelCompare != 0) {
            return labelCompare;
        }
        // 比较边集合
        List<List<Integer>> thisSortedEdges = new ArrayList<>(this.edges);
        List<List<Integer>> otherSortedEdges = new ArrayList<>(other.edges);
        thisSortedEdges.sort(this::compareEdgeLists);
        otherSortedEdges.sort(this::compareEdgeLists);

        for (int i = 0; i < Math.min(thisSortedEdges.size(), otherSortedEdges.size()); i++) {
            List<Integer> edge1 = thisSortedEdges.get(i);
            List<Integer> edge2 = otherSortedEdges.get(i);
            int edgeCompare = compareEdgeLists(edge1, edge2);
            if (edgeCompare != 0) {
                return edgeCompare;
            }
        }
        return Integer.compare(thisSortedEdges.size(), otherSortedEdges.size());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EdgeLabelKey)) return false;
        EdgeLabelKey that = (EdgeLabelKey) o;
        return label == that.label &&
                Objects.equals(edges, that.edges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(edges, label);
    }

    @Override
    public String toString() {
        return "EdgeLabelKey{" +
                "edges=" + edges +
                ", label=" + label +
                '}';
    }
}