package com.devendrap7.phonemoodtranslator.utils;

import com.devendrap7.phonemoodtranslator.models.MoodResult;

public class MoodCalculator {
    public static MoodResult calculateMood(long usageMinutes, int appOpenCount, boolean usedAtNight) {
        if (usageMinutes > 420) return new MoodResult("🤯", "Overdose", new String[]{"System overload detected.", "Time to disconnect."});
        if (usageMinutes >= 360) return new MoodResult("🔗", "Tethered", new String[]{"The phone feels glued to your hand.", "Borderline heavy usage."});
        if (usageMinutes < 30) return new MoodResult("🌿", "Unplugged", new String[]{"Real life took priority.", "Digital distance felt natural."});
        if (usedAtNight) return new MoodResult("🌙", "Late-Night Thinker", new String[]{"Sleep was sacrificed for scrolling.", "A midnight mind wandering."});
        if (usageMinutes > 180 && appOpenCount < 15) return new MoodResult("🔥", "Hyperfocused", new String[]{"Deep work defined your day.", "Sustained attention."});
        if (usageMinutes > 180 && appOpenCount > 15) return new MoodResult("🧠", "Restless Energy", new String[]{"Your mind was running sprints.", "Stimulation sought."});
        if (usageMinutes < 120 && appOpenCount > 15) return new MoodResult("😵", "Distracted Mind", new String[]{"Focus was impossible.", "A butterfly flitting."});
        if (usageMinutes < 90) return new MoodResult("😎", "Slick", new String[]{"In and out. Efficient.", "You rule the phone."});
        if (appOpenCount < 10) return new MoodResult("🧐", "Serious Mode", new String[]{"Usage was purposeful.", "Disciplined session."});
        if (appOpenCount > 10 && usageMinutes < 120) return new MoodResult("🎡", "Light-hearted", new String[]{"Just browsing and chatting.", "Casual wandering."});
        return new MoodResult("🧘", "Calm & Grounded", new String[]{"A balanced digital rhythm.", "Stable connection."});
    }

    public static boolean isLateNightUsage() {
        java.util.Calendar now = java.util.Calendar.getInstance(java.util.TimeZone.getDefault());
        int hour = now.get(java.util.Calendar.HOUR_OF_DAY);
        return (hour >= 23 || hour <= 4);
    }
}