package com.example.navigationneshan.data.model;

import android.os.Parcel;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class SearchModel  {

    @SerializedName("count")
    @Expose
    private int count;
    @SerializedName("items")
    @Expose
    private List<Item> items = null;

    public int getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }



    public static class Item implements SearchSuggestion {

        @SerializedName("title")
        @Expose
        private String title;
        @SerializedName("address")
        @Expose
        private String address;
        @SerializedName("neighbourhood")
        @Expose
        private String neighbourhood;
        @SerializedName("region")
        @Expose
        private String region;
        @SerializedName("type")
        @Expose
        private String type;
        @SerializedName("category")
        @Expose
        private String category;
        @SerializedName("location")
        @Expose
        private Location location;



        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getNeighbourhood() {
            return neighbourhood;
        }

        public void setNeighbourhood(String neighbourhood) {
            this.neighbourhood = neighbourhood;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }


        @Override
        public String getBody() {
            JSONObject jsonObject= new JSONObject();
            try {
                jsonObject.put("title",title);
                jsonObject.put("address",address);
                jsonObject.put("lat",location.getY());
                jsonObject.put("lng",location.getX());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject.toString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

        }


    }

    public static class Location {

        @SerializedName("x")
        @Expose
        private Double x;
        @SerializedName("y")
        @Expose
        private Double y;

        public Double getX() {
            return x;
        }

        public void setX(Double x) {
            this.x = x;
        }

        public Double getY() {
            return y;
        }

        public void setY(Double y) {
            this.y = y;
        }

    }
}



