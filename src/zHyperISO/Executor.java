package zHyperISO;

import org.json.JSONArray;
import org.json.JSONObject;
import zHGMatch.graph.PartitionedEdges;
import zHGMatch.graph.QueryGraph;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class Executor {
    private final static String node_path = "src/zHGMatch/data/house-committees/node-labels-house-committees.txt";
    private final static String edge_path = "src/zHGMatch/data/house-committees/hyperedges-house-committees.txt";
    private final static String query_path = "src/zHGMatch/data/house-committees/query.txt";

    public static void main(String[] args) {
        run_query();
    }

    public static void run_query() {
        Hypergraph dataGraph = read_text_to_graph(node_path, edge_path);
        dataGraph.status();

        List<Hypergraph> queryGraphs = readQueryFile();
        int num_of_queries = queryGraphs.size();

        boolean ifUseInverted = true;
        for (int i = 0; i < num_of_queries; i++) {
            Hypergraph queryGraph = queryGraphs.get(i);

            Map<Integer, Set<Integer>> candidates = HFilter.generateCandidates(dataGraph, queryGraph, ifUseInverted);
        }
    }

    public static List<Hypergraph> readQueryFile() {
        // 创建 GraphData 列表
        List<Hypergraph> queryGraphs = new ArrayList<>();

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
                Hypergraph graphData = Hypergraph.fromJSONObject(obj);
                queryGraphs.add(graphData);
            }
        } catch (Exception e) {
            System.err.println("Failed to load or parse JSON data: " + e.getMessage());
        }

        return queryGraphs;
    }

    private static Hypergraph read_text_to_graph(String nodePath, String edgePath) {
        // 打开节点和边文件
        try (BufferedReader nodeReader = new BufferedReader(new FileReader(node_path));
             BufferedReader edgeReader = new BufferedReader(new FileReader(edge_path))) {
            // 读取节点标签
            System.out.println("Reading node labels");

            Map<Integer, Integer> nodeLabels = new HashMap<>();
            String line;
            int i = 1;
            while ((line = nodeReader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    int id = Integer.parseInt(line);
                    nodeLabels.put(i, id);
                    i++;
                }
            }

            // 读取超边
            System.out.println("Reading edges and build inverted index");

            i = 1;
            Map<Integer, List<Integer>> hyperedges = new HashMap<>();
            Map<Integer, Set<Integer>> invertedIndex = new HashMap<>();
            while ((line = edgeReader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(",");
                    List<Integer> edge = new ArrayList<>();
                    for (String part : parts) {
                        int id = Integer.parseInt(part.trim());
                        edge.add(id);

                        // 构建倒排索引
                        invertedIndex.putIfAbsent(id, new HashSet<>());
                        invertedIndex.get(id).add(i);
                    }
                    hyperedges.put(i, edge);
                    i++;
                }
            }

            return new Hypergraph(nodeLabels, hyperedges, invertedIndex);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}