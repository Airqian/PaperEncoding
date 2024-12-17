package experiment;

import indextree.hyperedge.DataHyperedge;

import java.io.*;
import java.util.*;


// 生成查询超图，超图的超边数量分别为 2 4 6 8 10 12
public class QueryGraphGenerator {
    private final static String dataset = "NDC-classes";
    private final static String DATA_FILE = "src/dataset/temporal-restricted/" + dataset + "/hyperedge-id-unique.txt";

    public static void main(String[] args) throws IOException {
        // 1. 读取超边数据并构建顶点到超边的映射
        List<DataHyperedge> hyperedges = loadHyperedges(DATA_FILE);
        Map<Long, List<DataHyperedge>> vertexToEdges = buildVertexToEdgesMap(hyperedges);

        // 2. 生成查询超图
        Map<Integer, int[]> vertexLimit = new HashMap<>();
        vertexLimit.put(2, new int[]{5,15});
        vertexLimit.put(4, new int[]{10,20});
        vertexLimit.put(6, new int[]{15,30});
        vertexLimit.put(8, new int[]{25,40});

        int numQueries = 5;
        int[] edgeNums = new int[]{2,4,6,8};
        for (int i = 0; i < edgeNums.length; i++) {
            List<List<DataHyperedge>> queryHypergraphs = generateQueryHypergraphs(hyperedges, vertexToEdges, numQueries, edgeNums[i],
                    vertexLimit.get(edgeNums[i])[0], vertexLimit.get(edgeNums[i])[1]);

            // 3. 输出结果
            String outputFile = "src/experiment/queryGraph/" + dataset + ".txt";
            saveQueryHypergraphs(queryHypergraphs, outputFile);
        }

    }

    /**
     * 生成指定数量的查询超图，每个查询超图包含指定数量的超边且顶点数量在给定范围内。
     *
     * @param hyperedges    数据超图中的所有超边
     * @param vertexToEdges 顶点到超边的映射
     * @param numQueries    要生成的查询超图数量
     * @param edgeNums      每个查询超图包含的超边数量
     * @param vertexMin     每个查询超图的顶点数量下限
     * @param vertexMax     每个查询超图的顶点数量上限
     * @return 生成的查询超图列表
     */
    public static List<List<DataHyperedge>> generateQueryHypergraphs(List<DataHyperedge> hyperedges, Map<Long, List<DataHyperedge>> vertexToEdges,
                                                                     int numQueries, int edgeNums, int vertexMin, int vertexMax) {

        List<List<DataHyperedge>> queryHypergraphs = new ArrayList<>();
        Random random = new Random();
        int maxAttempts = numQueries * 100; // 防止无限循环
        int attempts = 0;

        while (queryHypergraphs.size() < numQueries && attempts < maxAttempts) {
            attempts++;

            // 随机选取起始超边
            DataHyperedge startEdge = hyperedges.get(random.nextInt(hyperedges.size()));

            // 初始化超图
            List<DataHyperedge> currentHypergraph = new ArrayList<>();
            currentHypergraph.add(startEdge);

            // 收集当前超图的所有顶点
            Set<Long> vertexSet = new HashSet<>(startEdge.getVertexIds());

            // 添加其他超边
            while (currentHypergraph.size() < edgeNums) {
                // 获取当前超图中最后一条超边
                DataHyperedge lastEdge = currentHypergraph.get(currentHypergraph.size() - 1);
                List<Long> lastEdgeVerticesSet = lastEdge.getVertexIds();

                if (lastEdgeVerticesSet.isEmpty()) {
                    break;
                }

                // 将最后一条超边的顶点转换为列表，以便随机选择
                List<Long> lastEdgeVertices = new ArrayList<>(lastEdgeVerticesSet);
                Long randomVertex = lastEdgeVertices.get(random.nextInt(lastEdgeVertices.size()));

                // 找到包含该顶点的候选超边，并复制列表以避免修改原始映射
                List<DataHyperedge> originalCandidateEdges = vertexToEdges.getOrDefault(randomVertex, Collections.emptyList());
                List<DataHyperedge> candidateEdges = new ArrayList<>(originalCandidateEdges);
                candidateEdges.removeAll(currentHypergraph); // 移除已经在超图中的超边

                // 如果没有合适的候选超边，重新生成超图
                if (candidateEdges.isEmpty()) {
                    break;
                }

                // 随机选择一个候选超边
                DataHyperedge nextEdge = candidateEdges.get(random.nextInt(candidateEdges.size()));

                // 合并顶点
                Set<Long> newVertexSet = new HashSet<>(vertexSet);
                newVertexSet.addAll(nextEdge.getVertexIds());

                // 检查顶点数量约束
                if (newVertexSet.size() <= vertexMax) { // 只检查上限
                    currentHypergraph.add(nextEdge);
                    vertexSet = newVertexSet;
                } else {
                    // 如果超过上限，尝试选择其他超边或放弃当前超图
                    continue;
                }
            }

            // 检查顶点数量是否满足下限
            if (currentHypergraph.size() == edgeNums && vertexSet.size() >= vertexMin) {
                queryHypergraphs.add(new ArrayList<>(currentHypergraph));
            }
        }

        if (queryHypergraphs.size() < numQueries) {
            System.err.println("警告：仅生成了 " + queryHypergraphs.size() + " 个查询超图，未达到预期的 " + numQueries + " 个。");
        }

        return queryHypergraphs;
    }

