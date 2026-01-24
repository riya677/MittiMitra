package com.mittimitra.network;

import java.util.List;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface HuggingFaceService {
    @POST
    Call<List<ClassificationResult>> classifyImage(
            @Url String url,
            @Header("Authorization") String token,
            @Body RequestBody image
    );
}