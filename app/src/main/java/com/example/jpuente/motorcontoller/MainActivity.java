package com.example.jpuente.motorcontoller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mBluetoothDevice = null;
    BluetoothSocket client_socket;
    UUID uuid = UUID.fromString("9ba4c432-79d3-4d2c-a1ec-134aefb1f94b");
    LinearLayout userInterface;
    OutputStream mOutputStream;
    ProgressBar loadingSpinner;
    ImageView errorImageView;
    TextView errorMessage;
    TextView powerButtonStateTextView;
    ImageView powerButton;
    char powerButtonState;
    SeekBar speedControlSeekbar;
    int pwmDutyCycle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        userInterface = (LinearLayout) findViewById(R.id.user_interface);
        powerButton = (ImageView) findViewById(R.id.power_button);

        errorMessage = (TextView) findViewById(R.id.empty_text_view);

        loadingSpinner = (ProgressBar) findViewById(R.id.loading_spinner);
        errorImageView = (ImageView) findViewById(R.id.error_connection);

        powerButtonStateTextView = (TextView) findViewById(R.id.power_button_state);

        speedControlSeekbar = (SeekBar) findViewById(R.id.seekBar);


       //Set up bluetooth
        setupBluetooth();

        //Get the remote bluetooth device (raspberry pi)
        getRemoteBluetoothDevice();

        //execute connectionAsyncTask
        new ConnectionAsyncTask().execute();

        powerButtonState = '0';
       //Test button to send data to the server
        powerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Try to send some data
                if (client_socket.isConnected()) {

                    try {
                    mOutputStream = client_socket.getOutputStream();

                    if(powerButtonState == '0'){

                    writeBytes("201".getBytes());
                    powerButtonState = '1';
                    Toast.makeText(getApplicationContext(), "Power ON", Toast.LENGTH_SHORT).show();
                    powerButtonStateTextView.setText("ON");

                    }
                    else if(powerButtonState == '1') {

                    mOutputStream.write("200".getBytes());
                    powerButtonState = '0';
                    Toast.makeText(getApplicationContext(), "Power OFF", Toast.LENGTH_SHORT).show();
                    powerButtonStateTextView.setText("OFF");

                    }


                    } catch (IOException e) {

                        e.printStackTrace();
                    }


                }else
                    Toast.makeText(getApplicationContext(), "Client is not connected", Toast.LENGTH_LONG).show();
            }
        });


        speedControlSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            pwmDutyCycle = progress;

            //send duty cycle update
            if(client_socket.isConnected()){

                try {
                    mOutputStream = client_socket.getOutputStream();

                    writeBytes(String.valueOf(progress).getBytes());

            }catch (IOException e){

                e.printStackTrace();
                }
             }

            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                Toast.makeText(getApplicationContext(), "Motor speed is set to: " + pwmDutyCycle + "%",Toast.LENGTH_SHORT).show();
            }
        });


    }


    //method for writing stream of bytes to OutputStream
    public void writeBytes(byte[] bytes){

        try {
            mOutputStream.write(bytes);
        }catch (IOException e){
            e.printStackTrace();
        }


    }


    public void setupBluetooth()
    {

        //Get the device bluetooth adapater
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Enable bluetooth
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
        }

    }


    public void getRemoteBluetoothDevice(){

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if(pairedDevices.size() > 0){

            for(BluetoothDevice device : pairedDevices){

                if(device.getName().equals("raspberrypi")){

                    mBluetoothDevice = device;

                    Log.i("MotorController", device.getName());
                    break;
                }
            }


        }
    }


    public class ConnectionAsyncTask extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... voids) {

            //Try to connect to the server
            try {

                //create a bluetooth client socket to start a secure outgoing connection to the remote bluetooth device
                /*
                 * createRfcommSocketToServiceRecord takes the UUID and uses SDP to decide what radio channel to use in the connection
                 * It also checks to make sure that a server is listening on the remote end point with the same UUID: most reliable way to get a connection
                 * */
                client_socket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);


                //initiate an outgoing connection
                if(!client_socket.isConnected()){

                    client_socket.connect();

                }

            }

            catch (IOException e){

                e.printStackTrace();

            }
            return null;
        }

        @Override
        protected void onPreExecute() {

            loadingSpinner.setVisibility(View.VISIBLE);

            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            loadingSpinner.setVisibility(View.INVISIBLE);

            if(client_socket.isConnected()){
                Toast.makeText(getApplicationContext(), "Connected to the server", Toast.LENGTH_LONG).show();

                //show the user interface
                userInterface.setVisibility(View.VISIBLE);

            }else{

                Toast.makeText(getApplicationContext(), "Not connected, make sure that the server is up", Toast.LENGTH_LONG).show();
                errorImageView.setVisibility(View.VISIBLE);
                errorMessage.setVisibility(View.VISIBLE);
            }

        }
    }


}
