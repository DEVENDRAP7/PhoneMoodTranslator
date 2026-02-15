package com.devendrap7.phonemoodtranslator.database;

public class MoodHistoryItem {
    public String date;
    public String emoji;
    public String title;
    public String usageStr;
    public long usageMillis;
    public String topAppsJson;
    public String selfNote;// <--- NEW: Stores the list of apps for that day

    public MoodHistoryItem(String date, String emoji, String title, String usageStr,long usageMillis, String topAppsJson,String selfNote) {
        this.date = date;
        this.emoji = emoji;
        this.title = title;
        this.usageStr = usageStr;
        this.usageMillis=usageMillis;
        this.topAppsJson = topAppsJson;
        this.selfNote=selfNote;
    }
}