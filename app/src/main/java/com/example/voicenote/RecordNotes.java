package com.example.voicenote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ai.picovoice.android.voiceprocessor.VoiceProcessor;
import ai.picovoice.koala.Koala;
import ai.picovoice.koala.KoalaActivationException;
import ai.picovoice.koala.KoalaActivationLimitException;
import ai.picovoice.koala.KoalaActivationRefusedException;
import ai.picovoice.koala.KoalaActivationThrottledException;
import ai.picovoice.koala.KoalaException;
import ai.picovoice.koala.KoalaInvalidArgumentException;

public class RecordNotes extends AppCompatActivity {
    private ImageButton recordButton;
    private TextView noteDuration;
    private static final String ACCESS_KEY = "8ajq9rfddzYFEbWAVy0N4Z43tElumLnuhiQwAsIuvptCe9nm/LCZOw==";

    private final VoiceProcessor voiceProcessor = VoiceProcessor.getInstance();
    private Koala koala;
    private String referenceFilepath;
    private String enhancedFilepath;
    private final ArrayList<Short> referenceData = new ArrayList<>();
    private final ArrayList<Short> enhancedData = new ArrayList<>();
    private Boolean isRecording, isPlaying;
    private MediaPlayer referenceMediaPlayer;
    private MediaPlayer enhancedMediaPlayer;
    private double secondsRecorded;
    private AppDatabase db;
    private VoiceNoteDAO vnDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_notes);
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "voiceNoteDB").fallbackToDestructiveMigration().build();
        vnDao = db.voiceNoteDAO();

        recordButton = findViewById(R.id.recordButton);
        noteDuration = findViewById(R.id.noteDuration);

        recordButton.setOnClickListener(this::onRecord);

        //STATE VALUES
        isRecording = false;
        isPlaying = false;

        //Setup original and enhanced file paths on seperate thread
        Thread tr = new Thread(new Runnable() {
            @Override
            public void run() {
                int count = vnDao.getAllVoiceNotes().size();

                referenceFilepath = getApplicationContext().getFileStreamPath(count + ".wav").getAbsolutePath();
                enhancedFilepath = getApplicationContext().getFileStreamPath(count + "E.wav").getAbsolutePath();
            }
        });
        tr.start();

        //init media players
        referenceMediaPlayer = new MediaPlayer();
        enhancedMediaPlayer = new MediaPlayer();
        referenceMediaPlayer.setVolume(0, 0);
        enhancedMediaPlayer.setVolume(1, 1);

        //setup koala
        try {
            koala = new Koala.Builder().setAccessKey(ACCESS_KEY).build(getApplicationContext());

        } catch (KoalaInvalidArgumentException e) {
            onKoalaInitError(e.getMessage());
        } catch (KoalaActivationException e) {
            onKoalaInitError("AccessKey activation error");
        } catch (KoalaActivationLimitException e) {
            onKoalaInitError("AccessKey reached its device limit");
        } catch (KoalaActivationRefusedException e) {
            onKoalaInitError("AccessKey refused");
        } catch (KoalaActivationThrottledException e) {
            onKoalaInitError("AccessKey has been throttled");
        } catch (KoalaException e) {
            onKoalaInitError("Failed to initialize Koala " + e.getMessage());
        }


        /*
            What actually records the audio data while the VP is started

            Saves audio samples in 'Frames', but samples are added one by one into
            reference data and enhanced data
         */
        voiceProcessor.addFrameListener(frame -> {
            synchronized (voiceProcessor) {
                try {
                    //save original samples for this frame
                    for (short sample : frame) {
                        referenceData.add(sample);
                    }

                    //save enhanced enhanced sample from enhanced frame
                    final short[] enhancedFrame = koala.process(frame);
                    if (referenceData.size() >= koala.getDelaySample()) {
                        for (short sample : enhancedFrame) {
                            enhancedData.add(sample);
                        }
                    }

                    //display time recorded so far
                    if ((referenceData.size() / koala.getFrameLength()) % 10 == 0) {
                        runOnUiThread(() -> {
                            double secondsRecorded = ((double) (referenceData.size()) / (double) (koala.getSampleRate()));
                            noteDuration.setText(String.format("Recording: %.1fs", secondsRecorded));
                        });
                    }
                } catch (KoalaException e) {
                    runOnUiThread(() -> displayError(e.toString()));
                }
            }
        });
        voiceProcessor.addErrorListener(error -> runOnUiThread(() -> displayError(error.toString())));
    }

    //release Media players and koala
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (referenceMediaPlayer != null) {
            referenceMediaPlayer.release();
        }
        if (enhancedMediaPlayer != null) {
            enhancedMediaPlayer.release();
        }
        koala.delete();
    }

    private void onKoalaInitError(String error) {
        displayError(error);
        recordButton.setVisibility(View.GONE);
    }

    /*
         Resets the media play and changes the data source to 'audioFile'

         Note: also prepares the Media Player so it can be started directly after

               - will also seek media play to the beginning if it is the same audioFile from
                 before the click
       */
    private void resetMediaPlayer(MediaPlayer mediaPlayer, String audioFile) throws IOException {
        mediaPlayer.reset();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );
        mediaPlayer.setLooping(true);
        mediaPlayer.setDataSource(audioFile);
        mediaPlayer.prepare();
    }

    /*
        On Click for record button (both record and stop recording functions)

        Note: the imageButton recordButton will change src after each press and change in state
        from isRecording: true -> isRecording: false

     */
    public void onRecord(View v){
        if(isRecording){
            //STOP RECORDING
            isRecording = false;

            stopRecording();
            //reset media players with new data
            try {
                resetMediaPlayer(referenceMediaPlayer, referenceFilepath);
                resetMediaPlayer(enhancedMediaPlayer, enhancedFilepath);
            } catch (IOException e) {
                displayError(e.getMessage());
            }

            //open popup
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View popup = getLayoutInflater().inflate(R.layout.voice_note_popup, null);
            builder.setView(popup);
            AlertDialog dialog = builder.create();
            dialog.show();

            //POPUP VIEWS
            Button playButton = popup.findViewById(R.id.playButton);
            Button cancelButton = popup.findViewById(R.id.cancelButton);
            Button saveButton = popup.findViewById(R.id.saveButton);
            SeekBar koalaSeek= popup.findViewById(R.id.koalaSlider);

            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isPlaying){
                        //CURRENTLY PLAYING -> STOP
                        referenceMediaPlayer.pause();
                        enhancedMediaPlayer.pause();
                        isPlaying = false;
                        playButton.setText("Play");

                    }else{
                        //NOT PLAYING -> START PLAYING
                        referenceMediaPlayer.start();
                        enhancedMediaPlayer.start();
                        isPlaying = true;
                        playButton.setText("Pause");
                        //todo: fix
                        referenceMediaPlayer.setVolume(0, 0);
                        enhancedMediaPlayer.setVolume(1, 1);
                    }
                }
            });

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //stop media players in case they are still playing
                    if (referenceMediaPlayer.isPlaying()) {
                        referenceMediaPlayer.stop();
                    }
                    if (enhancedMediaPlayer.isPlaying()) {
                        enhancedMediaPlayer.stop();
                    }

                    //reset state
                    isRecording = false;
                    isPlaying = false;

                    playButton.setText("Play");
                    recordButton.setImageResource(R.drawable.record);

                    dialog.dismiss();

                }
            });

            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    uploadVoiceNote();
                    displayError("Saved Voice Note!");
                    finish();
                }
            });

        }else{
            //START RECORDING
            isRecording = true;
            recordButton.setImageResource(R.drawable.stop);

            //stop media players in case they are still playing
            if (referenceMediaPlayer.isPlaying()) {
                referenceMediaPlayer.stop();
            }
            if (enhancedMediaPlayer.isPlaying()) {
                enhancedMediaPlayer.stop();
            }
            if (voiceProcessor.hasRecordAudioPermission(this)) {
                startRecording();
            } else {
                requestRecordPermission();
            }

        }

    }

    /*
        Uploads voice note to room database

        note: must not be performed on main UI thread
     */
    public void uploadVoiceNote(){
        Thread tr = new Thread(new Runnable(){
            @Override
            public void run(){
                Log.i("DEBUG", "run thread");
                vnDao.insertVoiceNote(new VoiceNote("test", referenceFilepath, secondsRecorded));

                ArrayList<VoiceNote> voiceNotes = (ArrayList<VoiceNote>) vnDao.getAllVoiceNotes();
                for(VoiceNote voiceNote: voiceNotes){
                    Log.i("DEBUG", voiceNote.toString());
                }
            }
        });
        tr.start();
    }

    /*
        Starts the VP, (starts recording)

        Pre Cond: Must have r ecord audio permission before calling this method
     */
    public void startRecording(){
        enhancedData.clear();
        referenceData.clear();

        try {
            voiceProcessor.start(koala.getFrameLength(), koala.getSampleRate());
        } catch (Exception e) {
            displayError("Start recording failed\n" + e.getMessage());
        }
    }

    /*
        Stops the recording

        - gets the length recorded
        - Creates RandomAccessFile for original and enhanced audio
        - Calls writeWavFile for both audios
        - Closes Random Access File
     */

    private void stopRecording(){
        try{
            voiceProcessor.stop();

            secondsRecorded = ((double) (referenceData.size()) / (double) (koala.getSampleRate()));
            noteDuration.setText(String.format("Recorded: %.1fs", secondsRecorded));

            synchronized (voiceProcessor) {
                short[] emptyFrame = new short[koala.getFrameLength()];
                Arrays.fill(emptyFrame, (short) 0);
                while (enhancedData.size() < referenceData.size()) {
                    final short[] enhancedFrame = koala.process(emptyFrame);
                    for (short sample : enhancedFrame) {
                        enhancedData.add(sample);
                    }
                }

                RandomAccessFile referenceFile = new RandomAccessFile(referenceFilepath, "rws");
                RandomAccessFile enhancedFile = new RandomAccessFile(enhancedFilepath, "rws");

                writeWavFile(
                        referenceFile,
                        (short) 1,
                        (short) 16,
                        koala.getSampleRate(),
                        referenceData);
                writeWavFile(
                        enhancedFile,
                        (short) 1,
                        (short) 16,
                        koala.getSampleRate(),
                        enhancedData);
                referenceFile.close();
                enhancedFile.close();
            }
        }catch(Exception e) {
            displayError("Stop recording failed\n" + e.getMessage());
        }
    }

    private void displayError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //requests record audio permission from the user
    private void requestRecordPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
    }

    //takes result from request permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            //reset record button
            isRecording = false;
            recordButton.setImageResource(R.drawable.record);
        } else {
            startRecording();
        }
    }

    /*
        This code actually writes the wav file using the current outputFile as destination

        Utilizes a ByteBuffer
     */
    private void writeWavFile(
            RandomAccessFile outputFile,
            short channelCount,
            short bitDepth,
            int sampleRate,
            ArrayList<Short> pcm
    ) throws IOException {
        final int wavHeaderLength = 44;
        ByteBuffer byteBuf = ByteBuffer.allocate(wavHeaderLength);
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);

        byteBuf.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        byteBuf.putInt((bitDepth / 8 * pcm.size()) + 36);
        byteBuf.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        byteBuf.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        byteBuf.putInt(16);
        byteBuf.putShort((short) 1);
        byteBuf.putShort(channelCount);
        byteBuf.putInt(sampleRate);
        byteBuf.putInt(sampleRate * channelCount * bitDepth / 8);
        byteBuf.putShort((short) (channelCount * bitDepth / 8));
        byteBuf.putShort(bitDepth);
        byteBuf.put("data".getBytes(StandardCharsets.US_ASCII));
        byteBuf.putInt(bitDepth / 8 * pcm.size());

        outputFile.seek(0);
        outputFile.write(byteBuf.array());

        byteBuf = ByteBuffer.allocate(2 * pcm.size());
        byteBuf.order(ByteOrder.LITTLE_ENDIAN);

        for (short s : pcm) {
            byteBuf.putShort(s);
        }
        outputFile.write(byteBuf.array());
    }
}