package com.irfanztrk.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.irfanztrk.artbook.databinding.ActivityArtBinding;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {

    Bitmap selectedImage;
    SQLiteDatabase database;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    private ActivityArtBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityArtBinding.inflate(getLayoutInflater());
        View view=binding.getRoot();
        setContentView(view);
        registerLauncher();

        database=this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent=getIntent();
        String info=intent.getStringExtra("info");
        if(info.matches("new")){
            //new art
            binding.nameText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.select_image_icon);
        }else{
            int artId=intent.getIntExtra("artId",1);
            binding.button.setVisibility(View.INVISIBLE);

            try{
                Cursor cursor=database.rawQuery("SELECT * FROM arts WHERE id= ? ",new String[]{String.valueOf(artId)});
                int artNameIx=cursor.getColumnIndex("artname");
                int painterNameIx=cursor.getColumnIndex("paintername");
                int  yearIx= cursor.getColumnIndex("year");
                int imageIx=cursor.getColumnIndex("image");

                while(cursor.moveToNext()){
                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.artistText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] bytes=cursor.getBlob(imageIx);
                    Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }
                cursor.close();


            }catch (Exception e){
                e.printStackTrace();
            }
        }


    }
    public void save(View view){
        String name=binding.nameText.getText().toString();
        String artistName=binding.artistText.getText().toString();
        String year=binding.yearText.getText().toString();

        Bitmap smallimage=makeSmallerImage(selectedImage,400);

        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        smallimage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray=outputStream.toByteArray();

        try {
            database=this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE İF NOT EXISTS arts(id INTEGER PRIMARY KEY,artname VARCHAR,paintername VARCHAR,year VARCHAR,image BLOB)");

            String sqlString="INSERT INTO arts(artname,paintername,year,image)VALUES(?,?,?,?)";
            SQLiteStatement sqLiteStatement=database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();


        }catch (Exception e){
            e.printStackTrace();
        }
        // Save den sonra MainActivity e gitmesini sağlamamız lazım
        Intent intent=new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//bu flag ile tüm activityleri kapat ve intetntte belirttiğim activitye gitmeyi sağladık
        startActivity(intent);



    }

    public Bitmap makeSmallerImage(Bitmap image,int maximumSize){ //resim boyutunu küçültme

     int width=image.getWidth();
     int height=image.getHeight();
     float bitmapeRatio=(float)width/(float)height;//boyut oranı ile resmin şeklini buluyoruz

      if(bitmapeRatio>1){
          //landscape image(yatay)
          width=maximumSize;
          height=(int)(width/bitmapeRatio);

      }else{
          //portre(dikey)
          height=maximumSize;
          width=(int)(height*bitmapeRatio);
      }
     return Bitmap.createScaledBitmap(image,width,height,true);
    }
    public void selectImage(View view){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){

                Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Gİve Permisson", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //galeriye gitmek için izin talebi
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            }else{
                //galeriye gitmek için izin talebi
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);

            }

            //galeriye ulaşmak için yukarıda izin verilmemişse izin istenmesini sağlayacak

        } else{
        //izin verildiyse tekrar izin istemeye gerek yok direkt galeriye gidicez

            Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);

        }

    }

    private void registerLauncher(){

        activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {

                if(result.getResultCode()==RESULT_OK){
                    Intent intentFromResult=result.getData();
                    if(intentFromResult!=null){
                      Uri imageData= intentFromResult.getData();
                      //binding.imageView.setImageURI(imageData);
                        try {
                            if(Build.VERSION.SDK_INT>=28){
                                //28 versiyonlu veya büyük android telefonlarda kullan
                                ImageDecoder.Source source=ImageDecoder.createSource(getContentResolver(),imageData);
                                selectedImage=ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            }else{
                                selectedImage=MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(),imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
             if(result){
                 //permission granted (izin verildi)
                 Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                 activityResultLauncher.launch(intentToGallery);
             }else{
                 //permission denies (izin verilmeedi)
                 Toast.makeText(ArtActivity.this,"Permission needed!",Toast.LENGTH_LONG).show();


             }
            }
        });

    }
}