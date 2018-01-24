package org.tensorflow.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DetectSourceActivity extends AppCompatActivity implements View.OnClickListener, ImageShowAdapter.IClick, ItemClick {

    private static final Logger LOGGER = new Logger();
    private static final int OPENIMAGE = 1;
    private Classifier detector;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE = "file:///android_asset/biensoxemay.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/biensoxemay.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.2f;
    private static final boolean MAINTAIN_ASPECT = false;
    private Integer sensorOrientation = 0;
    private ImageView imgDetect, imgCut;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private Handler handler;
    private HandlerThread handlerThread;
    private ImageShowAdapter adapterShow;
    private RecyclerView lvImage;
    private ArrayList<Uri> images;
    private Button btnChoose, btnSave;
    private RecyclerView rcImageDetect;
    private ArrayList<Bitmap> imageDetects;
    private DetectImageAdapter detectImageAdapter;
    private Boolean isCheckSave=false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_source);
        LOGGER.d("onCreate " + this);
        btnChoose = findViewById(R.id.btn_choose_folder);
        lvImage = findViewById(R.id.rc_images);
        imgDetect = findViewById(R.id.img_show);
        imgCut = findViewById(R.id.img_show_cut);
        rcImageDetect = findViewById(R.id.rc_images_detect);
        btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(this);
        btnChoose.setOnClickListener(this);
        images = new ArrayList<>();
        adapterShow = new ImageShowAdapter(images, this, this);
        lvImage.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        lvImage.setAdapter(adapterShow);

        imageDetects = new ArrayList<>();
        detectImageAdapter = new DetectImageAdapter(imageDetects, this);
        rcImageDetect.setLayoutManager(new GridLayoutManager(this, 2));
        rcImageDetect.setAdapter(detectImageAdapter);


        try {
            initTensorflow();
//            initViews();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void initTensorflow() throws Exception {
        detector = TensorFlowObjectDetectionAPIModel.create(
                getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);

    }

    private void detect(Bitmap bitmap) {
        final Bitmap mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int previewWidth = mutable.getWidth();
        int previewHeight = mutable.getHeight();
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        final Bitmap cropedBitmap = Bitmap.createScaledBitmap(mutable, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, false);
       /* runInBackground(new Runnable() {
            @Override
            public void run() {*/
        final long startTime = SystemClock.uptimeMillis();
        List<Classifier.Recognition> results = detector.recognizeImage(cropedBitmap);
        long time = SystemClock.uptimeMillis() - startTime;
        System.out.println("Time procs : " + time);
        Canvas canvas = new Canvas(mutable);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);
        for (final Classifier.Recognition result : results) {
            if (result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                RectF location = result.getLocation();
                cropToFrameTransform.mapRect(location);
                result.setLocation(location);
//                        System.out.println(result);
                canvas.drawRect(location, paint);
                int x = (int) location.left;
                int y = (int) location.top;
                int width = (int) (location.right - location.left);
                int height = (int) (location.bottom - location.top);
                final Bitmap cutBitmap = cutBitmap(mutable, x, y, width, height);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // imgResult.setImageBitmap(cutBitmap);
                        imgCut.setImageBitmap(cutBitmap);
                        imageDetects.add(cutBitmap);
                        detectImageAdapter.notifyDataSetChanged();
                    }
                });

            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imgDetect.setImageBitmap(mutable);
            }


        });

    }



    private Bitmap cutBitmap(Bitmap originalBitmap, int x, int y, int width, int height) {
        Bitmap cutBitmap = Bitmap.createBitmap(width,
                height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cutBitmap);
        Rect srcRect = new Rect(x, y, x + width, y + height);
        Rect desRect = new Rect(0, 0, width, height);
        canvas.drawBitmap(originalBitmap, srcRect, desRect, null);
        return cutBitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPENIMAGE) {
            images.clear();
            System.out.println(data.getData().toString());
            try {
                DocumentFile df = DocumentFile.fromTreeUri(getApplicationContext(), Uri.parse(data.getData().toString().replace("/document/", "/tree/")));
                DocumentFile[] files = df.listFiles();
                for (int i = 0; i < files.length; i++) {
                    File file = new File(files[i].getUri().toString());
                    System.out.println(file.getAbsolutePath());
                    System.out.println(files[i].getUri() + "," + files[i].getName());
                    // if (files[i].getName().endsWith(".png") || files[i].getName().endsWith(".jpg")) {
                    images.add(files[i].getUri());
                    // }

                    adapterShow.notifyDataSetChanged();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        /*handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
       */// initViews();
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

       /* if (!isFinishing()) {
            LOGGER.d("Requesting finish");
            finish();
        }

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }*/

        super.onPause();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_choose_folder:
                chooseFolderImage();
                break;
            case R.id.btn_save:
                if(isCheckSave){
                    saveImageCut(imageDetects);
                }
                isCheckSave=false;

                Toast.makeText(this,"Save success",Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }

    }

    private void saveImageCut(ArrayList<Bitmap> imageDetects) {
        for (Bitmap bitmap : imageDetects) {
            File file = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM +  "/" + System.currentTimeMillis() + ".png");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileOutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    private void chooseFolderImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(intent, OPENIMAGE);
    }

    @Override
    public void itemClick(int position) {
        //Bitmap bitmap= Glide.with(this).load(images.get(position)).asBitmap().into(300,300).get();
        //detect(bitmap);
        btnSave.setVisibility(View.VISIBLE);
        imageDetects.clear();
        isCheckSave=true;
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), images.get(position));
            imgDetect.setImageBitmap(bitmap);
            detect(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void click(int possition) {
        Toast.makeText(this, possition + "", Toast.LENGTH_SHORT).show();
    }
}

