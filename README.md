# NichiHab 🌙⚖️

<!--toc:start-->

- [NichiHab 🌙⚖️](#nichihab-🌙️)
  - [🚀 Key Features](#🚀-key-features)
  - [🛠️ Tech Stack](#🛠️-tech-stack)
  - [📦 Getting Started](#📦-getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
  - [🛡️ Security Note](#🛡️-security-note)
  - [📄 License](#📄-license)

<!--toc:end-->

**NichiHab** is a high-strictness habit-building and focus application for Android. It uses AI verification and system-level blocking to ensure you stay committed to your goals before accessing distracting applications.

## 🚀 Key Features

- **Smart App Blocking:** Uses Android's Accessibility Service to detect and block distracting apps during your focus sessions.
- **AI Task Verification:** Powered by **Google Gemini AI**. Complete a real-world task (like making your bed or reading) and prove it with a photo. The AI verifies the photo in real-time.
- **Hard Mode:** Prevents access to phone settings and app uninstallation during active sessions to eliminate the temptation to bypass blocks.
- **Earned Unlock Time:** Complete tasks to "earn" minutes of unlocked screen time.
- **Privacy First:** Photos are processed ephemerally. They are encrypted, sent for analysis, and never saved to your gallery or stored on our servers.

## 🛠️ Tech Stack

- **Language:** Kotlin (100%) & C++ (Security Layer)
- **UI:** Jetpack Compose with Material Design 3
- **Architecture:** MVVM with Dagger Hilt (Dependency Injection)
- **Asynchrony:** Kotlin Coroutines & Flow
- **Database:** Room (Session and Task persistence)
- **Storage:** Jetpack DataStore (Settings)
- **Cloud:** None (all local)
- **AI:** Google Gemini 2.0 Flash
- **Camera:** CameraX API

## 📦 Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/your-username/NichiHab.git
   ```

2. Open the project in Android Studio.
3. Sync Project with Gradle Files.
4. Configure your Gemini API Key in `app/src/main/cpp/security-lib.cpp`.
5. (Optional) Configure your Gemini API Key in `app/src/main/cpp/security-lib.cpp`.

## 🛡️ Security Note

The application includes a native C++ layer (`security-lib.cpp`) to handle sensitive keys and integrity checks, providing a robust defense against common APK modification tools.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
