package experiment;

import indextree.hyperedge.DataHyperedge;

import java.io.*;
import java.util.*;

import static experiment.FilePathConstants.*;

public class QueryGraphGenerator {
    public static void main(String[] args) throws IOException {
        // 1. 读取超边数据并构建顶点到超边的映射
        List<DataHyperedge> hyperedges = loadHyperedges(NDC_CLASSES_HYPEREDGE_ID_UNIQUE);
        Map<Long, List<DataHyperedge>> vertexToEdges = buildVertexToEdgesMap(hyperedges);

        // 2. 生成15个查询超图
        List<List<DataHyperedge>> queryHypergraphs = generateQueryHypergraphs(hyperedges, vertexToEdges, 15);

        // 3. 输出结果
        saveQueryHypergraphs(queryHypergraphs, NDC_CLASSES_QUERY_3);
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

    // 生成15个查询超图
    public static List<List<DataHyperedge>> generateQueryHypergraphs(List<DataHyperedge> hyperedges,
                                                                     Map<Long, List<DataHyperedge>> vertexToEdges, int numQueries) {
        List<List<DataHyperedge>> queryHypergraphs = new ArrayList<>();
        Random random = new Random();

        while (queryHypergraphs.size() < numQueries) {
            // 随机选取起始超边
            DataHyperedge startEdge = hyperedges.get(random.nextInt(hyperedges.size()));

            // 初始化超图
            List<DataHyperedge> currentHypergraph = new ArrayList<>();
            currentHypergraph.add(startEdge);

            // 收集当前超图的所有顶点
            Set<Long> vertexSet = new HashSet<>(startEdge.getVertexIds());

            // 添加其他超边
            while (currentHypergraph.size() < 3) {
                // 从前一条超边中随机选一个顶点
                DataHyperedge lastEdge = currentHypergraph.get(currentHypergraph.size() - 1);
                Long randomVertex = lastEdge.getVertexIds().get(random.nextInt(lastEdge.getVertexIds().size()));

                // 找到包含该顶点的候选超边
                List<DataHyperedge> candidateEdges = vertexToEdges.getOrDefault(randomVertex, new ArrayList<>());
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
                if (newVertexSet.size() >= 0 && newVertexSet.size() <= 20) {
                    currentHypergraph.add(nextEdge);
                    vertexSet = newVertexSet;
                }
            }

            // 如果超图满足条件，添加到结果列表
            if (currentHypergraph.size() == 3) {
                queryHypergraphs.add(new ArrayList<>(currentHypergraph));
            }
        }
        return queryHypergraphs;
    }

    // 保存生成的查询超图到文件
    public static void saveQueryHypergraphs(List<List<DataHyperedge>> queryHypergraphs, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (int i = 0; i < queryHypergraphs.size(); i++) {
                writer.write("Query Hypergraph " + (i + 1) + ":\n");
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
