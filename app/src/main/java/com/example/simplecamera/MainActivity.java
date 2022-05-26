package com.example.simplecamera;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    PreviewView previewViewCamera;
    ImageCapture imageCapture;
    ProcessCameraProvider cameraProvider = null;
    CameraSelector cameraSelector;
    Recording recording;
    Recorder recorder;

    MaterialButton imageSelectButton;
    MaterialButton videoSelectButton;
    MaterialButton imageCaptureButton;
    MaterialButton videoCaptureStartButton;
    MaterialButton videoCaptureStopButton;
    MaterialTextView recordingTimerView;

    Boolean RECORDING = Boolean.FALSE;

    Uri videoUri;
    String videoName;
    File videoFile;

    enum CameraState {
        IMAGE,
        VIDEO
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViewElements();

        if (checkPermission()) {
            startCameraForImage();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 100);
            }
        }
    }

    private void initializeViewElements() {
        previewViewCamera = findViewById(R.id.viewFinder);
        imageSelectButton = findViewById(R.id.imageSelectButton);
        videoSelectButton = findViewById(R.id.videoSelectButton);
        imageCaptureButton = findViewById(R.id.imageCaptureButton);
        recordingTimerView = findViewById(R.id.recordingTimerView);
        videoCaptureStartButton = findViewById(R.id.videoCaptureStartButton);
        videoCaptureStopButton = findViewById(R.id.videoCaptureStopButton);

        imageSelectButton.setOnClickListener(this);
        videoSelectButton.setOnClickListener(this);
        imageCaptureButton.setOnClickListener(this);
        videoCaptureStartButton.setOnClickListener(this);
        videoCaptureStopButton.setOnClickListener(this);
    }

    boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d("PERMISSION", "DEVICE ANDROID M");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return checkSelfPermission(Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED;
            }
            if (checkSelfPermission(Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED) {
                return false;
            } else if (checkSelfPermission(Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            } else return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED;
        } else {
            return false;
        }
    }

    private void startCameraForImage() {
        initializeCameraProvider();

        Preview preview = new Preview.Builder().build();
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        imageCapture = new ImageCapture.Builder().setTargetRotation(
                (int) previewViewCamera.getRotation()).build();

        preview.setSurfaceProvider(previewViewCamera.getSurfaceProvider());
        cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, preview);
    }

    private void startCameraForVideo() {
        initializeCameraProvider();

        Preview preview = new Preview.Builder().build();
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewViewCamera.getSurfaceProvider());
        List < Quality > quality = new LinkedList < Quality > () {
            {
                add(Quality.HD);
                add(Quality.SD);
            }
        };
        QualitySelector qualitySelector = QualitySelector.fromOrderedList(quality,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD));
        recorder = new Recorder.Builder().setQualitySelector(qualitySelector).build();
        VideoCapture < Recorder > videoCapture = VideoCapture.withOutput(recorder);
        cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector,
                videoCapture, preview);
    }

    private void initializeCameraProvider() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        } else {
            ListenableFuture < ProcessCameraProvider > cameraProviderFuture =
                    ProcessCameraProvider.getInstance(this);
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION", "CAMERA GRANTED");
            } else {
                Log.d("PERMISSION", "CAMERA DENIED");
            }
            if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION", "STORAGE GRANTED");
            } else {
                Log.d("PERMISSION", "STORAGE DENIED");
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.imageSelectButton) {
            if (!RECORDING) {
                startCameraForImage();
                toggleVideoImageButtons(CameraState.IMAGE);
            }
        } else if (view.getId() == R.id.videoSelectButton) {
            if (!RECORDING) {
                startCameraForVideo();
                toggleVideoImageButtons(CameraState.VIDEO);
            }
        } else if (view.getId() == R.id.imageCaptureButton) {
            captureImage();
        } else if (view.getId() == R.id.videoCaptureStartButton) {
            startRecording();
            setViewRecording(true);
        } else if (view.getId() == R.id.videoCaptureStopButton) {
            recording.stop();
            RECORDING = false;
            Toast.makeText(getApplicationContext(), "Video Saved", Toast.LENGTH_SHORT).show();
            setViewRecording(false);
            startVideoViewActivity(videoUri, videoName);
        }
    }

    private void setViewRecording(Boolean state) {
        if (state) {
            videoCaptureStartButton.setVisibility(View.GONE);
            videoCaptureStopButton.setVisibility(View.VISIBLE);
            imageSelectButton.setVisibility(View.GONE);
            videoSelectButton.setVisibility(View.GONE);
            recordingTimerView.setVisibility(View.VISIBLE);
        } else {
            videoCaptureStartButton.setVisibility(View.VISIBLE);
            videoCaptureStopButton.setVisibility(View.GONE);
            imageSelectButton.setVisibility(View.VISIBLE);
            videoSelectButton.setVisibility(View.VISIBLE);
            recordingTimerView.setVisibility(View.INVISIBLE);
        }
    }

    private void captureImage() {
        String nameTimeStamp = "Image_" + System.currentTimeMillis();
        String name = nameTimeStamp + ".jpeg";
        ImageCapture.OutputFileOptions outputFileOptions = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, nameTimeStamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.ORIENTATION, 90);

            outputFileOptions = new ImageCapture.OutputFileOptions.Builder(this.getContentResolver(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build();

        } else {
            File mImageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "SimpleImages");
            boolean isDirectoryCreated = mImageDir.exists() || mImageDir.mkdirs();
            if (isDirectoryCreated) {
                File file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES) + File.separator +
                        "SimpleImages", name);

                outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
            }
        }

        assert outputFileOptions != null;
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.e("IMAGE", "Image Captured Successfully");
                        Uri savedUri = outputFileResults.getSavedUri();
                        if (savedUri != null) {
                            Log.v("IMAGE", "Saved Image at " + savedUri);
                        }
                        Toast.makeText(getApplicationContext(), "Image Saved",
                                Toast.LENGTH_SHORT).show();

                        startImageViewActivity(savedUri, name);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("IMAGE", "Image Capture Failed: " + exception);
                        Toast.makeText(MainActivity.this, "Image Capture Failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startImageViewActivity(Uri savedUri, String imageName) {
        Intent imageViewIntent = new Intent(MainActivity.this, ImageViewActivity.class);
        imageViewIntent.putExtra("IMAGE_URI", savedUri);
        imageViewIntent.putExtra("IMAGE_NAME", imageName);
        startActivity(imageViewIntent);
    }

    private void startVideoViewActivity(Uri savedUri, String videoName) {
        Intent videoViewIntent = new Intent(MainActivity.this, VideoViewActivity.class);
        videoViewIntent.putExtra("VIDEO_URI", savedUri);
        videoViewIntent.putExtra("VIDEO_NAME", videoName);
        videoViewIntent.putExtra("VIDEO_FILE", videoFile);
        startActivity(videoViewIntent);
    }

    private void toggleVideoImageButtons(CameraState cameraState) {
        if (cameraState == CameraState.IMAGE) {
            imageSelectButton.setBackgroundColor(this.getResources().getColor(R.color.purple_500));
            videoSelectButton.setBackgroundColor(this.getResources().getColor(R.color.grey));
            imageCaptureButton.setVisibility(View.VISIBLE);
            videoCaptureStartButton.setVisibility(View.INVISIBLE);
        } else {
            imageSelectButton.setBackgroundColor(this.getResources().getColor(R.color.grey));
            videoSelectButton.setBackgroundColor(this.getResources().getColor(R.color.purple_500));
            imageCaptureButton.setVisibility(View.INVISIBLE);
            videoCaptureStartButton.setVisibility(View.VISIBLE);
        }
    }

    private void startRecording() {
        String name = "SimpleVideo_" + System.currentTimeMillis() + ".mp4";

        File mImageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "SimpleVideos");
        boolean isDirectoryCreated = mImageDir.exists() || mImageDir.mkdirs();

        if (isDirectoryCreated) {
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES) + File.separator +
                    "SimpleVideos", name);

            FileOutputOptions fileOutputOptions = new FileOutputOptions.Builder(file).build();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED) {
                Log.e("PERMISSION", "Audio recording permission denied");
                return;
            }
            recording = recorder.prepareRecording(MainActivity.this,
                    fileOutputOptions).withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
                        Log.d("VIDEO", "Video Recording Started");
                        if (videoUri == null) {
                            RECORDING = Boolean.TRUE;
                            videoUri = Uri.fromFile(file);
                            videoFile = file;
                        }
                        if (videoName == null) {
                            videoName = name;
                        }
                        recordingTimerView.setText(String
                                .format(Locale.getDefault(), "%d:%02d:%02d",
                                        TimeUnit.NANOSECONDS.toSeconds(videoRecordEvent
                                                .getRecordingStats().getRecordedDurationNanos()) / 3600,
                                        (TimeUnit.NANOSECONDS.toSeconds(videoRecordEvent
                                                .getRecordingStats().getRecordedDurationNanos()) % 3600) / 60,
                                        TimeUnit.NANOSECONDS.toSeconds(videoRecordEvent
                                                .getRecordingStats().getRecordedDurationNanos()) % 60));
                    });
        }
    }
}