    // 生成查询超图（无顶点数量限制）
    public static List<List<DataHyperedge>> generateQueryHypergraphs(List<DataHyperedge> hyperedges,
                                                                     Map<Long, List<DataHyperedge>> vertexToEdges, int numQueries, int edgeNums) {
        List<List<DataHyperedge>> queryHypergraphs = new ArrayList<>();
        Random random = new Random();
        int maxAttempts = numQueries * 100; // 防止无限循环
        int attempts = 0;

        while (queryHypergraphs.size() < numQueries && attempts < maxAttempts) {
            attempts++;

            // 随机选取起始超边
            DataHyperedge startEdge = hyperedges.get(random.nextInt(hyperedges.size()));

            // 初始化超图
            List<DataHyperedge> currentHypergraph = new ArrayList<>();
            currentHypergraph.add(startEdge);

            // 收集当前超图的所有顶点
            Set<Long> vertexSet = new HashSet<>(startEdge.getVertexIds());

            // 添加其他超边
            while (currentHypergraph.size() < edgeNums) {
                // 获取当前超图中最后一条超边
                DataHyperedge lastEdge = currentHypergraph.get(currentHypergraph.size() - 1);
                List<Long> lastEdgeVerticesSet = lastEdge.getVertexIds();

                if (lastEdgeVerticesSet.isEmpty()) {
                    break;
                }

                // 将最后一条超边的顶点转换为列表，以便随机选择
                List<Long> lastEdgeVertices = new ArrayList<>(lastEdgeVerticesSet);
                Long randomVertex = lastEdgeVertices.get(random.nextInt(lastEdgeVertices.size()));

                // 找到包含该顶点的候选超边，并复制列表以避免修改原始映射
                List<DataHyperedge> originalCandidateEdges = vertexToEdges.getOrDefault(randomVertex, Collections.emptyList());
                List<DataHyperedge> candidateEdges = new ArrayList<>(originalCandidateEdges);
                candidateEdges.removeAll(currentHypergraph); // 移除已经在超图中的超边

                // 如果没有合适的候选超边，重新生成超图
                if (candidateEdges.isEmpty()) {
                    break;
                }

                // 随机选择一个候选超边
                DataHyperedge nextEdge = candidateEdges.get(random.nextInt(candidateEdges.size()));

                // 合并顶点
                Set<Long> newVertexSet = new HashSet<>(vertexSet);
                newVertexSet.addAll(nextEdge.getVertexIds());

                // 检查顶点数量约束（此例中为不超过20个顶点，可根据需要调整）
                if (newVertexSet.size() <= 20) { // size >= 0 总是满足
                    currentHypergraph.add(nextEdge);
                    vertexSet = newVertexSet;
                }
            }

            // 如果超图满足条件，添加到结果列表
            if (currentHypergraph.size() == edgeNums) {
                queryHypergraphs.add(new ArrayList<>(currentHypergraph));
            }
        }

        if (queryHypergraphs.size() < numQueries) {
            System.err.println("警告：仅生成了 " + queryHypergraphs.size() + " 个查询超图，未达到预期的 " + numQueries + " 个。");
        }

        return queryHypergraphs;
    }

    private static Map<Long, List<DataHyperedge>> buildVertexToEdgesMap(List<DataHyperedge> hyperedges) {
        Map<Long, List<DataHyperedge>> vertexToEdges = new HashMap<>();
        for (DataHyperedge edge : hyperedges) {
            for (long vertexId : edge.getVertexIds()) {
                vertexToEdges.computeIfAbsent(vertexId, k -> new ArrayList<>()).add(edge);
            }
        }
        return vertexToEdges;
    }

    // 读取文件中的超边数据
    public static List<DataHyperedge> loadHyperedges(String filePath) throws IOException {
        List<DataHyperedge> hyperedges = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                long edgeId = Long.parseLong(parts[0]);
                List<Long> vertices = new ArrayList<>();

                for (int i = 1; i < parts.length - 1; i++)
                    vertices.add(Long.parseLong(parts[i]));
                long time = Long.parseLong(parts[parts.length - 1]);

                DataHyperedge hyperedge = new DataHyperedge(edgeId, time, 1);
                hyperedges.add(hyperedge);
                hyperedge.setVertexIds(vertices);
            }
        }
        return hyperedges;
    }

    // 保存生成的查询超图到文件
    public static void saveQueryHypergraphs(List<List<DataHyperedge>> queryHypergraphs, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            for (int i = 0; i < queryHypergraphs.size(); i++) {

                for (DataHyperedge edge : queryHypergraphs.get(i)) {
                    writer.write(edge.getId() + "\t");
                    for (long vertexId : edge.getVertexIds())
                        writer.write(vertexId + "\t");
                    writer.write(edge.getEdgeTime() + "\n");
                }
                writer.write("\n");
            }
        }
    }
}
