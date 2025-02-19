package experiment.ex2;



import DataHandler.TempralGraphDataHandler.DataSetReader;
import DataHandler.TempralGraphDataHandler.IndexTreeBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * 实验二测试索引树构建时间，其中固定 hashFuncCount = 2。
 * nProperty = 1..6，encodingLength = {50,75,100,125,150,175}
 */
public class BuildTimeWithArguments {
    public static void main(String[] args) {
        String dataset = args[0];
        int nProperty = Integer.valueOf(args[1]);

        String srcSata = "../src/dataset/temporal-restricted/";
        String hyperedgeIdFile = srcSata + dataset + "/hyperedge-id-unique.txt";
        String hyperedgeLabelFile = srcSata + dataset + "/hyperedge-label-unique.txt";
        String propertyFile = srcSata  + dataset + "/node-property" + nProperty + ".txt";
        String treeInfo = "./classes/experiment/ex1/files/" + dataset + "-TreeInfo.txt";

        // 构建索引树数据集信息
        List<long[]> idToTime = DataSetReader.getAllEdgeTimeASC(hyperedgeIdFile); // 将超边按照时间升序排序
        Map<String, List<String>> proMap = DataSetReader.getId2PropertyMap(propertyFile); // 顶点到属性的映射
        Map<String, String> idMap = DataSetReader.getEdgeIdMap(hyperedgeIdFile); // 超边id到整条超边的映射（包含顶点id以及属性）
        Map<String, String> labelMap = DataSetReader.getEdgeLabelMap(hyperedgeLabelFile); // 超边标签到整条超边的映射（包含顶点label以及属性）

        // 构建索引树所需要的参数
        int windowSize = 128;
        int hashFuncCount = 2;
        int secondaryIndexSize = 8;
        int minInternalNodeChilds = 15;
        int maxInternalNodeChilds = 15;

        // 代码预热，做JIT优化
        for (int i = 0; i < 20; i++) {
            IndexTreeBuilder.buildWithoutFileIOTime(idToTime, proMap, idMap, labelMap,
                    windowSize, 50, hashFuncCount, minInternalNodeChilds,
                    maxInternalNodeChilds, secondaryIndexSize, false);
        }
//        System.gc(); // 显式调用 GC，减少干扰

        // 跑十遍，去掉最高值和最低值，取平均值
        // 50,75,100,125,150, 175
        int[] arr = new int[]{50,75,100,125,150,175};
        for (int i = 0; i < arr.length; i++) {
            long[] times = new long[10]; // 测量 10 次
            System.out.println("编码长度：" + arr[i]);

            for (int j = 0; j < 10; j++) {
                long start = System.nanoTime();
                IndexTreeBuilder.buildWithoutFileIOTime(idToTime, proMap, idMap, labelMap,
                        windowSize, arr[i], hashFuncCount, minInternalNodeChilds,
                        maxInternalNodeChilds, secondaryIndexSize, false);
                long end = System.nanoTime();
                times[j] = end - start;
            }

            Arrays.sort(times);
            long sum = 0;
            for (int k = 1; k < times.length - 1; k++) {
                sum += times[k];
            }
            long avg = sum / (times.length - 2);
            System.out.println("索引树构建的平均时间（毫秒）：" + avg / 1_000_000);
            System.out.println();
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
