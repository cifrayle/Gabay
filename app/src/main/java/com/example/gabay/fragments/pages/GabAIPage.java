package com.example.gabay.fragments.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.camera.view.PreviewView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gabay.R;

public class GabAIPage extends Fragment {

    private PreviewView cameraPreview;
    private ExecutorService cameraExecutor;
    private View view;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Log.e("CameraPermission", "Permission denied!");
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_gabai_page, container, false);

        // Initialize cameraPreview and check if it's null
        cameraPreview = view.findViewById(R.id.cameraPreview);

        // Ensure cameraPreview is not null before using it
        if (cameraPreview == null) {
            Log.e("GabAIPage", "CameraPreview is null!");
            Toast.makeText(requireContext(), "Camera preview not found", Toast.LENGTH_LONG).show();
            return view;
        }

        // Initialize executor for camera
        cameraExecutor = Executors.newSingleThreadExecutor();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkCameraPermission()) {
            startCamera();  // Start camera if permission is granted
        } else {
            requestCameraPermission();  // Request permission if not granted
        }
    }

    /**
     * Initializes and starts the CameraX preview.
     * This method sets up the camera provider and binds the camera lifecycle to this fragment.
     */
    private void startCamera() {
        if (cameraPreview == null) {
            Log.e("GabAIPage", "CameraPreview is null in startCamera()!");
            return;
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Unbind any previous use cases before rebinding
                cameraProvider.unbindAll();

                // Set up the camera preview
                Preview preview = new Preview.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT) // Use front camera
                        .build();

                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                // Bind the camera lifecycle to the fragment's view lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        cameraSelector,
                        preview);

            } catch (Exception e) {
                Log.e("CameraX", "Error starting camera: " + e.getMessage(), e);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onPause() {
        super.onPause();
        // Make sure to release the camera when the fragment is paused
        if (getView() != null && isAdded()) {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get();
                cameraProvider.unbindAll();
            } catch (Exception e) {
                Log.e("CameraX", "Error unbinding camera: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}