package indextree.util;


import encoding.util.PeriodType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static DataHandler.shoppingDataHandler.FilePathConstants.SHOPPIONG_EVENT_IOFO;

/**
 * 按时间段、用户组织事件，对于给定的时间段和用户id，事件列表按时间进行排序
 */
public class EventOrganizerSortByTime {
    private Map<String, Map<Long, List<Event>>> eventMap;  // TreeMap 保证时间段有序
    private DateTimeFormatter originalFormat;
    private DateTimeFormatter targetFormat;

    public EventOrganizerSortByTime(PeriodType type) {
        eventMap = new TreeMap<>();
        originalFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if (type == PeriodType.MONTH)
            targetFormat = DateTimeFormatter.ofPattern("yyyy-MM");
        else if (type == PeriodType.YEAR)
            targetFormat = DateTimeFormatter.ofPattern("yyyy");
    }

    public Map<String, Map<Long, List<Event>>> getEventMap() {
        return eventMap;
    }

    // 添加事件，并使用二分查找插入到正确的位置
    public void addEvent(long user, String originalTimeStr, long time, String eventDetail) {
        // 将原始的时间字符串转化为对应的时间段key
        String targetTimeStr = LocalDateTime.parse(originalTimeStr, originalFormat).format(targetFormat);
        eventMap.putIfAbsent(targetTimeStr, new HashMap<>());
        eventMap.get(targetTimeStr).putIfAbsent(user, new ArrayList<>());

        List<Event> userEvents = eventMap.get(targetTimeStr).get(user);

        // 使用二分查找找到插入位置
        int insertIndex = findInsertPosition(userEvents, time);

        // 插入事件
        userEvents.add(insertIndex, new Event(user, time, eventDetail));
    }

    // 二分查找找到插入的位置
    private int findInsertPosition(List<Event> events, long time) {
        // 自定义 Comparator 对比 Event 的时间
        Comparator<Event> comparator = Comparator.comparing(Event::getTime);

        // 构建一个虚拟 Event 只用来比较时间，不存储
        Event searchKey = new Event(0l, time, null);
        int index = Collections.binarySearch(events, searchKey, comparator);

        // 如果找到精确匹配的索引，返回该位置
        // 如果没有找到，binarySearch 返回的是 -(插入点) - 1，需转为插入点
        if (index < 0) {
            index = -index - 1;
        }

        return index;
    }

    // 获取某个用户在某个月份的所有事件
    public List<Event> getEvents(String timePeriod, String user) {
        return eventMap.getOrDefault(timePeriod, new HashMap<>()).getOrDefault(user, new ArrayList<>());
    }

    // 获取某个月份所有用户的事件
    public Map<Long, List<Event>> getMonthlyEvents(String timePeriod) {
        return eventMap.getOrDefault(timePeriod, new HashMap<>());
    }

    // 按月份打印所有事件
    public void printAllEvents() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(SHOPPIONG_EVENT_IOFO));
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Map<Long, List<Event>>> periodEntry : eventMap.entrySet()) {
                sb.append("Month: " + periodEntry.getKey() + "\n");
                for (Map.Entry<Long, List<Event>> userEntry : periodEntry.getValue().entrySet()) {
                    sb.append("  User: " + userEntry.getKey() + "\n");
                    for (Event event : userEntry.getValue()) {
                        sb.append("    Event: " + event + "\n");
                    }
                }

            }
            writer.write(sb.toString());
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}