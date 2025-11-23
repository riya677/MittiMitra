package com.mittimitra.utils;

import java.util.HashMap;
import java.util.Map;

public class SoilNutrientMapper {

    public static class NutrientRange {
        public double avgP; // Phosphorus (kg/ha)
        public double avgK; // Potassium (kg/ha)

        public NutrientRange(double p, double k) {
            this.avgP = p;
            this.avgK = k;
        }
    }

    private static final Map<String, NutrientRange> SOIL_MAP = new HashMap<>();

    static {
        // Indian Soil Averages (Approximate Scientific Baselines)
        // Sources: ICAR & Indian Agronomy Standards
        SOIL_MAP.put("Alluvial", new NutrientRange(18.0, 300.0)); // Rich in K, Low in P
        SOIL_MAP.put("Black", new NutrientRange(15.0, 350.0));    // Rich in K, Low in P
        SOIL_MAP.put("Red", new NutrientRange(12.0, 200.0));      // Deficient in K & P
        SOIL_MAP.put("Laterite", new NutrientRange(10.0, 100.0)); // Leached, very low nutrients
        SOIL_MAP.put("Sandy", new NutrientRange(25.0, 250.0));    // Often higher P, variable K
        SOIL_MAP.put("Clay", new NutrientRange(20.0, 280.0));     // Good retention
        SOIL_MAP.put("Loam", new NutrientRange(25.0, 250.0));     // Balanced, fertile
        SOIL_MAP.put("Yellow", new NutrientRange(15.0, 180.0));   // Similar to Red
        SOIL_MAP.put("Peaty", new NutrientRange(10.0, 80.0));     // High Organic, Low minerals
        SOIL_MAP.put("Chalky", new NutrientRange(12.0, 120.0));   // Lime rich, poor nutrients
    }

    public static NutrientRange getEstimates(String detectedSoilType) {
        // Default to "Loam" values if unknown
        if (detectedSoilType == null) return SOIL_MAP.get("Loam");

        // Handle fuzzy matches (e.g., "Black Soil" -> "Black")
        for (String key : SOIL_MAP.keySet()) {
            if (detectedSoilType.contains(key)) {
                return SOIL_MAP.get(key);
            }
        }
        return SOIL_MAP.get("Loam");
    }
}