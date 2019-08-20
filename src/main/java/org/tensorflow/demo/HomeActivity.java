package org.tensorflow.demo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;


import org.tensorflow.demo.helper.BitmapUtil;
import org.tensorflow.demo.helper.CameraPreview;
import org.tensorflow.demo.helper.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;


public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    /*private final static DisplayImageOptions DEFAULT_IMAGE_LOADER_OPTIONS =
            new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .showImageOnLoading(null)
                    .resetViewBeforeLoading(true)
                    .build();*/

    private static final String TAG = "TAG";
    private static final int PERMISSION_CAMERA = 1;
    private static final int CAMERA_PIC_REQUEST = 2;
    private static final int SELECT_GALLERY_IMAGE = 3;
    private static final int REQUEST_IMAGE_CAPTURE = 4;
    private Camera mCamera = null;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;
    private LinearLayout btnsetting, btnFlash, btnSwitchCam, btnStorage, btnCapture, btnBlur;
    private Context myContext;
    private ProgressBar progressBar;
    private boolean flash = false;
    private CircleImageView imgThumb;
    private ImageView imgFlash;
    private LinearLayout cameraPreview;
    private boolean cameraFront = false;
    public static Bitmap bitmap;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnsetting = this.findViewById(R.id.btn_setting);
        btnSwitchCam=this.findViewById(R.id.btn_switch_cam);
        btnFlash = this.findViewById(R.id.btn_flash);
        btnStorage = this.findViewById(R.id.btn_storage);
        btnBlur = this.findViewById(R.id.btn_blur);
        imgFlash = this.findViewById(R.id.imgFlash);
        progressBar=this.findViewById(R.id.pr_process);
        btnCapture = findViewById(R.id.btn_capture);
        imgThumb = findViewById(R.id.imgThumb);
        btnSwitchCam = findViewById(R.id.btn_switch_cam);


        btnBlur.setOnClickListener(this);
        btnsetting.setOnClickListener(this);
        btnSwitchCam.setOnClickListener(this);
        btnFlash.setOnClickListener(this);
        btnStorage.setOnClickListener(this);

        checkPermission();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        ArrayList<String> images = BitmapUtil.getAllShownImagesPath(HomeActivity.this);
        try {
            if(images.size()>0){
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.fromFile(new File(images.get(images.size() - 1)))));
                imgThumb.setImageBitmap(bitmap);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }



        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                mCamera.takePicture(null, null, mPicture);

            }
        });


        btnSwitchCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get the number of cameras
                int camerasNumber = Camera.getNumberOfCameras();
                if (camerasNumber > 1) {
                    //release the old camera instance
                    //switch camera, from the front and the back and vice versa

                    releaseCamera();
                    chooseCamera();
                } else {

                }
            }
        });


    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CAMERA);
        } else {
            Log.d(TAG, "checkPermission: vao 1");
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
            cameraPreview = findViewById(R.id.cPreview);
            if (mPreview == null) {
                mPreview = new CameraPreview(this, mCamera);
            }
            mPreview = new CameraPreview(this, mCamera);
            cameraPreview.addView(mPreview);
            mCamera.startPreview();

            mCamera.setDisplayOrientation(90);
            mPicture = getPictureCallback();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: vao day");
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
        cameraPreview = findViewById(R.id.cPreview);
        if (mPreview == null) {
            mPreview = new CameraPreview(this, mCamera);
        }
        mPreview = new CameraPreview(this, mCamera);
        cameraPreview.addView(mPreview);
        mCamera.startPreview();
        mCamera.setDisplayOrientation(90);
        mPicture = getPictureCallback();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


    }

    private int findFrontFacingCamera() {

        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;

    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;

            }

        }
        return cameraId;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onResume() {

        super.onResume();
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onResume: da check");
            if (mCamera == null) {

                if (mPreview == null) {
                    mPreview = new CameraPreview(this, mCamera);
                }


                Log.d(TAG, "onResume: vao 2");
                mCamera = Camera.open();
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();

                mPreview.refreshCamera(mCamera);
                Log.d("nu", "null");
            } else {
                Log.d("nu", "no null????");
            }
        } else {
            Log.d("nu", "no null");
        }


    }

    public void chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview
                mCamera = Camera.open(cameraId);
                mCamera.setDisplayOrientation(90);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Camera.PictureCallback getPictureCallback() {
        Camera.PictureCallback picture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                if(cameraFront){
                    bitmap=BitmapUtil.rotation(bitmap,-90);
                }else{
                    bitmap=BitmapUtil.rotation(bitmap,90);
                }

                imgThumb.setImageBitmap(bitmap);


                Intent intent = new Intent(HomeActivity.this, PictureActivity.class);
                intent.putExtra(Constants.CHECK, 1);
                startActivity(intent);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                },1000);


            }
        };
        return picture;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_blur:
                loadImageBlur();
                break;
            case R.id.btn_storage:
                loadImageFromStorage();
                break;
            case R.id.btn_flash:
                onFlash();
                break;
            case R.id.btn_setting:
                onSetting();
                break;
            default:
                break;
        }
    }

    private void onSetting() {
        Toast.makeText(myContext, "Setting", Toast.LENGTH_SHORT).show();
    }

    private void loadImageBlur() {

    }

    private void onFlash() {
        flash = !flash;
        if (flash) {
            flashLightOn();
        } else {
            flashLightOff();
        }
    }

    private void loadImageFromStorage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_GALLERY_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        if (requestCode == SELECT_GALLERY_IMAGE) {
            bitmap = BitmapUtil.getBitmapFromGallery(this, data.getData());
          //  bitmap=BitmapUtil.rotation(bitmap);
            Intent intent = new Intent(HomeActivity.this, PictureActivity.class);
            intent.putExtra(Constants.CHECK, 1);
            startActivity(intent);
        }
        if (requestCode == CAMERA_PIC_REQUEST) {
            bitmap = (Bitmap) data.getExtras().get("data");

        }
        if (bitmap != null)
            try {
              //  Bitmap segmented = BlurImage.getBluredImage(bitmap, getApplicationContext(), Constants.BLUR);
                //    imgData.setImageBitmap(segmented);
            } catch (Exception e) {
                e.printStackTrace();
            }

    }

    public void flashLightOn() {

        try {
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                // mCamera = Camera.open();
                Camera.Parameters p = mCamera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                mCamera.setParameters(p);
                imgFlash.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.flashauto));
                //mCamera.startPreview();

            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Exception flashLightOn()",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void flashLightOff() {
        try {
            if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                Camera.Parameters p = mCamera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(p);
                imgFlash.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.flashoff));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Exception flashLightOff",
                    Toast.LENGTH_SHORT).show();
        }
    }



}

