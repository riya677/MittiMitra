package com.mittimitra.utils;

import com.mittimitra.R;

public class NutrientStatus {
    public enum Level { LOW, OPTIMAL, HIGH }

    private static final int COLOR_LOW = 0xFFFBC02D;
    private static final int COLOR_HIGH = 0xFFD32F2F;
    private static final int COLOR_OPTIMAL = 0xFF1976D2;

    public static class Status {
        public Level level;
        public int color;
        public String label;
        public int labelResId;
        public Status(Level level, int color, String label, int labelResId) {
            this.level = level;
            this.color = color;
            this.label = label;
            this.labelResId = labelResId;
        }
    }
    public static Status getNitrogenStatus(double val) {
        if (val < 280) return new Status(Level.LOW, COLOR_LOW, "Low", R.string.soil_status_low); // Yellow
        if (val > 560) return new Status(Level.HIGH, COLOR_HIGH, "High", R.string.soil_status_high); // Red
        return new Status(Level.OPTIMAL, COLOR_OPTIMAL, "Ideal", R.string.soil_status_ok); // Blue
    }

    public static Status getPhosphorusStatus(double val) {
        if (val < 10) return new Status(Level.LOW, COLOR_LOW, "Low", R.string.soil_status_low); // Yellow
        if (val > 25) return new Status(Level.HIGH, COLOR_HIGH, "High", R.string.soil_status_high); // Red
        return new Status(Level.OPTIMAL, COLOR_OPTIMAL, "Ideal", R.string.soil_status_ok); // Blue
    }

    public static Status getPotassiumStatus(double val) {
        if (val < 108) return new Status(Level.LOW, COLOR_LOW, "Low", R.string.soil_status_low); // Yellow
        if (val > 280) return new Status(Level.HIGH, COLOR_HIGH, "High", R.string.soil_status_high); // Red
        return new Status(Level.OPTIMAL, COLOR_OPTIMAL, "Ideal", R.string.soil_status_ok); // Blue
    }

    public static Status getPhStatus(double val) {
        if (val < 6.0) return new Status(Level.LOW, COLOR_LOW, "Acidic", R.string.soil_ph_acidic); // Yellow
        if (val > 7.5) return new Status(Level.HIGH, COLOR_HIGH, "Alkaline", R.string.soil_ph_alkaline); // Red
        return new Status(Level.OPTIMAL, COLOR_OPTIMAL, "Neutral", R.string.soil_ph_neutral); // Blue
    }
}
