package com.example.voicenote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MyNotes extends AppCompatActivity {
    SearchView searchView;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_notes);

        //init views
        searchView = findViewById(R.id.searchView);
        listView = findViewById(R.id.list_view);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return false;
            }
        });
    }

    /*
        This method will filter results displayed in the listView using the queried
        the user entered into the searchView
     */
    public void filter(String text){

    }

    private class VoiceNoteAdapter extends ArrayAdapter<VoiceNote> {
        private ArrayList<VoiceNote> displayedVNs;

        public VoiceNoteAdapter(@NonNull Context context, int resource, @NonNull List<VoiceNote> displayedVNs) {
            super(context, resource, displayedVNs);
        }

        @Override
        public int getCount(){
            return displayedVNs.size();
        }

        @Override
        public VoiceNote getItem(int position){
            return displayedVNs.get(position);
        }

        @Override
        public long getItemId(int position){
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            // convertView which is recyclable view
            View currentItemView = convertView;

            // of the recyclable view is null then inflate the custom layout for the same
            if (currentItemView == null) {
                currentItemView = LayoutInflater.from(getContext()).inflate(R.layout.voice_note_view, parent, false);
            }

            return currentItemView;
        }
    }
}