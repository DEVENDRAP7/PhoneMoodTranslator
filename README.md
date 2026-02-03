🧠 Phone Mood Translator
Turn your digital habits into a living, breathing personality.
Phone Mood Translator is a digital wellbeing application that goes beyond simple screen time charts. It analyzes your daily usage patterns—how long you scroll, how often you unlock, and when you use your phone—and translates that data into a relatable "Mood" and an animated "Mood Pet".
If you use your phone too much, your pet gets sick. If you focus on work, it wears glasses. If you stay up late, it becomes a zombie. It's a fun, non-judgmental way to become aware of your digital life.

______________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

✨ Key Features
1. 🐕 Dynamic Mood Pet
  Your digital companion changes its appearance based on your real-time stats:  
  The Zen Master (Green): Low usage, healthy balance.
  The Overdose (Red): Heavy usage (> 7 hours). The pet looks exhausted.
  The Professor (Blue + Glasses): "Hyperfocused" mode (Long usage, low unlocks).
  The Zombie (Purple): "Late Night" mode (Active between 11 PM - 4 AM).
  The Anxious (Yellow): High frequency unlocks/switching.

___________________________________________________________________________________________________________________________________________________________________________________________________________________

2. 📊 Smart Mood Analysis
  Instead of boring graphs, get a diagnosis of your day:
  🤯 Overdose: > 7 hours (System overload).
  🔥 Hyperfocused: Deep work sessions.
  🧠 Restless Energy: High switching, low focus.
  🧘 Calm & Grounded: The perfect balance.
  ...and 7 more unique moods!
   
____________________________________________________________________________________________________________________________________________________________________________________________________________________

4. 🎨 Adaptive UI & Theming
  Glassmorphism Design: Beautiful glass-like cards and overlays.
  Smart Contrast: Text and icons automatically adapt to your chosen background color (Dark/Light aware).
  Color Picker: Customize the entire app theme to match your style.

____________________________________________________________________________________________________________________________________________________________________________________________________________________

6. 🔒 Privacy-First Architecture
  100% Offline: All analysis happens on your device.
  No Internet Permission: Your usage data never leaves your phone.
  Safe Permissions: Uses standard Android Usage Stats API with strict privacy controls.

____________________________________________________________________________________________________________________________________________________________________________________________________________________

🛠️ Tech Stack
  Language: Java
  Architecture: MVVM / Clean Architecture Principles
  UI: Android XML (ConstraintLayout, RelativeLayout, RecyclerView, ViewPager2)
  Custom Views: MoodPetView.java (Canvas drawing for the animated pet)
  Storage: SharedPreferences (Local history)
  APIs: UsageStatsManager, PackageManager

____________________________________________________________________________________________________________________________________________________________________________________________________________________

🚀 Getting Started
  Prerequisites
  Android Studio Iguana or later
  JDK 17
  Android Device/Emulator (API 26+)
  Installation
  Clone the repository:
  Bash - git clone https://github.com/DEVENDRAP7/PhoneMoodTranslator.git

  Open in Android Studio:
    File > Open > Select the cloned folder.
    Build the Project:
    Wait for Gradle sync to complete.
    Click the Run (▶️) button.

____________________________________________________________________________________________________________________________________________________________________________________________________________________

Permissions:
  On first launch, grant the Usage Access permission when prompted. This is required to read your screen time stats.
  🛡️ Permissions Explained
  The app requires PACKAGE_USAGE_STATS to function.
  Why? To calculate total screen time and unlock counts.
  Safety: We use the <queries> filter to only recognize apps with a launcher icon (Games, Social, Tools), ensuring system processes remain private. No data is sent to any server.

____________________________________________________________________________________________________________________________________________________________________________________________________________________

🤝 Contributing
  Contributions are welcome!
  Fork the project.
  Create your feature branch (git checkout -b feature/AmazingFeature).
  Commit your changes (git commit -m 'Add some AmazingFeature').
  Push to the branch (git push origin feature/AmazingFeature).
  Open a Pull Request.

____________________________________________________________________________________________________________________________________________________________________________________________________________________

👨‍💻 Author
  Devendra Prajapati

____________________________________________________________________________________________________________________________________________________________________________________________________________________

Your digital habits define your digital mood. Translate yours today.
