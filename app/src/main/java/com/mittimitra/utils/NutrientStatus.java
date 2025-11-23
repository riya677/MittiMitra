package com.mittimitra.utils;

import android.graphics.Color;
public class NutrientStatus {
    public enum Level { LOW, OPTIMAL, HIGH }
    public static class Status {
        public Level level;
        public int color;
        public String label;
        public Status(Level level, int color, String label) {
            this.level = level;
            this.color = color;
            this.label = label;
        }
    }
    public static Status getNitrogenStatus(double val) {
        if (val < 280) return new Status(Level.LOW, Color.parseColor("#FBC02D"), "Low"); // Yellow
        if (val > 560) return new Status(Level.HIGH, Color.parseColor("#D32F2F"), "High"); // Red
        return new Status(Level.OPTIMAL, Color.parseColor("#1976D2"), "Ideal"); // Blue
    }

    public static Status getPhosphorusStatus(double val) {
        if (val < 10) return new Status(Level.LOW, Color.parseColor("#FBC02D"), "Low"); // Yellow
        if (val > 25) return new Status(Level.HIGH, Color.parseColor("#D32F2F"), "High"); // Red
        return new Status(Level.OPTIMAL, Color.parseColor("#1976D2"), "Ideal"); // Blue
    }

    public static Status getPotassiumStatus(double val) {
        if (val < 108) return new Status(Level.LOW, Color.parseColor("#FBC02D"), "Low"); // Yellow
        if (val > 280) return new Status(Level.HIGH, Color.parseColor("#D32F2F"), "High"); // Red
        return new Status(Level.OPTIMAL, Color.parseColor("#1976D2"), "Ideal"); // Blue
    }

    public static Status getPhStatus(double val) {
        if (val < 6.0) return new Status(Level.LOW, Color.parseColor("#FBC02D"), "Acidic"); // Yellow
        if (val > 7.5) return new Status(Level.HIGH, Color.parseColor("#D32F2F"), "Alkaline"); // Red
        return new Status(Level.OPTIMAL, Color.parseColor("#1976D2"), "Neutral"); // Blue
    }
}