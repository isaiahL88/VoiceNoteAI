package com.example.voicenote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.room.Room;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ai.picovoice.android.voiceprocessor.VoiceProcessor;
import ai.picovoice.octopus.Octopus;
import ai.picovoice.octopus.OctopusActivationException;
import ai.picovoice.octopus.OctopusActivationLimitException;
import ai.picovoice.octopus.OctopusActivationRefusedException;
import ai.picovoice.octopus.OctopusActivationThrottledException;
import ai.picovoice.octopus.OctopusException;
import ai.picovoice.octopus.OctopusInvalidArgumentException;
import ai.picovoice.octopus.OctopusMatch;
import ai.picovoice.octopus.OctopusMetadata;

public class MyNotes extends AppCompatActivity {
    private static final String ACCESS_KEY = "8ajq9rfddzYFEbWAVy0N4Z43tElumLnuhiQwAsIuvptCe9nm/LCZOw==";
    private AppDatabase db;
    private VoiceNoteDAO vnDao;
    SearchView searchView;
    ListView listView;
    private ProgressBar progressBar;
    private MediaPlayer referenceMediaPlayer;
    private MediaPlayer enhancedMediaPlayer;
    private int recentNote = -1;
    private VoiceNoteAdapter voiceNoteAdapter;
    private ArrayList<VoiceNote> displayedVNs;
    private ArrayList<VoiceNote> voiceNotes;
    private Octopus octopus;
    private ArrayList<OctopusMetadata> refMetadata;
    private ArrayList<OctopusMetadata> enhancedMetadata;
    VoiceProcessor vp;
    private boolean mdGenerated; //set true when meta data has finished being generated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_notes);
        //ROOM DB and DAO INIT
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "voiceNoteDB").fallbackToDestructiveMigration().build();
        vnDao = db.voiceNoteDAO();

        //init views
        searchView = findViewById(R.id.searchView);
        listView = findViewById(R.id.list_view);
        progressBar = findViewById(R.id.loadingBar);

        //init state
        referenceMediaPlayer = new MediaPlayer();
        enhancedMediaPlayer = new MediaPlayer();
        refMetadata = new ArrayList<OctopusMetadata>();
        enhancedMetadata = new ArrayList<OctopusMetadata>();
        mdGenerated = false;
        listView.setVisibility(View.GONE); //keep list view gone until meta data is generated

        voiceNotes = new ArrayList<>();
        displayedVNs = new ArrayList<VoiceNote>();
        voiceNoteAdapter = new VoiceNoteAdapter(this,1, displayedVNs);
        listView.setAdapter(voiceNoteAdapter);

        loadVoiceNotes(); //Load all voice notes
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

        try {
            octopus = new Octopus.Builder().setAccessKey(ACCESS_KEY).build(getApplicationContext());
        } catch (OctopusInvalidArgumentException e) {
            displayError(e.getMessage());
        } catch (OctopusActivationException e) {
            displayError("AccessKey activation error");
        } catch (OctopusActivationLimitException e) {
            displayError("AccessKey reached its device limit");
        } catch (OctopusActivationRefusedException e) {
            displayError("AccessKey refused");
        } catch (OctopusActivationThrottledException e) {
            displayError("AccessKey has been throttled");
        } catch (OctopusException e) {
            displayError("Failed to initialize Octopus " + e.getMessage());
        }

    }

    /*
        This method will filter results displayed in the listView using the queried
        the user entered into the searchView

        Filters by title name
     */
    public void filter(String text){

        displayedVNs.clear(); //first clear old query results

        //title filtering
        for(VoiceNote voiceNote: voiceNotes){
            if(voiceNote.title.contains(text)){
                displayedVNs.add(voiceNote);
            }
        }

        //Octopus indexxing
        for(VoiceNote voiceNote: voiceNotes){
//            HashMap<String, OctopusMatch[]> matches = octopus.search(metadata, text);
        }

        voiceNoteAdapter.notifyDataSetChanged();
    }

    /*
        async thread goes and laod all voice notes using the dao and notfies the adapter
     */
    public void loadVoiceNotes(){
        Thread tr = new Thread(new Runnable() {
            @Override
            public void run() {
                voiceNotes = (ArrayList<VoiceNote>) vnDao.getAllVoiceNotes();
                displayedVNs.addAll(voiceNotes);
                generateMetaData();
            }
        });
        tr.start();
    }

    /*
        Generates Metadata for each voice note
     */
    public void generateMetaData(){
        for(VoiceNote voiceNote: voiceNotes){
            // Convert ArrayList<Short> to short[]
            int size = voiceNote.referenceData.size();
            short[] refData = new short[size];
            short[] enhcData = new short[size];
            for (int i = 0; i < size; i++) {
                refData[i] = voiceNote.referenceData.get(i);
                enhcData[i] = voiceNote.enhancedData.get(i);
            }
            try{
                refMetadata.add(octopus.indexAudioData(refData));
                enhancedMetadata.add(octopus.indexAudioData(enhcData));
            }catch(Exception e){
                Log.i("DEBUG", e.getMessage());
            }
        }

        mdGenerated = true;
        updateUI();

    }

    public void updateUI(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                voiceNoteAdapter.notifyDataSetChanged();
                if(mdGenerated){
                    listView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    /*
        Plays the note that was clicked in the list view

        Note: we need the position of the prev played note so we can reset the playButton
        img if another note starts play
     */
    public void playNote(String audioFile, float referenceVol, float enhancedVol, int position){
        if(recentNote != -1){
            referenceMediaPlayer.reset();
            enhancedMediaPlayer.reset();
        }

        recentNote = position;
        String referenceFilePath =  audioFile;
        String enhancedFilePath =  audioFile.substring(0, audioFile.length() - 4) + "E.wav";

        try{
            referenceMediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            referenceMediaPlayer.setLooping(false);
            referenceMediaPlayer.setDataSource(referenceFilePath);
            referenceMediaPlayer.prepare();

            enhancedMediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            enhancedMediaPlayer.setLooping(false);
            enhancedMediaPlayer.setDataSource(enhancedFilePath);
            enhancedMediaPlayer.prepare();

            referenceMediaPlayer.setVolume(referenceVol, referenceVol);
            enhancedMediaPlayer.setVolume(enhancedVol, enhancedVol);

            referenceMediaPlayer.start();
            enhancedMediaPlayer.start();
        }catch(Exception e){
            Log.i("DEBUG", "Failure trying to setup media play for file: "+ audioFile);
            Log.i("DEBUG", e.getMessage());
        }

    }

    private void displayError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private class VoiceNoteAdapter extends ArrayAdapter<VoiceNote> {
        private ArrayList<VoiceNote> displayedVNs;

        public VoiceNoteAdapter(@NonNull Context context, int resource, @NonNull List<VoiceNote> displayedVNs) {
            super(context, resource, displayedVNs);
            this.displayedVNs = (ArrayList<VoiceNote>) displayedVNs;
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

            //get Voice Note Object
            VoiceNote voiceNote = getItem(position);

            //init view
            TextView title = currentItemView.findViewById(R.id.title);
            TextView noteDuration = currentItemView.findViewById(R.id.noteDuration);
            ImageButton playButton = currentItemView.findViewById(R.id.playButton);

            //update view
            title.setText(voiceNote.title);
            noteDuration.setText(String.format("Recorded: %.1fs", voiceNote.secondsRecorded));


            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Play this voice note
                    playNote(voiceNote.ref, voiceNote.referenceVol, voiceNote.enhanchedVol, position);

                }
            });

            return currentItemView;
        }
    }
}