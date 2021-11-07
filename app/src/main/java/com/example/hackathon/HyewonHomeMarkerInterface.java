package com.example.hackathon;



import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;

public interface HyewonHomeMarkerInterface {
    @GET("HyewonHomeMarkerRequest.php")
    Call<JsonObject> getHyewonHomeMarkerResult(
    );
}

