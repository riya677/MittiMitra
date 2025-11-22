package com.mittimitra.utils;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SoilDataManager {

    public static class SoilProfile {
        public double N = 0;
        public double P = 0;
        public double K = 0;
        public double pH = 0;
        public boolean isFound = false;
    }

    public static SoilProfile getDistrictAverage(Context context, String districtName) {
        SoilProfile profile = new SoilProfile();
        if (districtName == null) return profile;

        try {
            // Ensure "Soil data.csv" is in src/main/assets/
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("Soil data.csv"))
            );

            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");

                if (tokens.length >= 5) {
                    String csvDistrict = tokens[0].trim();

                    // Robust Matching
                    if (csvDistrict.equalsIgnoreCase(districtName) ||
                            districtName.toLowerCase().contains(csvDistrict.toLowerCase()) ||
                            csvDistrict.toLowerCase().contains(districtName.toLowerCase())) {

                        try {
                            profile.N = Double.parseDouble(tokens[1]);
                            profile.P = Double.parseDouble(tokens[2]);
                            profile.K = Double.parseDouble(tokens[3]);
                            profile.pH = Double.parseDouble(tokens[4]);
                            profile.isFound = true;
                            break;
                        } catch (NumberFormatException e) {
                            Log.e("SoilData", "Parse error for " + csvDistrict);
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception e) { e.printStackTrace(); }
        return profile;
    }
}