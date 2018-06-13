package dev.edmt.qrcodecamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final Pattern PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    final int RequestCameraPermissionID = 1001;
    SurfaceView cameraPreview;
    TextView txtResult;
    BarcodeDetector barcodeDetector;
    CameraSource cameraSource;
    String IP = "";
    String IDPACIENTE = "";
    String sentence = "";
    int a = 1;

    public static boolean validate(final String ip) {
        return PATTERN.matcher(ip).matches();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraPreview.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreview = (SurfaceView) findViewById(R.id.cameraPreview);
        txtResult = (TextView) findViewById(R.id.txtResult);

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        cameraSource = new CameraSource
                .Builder(this, barcodeDetector).setAutoFocusEnabled(true)
                .setRequestedPreviewSize(640, 480)
                .build();
        //Add Event
        cameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    //Request permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, RequestCameraPermissionID);
                    return;
                }
                try {
                    cameraSource.start(cameraPreview.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrcodes = detections.getDetectedItems();
                if (qrcodes.size() != 0) {
                    txtResult.post(new Runnable() {
                        @Override

                        public void run() {

                            //Create vibrate
                            if (a == 1) {
                                txtResult.setText("Enfoque el QR");
                                //txtResult.setText(qrcodes.valueAt(0).displayValue);
                                IP = qrcodes.valueAt(0).displayValue;
                                //Toast.makeText(getApplicationContext(),IP,Toast.LENGTH_LONG).show();
                                if (!IP.equals("") && validate(IP)) {
                                    Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                    vibrator.vibrate(1000);
                                    txtResult.setText("Escanear QR de paciente");
                                    a = 0;
                                }
                            }
                            if (a == 0) {
                                //Toast.makeText(getApplicationContext(),"Escanear QR de paciente",Toast.LENGTH_LONG).show();
                                IDPACIENTE = qrcodes.valueAt(0).displayValue;
                                //Toast.makeText(getApplicationContext(),IP,Toast.LENGTH_LONG).show();
                                if (!IDPACIENTE.equals("") && !(validate(IDPACIENTE))) {
                                    Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                    vibrator.vibrate(1000);
                                    a = 3;
                                }
                            }
                            if (a == 3) {
                                try {
                                    String host = IP;
                                    int port = 9876;

                                    byte[] message = IDPACIENTE.getBytes();

                                    // Get the internet address of the specified host
                                    InetAddress address = InetAddress.getByName(host);

                                    // Initialize a datagram packet with data and address
                                    DatagramPacket packet = new DatagramPacket(message, message.length,
                                            address, port);

                                    // Create a datagram socket, send the packet through it, close it.
                                    DatagramSocket dsocket = new DatagramSocket();
                                    dsocket.send(packet);
                                    dsocket.close();
                                    //Toast.makeText(getApplicationContext(),IP,Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                a = 1;
                            }
                        }
                    });
                }
            }
        });
    }
}
