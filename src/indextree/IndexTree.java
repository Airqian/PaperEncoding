package indextree;

import encoding.PPBitset;
import encoding.PropertyEncodingConstructor;
import indextree.hyperedge.DataHyperedge;
import indextree.hyperedge.Hyperedge;
import indextree.treeNode.InternalTreeNode;
import indextree.treeNode.LeafTreeNode;
import indextree.treeNode.TreeNode;
import indextree.util.DataSetInfo;
import indextree.util.Event;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static DataHandler.shoppingDataHandler.FilePathConstants.SHOPPIONG_TREE_IOFO;
import static indextree.util.GlobalConstants.HE_ID_INDEX;

public class IndexTree {
    private TreeNode root;

    private int windowSize;

    private int encodingLength;

    private int hashFuncCount;

    private int minInternalNodeChilds;

    // 叶子节点的子节点的最大个数
    private int maxInternalNodeChilds;

    private int secondaryIndexSize;

    private int layer;

    public IndexTree(int windowSize, int encodingLength, int hashFuncCount, int minInternalNodeChilds, int maxInternalNodeChilds, int secondaryIndexSize) {
        this.windowSize = windowSize;
        this.encodingLength = encodingLength;
        this.hashFuncCount = hashFuncCount;
        this.maxInternalNodeChilds = maxInternalNodeChilds;
        this.minInternalNodeChilds = minInternalNodeChilds;
        this.secondaryIndexSize = secondaryIndexSize;
    }

    // TODO: 构建倒排索引表，点到边的映射和边到点的映射
    /**
     * 根据时序超图构建索引树
     * @param idToTime 顶点到属性的映射
     * @param idMap 将超边按照时间升序排序
     * @param labelMap 超边id到整条超边的映射（包含顶点id以及属性）
     * @param proMap 超边id到整条超边的映射（包含顶点label以及属性）
     */
    public void buildTemporalHypergraphTree(List<long[]> idToTime, Map<String, String> idMap, Map<String, String> labelMap,
                                            Map<String, List<String>> proMap, boolean openSecondaryIndex) {
        // 构造叶子层
        Queue<TreeNode> treeNodes = new ArrayDeque<>();
        LeafTreeNode leafTreeNode = new LeafTreeNode(windowSize, encodingLength, secondaryIndexSize);
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;

        int windowEdgesNum = 0;
        int i = 0;
        for (; i < idToTime.size(); i++) {
//            System.out.println("处理第 " + i + " 条超边");
            String edgeId = String.valueOf(idToTime.get(i)[0]); // 超边id
            long edgeTime = idToTime.get(i)[1]; // 超边时间
            String edgeIdDetail = idMap.get(edgeId);
            String[] items = edgeIdDetail.split("\\t");

            DataHyperedge hyperedge = new DataHyperedge(Long.valueOf(edgeId), edgeTime, encodingLength);
            minTime = Math.min(minTime, edgeTime);
            maxTime = Math.max(maxTime, edgeTime);
            leafTreeNode.setStartTime(minTime);
            leafTreeNode.setEndTime(maxTime);

            // 对该超边中包含的所有顶点的所有属性进行编码，合成超边编码
            PPBitset bitset = new PPBitset(encodingLength);
            for (int j = 1; j < items.length - 1; j++) {
                String vertexId = items[j];
                hyperedge.addVertexId(Long.valueOf(vertexId));
                for (String prop : proMap.get(vertexId)) {
                    PPBitset tmp = PropertyEncodingConstructor.encoding(prop, encodingLength, hashFuncCount);
                    bitset.or(tmp);
                }
            }

            hyperedge.setEncoding(bitset);
            leafTreeNode.addHyperedge(hyperedge);
            windowEdgesNum++;

            if (windowEdgesNum == windowSize) {
                if (openSecondaryIndex)
                    leafTreeNode.buildSecondaryIndex();
                treeNodes.offer(leafTreeNode);
                leafTreeNode = new LeafTreeNode(windowSize, encodingLength, secondaryIndexSize);
                minTime = Integer.MAX_VALUE;
                maxTime = Integer.MIN_VALUE;

                windowEdgesNum = 0;
            }
        }

        if (i == idToTime.size() && windowEdgesNum != 0) {
            treeNodes.offer(leafTreeNode);
            if (openSecondaryIndex)
                leafTreeNode.buildSecondaryIndex();
        }

        // 构造中间节点层
        Queue<TreeNode> treeNode2 = new ArrayDeque<>();
        while (!treeNodes.isEmpty()) {
            if (treeNodes.size() == 1 && treeNodes.peek() instanceof InternalTreeNode) {
                this.root =treeNodes.poll();
                break;
            }

            InternalTreeNode parentNode = new InternalTreeNode(windowSize, encodingLength);
            for (int j = 1; j <= maxInternalNodeChilds && !treeNodes.isEmpty(); j++) {
                TreeNode childNode = treeNodes.poll();
                Hyperedge parentEdge = childNode.getTopHyperedge().clone();
                parentNode.addChildNode(parentEdge, childNode);

                childNode.setParentNode(parentNode);
                childNode.setParentEdge(parentEdge);
            }
            treeNode2.offer(parentNode);

            if (treeNodes.isEmpty()) {
                treeNodes = treeNode2;
                treeNode2 = new ArrayDeque<>();
            }
        }
    }

