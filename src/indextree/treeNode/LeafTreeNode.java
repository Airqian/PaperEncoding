package indextree.treeNode;

import encoding.PPBitset;
import indextree.hyperedge.DataHyperedge;
import indextree.hyperedge.Hyperedge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * - 叶节点存储真实的事件数据超边，每条数据超边都有一个指向超边对象的指针，目前表示为Hyperedge的Id
 * - 叶节点有且仅有一个父节点，不存在子节点。在其父节点中存在一个 Map 将汇聚产生的派生超边和该子节点进行映射绑定
 * - 叶节点的操作有：节点插入、节点分裂
 * - 节点插入时，需要将编码的变化同等的传播上上层节点
 * - 节点分裂时，需要在待分配的超边中选出两条权重值最大的超边分别作为两个新分裂节点的seed hyperedge，为了简化seed hyperedge的挑选过程，
 * 直接在叶节点中设置两个字段，在节点插入时进行实时更新（用Pair结构表示一条权重超边，第一个参数为超边对象，第二个参数为其权重），用新的超边替换
 * 权重值较小的那条超边
 */
public class LeafTreeNode extends TreeNode {
    // 当和DerivedHyperedge行为没啥太大的差别时再统一为Hyperedge
    private List<DataHyperedge> dataHyperedges;

    private List<PPBitset> secondaryBitsets;

    private int secondaryIndexSize;

    public LeafTreeNode(int capacity, int encodingLength, int secondaryIndexSize) {
        super(capacity, encodingLength);
        this.secondaryBitsets = new ArrayList<>();
        this.secondaryIndexSize = secondaryIndexSize;
        dataHyperedges = new ArrayList<>();
    }

    public LeafTreeNode(long startTime, long endTime, int capacity, int encodingLength) {
        super(startTime, endTime, capacity, encodingLength);
        dataHyperedges = new ArrayList<>();
    }

    /**
     * 一个叶节点中所有超边的主体属性都相同
     * 每一个叶节点都应该选定一个seed hyperedge，它是满足该时间窗口且 cardinality 最大的超边。
     * 叶节点插入逻辑：首先按超边比较顺序在特定位置进行插入，同时更新seed、cardinality以及globalbits，并将变化从下至上进行传播直到根节点
     * 在构造索引树时，我们应首先获取相同主体事件的最早和最晚时间，根据这两个时间切分时间窗口，以尽量平均每个时间窗口含有的编码数。
     * 整体的逻辑是，先看该时间范围内有多少个不同的主体属性，对这些主体属性进行分组
     *
     * @param addedEdge
     */
    public boolean addHyperedge(DataHyperedge addedEdge) {
        if (addedEdge.getEdgeTime() < getStartTime() || addedEdge.getEdgeTime() > getEndTime())
            throw new RuntimeException("The occurrence time of the event to be inserted is not within the time window.");

        if (!isFull()) {
            int index = binarySort(addedEdge);
            dataHyperedges.add(index, addedEdge);

            // 更新本节点的seed、cardinality以及globalbits
            updateSeedAndCardinality(addedEdge);
            updateTopHyperedge(addedEdge);
            updateParent(addedEdge);

            return true;
        }

        return false;
    }


    public void addHyperedge(List<DataHyperedge> dataHyperedges) {
        for (int i = 0; i < dataHyperedges.size(); i++)
            addHyperedge(dataHyperedges.get(i));
    }

    /**
     * 默认非叶子节点不进行分裂，因此不更新非叶节点的cardinality信息
     * 根据本节点的globalbits更新对应的父超边（不具有传递性）
     */
    public void updateParentEdge() {
        TreeNode pNode = getParentNode();
        Hyperedge pEdge = getParentEdge();
        Set<Integer> gbits = this.getGlobalbits();

        // 用子节点的gbits更新父节点的Hyperedge,再令globalbits与其同步
        for (int bit : gbits) {
            pEdge.getEncoding().set(bit);
        }
    }

    // 二分查找搜索给定hyperedge可插入的位置
    public int binarySort(DataHyperedge edge) {
        int left = 0;
        int right = dataHyperedges.size() - 1;

        while (left <= right) {
            int mid = (left + right) >> 1;
            int res = edge.compareTo(dataHyperedges.get(mid));

            // res < 0 说明bit为1的下标更加靠前，应该排在前面
            if (res < 0) {
                right = mid - 1;
            } else if (res > 0) {
                left = mid + 1;
            } else {
                return mid;
            }
        }

        return left;
    }

    public void buildSecondaryIndex() {
        int encodingLength = getTopHyperedge().getEncodingLength();
        PPBitset bitset = new PPBitset(encodingLength);

        for (int i = 0; i < dataHyperedges.size(); i++) {
            bitset.or(dataHyperedges.get(i).getEncoding()); // 合并编码
            // 每达到一个 secondaryIndexSize 或到达最后一个超边时，保存当前 bitset 并创建新的 bitset
            if ((i + 1) % secondaryIndexSize == 0 || i == dataHyperedges.size() - 1) {
                this.secondaryBitsets.add(bitset);
                bitset = new PPBitset(encodingLength); // 重新初始化 bitset
            }
        }
    }

    // 删除窗口中已有的超边
    public void remove(DataHyperedge edge) {
        int index = binarySort(edge);
        if (edge.getId() == dataHyperedges.get(index).getId()) {
            dataHyperedges.remove(index);
        }
    }

    public String printEdges(boolean openSecondaryIndex) {
        StringBuilder builder = new StringBuilder();

        if (openSecondaryIndex) {
            for (int i = 0; i < secondaryBitsets.size(); i++) {
                PPBitset secondaryBitset = secondaryBitsets.get(i);
                builder.append("secondaryBitset：").append(secondaryBitset.toString()).append("\n");

                for (int j = i * secondaryIndexSize; j < Math.min(dataHyperedges.size(), (i + 1) * secondaryIndexSize); j++) {
                    builder.append(dataHyperedges.get(j).printEncoding()).append("\n");
                }
            }
        } else {
            for (Hyperedge hyperedge : dataHyperedges) {
                builder.append(hyperedge.printEncoding()).append("\n");
            }
        }
        return builder.toString();
    }

    public List<DataHyperedge> getHyperedges() {
        return dataHyperedges;
    }

    public boolean isFull() {
        return getHyperedges().size() == getCapacity();
    }

    public boolean isEmpty() {
        return getHyperedges().size() == 0;
    }

    public int size() {
        return this.dataHyperedges.size();
    }

    public void clear() {
        this.getSeedHyperedges().clear();
        this.getGlobalbits().clear();
        this.dataHyperedges.clear();
        setMinCardinality(1000);
    }

    public List<PPBitset> getSecondaryBitsets() {
        return secondaryBitsets;
    }

    public void setSecondaryBitsets(List<PPBitset> secondaryBitsets) {
        this.secondaryBitsets = secondaryBitsets;
    }
}

