package com.devendrap7.phonemoodtranslator.models;

import java.util.Random;

public class MoodResult {
    public String emoji;
    public String title;
    public String[] descriptions;

    public MoodResult(String emoji, String title, String[] descriptions) {
        this.emoji = emoji;
        this.title = title;
        this.descriptions = descriptions;
    }

    public String getRandomDescription() {
        if (descriptions == null || descriptions.length == 0) return "";
        return descriptions[new Random().nextInt(descriptions.length)];
    }
}