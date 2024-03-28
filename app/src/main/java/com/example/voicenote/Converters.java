package com.example.voicenote;

import androidx.room.TypeConverter;


import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class Converters {
    @TypeConverter
    public static String fromShortArrayList(ArrayList<Short> values){
        JSONArray jsonArray = new JSONArray();

        for(Short value: values){
            try{
                jsonArray.put(value);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        return jsonArray.toString();
    }

    @TypeConverter
    public static ArrayList<Short> fromString(String values) {
        try {
            JSONArray jsonArray = new JSONArray(values);
            ArrayList<Short> newValues = new ArrayList<Short>();
            for (int i = 0; i < jsonArray.length(); i++) {
                newValues.add(Short.parseShort(jsonArray.getString(i)));
            }
            return newValues;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}