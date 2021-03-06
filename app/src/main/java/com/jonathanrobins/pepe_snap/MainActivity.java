package com.jonathanrobins.pepe_snap;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.microedition.khronos.opengles.GL10;


public class MainActivity extends ActionBarActivity {

    private Button cameraButton;
    private Button galleryButton;
    private ImageView mainImage;
    private Button secretButton;
    private final int REQUEST_IMAGE_CAPTURE = 1;
    private final int SELECT_PICTURE = 2;
    private String selectedImagePath;
    private int secretCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hides various bars
        getSupportActionBar().hide();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_main);
        cameraButton = (Button) findViewById(R.id.cameraButton);
        galleryButton = (Button) findViewById(R.id.galleryButton);
        mainImage = (ImageView) findViewById(R.id.mainImage);
        secretButton = (Button) findViewById(R.id.secretButton);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
        cameraButton.setTypeface(typeface);
        galleryButton.setTypeface(typeface);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(getTempFile(getApplicationContext())));
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                MainActivity.this.overridePendingTransition(android.R.anim.slide_out_right, android.R.anim.slide_in_left);
            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
            }
        });

        secretButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                secretCount++;
                if(secretCount == 10)
                {
                    mainImage.setBackgroundResource(R.drawable.mainscreenpicture2);
                }
            }
        });

        //ad
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up backButton, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Bitmap bitmap;
            switch (requestCode) {
                //camera
                case REQUEST_IMAGE_CAPTURE:
                    final File file = getTempFile(this);
                    try {
                        //retrieve bitmap for picture taken
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(file));
                        //check gl texture size
                        if (bitmap.getHeight() > GL10.GL_MAX_TEXTURE_SIZE) {
                            // this is the case when the bitmap fails to load
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 2000, 1600, false);
                            bitmap = scaledBitmap;
                        }
                        //get exif data and orientation to determine auto-rotation for picture
                        ExifInterface exif = new ExifInterface("" + file);
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

                        //rotate picture certain # of degrees depending on orientation then sets new matrix
                        bitmap = rotateBitmap(bitmap, orientation);

                        Intent i = new Intent(getBaseContext(), PictureActivity.class);

                        //assigns to global bitmap variable then goes to intent
                        GlobalContainer.bitmap = bitmap;
                        startActivity(i);
                        finish();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                //gallery
                case SELECT_PICTURE:
                    Uri selectedImageUri = data.getData();
                    if (Build.VERSION.SDK_INT < 19) {
                        try {
                            selectedImagePath = getPath(selectedImageUri);
                            bitmap = BitmapFactory.decodeFile(selectedImagePath);
                            //check gl texture size
                            if (bitmap.getHeight() > GL10.GL_MAX_TEXTURE_SIZE) {
                                // this is the case when the bitmap fails to load
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 2000, 1600, false);
                                bitmap = scaledBitmap;
                            }

                            String path = getImagePathForRotation(selectedImageUri);
                            ExifInterface exif = new ExifInterface(path);

                            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

                            //rotate picture certain # of degrees depending on orientation then sets new matrix
                            bitmap = rotateBitmap(bitmap, orientation);

                            Intent i = new Intent(getBaseContext(), PictureActivity.class);
                            //assigns to global bitmap variable then goes to intent
                            GlobalContainer.bitmap = bitmap;

                            startActivity(i);
                            finish();
                        }
                        catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    else {
                        ParcelFileDescriptor parcelFileDescriptor;
                        try {
                            parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedImageUri, "r");
                            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                            parcelFileDescriptor.close();

                            //check gl texture size
                            if (bitmap.getHeight() > GL10.GL_MAX_TEXTURE_SIZE) {
                                // this is the case when the bitmap fails to load
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 2000, 1600, false);
                                bitmap = scaledBitmap;
                            }

                            String path = getImagePathForRotation(selectedImageUri);

                            ExifInterface exif = new ExifInterface(path);
                            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

                            //rotate picture certain # of degrees depending on orientation then sets new matrix
                            bitmap = rotateBitmap(bitmap, orientation);

                            Intent i = new Intent(getBaseContext(), PictureActivity.class);
                            //assigns to global bitmap variable then goes to intent
                            GlobalContainer.bitmap = bitmap;
                            startActivity(i);
                            finish();

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

            }
        }
    }

    private Bitmap rotateBitmap(Bitmap originalBitmap, int orientation){
        Matrix matrix = new Matrix();

        switch (orientation) {
            case 0:
                originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                break;
            case 1:
                originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                break;
        }

        return originalBitmap;
    }

    private File getTempFile(Context context) {
        //it will return /sdcard/image.tmp
        final File path = new File(Environment.getExternalStorageDirectory(), context.getPackageName());
        if (!path.exists()) {
            path.mkdir();
        }
        return new File(path, "image.tmp");
    }

    public String getPath(Uri uri) {
        if( uri == null ) {
            return null;
        }
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    public String getImagePathForRotation(Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":")+1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }
}
