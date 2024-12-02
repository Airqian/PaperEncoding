package experiment.ex2;


import DataHandler.TempralGraphDataHandler.IndexTreeBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

@Deprecated
public class BuildTimeAverageCaculator {
    public static void main(String[] args) {
        final int nProperty = 2;
        String hyperedgeIdFile = "src/dataset/temporal-restricted/NDC-classes/hyperedge-id-unique.txt";
        String hyperedgeLabelFile = "src/dataset/temporal-restricted/NDC-classes/hyperedge-label-unique.txt";
        String propertyFile = "src/dataset/temporal-restricted/NDC-classes/node-property" + nProperty + ".txt";
        String treeInfo = "src/experiment/ex1/files/NDC-classes-TreeInfo.txt";

        List<long[]> idToTime = getAllEdgeTimeASC(hyperedgeIdFile); // 将超边按照时间升序排序
        Map<String, List<String>> proMap = getId2PropertyMap(propertyFile); // 顶点到属性的映射
        Map<String, String> idMap = getEdgeIdMap(hyperedgeIdFile); // 超边id到整条超边的映射（包含顶点id以及属性）
        Map<String, String> labelMap = getEdgeLabelMap(hyperedgeLabelFile); // 超边id到整条超边的映射（包含顶点label以及属性）

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
                    maxInternalNodeChilds, secondaryIndexSize, treeInfo, false);
        }
//        System.gc(); // 显式调用 GC，减少干扰

        // 跑十遍，去掉最高值和最低值，取平均值
        // 50,75,100,125,150, 175
        int[] arr = new int[]{50,75,100,125,150, 175};
        for (int i = 0; i < arr.length; i++) {
            long[] times = new long[10]; // 测量 10 次
            System.out.println("编码长度：" + arr[i]);

            for (int j = 0; j < 10; j++) {
                long start = System.nanoTime();
                IndexTreeBuilder.buildWithoutFileIOTime(idToTime, proMap, idMap, labelMap,
                        windowSize, arr[i], hashFuncCount, minInternalNodeChilds,
                        maxInternalNodeChilds, secondaryIndexSize, treeInfo, false);
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
