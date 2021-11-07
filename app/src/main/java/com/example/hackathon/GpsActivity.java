package com.example.hackathon;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

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
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Vector;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GpsActivity extends FragmentActivity implements OnMapReadyCallback {

    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private String m_id, m_latitude, m_longitude;
    private ArrayList<SookmyungMarkerItem> sookmyungMarkerItemArrayList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gps);

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        sookmyungMarkerItemArrayList = new ArrayList<>();
        getSookmyungMarkertItem();
        
        locationSource =
                new FusedLocationSource(this, PERMISSION_REQUEST_CODE);
    }

    private void getSookmyungMarkertItem() {
        HyewonHomeMarkerInterface hyewonHomeMarkerInterface = RetrofitClient.getClient().create(HyewonHomeMarkerInterface.class);
        Call <JsonObject> call = hyewonHomeMarkerInterface.getHyewonHomeMarkerResult();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if(response.isSuccessful()){
                    Log.d("TAG", response.body().toString());
                    try {
                        JSONObject jsonObject = new JSONObject(new Gson().toJson(response.body()));
                        JSONArray jsonArray = jsonObject.getJSONArray("response");
                        int count = 0;
                        markersPosition = new Vector<LatLng>();

                        while (count < jsonArray.length()) {
                            JSONObject object = jsonArray.getJSONObject(count);
                            Log.d("TAG", object.getString("latitude"));
                            m_id = object.getString("m_id");
                            m_latitude = object.getString("latitude");
                            m_longitude = object.getString("longitude");
                            markersPosition.add(new LatLng(
                                    Double.parseDouble(m_latitude),
                                    Double.parseDouble(m_longitude)
                            ));
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
        LatLng initialPosition = new LatLng(37.65754848737425, 127.04858505634708);
        //학교LatLng initialPosition = new LatLng(37.54669614736176, 126.96465941216847);
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(initialPosition);
        naverMap.moveCamera(cameraUpdate);

        //현재위치 불러오기
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Face);
        naverMap.getUiSettings().setLocationButtonEnabled(true);

        //현재위치가 변경되면 호출되는 이벤트
        naverMap.addOnLocationChangeListener(location -> {
            freeActiveMarkers();
            LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
            for (LatLng markerPosition: markersPosition) {
                if (!withinSightMarker(currentPosition, markerPosition))
                    continue;
                Marker marker = new Marker();
                marker.setPosition(markerPosition);
                marker.setMap(naverMap);
                activeMarkers.add(marker);
            }
        });

    }

    // 마커 정보 저장시킬 변수들 선언
    private Vector<LatLng> markersPosition;
    private Vector<Marker> activeMarkers;

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
}