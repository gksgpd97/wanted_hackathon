package com.example.hackathon;

import com.bumptech.glide.load.engine.Resource;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.naver.maps.geometry.LatLng;

import java.util.Vector;

public class SookmyungMarkerItem {
    private String FILE_PATH = "http://hack20.dothome.co.kr/";
    private String m_id;
    private LatLng markersPosition;
    private String img_url;
    private String name;

    public SookmyungMarkerItem(String m_id, LatLng markersPosition, String img_url, String name) {
        this.m_id = m_id;
        this.markersPosition = markersPosition;
        this.img_url = FILE_PATH+img_url;
        this.name = name;
    }

    public String getImg_url() {
        return img_url;
    }

    public void setImg_url(String img_url) {
        this.img_url = img_url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getM_id() {
        return m_id;
    }

    public void setM_id(String m_id) {
        this.m_id = m_id;
    }

    public LatLng getMarkersPosition() {
        return markersPosition;
    }

    public void setMarkersPosition(LatLng markersPosition) {
        this.markersPosition = markersPosition;
    }
}