    /**
     * 根据购物数据集构建索引树
     */
    public void buildShoppingTree(DataSetInfo dataSetInfo) {
        // 根据读取到的数据集统计信息构建索引树
        // 观察到，如果为了保证一个叶子节点内只有一个主体，那么可能会导致树的叶子层非常稀疏
        // 因此，树的构建原则是时间优先，在将编码插入叶子节点时会按照主体属性和时间顺序进行相对排序，保证主体属性相同的编码放置在一块
        Map<String, Map<Long, List<Event>>> eventMap = dataSetInfo.getOrganizer().getEventMap();
        Queue<TreeNode> treeNodes = new ArrayDeque<>();
        LeafTreeNode leafTreeNode = new LeafTreeNode(windowSize, encodingLength, secondaryIndexSize);
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 首先构建叶子结点
        int size = 0;
        for (Map.Entry<String, Map<Long, List<Event>>> periodEntry : eventMap.entrySet()) {
            for (Map.Entry<Long, List<Event>> userEntry : periodEntry.getValue().entrySet()) {
                for (Event event : userEntry.getValue()) {
                    // 对事件进行编码（编码信息保存在树上不是在树节点上）
                    String eventDetail = event.getEventDetail();
                    String[] items = eventDetail.split("\\t");
                    System.out.println(eventDetail);

                    try {
                        Date date  = format.parse(items[items.length - 1]);
                        long time = date.getTime();

                        DataHyperedge hyperedge = new DataHyperedge(Long.valueOf(items[HE_ID_INDEX]), time, encodingLength);
                        minTime = Math.min(minTime, time);  // 更新窗口时间
                        maxTime = Math.max(maxTime, time);
                        leafTreeNode.setStartTime(minTime);
                        leafTreeNode.setEndTime(maxTime);

                        PPBitset totalBitSet = new PPBitset(encodingLength);
                        for (int i = 1, j = 0; i < items.length - 1; i++, j++) {
                            PPBitset temp = PropertyEncodingConstructor.encoding(items[i], encodingLength, hashFuncCount);
                            totalBitSet.or(temp);
                        }
                        hyperedge.setEncoding(totalBitSet);
                        leafTreeNode.addHyperedge(hyperedge);
                        size++;

                        // 当窗口达到了最大容量则新建下一个窗口，这个容量可以使用类似负载因子的优化方法，避免下一次插入直接造成节点分裂
                        if (size == windowSize) {
                            treeNodes.offer(leafTreeNode);
                            leafTreeNode = new LeafTreeNode(windowSize, encodingLength, secondaryIndexSize);
                            minTime = Long.MAX_VALUE;
                            maxTime = Long.MIN_VALUE;

                            size = 0;
                        }
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        // 从叶节点层自底向上构建索引树
        Queue<TreeNode> treeNodes2 = new ArrayDeque<>();
        while (!treeNodes.isEmpty()) {
            if (treeNodes.size() == 1) {
                TreeNode node = treeNodes.poll();
                if (node instanceof InternalTreeNode) {
                    this.root = node;
                    break;
                }
            }

            InternalTreeNode parentNode = new InternalTreeNode(windowSize,  encodingLength);
            // TODO 取出对应数量的子节点构建父节点(后续需要算法优化，子节点的数量有范围)
            for (int i = 1; i <= minInternalNodeChilds; i++) {
                TreeNode childNode = treeNodes.poll();
                Hyperedge parentEdge = childNode.getTopHyperedge().clone();
                parentNode.addChildNode(parentEdge, childNode);

                childNode.setParentNode(parentNode);
                childNode.setParentEdge(parentEdge);
            }
            treeNodes2.offer(parentNode);

            if (treeNodes.isEmpty()) {
                treeNodes = treeNodes2;
                treeNodes2 = new ArrayDeque<>();
            }
        }
    }

    // 从根节点开始找到符合条件的叶子节点（使用二级索引）
    public List<Long> singleEdgeSearchWithSecondaryIndex(Hyperedge hyperedge) {
        DataHyperedge dataHyperedge = (DataHyperedge) hyperedge;
        Queue<TreeNode> queue1 = new ArrayDeque<>();
        Queue<TreeNode> queue2 = new ArrayDeque<>();

        // 先把所有可能符合的叶节点找出来
        queue1.offer(root);
        while (!queue1.isEmpty()) {
            if (queue1.peek() instanceof LeafTreeNode)
                break;

            InternalTreeNode curNode = (InternalTreeNode) queue1.poll();

            for (Hyperedge edge : curNode.getDerivedHyperedges()) {
                TreeNode node = curNode.getNodeByEdgeID(edge.getId());
                if (dataHyperedge.isBitwiseSubset(edge))
                    queue2.offer(node);
            }

            if (queue1.isEmpty()) {
                queue1 = queue2;
                queue2 = new ArrayDeque<>();
            }
        }

        // 在叶节点中查找编码
        List<Long> res = new LinkedList<>();
        while (!queue1.isEmpty()) {
            LeafTreeNode leafTreeNode = (LeafTreeNode) queue1.poll();
            List<PPBitset> secondaryBitsets = leafTreeNode.getSecondaryBitsets();
            List<DataHyperedge> hyperedges = leafTreeNode.getHyperedges();

            for (int i = 0; i < secondaryBitsets.size(); i++) {
                PPBitset ppBitset = secondaryBitsets.get(i);
                if (dataHyperedge.getEncoding().isBitwiseSubset(ppBitset)) {
                    int start = i * secondaryIndexSize;
                    int end = Math.min((i + 1) * secondaryIndexSize, hyperedges.size());

                    for (int j = start; j < end; j++) {
                        DataHyperedge edge = hyperedges.get(j);
                        if (dataHyperedge.isBitwiseSubset(edge))
                            res.add(edge.getId());
                    }
                }
            }
        }

        return res;
    }

    // 从根节点开始找到符合条件的叶子节点（不是用二级索引）
    public List<Long> singleEdgeSearch(Hyperedge hyperedge) {
        DataHyperedge dataHyperedge = (DataHyperedge) hyperedge;
        Queue<TreeNode> queue1 = new ArrayDeque<>();
        Queue<TreeNode> queue2 = new ArrayDeque<>();

        // 先把所有可能符合的叶节点找出来
        queue1.offer(root);
        while (!queue1.isEmpty()) {
            if (queue1.peek() instanceof LeafTreeNode)
                break;

            InternalTreeNode curNode = (InternalTreeNode) queue1.poll();

            for (Hyperedge edge : curNode.getDerivedHyperedges()) {
                TreeNode node = curNode.getNodeByEdgeID(edge.getId());
                if (dataHyperedge.isBitwiseSubset(edge))
                    queue2.offer(node);
            }

            if (queue1.isEmpty()) {
                queue1 = queue2;
                queue2 = new ArrayDeque<>();
            }
        }

        // 在叶节点中可以使用二分法查找
        List<Long> res = new LinkedList<>();
        while (!queue1.isEmpty()) {
            LeafTreeNode leafTreeNode = (LeafTreeNode) queue1.poll();

            for (DataHyperedge edge : leafTreeNode.getHyperedges()) {
                BigInteger bigInteger = edge.getEncoding().toBigInteger();
                if (dataHyperedge.isBitwiseSubset(edge))
                    res.add(edge.getId());
            }
        }
        return res;
    }

    /**
     * 计算每个中间节点的子节点数量
     *
     * @param n 总叶子节点数
     * @param x 每组最少子节点数
     * @param y 每组最多子节点数
     * @return 每组子节点数量的列表
     */
    public static List<Integer> determineGroupSizes(int n, int x, int y) {
        List<Integer> result = new ArrayList<>();

        // 计算最小和最大组数
        int minGroups = (int) Math.ceil((double) n / y);
        int maxGroups = (int) Math.floor((double) n / x);

        // 确定实际组数
        int groupCount = minGroups;

        // 平均分配子节点
        int avg = n / groupCount; // 每组平均子节点数
        int remainder = n % groupCount; // 余数

        // 分配子节点数量
        for (int i = 0; i < groupCount; i++) {
            if (i < remainder) {
                result.add(avg + 1); // 前 remainder 组多分配一个节点
            } else {
                result.add(avg);
            }
        }

        return result;
    }

    /**
     * @param dataHyperedge  新插入的数据超边
     * @param curleafNode    需要进行分裂的节点
     */
    private void splitNode(DataHyperedge dataHyperedge, LeafTreeNode curleafNode) {
//        // 1. 创建新的叶节点以及叶节点在父节点中的DerivedHyperedge
//        InternalTreeNode parentNode = curleafNode.getParentNode();
//        LeafTreeNode newLeafNode = new LeafTreeNode(curleafNode.getStartTime(), curleafNode.getEndTime(), // 时间容量和curleafNode一样
//                curleafNode.getCapacity(), bitsetNum, vertexToPropOffset, propEncodingLength);
//        Hyperedge newParentEdge = new Hyperedge(dataHyperedge.getEventTypeId(), dataHyperedge.getNumOfVertex(),
//                dataHyperedge.getMaxPropertyNum(), dataHyperedge.getVertexToPropOffset());
//
//        // 2. 获得两条seed edge（另原节点中的seed与新插入超边进行比较）以及要进行分配的所有超边
//        curleafNode.updateSeedAndCardinality(dataHyperedge);
//        DataHyperedge seed1 = (DataHyperedge) curleafNode.getSeedHyperedges().get(0).getFirst();
//        DataHyperedge seed2 = (DataHyperedge) curleafNode.getSeedHyperedges().get(1).getFirst();
//
//        List<DataHyperedge> edges = curleafNode.getHyperedges();
//        edges.add(dataHyperedge);
//        edges.remove(seed1);
//        edges.remove(seed2);
//
//        // 3. 清空curleafNode的seed、cardinality、globalbits等相关信息
//        curleafNode.clear();
//        curleafNode.getParentEdge().clear();
//
//        // 4. 新叶节点和旧叶节点分别分配一条 seed Hyperedge，接着计算权重增量开始分配超边（不要重复插入）
//        curleafNode.addHyperedge(seed1);
//        newLeafNode.addHyperedge(seed2);
//        extracted(curleafNode, newLeafNode, seed1, seed2, edges);
//
//        // TODO：此处逻辑再看看，理论上只要父节点往上更新就可以
//        // 5. 根据新插入的超边更新两个子节点的父超边
//        parentNode.updateParent(dataHyperedge);
//        // newLeafNode.updateParentEdgeByEdge(dataHyperedge);
//
//        // 6. 更新父节点的globalbits，并将新添加的属性信息在树中向上传播（继续用dataHyperedge往上更新即可，并且暂时不考虑父节点的seed hyperedge变化）
//        if (parentNode != null) {
//            parentNode.updateTopHyperedge(dataHyperedge);
//            parentNode = parentNode.getParentNode();
//        }
//
//        // 在父节点中添加新的叶节点并建立映射
//        newLeafNode.setParentNode(parentNode);
//        newLeafNode.setParentEdge(newParentEdge);
//        parentNode.addChildNode(newParentEdge, newLeafNode);
    }

//    private void extracted(LeafTreeNode curleafNode, LeafTreeNode newLeafNode, DataHyperedge seed1, DataHyperedge seed2, List<DataHyperedge> edges) {
//        int threshold = curleafNode.getCapacity() / 2;
//        for (int i = 0; i < edges.size(); i++) {
//            if (curleafNode.size() < threshold && newLeafNode.size() < threshold) {
//                // cur 为待插入超边
//                DataHyperedge cur = edges.get(i);
//                double res1 = cur.getWeightIncrease(seed1);
//                double res2 = cur.getWeightIncrease(seed2);
//
//                // 添加到权重增量较小的窗口中
//                if (Double.compare(res1, res2) <= 0)
//                    curleafNode.addHyperedge(cur);
//                else
//                    newLeafNode.addHyperedge(cur);
//
//                edges.remove(i);
//            } else
//                break;
//        }
//
//        if (edges.size() > 0) {
//            if (curleafNode.size() >= threshold)
//                newLeafNode.addHyperedge(edges);
//            else
//                curleafNode.addHyperedge(edges);
//        }
//    }

    // 从根节点开始打印树
    public void printTree(String treeInfoFilePath, boolean openSecondaryIndex) {
        Queue<TreeNode> treeNodes = new ArrayDeque<>();
        treeNodes.offer(this.root);

        while (!treeNodes.isEmpty()) {
            TreeNode node = treeNodes.poll();
            node.print(treeInfoFilePath, openSecondaryIndex);

            if (node instanceof InternalTreeNode) {
                for (TreeNode child : ((InternalTreeNode) node).getChildNodes())
                    treeNodes.offer(child);
            }
        }
    }

    public TreeNode getRoot() {
        return this.root;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public int getEncodingLength() {
        return encodingLength;
    }

    public int getLayer() {
        return layer;
    }

    public int getHashFuncCount() {
        return hashFuncCount;
    }


}

