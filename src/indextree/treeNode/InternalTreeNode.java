package indextree.treeNode;

import indextree.hyperedge.Hyperedge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 非叶子节点窗口中的一条超边对应一个子节点，Map中保存超边id到子节点id的映射
 * 非叶子节点在容量变满时也要进行节点分裂，同理也可以保存两条权重值最大的超边
 */
public class InternalTreeNode extends TreeNode {
    private List<Hyperedge> derivedHyperedges;

    private List<TreeNode> childNodes;

    /**
     * 保存超边id到子节点的映射
     * TODO 感觉要直接保存超边和节点，而不是id，此时就要用到TreeNode了
     */
    private Map<Long, TreeNode> edgeToNode;

    public InternalTreeNode(int capacity, int encodingLength) {
        super(capacity, encodingLength);

        derivedHyperedges = new ArrayList<>();
        childNodes = new ArrayList<>();
        edgeToNode = new HashMap<>();
    }

    public InternalTreeNode(long startTime, long endTime, int capacity, int encodingLength) {
        super(startTime, endTime, capacity, encodingLength);

        derivedHyperedges = new ArrayList<>();
        childNodes = new ArrayList<>();
        edgeToNode = new HashMap<>();
    }

    // 在非叶节点中添加超边和子节点（需同时添加）
    public void addChildNode(Hyperedge parentEdge, TreeNode childNode) {
        check();

        if (!isFull()) {
            // 添加子节点，建立超边到子节点的映射
            derivedHyperedges.add(parentEdge);
            childNodes.add(childNode);
            edgeToNode.put(parentEdge.getId(), childNode);

            // 更新节点的时间范围
            if (getStartTime() > childNode.getStartTime())
                setStartTime(childNode.getStartTime());
            if (getEndTime() < childNode.getEndTime())
                setEndTime(childNode.getEndTime());

            // 更新本seedHyperedge、cardinality以及globalbits
            updateSeedAndCardinality(parentEdge);
            updateTopHyperedge(parentEdge);
            updateParent(parentEdge);
        } else {
            // TODO 节点分裂
        }
    }

    public String printEdges() {
        StringBuilder builder = new StringBuilder();
        for (Hyperedge hyperedge : derivedHyperedges) {
            builder.append(hyperedge.printEncoding()).append("\n");
        }
        return builder.toString();
    }

    public TreeNode getNodeByEdgeID(long id) {
        return this.edgeToNode.get(id);
    }

    private void check() {
        assert (derivedHyperedges.size() == childNodes.size());
        assert (derivedHyperedges.size() == edgeToNode.size());
    }

    public int size() {
        check();
        return derivedHyperedges.size();
    }

    public boolean isFull() {
        return derivedHyperedges.size() == getCapacity();
    }


    public List<Hyperedge> getDerivedHyperedges() {
        return derivedHyperedges;
    }

    public void setDerivedHyperedges(List<Hyperedge> derivedHyperedges) {
        this.derivedHyperedges = derivedHyperedges;
    }

    public List<TreeNode> getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(List<TreeNode> childNodes) {
        this.childNodes = childNodes;
    }

    public Map<Long, TreeNode> getEdgeToNode() {
        return edgeToNode;
    }

    public void setEdgeToNode(Map<Long, TreeNode> edgeToNode) {
        this.edgeToNode = edgeToNode;
    }

}
