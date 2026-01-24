package com.mittimitra.models;

import com.google.gson.annotations.SerializedName;

public class Scheme {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("benefits")
    private String benefits;

    @SerializedName("description")
    private String description;

    @SerializedName("eligibility_text")
    private String eligibilityText;

    @SerializedName("max_land_hectares")
    private double maxLandHectares;

    @SerializedName("url")
    private String url;

    @SerializedName("state")
    private String state; // "Central", "Kerala", "Maharashtra", etc.

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getBenefits() { return benefits; }
    public String getDescription() { return description; }
    public String getEligibilityText() { return eligibilityText; }
    public double getMaxLandHectares() { return maxLandHectares; }
    public String getUrl() { return url; }
    public String getState() { return state; }
}
