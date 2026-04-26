package com.mittimitra.domain.repository;

import androidx.annotation.NonNull;

import com.mittimitra.backend.BackendCallback;
import com.mittimitra.backend.model.AiModels;

public interface PredictionRepository {
    void fetchSoilAdvisory(@NonNull AiModels.SoilAdvisoryRequest request,
                           @NonNull BackendCallback<AiModels.SoilAdvisoryData> callback);

    void fetchPlantDiagnosis(@NonNull AiModels.PlantDiagnosisRequest request,
                             @NonNull BackendCallback<AiModels.PlantDiagnosisData> callback);

    void fetchCropSchedule(@NonNull AiModels.CropScheduleRequest request,
                           @NonNull BackendCallback<AiModels.CropScheduleData> callback);

    void fetchChatResponse(@NonNull AiModels.ChatRequest request,
                           @NonNull BackendCallback<AiModels.ChatResponseData> callback);
}
