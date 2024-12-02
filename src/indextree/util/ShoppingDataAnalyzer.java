package indextree.util;



import encoding.util.PeriodType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static DataHandler.shoppingDataHandler.FilePathConstants.SHOPPING_EVENT_DATA_FILE_PATH;
import static indextree.util.GlobalConstants.SUBJECT_INDEX;

public class ShoppingDataAnalyzer {
    public static void main(String[] args) {
        ShoppingDataAnalyzer shoppingDataAnalyzer = new ShoppingDataAnalyzer();
        String shoppingfilePath = SHOPPING_EVENT_DATA_FILE_PATH;
        shoppingDataAnalyzer.readShoppingFile(shoppingfilePath,  PeriodType.MONTH);
    }

    // 默认主体属性是第一个元素
    // 对数据集进行基础分析，得到数据集中记录总条数、事件最早发生时间、事件最晚发生时间不同主体的事件数量数以及时间范围
    public static DataSetInfo readShoppingFile(String filePath, PeriodType periodType) {
        if (filePath == null || filePath.length() == 0)
            return null;

        int total = 0;
        int maxPropertyNum = 0;
        long globalMinTime = Long.MAX_VALUE;
        long globalMaxTime = Long.MIN_VALUE;
        Map<Long, List<String>> subjects = new HashMap<>(); // 按主体为单位保存事件数据

        // 1. 解析数据集，以月份（年份）为单位计算出数据集的时间跨度，统计每个月份（年份）发生的事件的主体以及相应的事件，将事件按照时间进行排序组织
        // 2. 根据数据集的特点，树的叶子窗口会存在不同的主体属性，需要制定算法计算不同的主体属性之间的相似度，在不超过窗口容量的条件下，
        //    优先将相似度更高的主体的事件放在同一个窗口里
        // 3. 设定好窗口容量以及树的高度后，优先以时间范围组织事件，构建索引树即可
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        EventOrganizerSortByTime organizer = new EventOrganizerSortByTime(periodType);
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] elements = line.split("\\t");
                maxPropertyNum = Math.max(maxPropertyNum, elements.length - 2); // 去掉超边id和时间

                // elements[0] 是事件id
                long subjectId = Long.valueOf(elements[SUBJECT_INDEX]);
                subjects.putIfAbsent(subjectId, new ArrayList<>());
                subjects.get(subjectId).add(line);

                String timeStr = elements[elements.length - 1];
                long time = sdf.parse(timeStr).getTime();

                organizer.addEvent(subjectId, timeStr, time, line);
                globalMinTime = Math.min(globalMinTime, time);
                globalMaxTime = Math.max(globalMaxTime, time);

                total++;
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
        organizer.printAllEvents();

        // 计算最早时间和最晚时间之间的月份差异
        int monthDiff = calculateMonthDifference(globalMinTime, globalMaxTime);

        // 组装 dataSetInfo
        DataSetInfo dataSetInfo = new DataSetInfo();
        dataSetInfo.setEventsNum(total);
        dataSetInfo.setGlobalMinTime(globalMinTime);
        dataSetInfo.setGlobalMaxTime(globalMaxTime);
        dataSetInfo.setMonthDiff(monthDiff);
        dataSetInfo.setOrganizer(organizer);
        dataSetInfo.setMaxPropertyNum(maxPropertyNum);

//        System.out.println("total = " + total);
//        System.out.println("数据集中主体的个数：" + subjects.size());
//        for (Map.Entry<Long, List<String>> entry : subjects.entrySet()) {
//            System.out.println("主体：" + entry.getKey() + " 对应的事件数量为:" + entry.getValue().size());
//        }
//        System.out.println("数据集中的总月份数为：" + monthDiff);

        return dataSetInfo;
    }

    private static int calculateMonthDifference(long startTime, long endTime) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTimeInMillis(startTime);
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(endTime);

        int yearDiff = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
        int monthDiff = endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);
        return yearDiff * 12 + monthDiff;
    }
}