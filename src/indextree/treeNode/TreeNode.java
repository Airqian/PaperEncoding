package indextree.treeNode;

import encoding.PPBitset;
import encoding.util.Pair;
import indextree.hyperedge.Hyperedge;
import indextree.util.IdGenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 树节点父类，定义了时间范围、节点容量等公共字段
 */
public class TreeNode {
    private long id;

    // Time Range
    private long startTime;

    private long endTime;

    private int capacity;

    private boolean isRootFlag;

    /**
     * 用这两个字段记录该节点的父节点以及在父节点中的父超边
     */
    private InternalTreeNode parentNode;

    private Hyperedge parentEdge;

    /**
     * seedHyperedges·保存用于节点分裂使用的两个seed hyperedge
     * minCardinality 记录seedHyperedges当中cardinality较小值
     * TODO 这块可以再优化一下，一共就保存两条边，是否直接用Hyperedge数组保存就可以
     */
    private List<Pair<Hyperedge, Integer>> seedHyperedges;

    private int minCardinality;

    // 记录该节点中所有超边属性编码上的位为1的情况
    // TODO globalbits和topHyperedge的作用重了
    private Set<Integer> globalbits;

    private Hyperedge topHyperedge;

    public TreeNode(int capacity, int encodingLength) {
        if (capacity <= 0)
            throw new IllegalArgumentException(String.format("The capacity cannot be 0. capacity = %d", capacity));

        this.id = IdGenerator.getNextId();
        this.capacity = capacity;
        this.seedHyperedges = new ArrayList<>(2);
        this.minCardinality = Integer.MAX_VALUE;

        this.startTime = Long.MAX_VALUE;
        this.endTime = Long.MIN_VALUE;


        this.initialGlobalBits();
        this.initialTopHyperedge(encodingLength);
    }

    // 叶节点和中间节点的容量可以用全局静态常量去设置
    public TreeNode(long startTime, long endTime, int capacity, int encodingLength) {
        this(capacity, encodingLength);

        if (startTime >= endTime)
            throw new IllegalArgumentException(String.format("endTime = %d cannot be less than startTime = %d.", endTime, startTime));

        this.startTime = startTime;
        this.endTime = endTime;

        this.initialGlobalBits();
        this.initialTopHyperedge(encodingLength);
    }

    public void initialGlobalBits() {
        this.globalbits = new HashSet<>();
    }

    public void initialTopHyperedge(int encodingLength) {
        this.topHyperedge = new Hyperedge(encodingLength);
        topHyperedge.setEncoding(new PPBitset(encodingLength));
    }

    // 更新本节点的 seed hyperedge
    public void updateSeedAndCardinality(Hyperedge hyperedge) {
        int curCardinality = hyperedge.cardinality();

        if (seedHyperedges.size() < 2) {
            seedHyperedges.add(new Pair(hyperedge, curCardinality));
        } else {
            if (curCardinality > getMinCardinality()) {
                if (seedHyperedges.get(0).getSecond() < curCardinality) {
                    seedHyperedges.set(0, new Pair(hyperedge, curCardinality));
                } else {
                    seedHyperedges.set(1, new Pair(hyperedge, curCardinality));
                }
            }
            setMinCardinality(Math.min(seedHyperedges.get(0).getSecond(), seedHyperedges.get(1).getSecond()));
        }
    }


    // 用 hyperedge 更新本节点的 globalbits 和 topHyperedge
    public void updateTopHyperedge(Hyperedge hyperedge) {
        List<Integer> bits = hyperedge.getEncoding().getAllOneBits();
        for (int bit : bits) {
            globalbits.add(bit);
            topHyperedge.getEncoding().set(bit, true);
        }

    }

    // 根据新插入的超边更新对应的父超边以及父节点的属性信息(自底向上具有传递性)
    public void updateParent(Hyperedge hyperedge) {
        TreeNode pNode = getParentNode();
        Hyperedge pEdge = getParentEdge();

        while (pNode != null) {
            pEdge.encodingOr(hyperedge);
            pNode.updateTopHyperedge(hyperedge);
            pNode.updateSeedAndCardinality(hyperedge);

            pEdge = pNode.getParentEdge();
            pNode = pNode.getParentNode();
        }
    }

    public void print(String treeInfoFilePath) {
        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(treeInfoFilePath, true));
            StringBuilder builder = new StringBuilder();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // 节点 id
            builder.append("Node ID: ").append(this.id).append("\n");

            // time range
            builder.append("start time: ").append(format.format(new Date(startTime)))
                    .append(", end time: ").append(format.format(new Date(endTime))).append("\n");

            // tophyperedge
            builder.append("TopHyperedge: ").append(this.topHyperedge.printEncoding()).append("\n");

            // 编码数据
            builder.append(printEdges()).append("\n");

            writer.write(builder.toString());
            writer.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String printEdges() {
        return "";
    }

    public void printTimeRange() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("start time: " + format.format(new Date(startTime)) + ", end time: " + format.format(new Date(endTime)));
    }

    public void printTopHyperedge() {
        System.out.print("TopHyperedge: ");
        this.topHyperedge.printEncoding();
    }

    public void printGlobalBits() {
        StringBuilder builder = new StringBuilder();
        builder.append("GlobalBits: ");
        builder.append("{");
        for (int bit : this.getGlobalbits()) {
            builder.append(bit + ", ");
        }
        builder.delete(builder.length() - 2, builder.length());
        builder.append("} ");
        System.out.println(builder);
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setIsRoot(boolean isRootFlag) {
        this.isRootFlag = isRootFlag;
    }

    public boolean getIsRoot() {
        return isRootFlag;
    }

    public long getId() {
        return id;
    }

    public Hyperedge getParentEdge() {
        return parentEdge;
    }

    public InternalTreeNode getParentNode() {
        return parentNode;
    }

    public void setParentNode(InternalTreeNode parentNode) {
        this.parentNode = parentNode;
    }

    public void setParentEdge(Hyperedge parentEdge) {
        this.parentEdge = parentEdge;
    }

    public Set<Integer> getGlobalbits() {
        return globalbits;
    }

    public List<Pair<Hyperedge, Integer>> getSeedHyperedges() {
        return seedHyperedges;
    }

    public int getMinCardinality() {
        return minCardinality;
    }

    public void setMinCardinality(int minCardinality) {
        this.minCardinality = minCardinality;
    }

    public Hyperedge getTopHyperedge() {
        return topHyperedge;
    }

}
