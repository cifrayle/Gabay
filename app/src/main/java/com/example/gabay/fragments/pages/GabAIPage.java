package com.example.gabay.fragments.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.gabay.R;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GabAIPage extends Fragment {

    private PreviewView cameraPreview;
    private View view;
    private ExecutorService cameraExecutor;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private int currentImageSize = 224;
    private final int numClasses = 5;

    private TextView predictionText, modelIndicatorText, instructionText;
    private Interpreter tfliteBasicSigns, tfliteFSLAlphabet, tfliteNumbers10;
    private Interpreter currentModel;
    private final String[] FSL_BasicSigns_labels = {"A", "C", "I", "MAHAL KITA", "N"}; //
    private final String[] FSL_alphabet_labels = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "J", "Z"};
    private final String[] FSL_numbers10_labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
    private int currentModelIndex = 0; // 0=Basic, 1=alphabet, 2=numbers
    private String[] currentLabels;
    private Button switchModelButton;

    // Frame rate limiting
    private long lastAnalysisTime = 0;
    private static final long MIN_TIME_BETWEEN_FRAMES = 200; // 5 FPS max
    private static final float CONFIDENCE_THRESHOLD = 0.6f;

    // Bounding box configuration
    private static final float CROP_PERCENTAGE = 0.5f; // use center 50% of the image

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = requireContext().getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_gabai_page, container, false);

        cameraPreview = view.findViewById(R.id.cameraPreview);
        predictionText = view.findViewById(R.id.predictionText);
        modelIndicatorText = view.findViewById(R.id.modelIndicatorText);
        instructionText = view.findViewById(R.id.instructionText);
        switchModelButton = view.findViewById(R.id.switchModelButton);
        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            Interpreter.Options options = new Interpreter.Options();
            tfliteBasicSigns = new Interpreter(loadModelFile("model_unquant.tflite"), options);
            tfliteFSLAlphabet = new Interpreter(loadModelFile("fsl_alphabet_model.tflite"), options);
            tfliteNumbers10 = new Interpreter(loadModelFile("fsl_numbers10_model.tflite"), options);

            debugModelInfo(tfliteBasicSigns, "Model 1 (Basic Signs)");
            debugModelInfo(tfliteFSLAlphabet, "Model 2 (Alphabet)");
            debugModelInfo(tfliteNumbers10, "Model 3 (Numbers)");

            if (tfliteBasicSigns != null && tfliteFSLAlphabet != null && tfliteNumbers10 != null) {
                switchToNextModel(); // Use the new 3-model method
                Log.d("TFLite", "All 3 models loaded successfully");
            } else {
                throw new IOException("One or more models failed to load");
            }

        } catch (IOException e) {
            Log.e("TFLite", "Model load failed: " + e.getMessage());
            predictionText.setText("Model load failed");
            Toast.makeText(requireContext(), "Failed to load AI model", Toast.LENGTH_LONG).show();
        }

        // Update button listener for 3 models
        switchModelButton.setOnClickListener(v -> {
            if (tfliteBasicSigns == null || tfliteFSLAlphabet == null || tfliteNumbers10 == null) {
                Toast.makeText(requireContext(), "Models not loaded properly", Toast.LENGTH_SHORT).show();
                return;
            }

            switchToNextModel();
        });

        return view;
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private Bitmap toBitmap(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) return null;

        Image.Plane[] planes = image.getPlanes();
        if (planes.length < 3) return null;

        try {
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            yBuffer.get(nv21, 0, ySize);
            uBuffer.get(nv21, ySize, uSize);
            vBuffer.get(nv21, ySize + uSize, vSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21,
                    image.getWidth(), image.getHeight(), null);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 80, out);
                byte[] imageBytes = out.toByteArray();
                return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            }
        } catch (Exception e) {
            Log.e("ImageConversion", "Error converting image", e);
            return null;
        }
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int imageSize = currentImageSize;

        // Calculate center crop dimensions
        int minDimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int cropSize = (int)(minDimension * CROP_PERCENTAGE);
        int left = (bitmap.getWidth() - cropSize) / 2;
        int top = (bitmap.getHeight() - cropSize) / 2;

        // Crop to bounding box area (center of image)
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, left, top, cropSize, cropSize);

        // Scale to model input size
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, imageSize, imageSize, true);

        // Apply transformations (rotation and flip for front camera)
        Matrix matrix = new Matrix();
        matrix.postRotate(270);
        matrix.postScale(-1, 1);

        Bitmap processedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        // Allocate buffer
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[imageSize * imageSize];
        processedBitmap.getPixels(pixels, 0, imageSize, 0, 0, imageSize, imageSize);

        // Normalize to [0, 1] range
        for (int pixelValue : pixels) {
            buffer.putFloat(((pixelValue >> 16) & 0xFF) / 255.0f);
            buffer.putFloat(((pixelValue >> 8) & 0xFF) / 255.0f);
            buffer.putFloat((pixelValue & 0xFF) / 255.0f);
        }

        // Cleanup bitmaps
        croppedBitmap.recycle();
        if (scaledBitmap != croppedBitmap) scaledBitmap.recycle();
        if (processedBitmap != scaledBitmap) processedBitmap.recycle();

        return buffer;
    }

    private int getMaxProbIndex(float[] probs) {
        if (probs == null || probs.length == 0) {
            return 0;
        }

        int maxIdx = 0;
        float maxProb = probs[0];
        for (int i = 1; i < probs.length; i++) {
            if (probs[i] > maxProb) {
                maxProb = probs[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    private void analyzeFrame(ImageProxy imageProxy) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAnalysisTime < MIN_TIME_BETWEEN_FRAMES) {
            imageProxy.close();
            return;
        }
        lastAnalysisTime = currentTime;

        if (currentModel == null) {
            Log.e("TFLite", "Current model is null!");
            imageProxy.close();
            return;
        }

        Bitmap bitmap = toBitmap(imageProxy);
        if (bitmap == null) {
            Log.e("TFLite", "Bitmap conversion failed");
            imageProxy.close();
            return;
        }

        try {
            ByteBuffer input = convertBitmapToByteBuffer(bitmap);
            float[][] output = new float[1][currentLabels.length];

            Log.d("TFLite", "Running inference with " + currentLabels.length + " classes");

            currentModel.run(input, output);

            requireActivity().runOnUiThread(() -> {
                int maxIdx = getMaxProbIndex(output[0]);
                float confidence = output[0][maxIdx];

                if (confidence > CONFIDENCE_THRESHOLD) {
                    predictionText.setText(currentLabels[maxIdx]);
//                    predictionText.setText(String.format("%s (%.1f%%)",
//                            currentLabels[maxIdx], confidence * 100));
                } else {
                    predictionText.setText("");
                }
            });

        } catch (Exception e) {
            Log.e("TFLite", "Inference error: " + e.getMessage(), e);
            requireActivity().runOnUiThread(() -> {
                predictionText.setText("Analysis error - check logs");
            });
        } finally {
            bitmap.recycle();
            imageProxy.close();
        }
    }

    private void switchToNextModel() {
        // Cycle through 0,1,2 and back to 0
        currentModelIndex = (currentModelIndex + 1) % 3;

        switch (currentModelIndex) {
            case 0: // basic signs
                currentModel = tfliteBasicSigns;
                currentLabels = FSL_BasicSigns_labels;
                currentImageSize = 224;
                modelIndicatorText.setText("Basic Signs Mode");
                instructionText.setText("Place your hand in the center box and show a basic sign");
                switchModelButton.setText("Switch model to Alphabet");
                Log.d("ModelSwitch", "Switched to Basic Signs - " + FSL_BasicSigns_labels.length + " labels");
                break;

            case 1: // fsl alphabet
                currentModel = tfliteFSLAlphabet;
                currentLabels = FSL_alphabet_labels;
                currentImageSize = 224;
                modelIndicatorText.setText("Alphabet Mode");
                instructionText.setText("Place your hand in the center box and show a letter");
                switchModelButton.setText("Switch model to Numbers");
                Log.d("ModelSwitch", "Switched to Alphabet - " + FSL_alphabet_labels.length + " labels");
                break;

            case 2: // numbers 1-10
                currentModel = tfliteNumbers10;
                currentLabels = FSL_numbers10_labels;
                currentImageSize = 224;
                modelIndicatorText.setText("Numbers Mode");
                instructionText.setText("Place your hand in the center box and show a number");
                switchModelButton.setText("Switch model to Basic Signs");
                Log.d("ModelSwitch", "Switched to Numbers - " + FSL_numbers10_labels.length + " labels");
                break;
        }

        // Verify the model input shape
        if (currentModel != null) {
            try {
                int[] inputShape = currentModel.getInputTensor(0).shape();
                int[] outputShape = currentModel.getOutputTensor(0).shape();
                Log.d("ModelSwitch", "Expected input shape: " + Arrays.toString(inputShape));
                Log.d("ModelSwitch", "Expected output shape: " + Arrays.toString(outputShape));
            } catch (Exception e) {
                Log.e("ModelSwitch", "Error getting model shapes: " + e.getMessage());
            }
        }

        Log.d("ModelSwitch", "Current image size: " + currentImageSize);
        predictionText.setText("...");
        lastAnalysisTime = 0;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void debugModelInfo(Interpreter model, String modelName) {
        try {
            int inputTensorCount = model.getInputTensorCount();
            int outputTensorCount = model.getOutputTensorCount();

            Log.d("ModelDebug", "=== " + modelName + " ===");
            Log.d("ModelDebug", "Input tensors: " + inputTensorCount);
            Log.d("ModelDebug", "Output tensors: " + outputTensorCount);

            for (int i = 0; i < inputTensorCount; i++) {
                int[] inputShape = model.getInputTensor(i).shape();
                DataType inputDataType = model.getInputTensor(i).dataType();
                Log.d("ModelDebug", "Input " + i + " shape: " + Arrays.toString(inputShape) +
                        ", type: " + inputDataType);
            }

            for (int i = 0; i < outputTensorCount; i++) {
                int[] outputShape = model.getOutputTensor(i).shape();
                DataType outputDataType = model.getOutputTensor(i).dataType();
                Log.d("ModelDebug", "Output " + i + " shape: " + Arrays.toString(outputShape) +
                        ", type: " + outputDataType);
            }
        } catch (Exception e) {
            Log.e("ModelDebug", "Error debugging model: " + e.getMessage());
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);

                Log.d("CameraX", "Camera started successfully");

            } catch (Exception e) {
                Log.e("CameraX", "Error starting camera: " + e.getMessage(), e);
                Toast.makeText(requireContext(), "Error starting camera", Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onPause() {
        super.onPause();
        stopCameraSafely();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCameraSafely();

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }

        // Close all models
        closeModelSafely(tfliteBasicSigns);
        closeModelSafely(tfliteFSLAlphabet);
        closeModelSafely(tfliteNumbers10);
        closeModelSafely(currentModel);

        Log.d("GabAIPage", "All 3 models closed");
    }

    private void closeModelSafely(Interpreter model) {
        if (model != null) {
            try {
                model.close();
            } catch (Exception e) {
                Log.e("TFLite", "Error closing model", e);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopCameraSafely();
    }

    private void stopCameraSafely() {
        if (!isAdded()) return;
        try {
            ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get();
            cameraProvider.unbindAll();
            Log.d("CameraX", "Camera stopped safely");
        } catch (Exception e) {
            Log.e("CameraX", "Error unbinding camera: " + e.getMessage());
        }
    }
}