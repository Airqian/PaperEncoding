package zHGMatch.extend;

import java.util.*;

public class GroupEntry {
    // nodes 里的顶点标签为 label，这些 nodes 都包含在 edges 里的边
    private final Set<List<Integer>> edges;
    private final int label;
    private final List<Integer> nodes;

    public GroupEntry(Set<List<Integer>> edges, int label, List<Integer> nodes) {
        this.edges = edges;
        this.label = label;
        this.nodes = nodes;
    }

    public Set<List<Integer>> getEdges() {
        return edges;
    }

    public int getLabel() {
        return label;
    }

    public List<Integer> getNodes() {
        return nodes;
    }

    @Override
    public String toString() {
        return "GroupEntry{" +
                "edges=" + edges +
                ", label=" + label +
                ", nodes=" + nodes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupEntry)) return false;
        GroupEntry that = (GroupEntry) o;
        return Objects.equals(edges, that.edges) &&
                Objects.equals(label, that.label) &&
                Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(edges, label, nodes);
    }
}
