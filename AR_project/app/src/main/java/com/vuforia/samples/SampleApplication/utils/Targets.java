package com.vuforia.samples.SampleApplication.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;

import com.vuforia.samples.VuforiaSamples.R;

import static com.vuforia.samples.VuforiaSamples.R.raw.song;

/**
 * Created by Mina Nabil on 9/5/2017.
 */

public class Targets {
    public float objectScale;
    public MeshObject itsModel;
    MediaPlayer mediaPlayer = new MediaPlayer();
    boolean sound= true;
    public boolean used=false;
    public boolean animated;
    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            // Now that the sound file has finished playing, release the media player resources.
            releaseMediaPlayer();
        }
    };
    public Targets(float s,MeshObject model,boolean a){

        objectScale=s;
        itsModel=model;
        animated=a;
        used=false;
      mediaPlayer=null;
    }
    public Targets(float s,MeshObject model){

        objectScale=s;
        itsModel=model;
        animated=false;
        used=false;
       mediaPlayer=null;
    }
    public Targets (float s, MeshObject model, Context context,int song){
            objectScale=s;
        itsModel=model;
            releaseMediaPlayer();
            mediaPlayer=MediaPlayer.create(context, song);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);



    }

    public void playMusic(){

                                if(!mediaPlayer.isPlaying()){
                            mediaPlayer.start();
                                    mediaPlayer.setOnCompletionListener(mCompletionListener);


                                }

    }
    private void releaseMediaPlayer() {
        // If the media player is not null, then it may be currently playing a sound.
        if (mediaPlayer != null) {
            // Regardless of the current state of the media player, release its resources
            // because we no longer need it.
            mediaPlayer.release();

            // Set the media player back to null. For our code, we've decided that
            // setting the media player to null is an easy way to tell that the media player
            // is not configured to play an audio file at the moment.
            mediaPlayer = null;
        }
    }
}
