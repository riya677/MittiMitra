package com.mittimitra.tasks;

import com.mittimitra.database.entity.FarmTask;

import java.util.ArrayList;
import java.util.List;

public final class FarmTaskTemplates {

    private FarmTaskTemplates() {
    }

    public static List<FarmTask> forCrop(String userId, String cropName, long startAt) {
        List<FarmTask> tasks = new ArrayList<>();
        long day = 24L * 60L * 60L * 1000L;

        tasks.add(build(userId, cropName, "Soil Preparation", "Prepare field and add compost before sowing.",
                "pre-sowing", "template", startAt + day));
        tasks.add(build(userId, cropName, "Seed Treatment", "Treat seeds with recommended fungicide or bio-agent.",
                "sowing", "template", startAt + (2 * day)));
        tasks.add(build(userId, cropName, "Irrigation Check", "Check irrigation requirement based on weather and soil moisture.",
                "growth", "template", startAt + (5 * day)));
        tasks.add(build(userId, cropName, "Pest Scouting", "Inspect leaves and stems for early pest symptoms.",
                "growth", "template", startAt + (7 * day)));
        return tasks;
    }

    private static FarmTask build(String userId, String cropName, String title, String description,
                                  String stage, String source, long dueAt) {
        FarmTask task = new FarmTask();
        task.userId = userId;
        task.cropName = cropName;
        task.title = title;
        task.description = description;
        task.stage = stage;
        task.source = source;
        task.dueAt = dueAt;
        task.status = "PENDING";
        task.priority = 2;
        task.createdAt = System.currentTimeMillis();
        return task;
    }
}
