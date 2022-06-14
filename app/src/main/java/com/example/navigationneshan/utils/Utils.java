package com.example.navigationneshan.utils;

import com.example.navigationneshan.data.model.SearchModel;

import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
    public static SearchModel.Item convertJSONToModel(JSONObject jsonObject) throws JSONException {
        SearchModel.Item model = new SearchModel.Item();
        SearchModel.Location location = new SearchModel.Location();
        model.setTitle(jsonObject.getString("title"));
        model.setAddress(jsonObject.getString("address"));
        location.setX(Double.parseDouble(jsonObject.getString("lng")));
        location.setY(Double.parseDouble(jsonObject.getString("lat")));
        model.setLocation(location);
        return model;
    }

}
