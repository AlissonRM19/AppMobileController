package com.example.controller;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private String SERVER_IP= "192.168.43.43"; // IP de la máquina Linux Red Celular
    //private String SERVER_IP= "192.168.0.102"; // IP de la máquina Linux Red Hogar
    private int SERVER_PORT = 8080; // Puerto en el que la máquina Linux está escuchando

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private TextView textView3;

    private TextView textView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtener la referencia del TextView3
        textView3 = findViewById(R.id.textView3);
        // Inicializa el SensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Obtener una instancia del sensor
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // Obtener la referencia del TextView
        textView = findViewById(R.id.textView);

        Refresh();

        if (isValidIPv4(SERVER_IP)) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        } else {
            NoValidIP();
        }
    }

    private void NoValidIP() {
        new Thread(() -> {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void Refresh(){
        new Thread(this::getNetworkIPs).start();

        SERVER_PORT = 8080;
        //SERVER_IP = "192.168.0.102";
        SERVER_IP = "192.168.43.43";

    }


    private void EnviarInfo(String message) {
        new Thread(() -> {
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                //message = "Hola, soy el dispositivo Android ";
                output.printf(message);
                Log.d("ENVIADO", message);


                String response = input.readLine();

                Log.d("RESPUESTA", response);


                runOnUiThread(()->{
                    try {
                        textView.setText(response);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });

                socket.close();
            } catch (IOException e) {
                Log.d("NO SE PUDO ENVIAR", "");
                e.printStackTrace();
            }
        }).start();
    }

    private static boolean isValidIPv4(String ip) {
        Pattern pattern = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

    public void getNetworkIPs() {

        byte[] ip = new byte[4];
        final StringLinkedList list = new StringLinkedList();
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        ip = ipv4ToBytes(addr.getHostAddress());
                        Log.d("HOST IP ADDRESS", addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;     // exit method, otherwise "ip might not have been initialized"
        }
        final byte[] ipFinal = ip;

        try {
            String ipID = InetAddress.getByAddress(ipFinal).toString().substring(1);
            ipID = ipID.substring(0, ipID.lastIndexOf(".")+1) + 'X';
            //entry1.setText(ipID);
            Log.d("Ip encontrada",ipID);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }
    public byte[] ipv4ToBytes(String ipv4) throws IllegalArgumentException {
        String[] octets = ipv4.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address format");
        }
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            int octet = Integer.parseInt(octets[i]);
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Invalid octet value: " + octet);
            }
            bytes[i] = (byte) octet;
        }
        return bytes;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrar el SensorEventListener
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause()  {
        super.onPause();
        // Desregistrar el SensorEventListener para ahorrar batería
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event)  {
        float x = event.values[0];
        float y = event.values[1];
        String message1 = String.format("X: %.2f, Y: %.2f", x, y);

        textView3.setText(String.format("X: %.2f\n  Y: %.2f", x, y));
        EnviarInfo(message1);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}