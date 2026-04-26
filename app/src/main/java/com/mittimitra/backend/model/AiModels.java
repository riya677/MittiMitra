package com.mittimitra.backend.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AiModels {

    private AiModels() {
    }

    public static class SoilAdvisoryRequest {
        public int nitrogen;
        public int phosphorus;
        public int potassium;
        public double ph;
        public String userNotes;
        public String weather;
        public String moisture;
        public String detectedSoil;
        public String languageCode;
        public String location;
    }

    public static class SoilAdvisoryData {
        public String advisoryMarkdown;
        public String contextSummary;
        public Integer confidence;
        public String uncertaintyMessage;
        public Map<String, Object> metadata;
    }

    public static class PlantDiagnosisRequest {
        public String imageBase64DataUrl;
        public String languageCode;
        public String location;
    }

    public static class PlantDiagnosisData {
        public String cropIdentified;
        public String healthStatus;
        public Integer confidence;
        public List<String> issuesDetected = new ArrayList<>();
        public List<String> recommendations = new ArrayList<>();
        public String uncertaintyMessage;
        public String rawJson;
    }

    public static class CropScheduleRequest {
        public String cropName;
        public String location;
        public String currentMonth;
        public String languageCode;
    }

    public static class CropScheduleData {
        public String crop;
        public String bestPlantingMonth;
        public String bestHarvestMonth;
        public Integer durationDays;
        public String fertilizerSchedule;
        public String irrigationTips;
        public String pestWatch;
        public Integer confidence;
        public String uncertaintyMessage;
        public List<ScheduleItem> schedule = new ArrayList<>();
    }

    public static class ScheduleItem {
        public Integer week;
        public String activity;
        public String tips;
        public String dueDate;
    }

    public static class ChatRequest {
        public String prompt;
        public String userQuery;
        public String languageCode;
        public String location;
        public String soilContext;
    }

    public static class ChatResponseData {
        public String replyMarkdown;
        public Integer confidence;
        public String uncertaintyMessage;
    }
}
