
# SkyCast - Modern Weather Application

![SkyCast Banner](https://via.placeholder.com/800x400?text=SkyCast+App+Banner) <!-- මෙතනට ඔයාගේ App එකේ Screenshot එකක් හෝ Banner එකක් දාන්න -->

SkyCast is a high-performance, responsive weather application for Android that combines a sleek web-based UI with native Android power. It provides real-time weather data, interactive radar maps, and detailed forecasts using a hybrid architecture (Kotlin + WebView).

### 📺 Project Demo
[![Watch the Video](https://img.shields.io/badge/Watch-Demo_Video-red?style=for-the-badge&logo=youtube)]([VIDEO_URL_HERE])

---

## 🎨 Design & UI/UX
The interface was meticulously designed to provide a modern "Glass-morphism" experience. You can view the original design system and wireframes here:

👉 **[View Figma Design Case Study]([FIGMA_LINK_HERE])**

---

## 🚀 Key Features

*   **Real-time Weather:** Accurate current weather data based on your GPS location.
*   **City Search:** Instantly search for any city or airport worldwide to view its weather.
*   **5-Day Detailed Forecast:** Plan ahead with a comprehensive 5-day weather breakdown.
*   **Interactive Weather Radar:** Dynamic map view with a real-time precipitation (rain) overlay using Leaflet and OpenWeatherMap.
*   **Account Management:** Google Sign-In support with persistent login sessions and profile management.
*   **Adaptive Settings:** Customize temperature units (Celsius/Fahrenheit) and wind speed units (km/h/mph).
*   **Responsive UI:** A beautiful, glass-morphism inspired interface built with Tailwind CSS.

---

## 🛠 Tech Stack

| Core | Technologies Used |
| :--- | :--- |
| **Native Core** | Android (Kotlin), GPS Services, API Management |
| **Frontend UI** | HTML5, JavaScript, **Tailwind CSS** |
| **Architecture** | Hybrid (WebView + Native Bridge) |
| **Maps** | Leaflet.js |
| **API** | OpenWeatherMap API |

---

## 📋 Prerequisites

Before running the app, you must set up your OpenWeatherMap API Key:

1.  Go to [OpenWeatherMap](https://openweathermap.org/) and create a free account.
2.  Generate an **API Key** from your dashboard.
3.  Open `app/src/main/java/com/example/skycast/MainActivity.kt`.
4.  Locate the line:
    ```kotlin
    private val WEATHER_API_KEY = "YOUR_API_KEY"
    ```
5.  Replace `"YOUR_API_KEY"` with your actual key.

---

## ⚙️ How to Run

1.  **Clone the Repository:** Open the project in **Android Studio**.
2.  **Sync Gradle:** Click the "Elephant" icon (Sync Project with Gradle Files).
3.  **Permissions:** Ensure your device/emulator has internet access and location services enabled.
4.  **Build:** 
    *   Go to `Build` -> `Clean Project`
    *   Then `Build` -> `Rebuild Project`
5.  **Launch:** Click the **Run** button (green play icon) and select your target device.

---

## 📂 Project Structure

*   `app/src/main/java/.../MainActivity.kt`: The bridge between the web UI and Android native features.
*   `app/src/main/assets/`: Contains all the HTML, CSS, and JS files for the app's interface.
    *   `index.html`: The entry onboarding screen.
    *   `Weather Dashboard.html`: Main real-time weather overview.
    *   `Weather Radar.html`: Dynamic map with rain radar.
    *   `Detailed Forecast.html`: 5-day breakdown.
    *   `Settings.html`: App configuration.
*   `app/src/main/AndroidManifest.xml`: Android manifest with required permissions (Internet, Location, Accounts).

---

## ⚖️ License

This project is for educational and portfolio purposes. Weather data is provided by OpenWeatherMap.

---
