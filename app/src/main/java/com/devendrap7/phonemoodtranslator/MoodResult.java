package com.devendrap7.phonemoodtranslator;

import java.util.Random;

public class MoodResult {

    public String emoji;
    public String title;
    private String[] descriptions;

    public MoodResult(String emoji, String title, String[] descriptions) {
        this.emoji = emoji;
        this.title = title;
        this.descriptions = descriptions;
    }

    public String getRandomDescription() {
        int index = new Random().nextInt(descriptions.length);
        return descriptions[index];
    }
}

