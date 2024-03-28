package com.example.voicenote;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;

@Entity
public class VoiceNote {
    @ColumnInfo(name= "title")
    String title;
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "ref")
    String ref;
    @ColumnInfo(name = "secondsRecorded")
    double secondsRecorded;
    @ColumnInfo(name = "enhancedVol")
    float enhanchedVol;

    @ColumnInfo(name = "referenceVol")
    float referenceVol;

    ArrayList<Short> referenceData;
    ArrayList<Short> enhancedData;

    public VoiceNote(){

    }

    public VoiceNote(String title, String ref, double secondsRecorded, float referenceVol, float enhancedVol, ArrayList<Short> referenceData, ArrayList<Short> enhancedData){
        this.title = title;
        this.ref = ref;
        this.secondsRecorded = secondsRecorded;
        this.referenceVol = referenceVol;
        this.enhanchedVol = enhancedVol;
        this.referenceData = referenceData;
        this.enhancedData = enhancedData;
    }

    public String toString(){
        return "Title: " + title + "\nRef: " + ref + "\nsecondsRecorded: " +secondsRecorded + "\nreferenceVol: " + referenceVol + "\nenhancedVol: " + enhanchedVol;
    }
}
