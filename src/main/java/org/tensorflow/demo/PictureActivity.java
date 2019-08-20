package org.tensorflow.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.helper.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PictureActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView imageView, imgView;
    private SeekBar sbBlur;
    private TextView txtDepth;
    private ProgressBar progressBar;
    private LinearLayout llP1, llP2;
    private static final String IMAGE_DIRECTORY = "/CustomImage";
    private Bitmap bitmap;
    private int check;
    Bitmap bitmapBlur = null;
    private static final Logger LOGGER = new Logger();
    private static final int OPENIMAGE = 1;
    private Classifier detector;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final String TF_OD_API_MODEL_FILE = "file:///android_asset/biensoxemay.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/biensoxemay.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.2f;
    private static final boolean MAINTAIN_ASPECT = false;
    private Integer sensorOrientation = 0;
    private ImageView  imgCut;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private Handler handler;
    private HandlerThread handlerThread;
    private Boolean isCheckSave=false;
    private String title;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        try {
            initTensorflow();
//            initViews();
        } catch (Exception e) {
            e.printStackTrace();
        }
        getSupportActionBar().setTitle("Ảnh");
        imgView = this.findViewById(R.id.img_view);
        imgCut = findViewById(R.id.img);
        llP1 = this.findViewById(R.id.ll_p1);
        llP2 = this.findViewById(R.id.ll_p2);
        txtDepth = this.findViewById(R.id.txt_depth);
        progressBar = this.findViewById(R.id.pr_process);


        bitmap = HomeActivity.bitmap;

        Intent intent = getIntent();
        check = intent.getIntExtra(Constants.CHECK, 0);
        if (check == 1) {
            llP1.setVisibility(View.VISIBLE);
            llP2.setVisibility(View.GONE);

            //saveImage(bitmapBlur);
        } else {
            llP1.setVisibility(View.GONE);
            llP2.setVisibility(View.VISIBLE);
            String link = intent.getStringExtra(Constants.LINKIMAGE);
            try {
                Bitmap bitmapTemp = BitmapFactory.decodeStream(this.getContentResolver().openInputStream(Uri.fromFile(new File(link))));
                imgCut.setImageBitmap(bitmapTemp);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
        imgCut.setImageBitmap(bitmap);

        //imageView.setImageBitmap(resizedBitmap);
        //  Toast.makeText(this, resizedBitmap.getWidth() + " ; " + resizedBitmap.getHeight(), Toast.LENGTH_SHORT).show();
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
                txtDepth.setText(result.getTitle());
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

                    }
                });

            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imgCut.setImageBitmap(mutable);
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
    private Bitmap showImage(final int blur, final Bitmap bitmap) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    //   progressBar.setVisibility(View.VISIBLE);
                  //  bitmapBlur = BlurImage.getBluredImage(bitmap, PictureActivity.this, blur);
                    //Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmapBlur, 2000, 2000, false);
                    imgCut.setImageBitmap(bitmap);
                    //  progressBar.setVisibility(View.GONE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return bitmapBlur;
    }

    public String saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // have the object build the directory structure, if needed.

        if (!wallpaperDirectory.exists()) {
            Log.d("dirrrrrr", "" + wallpaperDirectory.mkdirs());
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();   //give read write permission
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Toast.makeText(this, "Save Image success", Toast.LENGTH_SHORT).show();
          //  startActivity(new Intent(this, ListImageActivity.class));
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";

    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (check == 1) {
            getMenuInflater().inflate(R.menu.menu, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.btn_save) {
            detect(bitmap);

        }
        return super.onOptionsItemSelected(item);
    }
}