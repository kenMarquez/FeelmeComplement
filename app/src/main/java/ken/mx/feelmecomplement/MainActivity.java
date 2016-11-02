package ken.mx.feelmecomplement;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.android.multistateanimation.MultiStateAnimation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import ken.mx.feelmecomplement.cloudinary.CloudinaryCallback;
import ken.mx.feelmecomplement.cloudinary.CloudinaryInstance;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

public class MainActivity extends AppCompatActivity implements MultiStateAnimation.AnimationSeriesListener {

    public static final String FILE_NAME = "temp.jpg";
    private MultiStateAnimation mAnimation;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.tv);
        ImageView animationView = (ImageView) findViewById(R.id.imageAnimation);
        mAnimation = makeAnimation(animationView);
        mAnimation.setSeriesAnimationFinishedListener(this);
        mAnimation.transitionNow("pending");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder
                        .setMessage("Selecciona una opción")
                        .setPositiveButton("Galeria", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startGalleryChooser();
                            }
                        })
                        .setNegativeButton("Camera", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startCamera();
                            }
                        });
                builder.create().show();
            }
        });
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                1,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
//            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getCameraFile()));
//            startActivityForResult(intent, 1);
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            startActivityForResult(intent, 1);
        }
    }

    public File getCameraFile() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }


    public void startGalleryChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                2);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
