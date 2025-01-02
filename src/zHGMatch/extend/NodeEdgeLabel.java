package zHGMatch.extend;

import java.util.List;
import java.util.Objects;

// 辅助类：用于存储节点、边列表和标签，并实现 Comparable 接口
public class NodeEdgeLabel implements Comparable<NodeEdgeLabel>{
    private final int nodeId;
    private final List<List<Integer>> edges;
    private final int label;

    public NodeEdgeLabel(int nodeId, List<List<Integer>> edges, int label) {
        this.nodeId = nodeId;
        this.edges = edges;
        this.label = label;
    }

    public int getNodeId() {
        return nodeId;
    }

    public List<List<Integer>> getEdges() {
        return edges;
    }

    public int getLabel() {
        return label;
    }

    @Override
    public int compareTo(NodeEdgeLabel other) {
        return this.nodeId - other.nodeId;
    }

    @Override
    public String toString() {
        return "NodeEdgeLabel{" +
                "nodeId=" + nodeId +
                ", edges=" + edges +
                ", label=" + label +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeEdgeLabel)) return false;
        NodeEdgeLabel that = (NodeEdgeLabel) o;
        return Objects.equals(nodeId, that.nodeId) &&
                Objects.equals(edges, that.edges) &&
                Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, edges, label);
    }
}
