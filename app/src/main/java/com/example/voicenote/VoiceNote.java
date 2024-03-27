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

    VoiceNote(String title, String ref, double secondsRecorded){
        this.title = title;
        this.ref = ref;
        this.secondsRecorded = secondsRecorded;
    }

    public String toString(){
        return "Title: " + title + "\nRef: " + ref + "\nsecondsRecorded: " +secondsRecorded;
    }
}
