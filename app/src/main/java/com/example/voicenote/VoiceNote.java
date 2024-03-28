package com.example.voicenote;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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
    public VoiceNote(){

    }

    public VoiceNote(String title, String ref, double secondsRecorded, float referenceVol, float enhancedVol){
        this.title = title;
        this.ref = ref;
        this.secondsRecorded = secondsRecorded;
        this.referenceVol = referenceVol;
        this.enhanchedVol = enhancedVol;
    }

    public String toString(){
        return "Title: " + title + "\nRef: " + ref + "\nsecondsRecorded: " +secondsRecorded + "\nreferenceVol: " + referenceVol + "\nenhancedVol: " + enhanchedVol;
    }
}
