package DataHandler.TempralGraphDataHandler;

import indextree.IndexTree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class IndexTreeBuilder {

    public static void main(String[] args) {
        String hyperedgeIdFile = "dataset/temporal-restricted/NDC-classes/hyperedge-id-unique.txt";
        String hyperedgeLabelFile = "dataset/temporal-restricted/NDC-classes/hyperedge-label-unique.txt";
        String propertyFile = "dataset/temporal-restricted/NDC-classes/node-property1.txt";
    }

    public static IndexTree build(String hyperedgeIdFile,
                                  String hyperedgeLabelFile,
                                  String propertyFile,
                                  int windowSize,
                                  int encodingLength,
                                  int hashFuncCount,
                                  int minInternalNodeChilds,
                                  int maxInternalNodeChilds,
                                  int secondaryIndexSize,
                                  String treePrintOutFile,
                                  boolean openSecondaryIndex) {
        List<long[]> idToTime = getAllEdgeTimeASC(hyperedgeIdFile); // 将超边按照时间升序排序
        Map<String, List<String>> proMap = getId2PropertyMap(propertyFile); // 顶点到属性的映射
        Map<String, String> idMap = getEdgeIdMap(hyperedgeIdFile); // 超边id到整条超边的映射（包含顶点id以及属性）
        Map<String, String> labelMap = getEdgeLabelMap(hyperedgeLabelFile); // 超边id到整条超边的映射（包含顶点label以及属性）

//        for (int i = idToTime.size() - 1; i >= idToTime.size() - 10; i--)
//            System.out.println(idToTime.get(i)[0] + " " + idToTime.get(i)[1]);

        IndexTree indexTree = new IndexTree(windowSize, encodingLength, hashFuncCount, minInternalNodeChilds, maxInternalNodeChilds, secondaryIndexSize);
        indexTree.buildTemporalHypergraphTree(idToTime, idMap, labelMap, proMap, treePrintOutFile, openSecondaryIndex);
        return indexTree;
    }

    public static IndexTree buildWithoutFileIOTime(List<long[]> idToTime,
                                                   Map<String, List<String>> proMap,
                                                   Map<String, String> idMap,
                                                   Map<String, String> labelMap,
                                                   int windowSize,
                                                   int encodingLength,
                                                   int hashFuncCount,
                                                   int minInternalNodeChilds,
                                                   int maxInternalNodeChilds,
                                                   int secondaryIndexSize,
                                                   String treePrintOutFile,
                                                   boolean openSecondaryIndex) {

        IndexTree indexTree = new IndexTree(windowSize, encodingLength, hashFuncCount, minInternalNodeChilds, maxInternalNodeChilds, secondaryIndexSize);
        indexTree.buildTemporalHypergraphTree(idToTime, idMap, labelMap, proMap, treePrintOutFile, openSecondaryIndex);
        return indexTree;
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
                list.add(new long[]{Long.parseLong(items[0]), Long.parseLong(items[items.length - 1])});
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
