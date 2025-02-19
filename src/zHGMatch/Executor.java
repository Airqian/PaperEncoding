package zHGMatch;


import org.json.JSONArray;
import org.json.JSONObject;
import zHGMatch.extend.ExecutionPlan;
import zHGMatch.extend.MatchDriver;
import zHGMatch.graph.PartitionedEdges;
import zHGMatch.graph.QueryGraph;
import zHGMatch.graph.util.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Executor {
    private static final Logger logger = Logger.getLogger(Executor.class.getName());

    private static String node_path;
    private static String edge_path;
    private static String query_path;
    private final static String[] paths = {"CP"}; // "CH", "CP", "HB", "HC", "MA", "SA", "SB", "TC", "WT"

    public static void main(String[] args) {
        for (String dataName : paths) {
            node_path = "src/dataset/hypergraph/" + dataName + "/node-labels.txt";
            edge_path = "src/dataset/hypergraph/" + dataName + "/hyperedge-removeduplicate.txt";
            query_path = "src/dataset/hypergraph/" + dataName + "/s_3_m.txt";  // m_4_m、m_6_l、s_2_m、s_3_m

            run_query();
        }
    }

    public static void run_query() {
        // 先读取query文件
        List<QueryGraph> queryGraphs = readQueryFile();
        int num_of_queries = queryGraphs.size();

        // 读取数据集文件
        PartitionedEdges data_graph = read_text_to_graph(node_path, edge_path);
        data_graph.status();

        int total_count = 0;
        long total_time = 0l;
        boolean all_plans = false;

        for (int i = 0; i < num_of_queries; i++) {
            QueryGraph queryGraph = queryGraphs.get(i);
            MatchDriver driver;

            if (all_plans) {
                List<ExecutionPlan> executionPlans = new ExecutionPlan().all_plans_from_query(queryGraph);
                driver = new MatchDriver("Query-" + i, queryGraph, executionPlans, true);
                driver.setPrint_results(false);
            } else {
                ExecutionPlan plan = new ExecutionPlan().from_query(queryGraph, data_graph);
                driver = new MatchDriver("Query-" + i, queryGraph, new ArrayList<>(Arrays.asList(plan)), true);
                driver.setPrint_results(false);
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

    public static PartitionedEdges read_text_to_graph(String node_path, String edge_path) {
        // 打开节点和边文件
        try (BufferedReader nodeReader = new BufferedReader(new FileReader(node_path));
             BufferedReader edgeReader = new BufferedReader(new FileReader(edge_path))) {
            System.out.println("Reading node labels");

            // 读取节点标签
            List<Integer> nodeLabels = new ArrayList<>();
            String line;
            while ((line = nodeReader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    int id = Integer.parseInt(line);
                    nodeLabels.add(id);
                }
            }

            System.out.println("Reading edges");
            // 读取边
            PartitionedEdges edgeIndex = new PartitionedEdges(nodeLabels);

            while ((line = edgeReader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(",");
                    List<Integer> edge = new ArrayList<>();
                    for (String part : parts) {
                        int id = Integer.parseInt(part.trim());
                        edge.add(id);
                    }
                    edgeIndex.add_edge(edge);
                }
            }

            System.out.println("Building graph");
            edgeIndex.build_index();

            return edgeIndex;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}