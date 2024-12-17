package experiment.ex3;

import DataHandler.TempralGraphDataHandler.DataSetReader;
import encoding.PPBitset;
import encoding.PropertyEncodingConstructor;
import experiment.ex3.btree.BPlusTree;
import indextree.hyperedge.DataHyperedge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class BPlusTreeBuilder {
    public static void main(String[] args) {
        Map<String, Integer> encodingLengthMap = new HashMap<>();
        encodingLengthMap.put("NDC-classes", 90);
        encodingLengthMap.put("NDC-substances", 100);
        encodingLengthMap.put("congress-bills", 120);
        encodingLengthMap.put("tags-ask-ubuntu", 90);
        encodingLengthMap.put("coauth-MAG-Geology", 83);
        encodingLengthMap.put("coauth-DBLP", 84);

        // 数据集信息
        String dataset = "coauth-DBLP";
        String srcSata = "src/dataset/temporal-restricted/";
        String hyperedgeIdFile = srcSata + dataset + "/hyperedge-id-unique.txt";
        String hyperedgeLabelFile = srcSata + dataset + "/hyperedge-label-unique.txt";
        String propertyFile = srcSata  + dataset + "/node-property3.txt";
        String outputFile = "src/experiment/ex3/indexFile/" + dataset + "-BPlusTree.txt";

        // 构建索引树数据集信息
        List<long[]> idToTime = DataSetReader.getAllEdgeTimeASC(hyperedgeIdFile); // 将超边按照时间升序排序
        Map<String, List<String>> proMap = DataSetReader.getId2PropertyMap(propertyFile); // 顶点到属性的映射
        Map<String, String> idMap = DataSetReader.getEdgeIdMap(hyperedgeIdFile); // 超边id到整条超边的映射（包含顶点id以及属性）
        Map<String, String> labelMap = DataSetReader.getEdgeLabelMap(hyperedgeLabelFile); // 超边标签到整条超边的映射（包含顶点label以及属性）

        // 编码相关信息
        int encodingLength = encodingLengthMap.get(dataset);
        int hashFuncCount = 2;

        // b+树
        long sum = 0;
        for (int k = 0; k < 5; k++) {
            long start = System.currentTimeMillis();

            int order = 128;
            BPlusTree<DataHyperedge, Long> bPlusTree = new BPlusTree<>(order);
            for (int i = 0; i < idToTime.size(); i++) {
                long edgeId = idToTime.get(i)[0];   // 超边id
                long edgeTime = idToTime.get(i)[1]; // 超边时间
                String edgeIdDetail = idMap.get(String.valueOf(edgeId)); // 整条超边详情
                String[] items = edgeIdDetail.split("\\t");

                DataHyperedge hyperedge = new DataHyperedge(Long.valueOf(edgeId), edgeTime, encodingLength);
                // 对该超边中包含的所有顶点的所有属性进行编码，合成超边编码
                PPBitset bitset = new PPBitset(encodingLength);
                for (int j = 1; j < items.length - 1; j++) {
                    String vertexId = items[j];
                    hyperedge.addVertexId(Long.valueOf(vertexId));
                    for (String prop : proMap.get(vertexId)) {
                        PPBitset tmp = PropertyEncodingConstructor.encoding(prop, encodingLength, hashFuncCount);
                        bitset.or(tmp);
                    }
                }

                hyperedge.setEncoding(bitset);
                bPlusTree.insertOrUpdate(hyperedge, edgeId);
            }

            long end = System.currentTimeMillis();
            sum += (end - start);
            System.out.println("第" + (k+1) + "次构建时间为：" + (end - start));
        }
        System.out.println("平均构建时间为：" + (sum * 1.0 / 5));
//        bPlusTree.printBPlusTree(outputFile);
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