//            uploadImage(data.getData());
        } else if (requestCode == 1 && resultCode == RESULT_OK) {
//            uploadImage(Uri.fromFile(getCameraFile()));
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            upluadImageCamera(data);
        }
    }

    private void upluadImageCamera(Intent data) {

        Bundle extras = data.getExtras();
        Bitmap imageBitmap = (Bitmap) extras.get("data");
        tv.setText("Uploading");
        mAnimation.queueTransition("loading");

        CloudinaryInstance.uploadImage(getByteArrayFromBitmap(imageBitmap), new CloudinaryCallback() {
            @Override
            public void onCloudinaryUploadSucces(String link) {

                tv.setText("Uploaded");
                mAnimation.queueTransition("finished");
                log(link);

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("https://feelmepush.herokuapp.com")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                Api api = retrofit.create(Api.class);
                api.notifyUrl(link).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        tv.setText("notify");
                        mAnimation.queueTransition("pending");
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        log(t.getMessage());
                        tv.setText("error");
                        mAnimation.queueTransition("pending");
                    }
                });

            }

            @Override
            public void onCloudinaryUploadError() {
                Toast.makeText(MainActivity.this, "Lo sentimos hubo un error al subir tu imagen", Toast.LENGTH_SHORT).show();
                mAnimation.queueTransition("pending");
            }
        });
    }

    public ByteArrayInputStream getByteArrayFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();
        return new ByteArrayInputStream(bitmapdata);
    }

    private void uploadImage(Uri data) {

    }

    public void log(String contetn) {
        Log.e("myLog", contetn);
    }

    public interface Api {

        @GET("/push")
        Call<ResponseBody> notifyUrl(@Query("photo") String url);

    }

    @Override
    public void onAnimationFinished() {

    }

    @Override
    public void onAnimationStarting() {

    }

    private MultiStateAnimation makeAnimation(View view) {
        MultiStateAnimation.SectionBuilder startSection = new MultiStateAnimation.SectionBuilder("pending")
                .setOneshot(true)
                .addFrame(R.mipmap.pending_animation_000);

        MultiStateAnimation.SectionBuilder endSection = new MultiStateAnimation.SectionBuilder("finished")
                .setOneshot(true)
                .addFrame(R.mipmap.pending_animation_099);

        MultiStateAnimation.SectionBuilder loadingSection = new MultiStateAnimation.SectionBuilder("loading")
                .setOneshot(false)
                .setFrameDuration(33)
                .addFrame(R.mipmap.pending_animation_001)
                .addFrame(R.mipmap.pending_animation_002)
                .addFrame(R.mipmap.pending_animation_003)
                .addFrame(R.mipmap.pending_animation_004)
                .addFrame(R.mipmap.pending_animation_005)
                .addFrame(R.mipmap.pending_animation_006)
                .addFrame(R.mipmap.pending_animation_007)
                .addFrame(R.mipmap.pending_animation_008)
                .addFrame(R.mipmap.pending_animation_009)
                .addFrame(R.mipmap.pending_animation_010)
                .addFrame(R.mipmap.pending_animation_011)
                .addFrame(R.mipmap.pending_animation_012)
                .addFrame(R.mipmap.pending_animation_013)
                .addFrame(R.mipmap.pending_animation_014)
                .addFrame(R.mipmap.pending_animation_015)
                .addFrame(R.mipmap.pending_animation_016)
                .addFrame(R.mipmap.pending_animation_017)
                .addFrame(R.mipmap.pending_animation_018)
                .addFrame(R.mipmap.pending_animation_019)
                .addFrame(R.mipmap.pending_animation_020)
                .addFrame(R.mipmap.pending_animation_021)
                .addFrame(R.mipmap.pending_animation_022)
                .addFrame(R.mipmap.pending_animation_023)
                .addFrame(R.mipmap.pending_animation_024)
                .addFrame(R.mipmap.pending_animation_025)
                .addFrame(R.mipmap.pending_animation_026)
                .addFrame(R.mipmap.pending_animation_027)
                .addFrame(R.mipmap.pending_animation_028)
                .addFrame(R.mipmap.pending_animation_029)
                .addFrame(R.mipmap.pending_animation_030)
                .addFrame(R.mipmap.pending_animation_031)
                .addFrame(R.mipmap.pending_animation_032)
                .addFrame(R.mipmap.pending_animation_033)
                .addFrame(R.mipmap.pending_animation_034)
                .addFrame(R.mipmap.pending_animation_035)
                .addFrame(R.mipmap.pending_animation_036)
                .addFrame(R.mipmap.pending_animation_037)
                .addFrame(R.mipmap.pending_animation_038)
                .addFrame(R.mipmap.pending_animation_039)
                .addFrame(R.mipmap.pending_animation_040)
                .addFrame(R.mipmap.pending_animation_041)
                .addFrame(R.mipmap.pending_animation_042)
                .addFrame(R.mipmap.pending_animation_043)
                .addFrame(R.mipmap.pending_animation_044)
                .addFrame(R.mipmap.pending_animation_045)
                .addFrame(R.mipmap.pending_animation_046)
                .addFrame(R.mipmap.pending_animation_047)
                .addFrame(R.mipmap.pending_animation_048)
                .addFrame(R.mipmap.pending_animation_049)
                .addFrame(R.mipmap.pending_animation_050)
                .addFrame(R.mipmap.pending_animation_051)
                .addFrame(R.mipmap.pending_animation_052)
                .addFrame(R.mipmap.pending_animation_053)
                .addFrame(R.mipmap.pending_animation_054)
                .addFrame(R.mipmap.pending_animation_055)
                .addFrame(R.mipmap.pending_animation_056)
                .addFrame(R.mipmap.pending_animation_057)
                .addFrame(R.mipmap.pending_animation_058)
                .addFrame(R.mipmap.pending_animation_059)
                .addFrame(R.mipmap.pending_animation_060)
                .addFrame(R.mipmap.pending_animation_061)
                .addFrame(R.mipmap.pending_animation_062)
                .addFrame(R.mipmap.pending_animation_063)
                .addFrame(R.mipmap.pending_animation_064)
                .addFrame(R.mipmap.pending_animation_065)
                .addFrame(R.mipmap.pending_animation_066)
                .addFrame(R.mipmap.pending_animation_067)
                .addFrame(R.mipmap.pending_animation_068)
                .addFrame(R.mipmap.pending_animation_069)
                .addFrame(R.mipmap.pending_animation_070)
                .addFrame(R.mipmap.pending_animation_071)
                .addFrame(R.mipmap.pending_animation_072)
                .addFrame(R.mipmap.pending_animation_073)
                .addFrame(R.mipmap.pending_animation_074)
                .addFrame(R.mipmap.pending_animation_075)
                .addFrame(R.mipmap.pending_animation_076)
                .addFrame(R.mipmap.pending_animation_077)
                .addFrame(R.mipmap.pending_animation_078)
                .addFrame(R.mipmap.pending_animation_079)
                .addFrame(R.mipmap.pending_animation_080)
                .addFrame(R.mipmap.pending_animation_081)
                .addFrame(R.mipmap.pending_animation_082)
                .addFrame(R.mipmap.pending_animation_083)
                .addFrame(R.mipmap.pending_animation_084)
                .addFrame(R.mipmap.pending_animation_085)
                .addFrame(R.mipmap.pending_animation_086)
                .addFrame(R.mipmap.pending_animation_087)
                .addFrame(R.mipmap.pending_animation_088)
                .addFrame(R.mipmap.pending_animation_089)
                .addFrame(R.mipmap.pending_animation_090);

        return new MultiStateAnimation.Builder(view)
                .addSection(startSection)
                .addSection(loadingSection)
                .addSection(endSection)
                .build(this);
    }

}
