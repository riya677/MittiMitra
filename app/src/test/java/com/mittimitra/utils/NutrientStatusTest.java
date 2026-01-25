package com.mittimitra.utils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for NutrientStatus utility class.
 * Tests the nutrient level classification logic for NPK and pH values.
 */
public class NutrientStatusTest {

    // ========== NITROGEN TESTS ==========
    
    @Test
    public void nitrogen_lowValue_returnsLowStatus() {
        NutrientStatus.Status status = NutrientStatus.getNitrogenStatus(100);
        assertEquals(NutrientStatus.Level.LOW, status.level);
        assertEquals("Low", status.label);
    }

    @Test
    public void nitrogen_optimalValue_returnsOptimalStatus() {
        NutrientStatus.Status status = NutrientStatus.getNitrogenStatus(400);
        assertEquals(NutrientStatus.Level.OPTIMAL, status.level);
        assertEquals("Ideal", status.label);
    }

    @Test
    public void nitrogen_highValue_returnsHighStatus() {
        NutrientStatus.Status status = NutrientStatus.getNitrogenStatus(600);
        assertEquals(NutrientStatus.Level.HIGH, status.level);
        assertEquals("High", status.label);
    }

    @Test
    public void nitrogen_boundaryLow_returnsOptimalStatus() {
        NutrientStatus.Status status = NutrientStatus.getNitrogenStatus(280);
        assertEquals(NutrientStatus.Level.OPTIMAL, status.level);
    }

    @Test
    public void nitrogen_boundaryHigh_returnsOptimalStatus() {
        NutrientStatus.Status status = NutrientStatus.getNitrogenStatus(560);
        assertEquals(NutrientStatus.Level.OPTIMAL, status.level);
    }

    // ========== PHOSPHORUS TESTS ==========
    
    @Test
    public void phosphorus_lowValue_returnsLowStatus() {
        NutrientStatus.Status status = NutrientStatus.getPhosphorusStatus(5);
        assertEquals(NutrientStatus.Level.LOW, status.level);
    }

    @Test
    public void phosphorus_optimalValue_returnsOptimalStatus() {
        NutrientStatus.Status status = NutrientStatus.getPhosphorusStatus(15);
        assertEquals(NutrientStatus.Level.OPTIMAL, status.level);
    }

    @Test
    public void phosphorus_highValue_returnsHighStatus() {
        NutrientStatus.Status status = NutrientStatus.getPhosphorusStatus(30);
        assertEquals(NutrientStatus.Level.HIGH, status.level);
    }

    // ========== POTASSIUM TESTS ==========
    
    @Test
    public void potassium_lowValue_returnsLowStatus() {
        NutrientStatus.Status status = NutrientStatus.getPotassiumStatus(50);
        assertEquals(NutrientStatus.Level.LOW, status.level);
    }

    @Test
    public void potassium_optimalValue_returnsOptimalStatus() {
        NutrientStatus.Status status = NutrientStatus.getPotassiumStatus(200);
        assertEquals(NutrientStatus.Level.OPTIMAL, status.level);
    }

    @Test
    public void potassium_highValue_returnsHighStatus() {
        NutrientStatus.Status status = NutrientStatus.getPotassiumStatus(300);
        assertEquals(NutrientStatus.Level.HIGH, status.level);
    }

    // ========== pH TESTS ==========
    
    @Test
    public void ph_acidicValue_returnsLowStatus() {
        NutrientStatus.Status status = NutrientStatus.getPhStatus(5.5);
        assertEquals(NutrientStatus.Level.LOW, status.level);
        assertEquals("Acidic", status.label);
    }

    @Test
    public void ph_neutralValue_returnsOptimalStatus() {
        NutrientStatus.Status status = NutrientStatus.getPhStatus(6.8);
        assertEquals(NutrientStatus.Level.OPTIMAL, status.level);
        assertEquals("Neutral", status.label);
    }

    @Test
    public void ph_alkalineValue_returnsHighStatus() {
        NutrientStatus.Status status = NutrientStatus.getPhStatus(8.0);
        assertEquals(NutrientStatus.Level.HIGH, status.level);
        assertEquals("Alkaline", status.label);
    }

    @Test
    public void ph_boundaryLow_returnsOptimalStatus() {
        NutrientStatus.Status status = NutrientStatus.getPhStatus(6.0);
        assertEquals(NutrientStatus.Level.OPTIMAL, status.level);
    }

    @Test
    public void ph_boundaryHigh_returnsOptimalStatus() {
        NutrientStatus.Status status = NutrientStatus.getPhStatus(7.5);
        assertEquals(NutrientStatus.Level.OPTIMAL, status.level);
    }
}
