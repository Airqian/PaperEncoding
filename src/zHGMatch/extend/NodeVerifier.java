package zHGMatch.extend;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 同下标 i 下，表示有 nodeNum[i] 个顶点同时出现在 edgeNum[i] 条边里，indices[i] 则记录这些边里标签为 label 的顶点索引位置
 */

public class NodeVerifier {
    private List<List<Integer>> indices;
    private List<Integer> edgeNums; // edgeNums
    private List<Integer> nodeNums;
    private List<List<Integer>> contains;

    public NodeVerifier(List<List<Integer>> indices, List<Integer> edgeNums, List<Integer> nodeNums, List<List<Integer>> contains) {
        this.indices = indices;
        this.edgeNums = edgeNums;
        this.nodeNums = nodeNums;
        this.contains = contains;
    }

    // 查询超边和候选超边中对应的顶点的 顶点标签 和 所关联的边集 也应该相等
    public boolean filter(List<Integer> partial) {
        List<Set<Integer>> edgeSets = new ArrayList<>();

        /**
         * 举例：
         * · indices = [[0, 1, 2], [1, 2, 3]]
         * · degrees = [2, 1]
         * · partial = [1, 2, 2, 3]
         * 第一组处理：
         *   pos = [0, 1, 2]，degree = 2
         *   partial[0] = 1，partial[1] = 2，partial[2] = 2
         *   counter = {1: 1，2: 2}
         *   过滤后 edgeSet = {2}
         * 第二组处理：
         *  pos = [1, 2, 3], degree = 1
         *  partial[1] = 2，partial[2] = 2，partial[3] = 3
         *  counter = {2: 2, 3: 1}
         *  过滤后 edgeSet = {3}
         */
        // 在 last_edge 中，同下标 i 下，表示有 nodeNum[i] 个顶点同时出现在 edgeNum[i] 条边里，indices[i] 则记录这些边里标签为 label 的顶点索引位置
        for (int i = 0; i < indices.size(); i++) {
            List<Integer> pos = indices.get(i);
            int edgeNum = edgeNums.get(i);

            // 记录 indices 中节点的度数
            Map<Integer, Integer> counter = new HashMap<>();
            for (int index : pos) {
                int n = partial.get(index);
                counter.put(n, counter.getOrDefault(n, 0) + 1);
            }

            // 第一步：只保留度数等于degree的节点，保存在 edgeSet 中，因为这些性质都从部分查询中来，部分嵌入也应该遵循，留下来的点才可能在候选超边中出现
            Set<Integer> edgeSet = counter.entrySet().stream()
                    .filter(entry -> entry.getValue() == edgeNum)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            edgeSets.add(edgeSet);
        }

        /**
         * 假设 contains = [[1]]
         * i= 0 时处理第一组，containIndices = [1]
         * edgeSet = {2}
         * j = 1, containedEdgeSet = {3}
         * 尝试从 edgeSet 移除 3
         * 但是 3 不存在于 edgeSet 中，移除失败，返回 false
         */
        // 第二步：验证候选超边的 contains 关系
        // contains[i] 是一个列表，包含应从 edgeSets[i] 中移除的其他 edgeSets 的索引。
        for (int i = 0; i < contains.size(); i++) {
            List<Integer> containIndices = contains.get(i); // 值是包含集，下标是被包含集
            if (containIndices.isEmpty())
                continue;

            /**
             * 位置j上的边集包含位置i上的边集，那么从顶点的角度来看条件更加严苛，因此位置j上的点集通常比位置i上的点集小，且位置i上的点集大于等于位置j上的点集
             * 由于 indices 中保存的顶点索引是全量的，因此得到的 edgeSets 的点集会有重复，而 this.nodeNums 中的顶点是去重的，也就是点集j会包含在点集i里
             * 因此需要在点集i中去除点集j中的节点，那么剩余的顶点数量才满足 this.edgeNums[i] 以及严格的 this.nodeNums[i]
             *
             */
            Set<Integer> edgeSet = new HashSet<>(edgeSets.get(i));
            for (int j : containIndices) {
                Set<Integer> containedEdgeSet = edgeSets.get(j); // 大的集合
                for (int n : containedEdgeSet) {
                    if (!edgeSet.remove(n))
                        return false;
                }
            }

            edgeSets.set(i, edgeSet);
        }

        /**
         * 经过了前面两步的筛选之后，终于可以进行验证了
         * edgeSets 是从候选超边中找出的符合条件的点的数量，而 nodeNums 是从查询超边中找出的点的数量，两者只要有一个不相等，该嵌入就不合法
         */
        boolean sizeCheck = true;
        for (int i = 0; i < edgeSets.size(); i++) {
            if (edgeSets.get(i).size() != nodeNums.get(i)) {
                sizeCheck = false;
                break;
            }
        }

        // 检查 edgeSets 中的所有元素是否在各个集合之间是唯一的，也就是说将 edgeSets 展平之后每个顶点都是唯一的
        boolean uniqueCheck = edgeSets.stream()
                .flatMap(Set::stream)
                .distinct()
                .count() == edgeSets.stream().mapToInt(Set::size).sum();

        return sizeCheck && uniqueCheck;
    }

    public static void main(String[] args) {
        // 定义 indices, degrees, nodeNums, contains
        List<List<Integer>> indices = Arrays.asList(
                Arrays.asList(0, 1, 2),
                Arrays.asList(1, 2, 3)
        );
        List<Integer> edgeNums = Arrays.asList(2, 1);
        List<Integer> nodeNums = Arrays.asList(1, 1);
        List<List<Integer>> contains = Arrays.asList(
                Arrays.asList(1),
                Collections.emptyList()
        );

        // 创建 partial 列表
        List<Integer> partial = Arrays.asList(1,2,2,3);

        // 初始化 NodeVerifier
        NodeVerifier verifier = new NodeVerifier(indices, edgeNums, nodeNums, contains);

        // 调用 filter 方法，预期为 false（因为在 contains 0 的时候尝试移除不存在的 Id(3)）
        boolean result = verifier.filter(partial);
        System.out.println(result + ": Expected filter to return false due to removal failure");

    }
}
