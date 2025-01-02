package zHGMatch.extend;

import java.util.*;
import java.util.stream.Collectors;

/**
 * indices:
 *      每个内部 Vec<usize> 代表一组节点的索引。例如，indices[0] = vec![1, 2, 3] 表示第一组节点位于 partial 切片的索引 1、2、3 处。
 * degrees:
 *      对应于每组 indices，表示该组节点的期望度数。例如，degrees[0] = 2 表示第一组节点的度数应为 2。
 * len_s:
 *      对应于每组 indices，表示该组节点的期望大小。例如，len_s[0] = 3 表示第一组节点应包含 3 个元素。
 * contains:
 *      描述 indices 之间的包含关系。contains[i] 是一个向量，包含应包含在 indices[i] 中的其他 indices 的索引。
 *      例如，contains[0] = [1, 2] 表示 indices[0] 应该包含 indices[1] 和 indices[2] 中的元素。
 */

public class NodeVerifier {
    private List<List<Integer>> indices;
    private List<Integer> degrees;
    private List<Integer> len_s;
    private List<List<Integer>> contains;

    public NodeVerifier(List<List<Integer>> indices, List<Integer> degrees, List<Integer> len_s, List<List<Integer>> contains) {
        this.indices = indices;
        this.degrees = degrees;
        this.len_s = len_s;
        this.contains = contains;
    }

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
        for (int i = 0; i < indices.size(); i++) {
            List<Integer> pos = indices.get(i);
            int degree = degrees.get(i);

            // 记录节点出现的次数
            Map<Integer, Integer> counter = new HashMap<>();
            for (int index : pos) {
                int n = partial.get(index);
                counter.put(n, counter.getOrDefault(n, 0) + 1);
            }

            // 过滤度数等于degree的id
            Set<Integer> edgeSet = counter.entrySet().stream()
                    .filter(entry -> entry.getValue() == degree)
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
        // 根据 contains 进一步过滤 edgeSets
        // contains[i] 是一个列表，包含应从 edgeSets[i] 中移除的其他 edgeSets 的索引。
        for (int i = 0; i < contains.size(); i++) {
            List<Integer> containIndices = contains.get(i);
            if (containIndices.isEmpty())
                continue;

            Set<Integer> edgeSet = new HashSet<>(edgeSets.get(i));
            for (int j : containIndices) {
                Set<Integer> containedEdgeSet = edgeSets.get(j);
                for (int n : containedEdgeSet) {
                    if (!edgeSet.remove(n))
                        return false;
                }
            }

            edgeSets.set(i, edgeSet);
        }

        // 遍历 edgeSets 和 len_s，确保每个 edgeSet 的大小等于对应的 len_s
        // 如果有任何一个不匹配，设置 sizeCheck = false 并跳出循环
        boolean sizeCheck = true;
        for (int i = 0; i < edgeSets.size(); i++) {
            if (edgeSets.get(i).size() != len_s.get(i)) {
                sizeCheck = false;
                break;
            }
        }

        // 检查所有 edgeSets 中的元素是否唯一
        boolean uniqueCheck = edgeSets.stream()
                .flatMap(Set::stream)
                .distinct()
                .count() == edgeSets.stream().mapToInt(Set::size).sum();

        return sizeCheck && uniqueCheck;
    }

    public static void main(String[] args) {
        // 定义 indices, degrees, len_s, contains
        List<List<Integer>> indices = Arrays.asList(
                Arrays.asList(0, 1, 2),
                Arrays.asList(1, 2, 3)
        );
        List<Integer> degrees = Arrays.asList(2, 1);
        List<Integer> len_s = Arrays.asList(1, 1);
        List<List<Integer>> contains = Arrays.asList(
                Arrays.asList(1),
                Collections.emptyList()
        );

        // 创建 partial 列表
        List<Integer> partial = Arrays.asList(1,2,2,3);

        // 初始化 NodeVerifier
        NodeVerifier verifier = new NodeVerifier(indices, degrees, len_s, contains);

        // 调用 filter 方法，预期为 false（因为在 contains 0 的时候尝试移除不存在的 Id(3)）
        boolean result = verifier.filter(partial);
        System.out.println(result + ": Expected filter to return false due to removal failure");

    }
}
