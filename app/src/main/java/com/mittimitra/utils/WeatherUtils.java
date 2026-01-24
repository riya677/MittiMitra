package com.mittimitra.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherUtils {

    private static final Map<Integer, String[]> WEATHER_CODES = new HashMap<>();

    static {
        WEATHER_CODES.put(0, new String[]{"☀️", "Clear sky"});
        WEATHER_CODES.put(1, new String[]{"🌤️", "Mainly clear"});
        WEATHER_CODES.put(2, new String[]{"⛅", "Partly cloudy"});
        WEATHER_CODES.put(3, new String[]{"☁️", "Overcast"});
        WEATHER_CODES.put(45, new String[]{"🌫️", "Foggy"});
        WEATHER_CODES.put(48, new String[]{"🌫️", "Depositing rime fog"});
        WEATHER_CODES.put(51, new String[]{"🌦️", "Light drizzle"});
        WEATHER_CODES.put(53, new String[]{"🌦️", "Moderate drizzle"});
        WEATHER_CODES.put(55, new String[]{"🌧️", "Dense drizzle"});
        WEATHER_CODES.put(61, new String[]{"🌧️", "Slight rain"});
        WEATHER_CODES.put(63, new String[]{"🌧️", "Moderate rain"});
        WEATHER_CODES.put(65, new String[]{"⛈️", "Heavy rain"});
        WEATHER_CODES.put(71, new String[]{"🌨️", "Slight snow"});
        WEATHER_CODES.put(73, new String[]{"🌨️", "Moderate snow"});
        WEATHER_CODES.put(75, new String[]{"❄️", "Heavy snow"});
        WEATHER_CODES.put(77, new String[]{"🌨️", "Snow grains"});
        WEATHER_CODES.put(80, new String[]{"🌦️", "Slight rain showers"});
        WEATHER_CODES.put(81, new String[]{"⛈️", "Moderate rain showers"});
        WEATHER_CODES.put(82, new String[]{"⛈️", "Violent rain showers"});
        WEATHER_CODES.put(85, new String[]{"🌨️", "Slight snow showers"});
        WEATHER_CODES.put(86, new String[]{"❄️", "Heavy snow showers"});
        WEATHER_CODES.put(95, new String[]{"⛈️", "Thunderstorm"});
        WEATHER_CODES.put(96, new String[]{"⛈️", "Thunderstorm with slight hail"});
        WEATHER_CODES.put(99, new String[]{"⛈️", "Thunderstorm with heavy hail"});
    }

    public static String[] getWeatherDescription(int code) {
        return WEATHER_CODES.getOrDefault(code, new String[]{"🌈", "Unknown"});
    }

    public static List<String> getAgriculturalRecommendations(double temp, double humidity, double windSpeed, double precipitation) {
        List<String> recommendations = new ArrayList<>();

        // Temperature
        if (temp < 5) {
            recommendations.add("❄️ Frost Risk: Protect sensitive crops from cold damage.");
        } else if (temp > 35) {
            recommendations.add("🌡️ Heat Warning: Increase irrigation and provide shade.");
        } else if (temp > 30) {
            recommendations.add("☀️ High Temperature: Monitor water needs closely.");
        }

        // Precipitation
        if (precipitation > 5) {
            recommendations.add("🌧️ Heavy Rain: Delay irrigation and check drainage.");
        } else if (precipitation > 0) {
            recommendations.add("🌦️ Light Rain: Adjust irrigation schedule.");
        }

        // Wind
        if (windSpeed > 50) {
            recommendations.add("💨 Strong Winds: Secure equipment and check supports.");
        } else if (windSpeed > 30) {
            recommendations.add("🍃 Moderate Winds: Monitor young plants.");
        }

        // Humidity
        if (humidity > 85) {
            recommendations.add("💧 High Humidity: Watch for fungal diseases.");
        } else if (humidity < 30) {
            recommendations.add("🏜️ Low Humidity: Increase watering frequency.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("✅ Favorable Conditions: Good for agricultural activities.");
        }

        return recommendations;
    }

    /**
     * Calculate pest/disease risk based on weather conditions.
     * High humidity + warm temperature = higher pest risk.
     */
    public static String getPestRiskLevel(double temp, double humidity) {
        // Conditions favorable for fungal diseases and pests
        if (humidity > 80 && temp > 20 && temp < 35) {
            return "High - Fungal diseases likely! Apply preventive fungicide.";
        } else if (humidity > 70 && temp > 25) {
            return "Medium - Monitor for aphids and whiteflies.";
        } else if (humidity > 85) {
            return "Medium - High humidity may cause mold. Improve ventilation.";
        } else if (temp < 10) {
            return "Low - Cold weather suppresses pest activity.";
        }
        return "Low";
    }

    /**
     * Get irrigation recommendation based on weather.
     */
    public static String getIrrigationAdvice(double temp, double humidity, double precipitation, double soilMoisture) {
        if (precipitation > 10) {
            return "🚫 Skip irrigation today - sufficient rainfall received.";
        } else if (soilMoisture > 0.3) {
            return "💧 Soil moisture adequate. Light irrigation if needed.";
        } else if (temp > 35 && humidity < 40) {
            return "🚨 Critical! Irrigate immediately - high evaporation risk.";
        } else if (temp > 30) {
            return "💦 Irrigate in early morning or evening to reduce evaporation.";
        }
        return "💧 Normal irrigation schedule recommended.";
    }
}
