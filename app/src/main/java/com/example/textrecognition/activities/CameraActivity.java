package com.example.textrecognition.activities;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.pm.PackageInfoCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.textrecognition.R;
import com.example.textrecognition.processors.ImageProcessor;
import com.example.textrecognition.processors.MobileVisionProcessor;
import com.example.textrecognition.utils.TempString;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;


import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;

public class CameraActivity extends AppCompatActivity {

    private Bitmap processedBitmap;
    private static final String TAG = "AndroidCameraApi";
    private Button btn_deviceScan,btn_cloudScan, btn_MobileVision;
    Switch sFlash;
    private TextureView textureView;
    View rectangleView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    private Camera cam;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Boolean checkDeviceBtn = false;
    private Boolean checkCloudBtn = false;
    private Boolean checkMobileVisionBtn = false;
    private static final String HOSTED_MODEL_NAME = "cloud_model_1";
    private static final String LOCAL_MODEL_ASSET = "mobilenet_v1_1.0_224_quant.tflite";
    private static final int RESULTS_TO_SHOW = 3;
    Boolean mFlash = false;
    Boolean mFocus = false;
    MeteringRectangle focusAreaTouch = null;
    private HandlerThread mBackgroundThread;
    private Handler handler;
    private Runnable runnable;
    public Handler mBackgroundHandler;

