package com.example.voicenote;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {VoiceNote.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract VoiceNoteDAO voiceNoteDAO();
}
