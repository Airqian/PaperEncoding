package indextree.util;

public class DataSetInfo {
    // 数据集中的事件总数
    private int eventsNum;

    // 数据集中的主体数
    private int subjectsNum;

    // 数据集中的所有事件，先按时间段再按属性主体进行组织
    private EventOrganizerSortByTime organizer;

    // 数据集中事件发生的最大时间
    private long globalMinTime;

    // 数据集中事件发生的最小时间
    private long globalMaxTime;

    private int maxPropertyNum;

    private int monthDiff;

    public DataSetInfo(){}

    public int getEventsNum() {
        return eventsNum;
    }

    public void setEventsNum(int eventsNum) {
        this.eventsNum = eventsNum;
    }

    public long getGlobalMinTime() {
        return globalMinTime;
    }

    public void setGlobalMinTime(long globalMinTime) {
        this.globalMinTime = globalMinTime;
    }

    public long getGlobalMaxTime() {
        return globalMaxTime;
    }

    public void setGlobalMaxTime(long globalMaxTime) {
        this.globalMaxTime = globalMaxTime;
    }

    public EventOrganizerSortByTime getOrganizer() {
        return organizer;
    }

    public void setOrganizer(EventOrganizerSortByTime organizer) {
        this.organizer = organizer;
    }

    public int getMonthDiff() {
        return monthDiff;
    }

    public void setMonthDiff(int monthDiff) {
        this.monthDiff = monthDiff;
    }

    public int getSubjectsNum() {
        return subjectsNum;
    }

    public void setSubjectsNum(int subjectsNum) {
        this.subjectsNum = subjectsNum;
    }

    public int getMaxPropertyNum() {
        return maxPropertyNum;
    }

    public void setMaxPropertyNum(int maxPropertyNum) {
        this.maxPropertyNum = maxPropertyNum;
    }
}