    public CameraActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAppIdleTimeout();
        setContentView(R.layout.activity_camera_activity);
        FirebaseApp.initializeApp(this);
        textureView = (TextureView) findViewById(R.id.texture);
        rectangleView = findViewById(R.id.myRectangleView);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        btn_deviceScan = (Button) findViewById(R.id.btn_devicePicture);
        btn_cloudScan = (Button) findViewById(R.id.btn_cloudPicture);
        btn_MobileVision = (Button) findViewById(R.id.btn_MobileVision);
        sFlash = (Switch) findViewById(R.id.flash_switch);
        //assert btn_deviceScan != null;
        textureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    int x = v.getWidth();
                    int y = v.getHeight();
                    //int a = sensorArraySize.height();
                    manualFocus(v,event);
                    // Do what you want
                    return true;
                }
                return false;
            }
        });
        sFlash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    mFlash = true;
                }
                else {
                    mFlash = false;
                }
                cameraDevice.close();
                if (textureView.isAvailable()) {
                    openCamera();
                } else {
                    textureView.setSurfaceTextureListener(textureListener);
                }
            }
        });
        btn_deviceScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkDeviceBtn = true;
                takePicture();
            }
        });
        btn_MobileVision.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkMobileVisionBtn = true;
                takePicture();

            }
        });
        btn_cloudScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCloudBtn = true;
                takePicture();

            }
        });

    }

    private void setAppIdleTimeout() {

        handler = new Handler();
        runnable = new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        finish();
                    }
                });
            }
        };
        handler.postDelayed(runnable, 120000);
    }

    //reset timer on user interaction and in onResume
    public void resetAppIdleTimeout() {
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, 120000);
    }

    /*@Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        resetAppIdleTimeout();
    }*/

    @Override
    public void onUserInteraction() {
        // TODO Auto-generated method stub
        Log.i(TAG, "interacted");
        resetAppIdleTimeout();
        super.onUserInteraction();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        handler.removeCallbacks(runnable);
        super.onDestroy();
    }
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();

        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            createCameraPreview();
        }
    };

    private void manualFocus(View view, MotionEvent motionEvent)
    {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            final Rect sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            //TODO: here I just flip x,y, but this needs to correspond with the sensor orientation (via SENSOR_ORIENTATION)
            final int y = (int)((motionEvent.getX() / (float)view.getWidth())  * (float)sensorArraySize.height());
            final int x = (int)((motionEvent.getY() / (float)view.getHeight()) * (float)sensorArraySize.width());
            final int halfTouchWidth  = 5; //(int)motionEvent.getTouchMajor(); //TODO: this doesn't represent actual touch size in pixel. Values range in [3, 10]...
            final int halfTouchHeight = 5; //(int)motionEvent.getTouchMinor();
            MeteringRectangle focusAreaTouch = new MeteringRectangle(Math.max(x - halfTouchWidth,  0),
                    Math.max(y - halfTouchHeight, 0),
                    halfTouchWidth  * 2,
                    halfTouchHeight * 2,
                    MeteringRectangle.METERING_WEIGHT_MAX - 1);
            /*MeteringRectangle focusAreaTouch = new MeteringRectangle((int) motionEvent.getX() - 50,
                    (int) motionEvent.getY() - 50,
                    100,
                    100,
                    MeteringRectangle.METERING_WEIGHT_MAX - 1);*/
            CameraCaptureSession.CaptureCallback captureCallbackHandler = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //mManualFocusEngaged = false;

                    if (request.getTag() == "FOCUS_TAG") {
                        //the focus trigger is complete -
                        //resume repeating (preview surface will get frames), clear AF trigger
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);
                        try {
                            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                        }
                        catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    Log.e(TAG, "Manual AF failure: " + failure);
                    //mManualFocusEngaged = false;
                }
            };

            //first stop the existing repeating request
            cameraCaptureSessions.stopRepeating();

            //cancel any existing AF trigger (repeated touches, etc.)
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            cameraCaptureSessions.capture(captureRequestBuilder.build(), captureCallbackHandler, mBackgroundHandler);

            //Now add a new AF trigger with focus region
            if (true/*isMeteringAreaAFSupported()*/) {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaTouch});
            }
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            captureRequestBuilder.setTag("FOCUS_TAG"); //we'll capture this later for resuming the preview

            //then we ask for a single request (not repeating!)
            cameraCaptureSessions.capture(captureRequestBuilder.build(), captureCallbackHandler, mBackgroundHandler);
            //mManualFocusEngaged = true;

        }catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            if(mFlash) {
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            }
            else {
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }

            if(focusAreaTouch != null)
            {

            }

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 0;
            int height = 0;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            if(mFlash) {
                captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            }
            else {
                captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireNextImage();
                        long u = PackageInfoCompat.getLongVersionCode(getPackageManager().getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0 ));
                        ImageProcessor imageProcessor = new ImageProcessor(image);
                        processedBitmap = imageProcessor.processImage();
                        runScanner();
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();

                        }
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener, null);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void runScanner()
    {
        if(checkMobileVisionBtn){
            checkMobileVisionBtn = false;
            runMobileVisionRecognition();
        }

        else {
            initCustomModel();
        }
    }
    private void runMobileVisionRecognition() {

        Frame imageFrame = new Frame.Builder()
                .setBitmap(processedBitmap)
                .build();
        Context context = CameraActivity.this;
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Toast.makeText(this, "Google Service not Present", Toast.LENGTH_LONG).show();
        }
        else{
            textRecognizer.setProcessor(new MobileVisionProcessor(context));
            textRecognizer.receiveFrame(imageFrame);
            startActivity(new Intent(CameraActivity.this,ConfirmationActivity.class));
            finish();
        }
    }

    private void initCustomModel() {

        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions
                .Builder()
                .requireWifi()
                .build();

        FirebaseRemoteModel remoteModel = new FirebaseRemoteModel.Builder
                (HOSTED_MODEL_NAME)
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build();

        FirebaseLocalModel localModel =
                new FirebaseLocalModel.Builder("asset")
                        .setAssetFilePath(LOCAL_MODEL_ASSET).build();

        FirebaseModelManager manager = FirebaseModelManager.getInstance();
        manager.registerRemoteModel(remoteModel);
        manager.registerLocalModel(localModel);

        if(checkDeviceBtn) {
            checkDeviceBtn = false;
            runTextRecognition();
        }

        if(checkCloudBtn) {
            checkCloudBtn = false;
            runCloudTextRecognition();
        }
    }

    private void runTextRecognition() {
        btn_deviceScan.setEnabled(false);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(processedBitmap);
        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        recognizer.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                btn_deviceScan.setEnabled(true);
                                String text = texts.getText();
                                TempString.getInstance().saveTextDetails(text);
                                startActivity(new Intent(CameraActivity.this, ConfirmationActivity.class));
                                finish();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@android.support.annotation.NonNull Exception e) {
                                // Task failed with an exception
                                btn_deviceScan.setEnabled(true);
                                Toast.makeText(CameraActivity.this, "Device based can't Recognize Text", Toast.LENGTH_SHORT).show();
                                createCameraPreview();
                                e.printStackTrace();
                            }
                        });
    }

    private void runCloudTextRecognition() {
        btn_cloudScan.setEnabled(false);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(processedBitmap);
        FirebaseVisionDocumentTextRecognizer recognizer = FirebaseVision.getInstance()
                .getCloudDocumentTextRecognizer();
        recognizer.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionDocumentText>() {
                            @Override
                            public void onSuccess(FirebaseVisionDocumentText texts) {
                                btn_cloudScan.setEnabled(true);
                                String text = texts.getText();
                                //String[] lines = text.split("\\r?\\n");
                                TempString.getInstance().saveTextDetails(text);
                                startActivity(new Intent(CameraActivity.this,ConfirmationActivity.class));
                                finish();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@android.support.annotation.NonNull Exception e) {
                                // Task failed with an exception
                                btn_cloudScan.setEnabled(true);
                                Toast.makeText(CameraActivity.this, "Cloud based can't Recognize Text", Toast.LENGTH_SHORT).show();
                                createCameraPreview();
                                e.printStackTrace();
                            }
                        });
    }



    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    protected void updatePreview() {

        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(CameraActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        resetAppIdleTimeout();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        super.onPause();
    }

    /*@Override
    public void onBackPressed() {
        if(TransactionHUB.bIsIrisRequest)
            IrisPadComm.GetInstance().sendMessage(IrisPadComm.RESPONSE_FOR_OCR_TO_IRIS, 0, 0, 0);
        finish();
    }*/


}