package indextree.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// 内部的事件类
public  class Event {
    private long user;
    private long time;
    private String eventDetail;

    public Event(long user, long time, String eventDetail) {
        this.user = user;
        this.time = time;
        this.eventDetail = eventDetail;
    }

    public long getTime() {
        return time;
    }

    public String getEventDetail() {
        return eventDetail;
    }

    @Override
    public String toString() {
        // 使用 DateTimeFormatter 将毫秒数转为日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        String formattedDate = dateTime.format(formatter);
        return formattedDate + " - " + eventDetail;
    }


}
