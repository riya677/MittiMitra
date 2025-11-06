# Input/Output Schema for Project Functionalities

This document describes the input and output data for each functionality in the project.

---

## 1. Soil Analysis
- **Functionality:** AI model-based NPK/pH detection
- **Input:**
    - `soil_image`: Image of the soil sample (JPEG/PNG)
- **Output:**
    - `nitrogen_level`: Integer or float
    - `phosphorus_level`: Integer or float
    - `potassium_level`: Integer or float
    - `ph_value`: Float
- **Description:** The AI model analyzes the soil image and predicts nutrient levels and pH value.

---

## 2. Weather
- **Functionality:** Show real-time weather & location
- **Input:**
    - `location`: GPS coordinates (latitude, longitude)
- **Output:**
    - `temperature`: Float (°C)
    - `humidity`: Float (%)
    - `weather_condition`: String (e.g., Sunny, Rainy)
- **Description:** Retrieves current weather information for the farmer's location.

---

## 3. Recommendations
- **Functionality:** INM & fertilizer guidance
- **Input:**
    - `soil_analysis_results`: JSON containing NPK levels and pH
- **Output:**
    - `recommended_fertilizers`: List of fertilizers with quantity
    - `guidance_notes`: Text recommendations for soil management
- **Description:** Suggests proper fertilizers based on soil analysis.

---

## 4. Camera
- **Functionality:** Capture soil sample image
- **Input:**
    - `capture_action`: User triggers camera
- **Output:**
    - `soil_image`: Captured image file (JPEG/PNG)
- **Description:** Allows the user to take a picture of soil for analysis.

---

## 5. Profile
- **Functionality:** Farmer info, preferences
- **Input:**
    - `farmer_name`: String
    - `farmer_id`: String/Integer
    - `preferences`: JSON object (language, units, etc.)
- **Output:**
    - `profile_data`: Stored profile with input fields
- **Description:** Maintains farmer profile and app preferences.

---

## 6. History
- **Functionality:** Store & view previous analyses
- **Input:**
    - `farmer_id`: String/Integer
- **Output:**
    - `analysis_history`: List of past soil analysis results (JSON)
- **Description:** Keeps track of all previous soil tests for the farmer.

---

## 7. Notifications
- **Functionality:** Alerts, reminders
- **Input:**
    - `event_type`: String (e.g., fertilizer reminder)
    - `farmer_id`: String/Integer
- **Output:**
    - `notification_message`: String
- **Description:** Sends alerts or reminders to farmers.

---

## 8. Offline/Sync
- **Functionality:** Data backup and sync
- **Input:**
    - `local_data`: JSON of offline data collected
- **Output:**
    - `sync_status`: Boolean (true if sync successful)
- **Description:** Syncs offline data to server when connection is available.

---

## 9. Multilingual
- **Functionality:** Regional language support
- **Input:**
    - `selected_language`: String (e.g., Malayalam, Hindi)
- **Output:**
    - `app_text`: App interface translated to selected language
- **Description:** Displays app content in the chosen regional language.

---

## 10. Analytics
- **Functionality:** Track app usage
- **Input:**
    - `user_action_logs`: JSON of user interactions
- **Output:**
    - `usage_report`: Aggregated stats (e.g., most used features)
- **Description:** Tracks app usage and generates analytical reports.
