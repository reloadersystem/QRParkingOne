package pe.reloadersystem.qrparking;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button b_on, b_off, b_list, b_disc;
    BluetoothAdapter bluetoothAdapter;
    ListView list;

    private static int REQUEST_ENABLED = 0;
    private static int REQUEST_DISCOVERABLE = 0;

    SurfaceView surface;

    private String token = "";
    private String tokenanterior = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        b_on = findViewById(R.id.b_on);
        b_off = findViewById(R.id.b_off);
        b_list = findViewById(R.id.b_list);
        b_disc = findViewById(R.id.b_discoverable);

        surface = (SurfaceView) findViewById(R.id.surfaceView);

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);


        if (rc == PackageManager.PERMISSION_GRANTED) {
            setUp();
        } else {
            requestCamaraPermission();
        }

        list = findViewById(R.id.list);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Tu Dispositivo no tiene Soporte Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }

        b_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLED);

            }
        });

        b_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bluetoothAdapter.disable();

            }
        });


        b_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                ArrayList<String> deviced = new ArrayList<String>();

                for (BluetoothDevice bt : pairedDevices) {
                    deviced.add(bt.getName());
                }

                ArrayAdapter arrayAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_expandable_list_item_1, deviced);

                list.setAdapter(arrayAdapter);


            }
        });


        b_disc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!bluetoothAdapter.isDiscovering()) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(intent, REQUEST_DISCOVERABLE);
                }


            }
        });

    }


    private void requestCamaraPermission() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                }
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
                return;
            }
        }
    }

    private void setUp() {
        BarcodeDetector bar = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        final CameraSource camara = new CameraSource.Builder(this,bar).build();

        bar.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcode = detections.getDetectedItems();
                if(barcode.size()>0){

                    token = barcode.valueAt(0).displayValue;

                    if(!token.equals(tokenanterior))
                    {
                        tokenanterior=token;
                        Log.i("token", token);

                        if(URLUtil.isValidUrl(token)){

                            //un QR que tiene una direccion de URL y  segun la URL abre  la pagina...


//                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(token));
//                            startActivity(browserIntent);

                        }else
                        {

                            //si el QR es un TEXTO, lo lee y lo puede compartir...
                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_TEXT, token);
                            shareIntent.setType("text/plain");
                            startActivity(shareIntent);

                        }

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    synchronized (this)
                                    {
                                        wait(5000);
                                        // limpiamos el token
                                        tokenanterior = "";

                                    }
                                }catch (InterruptedException e)
                                {
                                    Log.e("Error", "Espera");
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    }


                    /*information.post(new Runnable() {
                        @Override
                        public void run() {

                            information.setText(barcode.valueAt(0).displayValue.toString());



                                Toast.makeText( PromoActivity.this,"Escaneo Exitoso",Toast.LENGTH_SHORT).show();
                                          }
                    });*/
                }
            }
        });

        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try{
                    if(ActivityCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                        return ;
                    }
                    camara.start(surface.getHolder());
                }catch (Exception ex){

                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                camara.stop();
            }


        });
    }
}
