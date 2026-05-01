package com.mittimitra.backend;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mittimitra.backend.model.AccountModels;
import com.mittimitra.backend.model.AiModels;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BackendFunctionsClient {

    private static final String TAG = "BackendFunctionsClient";
    private static final String REGION = "asia-south1";
    private static final Type STRING_LIST_TYPE =
            TypeToken.getParameterized(List.class, String.class).getType();

    private final FirebaseFunctions functions;
    private final Gson gson;

    public BackendFunctionsClient() {
        functions = FirebaseFunctions.getInstance(REGION);
        gson = new GsonBuilder()
                .registerTypeAdapter(
                        STRING_LIST_TYPE,
                        new StringOrListTypeAdapter())
                .create();
    }

    // Accepts both JSON string and JSON array for List<String> fields.
    // Backend AI models sometimes return a single string instead of a one-element array.
    private static class StringOrListTypeAdapter extends TypeAdapter<List<String>> {
        @Override
        public void write(JsonWriter out, List<String> value) throws IOException {
            out.beginArray();
            if (value != null) {
                for (String s : value) out.value(s);
            }
            out.endArray();
        }

        @Override
        public List<String> read(JsonReader in) throws IOException {
            List<String> result = new ArrayList<>();
            if (in.peek() == JsonToken.BEGIN_ARRAY) {
                in.beginArray();
                while (in.hasNext()) {
                    if (in.peek() == JsonToken.STRING) {
                        String s = in.nextString().trim();
                        if (!s.isEmpty()) result.add(s);
                    } else {
                        in.skipValue();
                    }
                }
                in.endArray();
            } else if (in.peek() == JsonToken.STRING) {
                String s = in.nextString().trim();
                if (!s.isEmpty()) result.add(s);
            } else {
                in.skipValue();
            }
            return result;
        }
    }

    public void getSoilAdvisory(@NonNull AiModels.SoilAdvisoryRequest request,
                                @NonNull BackendCallback<AiModels.SoilAdvisoryData> callback) {
        callEndpoint("getSoilAdvisory", request,
                envelopeType(AiModels.SoilAdvisoryData.class), callback);
    }

    public void getPlantDiagnosis(@NonNull AiModels.PlantDiagnosisRequest request,
                                  @NonNull BackendCallback<AiModels.PlantDiagnosisData> callback) {
        callEndpoint("getPlantDiagnosis", request,
                envelopeType(AiModels.PlantDiagnosisData.class), callback);
    }

    public void getCropSchedule(@NonNull AiModels.CropScheduleRequest request,
                                @NonNull BackendCallback<AiModels.CropScheduleData> callback) {
        callEndpoint("getCropSchedule", request,
                envelopeType(AiModels.CropScheduleData.class), callback);
    }

    public void checkDuplicateAccount(@NonNull AccountModels.DuplicateAccountRequest request,
                                      @NonNull BackendCallback<AccountModels.DuplicateAccountData> callback) {
        callEndpoint("checkDuplicateAccount", request,
                envelopeType(AccountModels.DuplicateAccountData.class), callback);
    }

    public void linkAccountIdentity(@NonNull AccountModels.LinkIdentityRequest request,
                                    @NonNull BackendCallback<AccountModels.LinkIdentityData> callback) {
        callEndpoint("linkAccountIdentity", request,
                envelopeType(AccountModels.LinkIdentityData.class), callback);
    }

    public void getFarmerChatResponse(@NonNull AiModels.ChatRequest request,
                                      @NonNull BackendCallback<AiModels.ChatResponseData> callback) {
        callEndpoint("getFarmerChatResponse", request,
                envelopeType(AiModels.ChatResponseData.class), callback);
    }

    private static Type envelopeType(@NonNull Class<?> dataClass) {
        return TypeToken.getParameterized(ApiEnvelope.class, dataClass).getType();
    }

    private <T> void callEndpoint(@NonNull String endpoint,
                                  @NonNull Object payload,
                                  @NonNull Type envelopeType,
                                  @NonNull BackendCallback<T> callback) {
        Object callablePayload = toCallablePayload(payload);
        functions.getHttpsCallable(endpoint).call(callablePayload)
                .addOnSuccessListener(result -> {
                    ApiEnvelope<T> envelope = parseEnvelope(result, envelopeType);
                    if (envelope.isSuccess()) {
                        callback.onSuccess(envelope);
                    } else {
                        Log.w(TAG, endpoint + " returned non-success envelope: code="
                                + envelope.code + ", message=" + envelope.message
                                + ", traceId=" + envelope.traceId);
                        callback.onFailure(envelope, null);
                    }
                })
                .addOnFailureListener(error -> {
                    ApiEnvelope<T> envelope = mapThrowableToEnvelope(error);
                    Log.e(TAG, endpoint + " failed: code=" + envelope.code + ", message="
                            + envelope.message + ", traceId=" + envelope.traceId, error);
                    callback.onFailure(envelope, error);
                });
    }

    private Object toCallablePayload(@NonNull Object payload) {
        try {
            JsonElement root = gson.toJsonTree(payload);
            if (root == null || root.isJsonNull()) {
                return Collections.emptyMap();
            }
            // Firebase Functions serializer accepts primitives, lists, and maps.
            return gson.fromJson(root, Object.class);
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert callable payload. Falling back to empty map.", e);
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ApiEnvelope<T> parseEnvelope(HttpsCallableResult result, Type envelopeType) {
        try {
            JsonElement root = gson.toJsonTree(result.getData());

            if (root != null && root.isJsonObject() && root.getAsJsonObject().has("status")) {
                ApiEnvelope<T> envelope = gson.fromJson(root, envelopeType);
                if (envelope != null) {
                    return envelope;
                }
            }

            JsonObject compat = new JsonObject();
            compat.addProperty("status", "success");
            compat.addProperty("code", "OK");
            compat.addProperty("message", "Success");
            compat.add("data", root);
            return gson.fromJson(compat, envelopeType);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse backend envelope", e);
            return ApiEnvelope.error("PARSE_ERROR", "Invalid backend response format", null);
        }
    }

    private <T> ApiEnvelope<T> mapThrowableToEnvelope(Exception error) {
        if (error instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException ex = (FirebaseFunctionsException) error;
            String code = ex.getCode().name();
            String message = ex.getMessage() != null ? ex.getMessage() : "Backend call failed";
            return ApiEnvelope.error(code, message, null);
        }
        String msg = error.getMessage() != null ? error.getMessage() : "Unexpected backend error";
        return ApiEnvelope.error("UNKNOWN_ERROR", msg, null);
    }
}
