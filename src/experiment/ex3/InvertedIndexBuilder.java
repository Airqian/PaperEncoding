package experiment.ex3;

import java.io.*;
import java.util.*;

// 为数据集构建倒排索引
public class InvertedIndexBuilder {
    public static void main(String[] args) {
        // 数据集信息
        String dataset = "coauth-DBLP";
        String srcSata = "src/dataset/temporal-restricted/";
        String hyperedgeIdFile = srcSata + dataset + "/hyperedge-id-unique.txt";
        String hyperedgeLabelFile = srcSata + dataset + "/hyperedge-label-unique.txt";
        String propertyFile = srcSata  + dataset + "/node-property3.txt";
        String outputFile = "src/experiment/ex3/indexFile/" + dataset + "-inverted.txt";



        long sum = 0;
        for (int i = 0; i < 5; i++) {
            long start = System.currentTimeMillis();

            // 构建索引树数据集信息
//            List<long[]> idToTime = getAllEdgeTimeASC(hyperedgeIdFile); // 将超边按照时间升序排序
            Map<String, List<String>> proMap = getId2PropertyMap(propertyFile); // 顶点id到属性的映射
            Map<String, String> idMap = getEdgeIdMap(hyperedgeIdFile); // 超边id到整条超边的映射（包含顶点id以及属性）
//            Map<String, String> labelMap = getEdgeLabelMap(hyperedgeLabelFile); // 超边标签到整条超边的映射（包含顶点label以及属性）

            buildPropertyToVertexIdMap(proMap, outputFile);
            buildVertexIdtimeToEdgeId(idMap, outputFile);

            long end = System.currentTimeMillis();
            sum += (end - start);
            System.out.println("第" + (i+1) + "次构建时间为：" + (end - start));
        }
        System.out.println("平均构建时间为：" + (sum * 1.0 / 5));
    }

    // 第一部分：属性值到顶点id的倒排索引
    private static void buildPropertyToVertexIdMap(Map<String, List<String>> proMap, String outputFile) {
        // 构建属性到顶点id的索引
        Map<String, List<String>> proToIdMap = new HashMap<>();
        for (String key : proMap.keySet()) {
            List<String> pros = proMap.get(key);
            for (String pro : pros) {
                proToIdMap.putIfAbsent(pro, new ArrayList<>());
                proToIdMap.get(pro).add(key);
            }
        }

//        printFile(proToIdMap, outputFile);
    }

    // 第二部分：顶点id到超边id、时间到超边id
    private static void buildVertexIdtimeToEdgeId(Map<String, String> idMap, String outputFile) {
        Map<String, List<String>> vertexToEdge = new HashMap<>();
        Map<String, List<String>> timeToEdge = new HashMap<>();

        for (String edge : idMap.values()) {
            String[] items = edge.split("\\t");
            String edgeId  = items[0];

            for (int i = 1; i < items.length - 1; i++) {
                String vertexId = items[i];
                vertexToEdge.putIfAbsent(vertexId, new ArrayList<>());
                vertexToEdge.get(vertexId).add(edgeId);
            }

            String time = items[items.length - 1];
            timeToEdge.putIfAbsent(time, new ArrayList<>());
            timeToEdge.get(time).add(edgeId);
        }

//        printFile(vertexToEdge, outputFile);
//        printFile(timeToEdge, outputFile);
    }

    public static void printFile(Map<String, List<String>> map, String outputFile) {
        // 打印文件
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(outputFile, true));
            StringBuilder builder = new StringBuilder();
            for (String key : map.keySet()) {
                builder.append(key).append("\t");
                List<String> ids = map.get(key);
                for (String id : ids)
                    builder.append(id).append("\t");
                builder.append("\n");
            }

            writer.write(builder.toString());
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> getEdgeIdMap(String hyperedgeIdFile) {
        Map<String, String> map = new HashMap<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(hyperedgeIdFile));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] items = line.split("\\t");
                map.put(items[0], line);
            }

            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return map;
    }

    private static Map<String, String> getEdgeLabelMap(String hyperedgeLabelFile) {
        Map<String, String> map = new HashMap<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(hyperedgeLabelFile));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] items = line.split("\\t");
                map.put(items[0], line);
            }

            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return map;
    }

    private static Map<String, List<String>> getId2PropertyMap(String propertyFile) {
        Map<String, List<String>> map = new HashMap<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(propertyFile));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] items = line.split("\\t");
                map.putIfAbsent(items[0], new ArrayList<>());

                for (int i = 1; i < items.length; i++) {
                    map.get(items[0]).add(items[i]);
                }
            }

            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return map;
    }

    private static List<long[]> getAllEdgeTimeASC(String hyperedgeIdFile) {
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(hyperedgeIdFile));
            String line;
            List<long[]> list = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                String[] items = line.split("\\t");
                // 第 0 位是超边id，第 1 位是超边时间
                list.add(new long[] {Long.parseLong(items[0]), Long.parseLong(items[items.length - 1])});
            }

            Collections.sort(list, new Comparator<long[]>() {
                @Override
                public int compare(long[] o1, long[] o2) {
                    return Long.compare(o1[1], o2[1]);
                }
            });
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
