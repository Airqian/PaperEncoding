package zNIWGraph;


import org.json.JSONArray;
import org.json.JSONObject;
import zNIWGraph.extend.ExecutionPlan;
import zNIWGraph.extend.ExecutionPlanUtils;
import zNIWGraph.extend.MatchDriver;
import zNIWGraph.graph.NIWHypergraph;
import zNIWGraph.graph.PartitionedEdges;
import zNIWGraph.graph.QueryGraph;
import zNIWGraph.graph.util.Pair;
import zNIWGraph.index.IntersectionLabelGraph;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NIWExecutor {
    private static final Logger logger = Logger.getLogger(NIWExecutor.class.getName());

    private static String node_path;
    private static String edge_path;
    private static String query_path;


    public static void main(String[] args) {
        String[] paths = {"CP"}; // "CH", "CP", "HB", "HC", "MA", "SA", "SB", "TC", "WT"

        for (String dataName : paths) {
            node_path = "src/dataset/hypergraph/" + dataName + "/node-labels.txt";
            edge_path = "src/dataset/hypergraph/" + dataName + "/hyperedge-removeduplicate.txt";
            query_path = "src/dataset/hypergraph/" + dataName + "/s_3_m.txt"; // m_4_m、m_6_l、s_2_m、s_3_m、query

            run_query();
        }
    }

    public static void run_query() {
        // 先读取query文件，会同时获得每个查询超图和其对应的超图数据（用于构建二元交叉邻居权重图）
        Pair<List<QueryGraph>, List<NIWHypergraph>> queryPair = readQueryFile();
        List<QueryGraph> queryGraphs = queryPair.getKey();
        List<NIWHypergraph> queryNIWGraphs = queryPair.getValue();

        // 读取数据集文件，构建数据图的二元交叉邻居权重图
        Pair<PartitionedEdges, NIWHypergraph> dataPair = read_text_to_graph(node_path, edge_path);
        PartitionedEdges dataGraph = dataPair.getKey();
        NIWHypergraph dataNIWGraph = dataPair.getValue();  // 里面包含超边id、顶点倒排索引

        System.out.print("--- Datagraph build_weighted_graph_inverted: ");
        IntersectionLabelGraph dataInterLabelGraph = dataNIWGraph.build_weighted_graph_inverted();

        dataGraph.status();
        System.out.printf("数据二元邻居权重图占用的空间大小约为 %.2f KB\n\n", dataInterLabelGraph.status());

        int total_count = 0;
        long total_time = 0l;
        boolean all_plans = false;

        int num_of_queries = queryGraphs.size();
        for (int i = 0; i < num_of_queries; i++) {
            QueryGraph queryGraph = queryGraphs.get(i);

            System.out.print("--------------------- Query " + i + " build_weighted_graph_inverted: ");
            NIWHypergraph queryNIWGraph = queryNIWGraphs.get(i);
            IntersectionLabelGraph queryInterLabelGraph = queryNIWGraph.build_weighted_graph_inverted();
            System.out.printf("查询" + i + "二元邻居权重图占用的空间大小约为 %.2f KB\n\n", queryInterLabelGraph.status());

            MatchDriver driver;
            if (all_plans) {
                List<ExecutionPlan> executionPlans = new ExecutionPlan().all_plans_from_query(queryGraph);
                driver = new MatchDriver("Query-" + i, queryGraph, executionPlans, true);
                driver.setPrint_results(false);
            } else {
                ExecutionPlan plan = new ExecutionPlan().from_query(queryGraph, dataGraph, dataNIWGraph, queryNIWGraph, dataInterLabelGraph, queryInterLabelGraph);
                driver = new MatchDriver("Query-" + i, queryGraph, new ArrayList<>(Arrays.asList(plan)), true);
                driver.setPrint_results(true);
            }

            PartitionedEdges candidateDataGraph = generateCandidateDataGraph(queryGraph, dataGraph.getNodeLabels(), dataNIWGraph);
            Pair<Integer, Long> res = driver.run(candidateDataGraph, 100);
            total_count += res.getKey();
            total_time = Math.addExact(total_time, res.getValue());
        }

        System.out.println("total_time = " + total_time);
        double time = total_time / 1_000_000_000.0;
        System.out.print("Completed " + num_of_queries + " Queries  - Total Count: " + total_count);
        System.out.printf(", Total Time: %.6f s", time);
    }

    /**
     * 根据双重约束过滤机制剩下的候选超边构建候选数据图
     */
    public static PartitionedEdges generateCandidateDataGraph(QueryGraph queryGraph, List<Integer> nodelabels, NIWHypergraph dataNIWGraph) {
        PartitionedEdges partitionedEdges = new PartitionedEdges(nodelabels);
        Set<Integer> hasAdded = new HashSet<>();

        for (List<Integer> ids : queryGraph.getFilteredCandidates()) {
            for (int id : ids) {
                if (hasAdded.contains(id)) continue;
                List<Integer> edge = dataNIWGraph.getEdge(id);
                partitionedEdges.add_edge(new ArrayList<>(edge));
                hasAdded.add(id);
            }
        }

        return partitionedEdges;
    }

    public static Pair<List<QueryGraph>, List<NIWHypergraph>> readQueryFile() {
        // 创建 GraphData 列表
        List<QueryGraph> queryGraphs = new ArrayList<>();
        List<NIWHypergraph> hypergraphs = new ArrayList<>();

        try {
            // 读取整个文件内容为字符串
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(query_path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            // 解析为 JSONArray
            JSONArray jsonArray = new JSONArray(sb.toString());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Pair<QueryGraph, NIWHypergraph> pair = QueryGraph.fromJSONObject(obj);
                QueryGraph graphData = pair.getKey();
                NIWHypergraph queryHG = pair.getValue();

                queryGraphs.add(graphData);
                hypergraphs.add(queryHG);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load or parse JSON data.", e);
            System.err.println("Failed to load or parse JSON data: " + e.getMessage());
        }

        return new Pair<>(queryGraphs, hypergraphs);
    }

    // 读取数据超图并建立超边映射与顶点到超边的倒排索引
    // ⚠️⚠️⚠️ 数据图里下标0处的标签代表顶点1
    public static Pair<PartitionedEdges, NIWHypergraph> read_text_to_graph(String node_path, String edge_path) {
        // 打开节点和边文件
        try (BufferedReader nodeReader = new BufferedReader(new FileReader(node_path));
             BufferedReader edgeReader = new BufferedReader(new FileReader(edge_path))) {
            Map<Integer, List<Integer>> idToEdge = new HashMap<>();
            Map<List<Integer>, Integer> edgeToId = new HashMap<>();
            Map<Integer, Set<Integer>> vertexToEdges = new HashMap<>();

            // 读取节点标签，节点的标签是从 0 开始计数
            System.out.println("Reading node labels");

            List<Integer> nodeLabels = new ArrayList<>();
            String line;
            while ((line = nodeReader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    int label = Integer.parseInt(line);
                    nodeLabels.add(label);
                }
            }

            // 读取超边
            System.out.println("Reading edges");
            PartitionedEdges edgeIndex = new PartitionedEdges(nodeLabels);

            int i = 1;
            while ((line = edgeReader.readLine()) != null) {
                line = line.trim();

                if (!line.isEmpty()) {
                    String[] parts = line.split(",");
                    List<Integer> edge = new ArrayList<>();

                    for (String part : parts) {
                        int vertexId = Integer.parseInt(part.trim());
                        vertexToEdges.putIfAbsent(vertexId, new HashSet<>());
                        vertexToEdges.get(vertexId).add(i);
                        edge.add(vertexId);
                    }
//                    Collections.sort(edge);
                    List<Integer> newEdge = edgeIndex.add_edge(edge);

                    // 构建超边到id、id到超边的映射map
                    edgeToId.put(newEdge, i);
                    idToEdge.put(i, newEdge);

                    i++;
                }
            }

            NIWHypergraph hyperGraph = new NIWHypergraph(idToEdge, edgeToId, vertexToEdges, nodeLabels);
            hyperGraph.setIfQueryGraph(false);

            System.out.println("Building graph");
            edgeIndex.build_index(); // 超边分区索引

            return new Pair<>(edgeIndex, hyperGraph);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}