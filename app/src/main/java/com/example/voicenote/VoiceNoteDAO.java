package com.example.voicenote;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface VoiceNoteDAO {
    @Query("SELECT * FROM voicenote")
    List<VoiceNote> getAllVoiceNotes();

    @Insert
    void insertVoiceNote(VoiceNote voiceNote);

}
