package com.example.simplecamera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class VideoViewActivity extends AppCompatActivity implements View.OnClickListener {
    StyledPlayerView recordedVideoView;
    ImageView backImageView;
    MaterialButton videoShareButton;
    MaterialButton videoDeleteButton;

    Uri videoUri;
    String videoName;
    File videoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_view);

        Bundle extras = getIntent().getExtras();
        videoUri = (Uri) extras.get("VIDEO_URI");
        videoName = (String) extras.get("VIDEO_NAME");
        videoFile = (File) extras.get("VIDEO_FILE");

        Log.d("VIDEO_URI", videoUri.toString());

        initializeViewElements();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Toast.makeText(this, videoUri.toString(), Toast.LENGTH_SHORT).show();
        loadVideo();
    }

    private void loadVideo() {
        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        ExoPlayer player = new ExoPlayer.Builder(this).build();

        recordedVideoView.setPlayer(player);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void initializeViewElements() {
        recordedVideoView = findViewById(R.id.recorderVideoView);
        backImageView = findViewById(R.id.backImageView);
        videoShareButton = findViewById(R.id.videoShareButton);
        videoDeleteButton = findViewById(R.id.videoDeleteButton);

        backImageView.setOnClickListener(this);
        videoShareButton.setOnClickListener(this);
        videoDeleteButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.backImageView) {
            super.onBackPressed();
        } else if (view.getId() == R.id.videoShareButton) {
            shareVideo();
        } else if (view.getId() == R.id.videoDeleteButton) {
            deleteVideo();
        }
    }

    private void shareVideo() {
        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.putExtra(Intent.EXTRA_STREAM, getVideoToShare());
        intent.putExtra(Intent.EXTRA_TEXT, "Example video description");
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setType("video/mp4");
        startActivity(Intent.createChooser(intent, "Share Via"));
    }

    private Uri getVideoToShare() {
        File videoFolder = new File(getFilesDir(), "SimpleVideos");
        Uri uri = null;

        try {
            videoFolder.mkdirs();
            File file = new File(videoFolder, videoName);
            FileOutputStream outputStream = new FileOutputStream(file);
            InputStream inputStream = new FileInputStream(videoFile);
            byte[] buf = new byte[1024];
            int len;

            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            outputStream.flush();
            outputStream.close();
            uri = FileProvider.getUriForFile(this, this.getPackageName(), file);
        } catch (Exception e) {
            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return uri;
    }

    private void deleteVideo() {
        if (!videoFile.delete()) {
            Toast.makeText(this, "Failed to delete video",
                    Toast.LENGTH_LONG).show();
        }
        super.onBackPressed();
    }
}