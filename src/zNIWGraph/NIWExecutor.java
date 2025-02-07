package zNIWGraph;


import org.json.JSONArray;
import org.json.JSONObject;
import zNIWGraph.extend.ExecutionPlan;
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

    private final static String node_path = "src/zNIWGraph/data/house-committees/node-labels-house-committees.txt";
    private final static String edge_path = "src/zNIWGraph/data/house-committees/hyperedge-removeduplicate.txt";
    private final static String query_path = "src/zNIWGraph/data/house-committees/query.txt";

    public static void main(String[] args) {
        run_query();
    }

    public static void run_query() {
        // 先读取query文件
        List<QueryGraph> queryGraphs = readQueryFile();
        int num_of_queries = queryGraphs.size();

        // 读取数据集文件
        Pair<PartitionedEdges, NIWHypergraph> pair = read_text_to_graph(node_path, edge_path);
        PartitionedEdges data_graph = pair.getKey();
        NIWHypergraph niwHypergraph = pair.getValue();  // 里面包含超边id、顶点倒排索引
        IntersectionLabelGraph intersectionLabelGraph = niwHypergraph.build_weighted_graph_inverted();

        data_graph.status();
        intersectionLabelGraph.status();

        int total_count = 0;
        long total_time = 0l;
        boolean all_plans = false;

        for (int i = 0; i < num_of_queries; i++) {
            QueryGraph queryGraph = queryGraphs.get(i);
            MatchDriver driver;

            if (all_plans) {
                List<ExecutionPlan> executionPlans = new ExecutionPlan().all_plans_from_query(queryGraph);
                driver = new MatchDriver("Query-" + i, queryGraph, executionPlans, true);
            } else {
                ExecutionPlan plan = new ExecutionPlan().from_query(queryGraph, data_graph);
                driver = new MatchDriver("Query-" + i, queryGraph, new ArrayList<>(Arrays.asList(plan)), true);
            }

            Pair<Integer, Long> res = driver.run(data_graph, 100);
            total_count += res.getKey();
            total_time = Math.addExact(total_time, res.getValue());
        }

        System.out.println("total_time = " + total_time);
        double time = total_time / 1_000_000_000.0;
        System.out.print("Completed " + num_of_queries + " Queries  - Total Count: " + total_count);
        System.out.printf(", Total Time: %.6f s", time);
    }

    public static List<QueryGraph> readQueryFile() {
        // 创建 GraphData 列表
        List<QueryGraph> queryGraphs = new ArrayList<>();

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
                QueryGraph graphData = QueryGraph.fromJSONObject(obj);
                queryGraphs.add(graphData);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load or parse JSON data.", e);
            System.err.println("Failed to load or parse JSON data: " + e.getMessage());
        }

        return queryGraphs;
    }

    // 读取数据超图并建立超边映射与顶点到超边的倒排索引
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

                    edgeIndex.add_edge(edge);

                    // 构建超边到id、id到超边的映射map
                    edgeToId.put(edge, i);
                    idToEdge.put(i, edge);

                    i++;
                }
            }

            NIWHypergraph hyperGraph = new NIWHypergraph(idToEdge, edgeToId, vertexToEdges, nodeLabels);

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