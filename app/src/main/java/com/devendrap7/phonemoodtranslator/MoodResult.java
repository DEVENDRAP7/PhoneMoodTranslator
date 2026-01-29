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

    /**
     * Returns a random description from the available descriptions
     */
    public String getRandomDescription() {
        if (descriptions == null || descriptions.length == 0) {
            return "Reflecting on your digital patterns.";
        }

        int index = new Random().nextInt(descriptions.length);
        return descriptions[index];
    }

    /**
     * Returns all descriptions
     */
    public String[] getAllDescriptions() {
        return descriptions;
    }

    /**
     * Returns a formatted string representation
     */
    @Override
    public String toString() {
        return emoji + " " + title;
    }
}