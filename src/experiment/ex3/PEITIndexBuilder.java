package experiment.ex3;

import DataHandler.TempralGraphDataHandler.DataSetReader;
import DataHandler.TempralGraphDataHandler.IndexTreeBuilder;
import indextree.IndexTree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class PEITIndexBuilder {
    // coauth-DBLP-100 coauth-DBLP-400 coauth-MAG-Geology-100 coauth-MAG-Geology-400
    // congress-bills-100 congress-bills-400 NDC-classes-100 NDC-classes-400
    // NDC-substances-100 NDC-substances-400 tags-ask-ubuntu-100 tags-ask-ubuntu-400
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
        String propertyFile = srcSata + dataset + "/node-property3.txt";

        // 构建索引树数据集信息
        // 构建索引树数据集信息
        List<long[]> idToTime = DataSetReader.getAllEdgeTimeASC(hyperedgeIdFile); // 将超边按照时间升序排序
        Map<String, List<String>> proMap = DataSetReader.getId2PropertyMap(propertyFile); // 顶点到属性的映射
        Map<String, String> idMap = DataSetReader.getEdgeIdMap(hyperedgeIdFile); // 超边id到整条超边的映射（包含顶点id以及属性）
        Map<String, String> labelMap = DataSetReader.getEdgeLabelMap(hyperedgeLabelFile); // 超边标签到整条超边的映射（包含顶点label以及属性）

        // 索引树原信息
        int hashFuncCount = 2;
        int windowSize = 128;
        int secondaryIndexSize = 8;
        int minInternalNodeChilds = 15;
        int maxInternalNodeChilds = 15;

        // openSecondaryIndex 控制是否构建索引时是否进行优化
        boolean openSecondaryIndex = true;
        int encodingLength = encodingLengthMap.get(dataset);
        String treeInfo;

        if (openSecondaryIndex)
            treeInfo = "src/experiment/ex3/indexFile/" + dataset + "-PEIT-O-" + encodingLength + ".txt";
        else
            treeInfo = "src/experiment/ex3/indexFile/" + dataset + "-PEIT-" + encodingLength + ".txt";

        for (int i = 0; i < 5; i++) {
            IndexTreeBuilder.buildWithoutFileIOTime(idToTime, proMap, idMap, labelMap, windowSize, encodingLength,
                    hashFuncCount, minInternalNodeChilds, maxInternalNodeChilds, secondaryIndexSize, openSecondaryIndex);
        }

        long sum = 0;
        for (int i = 0; i < 5; i++) {
            long start = System.currentTimeMillis();
            IndexTreeBuilder.buildWithoutFileIOTime(idToTime, proMap, idMap, labelMap, windowSize, encodingLength,
                    hashFuncCount, minInternalNodeChilds, maxInternalNodeChilds, secondaryIndexSize, openSecondaryIndex);
            long end = System.currentTimeMillis();
            System.out.println("第" + (i+1) + "次构建时间为：" + (end - start));
            sum += (end - start);
        }
        System.out.println("平均构建时间为：" + (sum * 1.0 / 5));

        // 打印索引树查看索引大小
//        indexTree.printTree(treeInfo, openSecondaryIndex);
//        readLine(treeInfo);

    }

    // 查看生成的索引树文件的行数
    public static void readLine(String filePath) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            int lines = 0;

            while ((line = reader.readLine()) != null)
                lines++;

            reader.close();
            System.out.println("行数: " + lines);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
