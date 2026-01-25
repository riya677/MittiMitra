package com.mittimitra.utils;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Unit tests for WeatherUtils utility class.
 * Tests weather descriptions, agricultural recommendations, pest risk, and irrigation advice.
 */
public class WeatherUtilsTest {

    // ========== WEATHER DESCRIPTION TESTS ==========
    
    @Test
    public void weatherDescription_clearSky_returnsCorrectDescription() {
        String[] result = WeatherUtils.getWeatherDescription(0);
        assertEquals("☀️", result[0]);
        assertEquals("Clear sky", result[1]);
    }

    @Test
    public void weatherDescription_thunderstorm_returnsCorrectDescription() {
        String[] result = WeatherUtils.getWeatherDescription(95);
        assertEquals("⛈️", result[0]);
        assertEquals("Thunderstorm", result[1]);
    }

    @Test
    public void weatherDescription_unknownCode_returnsDefault() {
        String[] result = WeatherUtils.getWeatherDescription(999);
        assertEquals("🌈", result[0]);
        assertEquals("Unknown", result[1]);
    }

    // ========== AGRICULTURAL RECOMMENDATIONS TESTS ==========
    
    @Test
    public void recommendations_frostConditions_returnsFrostWarning() {
        List<String> recommendations = WeatherUtils.getAgriculturalRecommendations(2, 50, 10, 0);
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("Frost Risk")));
    }

    @Test
    public void recommendations_heatWave_returnsHeatWarning() {
        List<String> recommendations = WeatherUtils.getAgriculturalRecommendations(38, 30, 10, 0);
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("Heat Warning")));
    }

    @Test
    public void recommendations_heavyRain_returnsRainWarning() {
        List<String> recommendations = WeatherUtils.getAgriculturalRecommendations(25, 70, 10, 10);
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("Heavy Rain")));
    }

    @Test
    public void recommendations_strongWinds_returnsWindWarning() {
        List<String> recommendations = WeatherUtils.getAgriculturalRecommendations(25, 50, 60, 0);
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("Strong Winds")));
    }

    @Test
    public void recommendations_highHumidity_returnsHumidityWarning() {
        List<String> recommendations = WeatherUtils.getAgriculturalRecommendations(25, 90, 10, 0);
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("High Humidity")));
    }

    @Test
    public void recommendations_lowHumidity_returnsLowHumidityWarning() {
        List<String> recommendations = WeatherUtils.getAgriculturalRecommendations(25, 25, 10, 0);
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("Low Humidity")));
    }

    @Test
    public void recommendations_favorableConditions_returnsFavorable() {
        List<String> recommendations = WeatherUtils.getAgriculturalRecommendations(25, 50, 15, 0);
        assertTrue(recommendations.stream().anyMatch(r -> r.contains("Favorable Conditions")));
    }

    // ========== PEST RISK TESTS ==========
    
    @Test
    public void pestRisk_highHumidityWarmTemp_returnsHigh() {
        String risk = WeatherUtils.getPestRiskLevel(28, 85);
        assertTrue(risk.contains("High"));
    }

    @Test
    public void pestRisk_mediumConditions_returnsMedium() {
        String risk = WeatherUtils.getPestRiskLevel(30, 75);
        assertTrue(risk.contains("Medium"));
    }

    @Test
    public void pestRisk_coldWeather_returnsLow() {
        String risk = WeatherUtils.getPestRiskLevel(5, 50);
        assertTrue(risk.contains("Low"));
    }

    // ========== IRRIGATION ADVICE TESTS ==========
    
    @Test
    public void irrigation_heavyRainfall_advicesSkipIrrigation() {
        String advice = WeatherUtils.getIrrigationAdvice(25, 60, 15, 0.2);
        assertTrue(advice.contains("Skip irrigation"));
    }

    @Test
    public void irrigation_adequateSoilMoisture_advicesLightIrrigation() {
        String advice = WeatherUtils.getIrrigationAdvice(25, 60, 0, 0.35);
        assertTrue(advice.contains("adequate") || advice.contains("Light"));
    }

    @Test
    public void irrigation_criticalConditions_advicesImmediate() {
        String advice = WeatherUtils.getIrrigationAdvice(38, 35, 0, 0.1);
        assertTrue(advice.contains("Critical") || advice.contains("immediately"));
    }

    @Test
    public void irrigation_normalConditions_returnsNormalAdvice() {
        String advice = WeatherUtils.getIrrigationAdvice(25, 50, 0, 0.2);
        assertTrue(advice.contains("Normal") || advice.contains("recommended"));
    }
}
