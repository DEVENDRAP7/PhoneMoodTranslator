package com.devendrap7.phonemoodtranslator.onboarding;

public class OnboardingPage {
    public final String emoji;
    public final String title;
    public final String description;
    public final String actionButtonText; // null = no button shown
    public final int pageType; // 0=info, 1=usage, 2=notif, 3=battery

    public static final int TYPE_INFO = 0;
    public static final int TYPE_USAGE = 1;
    public static final int TYPE_NOTIF = 2;
    public static final int TYPE_BATTERY = 3;

    public OnboardingPage(String emoji, String title, String description,
            String actionButtonText, int pageType) {
        this.emoji = emoji;
        this.title = title;
        this.description = description;
        this.actionButtonText = actionButtonText;
        this.pageType = pageType;
    }
}
