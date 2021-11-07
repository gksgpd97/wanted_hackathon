package com.example.hackathon;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Vector;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraActivity extends FragmentActivity implements OnMapReadyCallback {

    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private ArrayList<SookmyungMarkerItem> sookmyungMarkerItems;

    // 마커 정보 저장시킬 변수들 선언
    private Vector<LatLng> markersPosition;
    private Vector<Marker> activeMarkers;
    private AlertDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        loadSookmyungMarkertItem();

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        sookmyungMarkerItems = new ArrayList<>();

        locationSource =
                new FusedLocationSource(this, PERMISSION_REQUEST_CODE);
    }

    private void loadSookmyungMarkertItem() {
        SookmyungMarkerInterface sookmyungMarkerInterface = RetrofitClient.getClient().create(SookmyungMarkerInterface.class);
        Call <JsonObject> call = sookmyungMarkerInterface.getSookmyungMarkerResult();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if(response.isSuccessful()){
                    Log.d("TAG", response.body().toString());
                    try {
                        JSONObject jsonObject = new JSONObject(new Gson().toJson(response.body()));
                        JSONArray jsonArray = jsonObject.getJSONArray("response");
                        int count = 0;
                        LatLng latLng;
                        String m_id, m_latitude, m_longitude, m_img_url, m_name;
                        while (count < jsonArray.length()) {
                            JSONObject object = jsonArray.getJSONObject(count);
                            Log.d("TAG", object.getString("latitude"));
                            m_id = object.getString("m_id");
                            m_latitude = object.getString("latitude");
                            m_longitude = object.getString("longitude");
                            m_img_url = object.getString("img_url");
                            m_name = object.getString("name");
                            latLng= new LatLng(
                                    Double.parseDouble(m_latitude),
                                    Double.parseDouble(m_longitude)
                            );
                            SookmyungMarkerItem sookmyungMarkerItem = new SookmyungMarkerItem(m_id, latLng, m_img_url, m_name);
                            sookmyungMarkerItems.add(sookmyungMarkerItem);
                            count++;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });

    }



    @UiThread
    @Override
    public void onMapReady(@NonNull final NaverMap naverMap) {
        // 카메라 초기 위치 설정
        //37.54669614736176, 126.96465941216847
        //집LatLng initialPosition = new LatLng(37.65754848737425, 127.04858505634708);
        this.naverMap = naverMap;
        LatLng initialPosition = new LatLng(37.54669614736176, 126.96465941216847);
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(initialPosition);
        naverMap.moveCamera(cameraUpdate);

        naverMap.addOnCameraChangeListener(new NaverMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(int reason, boolean animated) {
                freeActiveMarkers();
                // 정의된 마커위치들중 가시거리 내에있는것들만 마커 생성
                LatLng currentPosition = getCurrentPosition(naverMap);
                for (SookmyungMarkerItem sookmyungMarkerItem : sookmyungMarkerItems) {
                    LatLng markerPosition = sookmyungMarkerItem.getMarkersPosition();
                    String m_name = sookmyungMarkerItem.getName();
                    String m_img_url = sookmyungMarkerItem.getImg_url();

                    if (!withinSightMarker(currentPosition, markerPosition))
                        continue;
                    Marker marker = new Marker();
                    marker.setPosition(markerPosition);
                    marker.setCaptionText(m_name);
                    RequestOptions options = new RequestOptions().skipMemoryCache(true).override(100).placeholder(R.color.white);
                    Glide.with(getApplicationContext())
                            .asBitmap()
                            .load(m_img_url)
                            .apply(options)
                            .into(new CustomTarget<Bitmap>() {

                                @Override
                                public void onResourceReady(@NonNull @NotNull Bitmap resource, @Nullable @org.jetbrains.annotations.Nullable Transition<? super Bitmap> transition) {
                                    Bitmap bitmap = resource;
                                    OverlayImage image = OverlayImage.fromBitmap(bitmap);
                                    marker.setIcon(image);
                                }

                                @Override
                                public void onLoadCleared(@Nullable @org.jetbrains.annotations.Nullable Drawable placeholder) {

                                }
                            });
                    marker.setMap(naverMap);
                    marker.setOnClickListener(overlay -> {
                        Log.d("TAG", "CLICK");
                        AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
                        dialog = builder.setMessage(m_name + "을(를) 획득하셨습니다!")
                                .setNegativeButton("확인", null)
                                .create();
                        dialog.show();
                        return true;
                    });
                    activeMarkers.add(marker);
                }
            }
        });

    }

    // 현재 카메라가 보고있는 위치
    public LatLng getCurrentPosition(NaverMap naverMap) {
        CameraPosition cameraPosition = naverMap.getCameraPosition();
        return new LatLng(cameraPosition.target.latitude, cameraPosition.target.longitude);
    }

    // 선택한 마커의 위치가 가시거리(카메라가 보고있는 위치 반경 3km 내)에 있는지 확인
    public final static double REFERANCE_LAT_X3 = 0.04 / 109.958489129649955;
    public final static double REFERANCE_LNG_X3 = 0.04 / 88.74;
    public boolean withinSightMarker(LatLng currentPosition, LatLng markerPosition) {
        boolean withinSightMarkerLat = Math.abs(currentPosition.latitude - markerPosition.latitude) <= REFERANCE_LAT_X3;
        boolean withinSightMarkerLng = Math.abs(currentPosition.longitude - markerPosition.longitude) <= REFERANCE_LNG_X3;
        return withinSightMarkerLat && withinSightMarkerLng;
    }

    // 지도상에 표시되고있는 마커들 지도에서 삭제
    private void freeActiveMarkers() {
        if (activeMarkers == null) {
            activeMarkers = new Vector<Marker>();
            return;
        }
        for (Marker activeMarker: activeMarkers) {
            activeMarker.setMap(null);
        }
        activeMarkers = new Vector<Marker>();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    protected  void onStop() {
        super.onStop();
        if(dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }
}