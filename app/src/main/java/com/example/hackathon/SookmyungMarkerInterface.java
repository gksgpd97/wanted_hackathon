package com.example.hackathon;



import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SookmyungMarkerInterface {
    @GET("SookmyungMarkerRequest.php")
    Call<JsonObject> getSookmyungMarkerResult(
    );
}

