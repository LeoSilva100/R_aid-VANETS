 package com.example.leosi.r_aid;
 import android.Manifest;
 import android.app.Activity;
 import android.bluetooth.BluetoothAdapter;
 import android.bluetooth.BluetoothDevice;
 import android.bluetooth.BluetoothSocket;
 import android.content.Context;
 import android.content.Intent;
 import android.content.pm.PackageManager;
 import android.location.Location;
 import android.location.LocationListener;
 import android.location.LocationManager;
 import android.os.Build;
 import android.os.Handler;
 import android.os.Message;
 import android.provider.Settings;
 import android.support.annotation.NonNull;
 import android.support.v4.app.ActivityCompat;
 import android.support.v7.app.AppCompatActivity;
 import android.os.Bundle;
 import android.util.Log;
 import android.view.View;
 import android.widget.Button;
 import android.widget.TextView;
 import android.widget.Toast;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.util.UUID;

 public class MainActivity extends AppCompatActivity {

     private static final int START_BLUETOOTH=1;
     private static final int SOLICITA_CONEXAO=2;

     private Button localizacao;
     private TextView t;
     private LocationManager locationManager;
     private LocationListener listener;
     boolean GpsStatus;
     String TAG;
     Intent intent1;

     ConnectedThread connectedThread;

     BluetoothAdapter meuBluetoothAdapter = null;
     BluetoothDevice meuDevice = null;
     BluetoothSocket meuSocket = null;
     Button btnConexao, btnEnviar;
     TextView dadosRecebidos;
     boolean conexao =  false;
     private static String MAC = null;
     public String latlog;
     String dados;
     UUID MEU_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_main);
         btnConexao = (Button) findViewById(R.id.btnConexao);
         btnEnviar = (Button) findViewById(R.id.btnEnviar);
         meuBluetoothAdapter =  BluetoothAdapter.getDefaultAdapter();
         dadosRecebidos = (TextView) findViewById(R.id.dadosRecebidos);

         if(meuBluetoothAdapter ==  null){
             Toast.makeText(getApplicationContext(),"Seu dispositivo não pussui bluetooth", Toast.LENGTH_LONG).show();
         }else if(!meuBluetoothAdapter.isEnabled()) {
             Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
             startActivityForResult(ativaBluetooth, START_BLUETOOTH);

         }
         btnConexao.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 if (conexao){
                     try {
                         meuSocket.close();
                         conexao =  false;
                         btnConexao.setText("Conectar");
                         Toast.makeText(getApplicationContext(),"O Bluetooth foi desconectado!", Toast.LENGTH_LONG).show();
                     }catch (IOException erro){
                         Toast.makeText(getApplicationContext(),"Ocorreu um erro!"+erro, Toast.LENGTH_LONG).show();
                     }

                 }else{
                     Intent openList = new Intent(MainActivity.this, ListaDispositivos.class);
                     startActivityForResult(openList, SOLICITA_CONEXAO);
                 }
             }
         });
         btnEnviar.setOnClickListener(new View.OnClickListener(){
             @Override
             public void onClick(View vk) {
                 if (conexao){
                     connectedThread.write(latlog);
                 }else{
                     Toast.makeText(getApplicationContext(),"O bluetooth não está conectado!", Toast.LENGTH_LONG).show();
                 }
             }
         });
         dadosRecebidos.setOnClickListener(new View.OnClickListener(){
             @Override
             public void onClick(View vk) {
                dadosRecebidos.setText("");
             }
         });

         CheckGpsStatus();

         t = (TextView) findViewById(R.id.textView);
         localizacao = (Button) findViewById(R.id.button);

         locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

         listener = new LocationListener() {
             @Override
             public void onLocationChanged(Location location) {
                 //t.setText(" ");
                 //t.append("\n " + location.getLongitude() + " " + location.getLatitude());
                 t.setText("\n " + location.getLongitude() + " " + location.getLatitude());
                 latlog = "\n " + location.getLongitude() + " " + location.getLatitude();
             }

             @Override
             public void onStatusChanged(String s, int i, Bundle bundle) {

             }

             @Override
             public void onProviderEnabled(String s) {

             }

             @Override
             public void onProviderDisabled(String s) {
                 Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                 startActivity(i);
             }
         };
         configure_button();
     }

     @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         //super.onActivityResult(requestCode, resultCode, data);
         switch (requestCode){
             case  START_BLUETOOTH:
                 if (resultCode == Activity.RESULT_OK){
                     Toast.makeText(getApplicationContext(),"O bluetooth foi ativado", Toast.LENGTH_LONG).show();
                 }else{
                     Toast.makeText(getApplicationContext(),"O bluetooth não foi ativado o app será encerrado", Toast.LENGTH_LONG).show();
                     finish();
                 }
                 break;
             case SOLICITA_CONEXAO:
                 if (resultCode == Activity.RESULT_OK){
                     MAC = data.getExtras().getString(ListaDispositivos.ENDERECOMAC);
                     Toast.makeText(getApplicationContext(),"MAC FINAL:"+MAC, Toast.LENGTH_LONG).show();
                     meuDevice = meuBluetoothAdapter.getRemoteDevice(MAC);
                     try {
                         meuSocket = meuDevice.createRfcommSocketToServiceRecord(MEU_UUID);
                         meuSocket.connect();
                         conexao = true;
                         connectedThread =  new ConnectedThread(meuSocket);
                         connectedThread.start();
                         btnConexao.setText("Desconectar");
                         Toast.makeText(getApplicationContext(),"Você foi Conectado ao:"+MAC, Toast.LENGTH_LONG).show();
                     }catch (IOException erro){
                         conexao = false;
                         Toast.makeText(getApplicationContext(),"Ocorreu um erro!"+erro, Toast.LENGTH_LONG).show();
                     }
                 }else {
                     Toast.makeText(getApplicationContext(),"Falha ao Obter o MAC", Toast.LENGTH_LONG).show();
                 }
         }
     }

     Handler mHandler = new Handler() {
         @Override
         public void handleMessage(Message msg) {
         }
     };

     private class ConnectedThread extends Thread {
         private final InputStream mmInStream;
         private final OutputStream mmOutStream;

         public ConnectedThread(BluetoothSocket socket) {

             InputStream tmpIn = null;
             OutputStream tmpOut = null;

             // Get the input and output streams, using temp objects because
             // member streams are final
             try {
                 tmpIn = socket.getInputStream();
                 tmpOut = socket.getOutputStream();
             } catch (IOException e) { }

             mmInStream = tmpIn;
             mmOutStream = tmpOut;
         }
         public void run() {
             int bytes = 0;
             while (true) {
                 try {
                     int bytesAvailable  =  mmInStream.available();
                     if (bytesAvailable > 0) {
                         byte[] buffer = new byte[bytesAvailable];
                         mmInStream.read(buffer);
                         for (int i = 0; i < bytesAvailable; i++) {
                             byte b = buffer[i];
                            dados += (char) b;
                             if (dados.length() > 5 )
                             mHandler.post(new Runnable() {
                                 @Override
                                 public void run() {
                                     imprime(dados);
                                 }
                             });
                         }
                     }
                 } catch (IOException e) {
                     break;
                 }
             }
         }
         public void imprime(String message){
             dadosRecebidos.setText(message);
         }

         /* Call this from the main activity to send data to the remote device */
         public void write(String dadosEnviar) {
             byte[] mgsBuffer = dadosEnviar.getBytes();
             try {
                 mmOutStream.write(mgsBuffer);
             } catch (IOException e) { }
         }
     }
     //=========================================================================================================================================================

     public void CheckGpsStatus(){

         locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

         GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

         if(GpsStatus == false)
         {
             intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
             startActivity(intent1);
         }

     }

     @Override
     public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
         switch (requestCode){
             case 10:
                 configure_button();
                 break;
             default:
                 break;
         }
     }

     void configure_button(){
         // first check for permissions
         if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                 requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                         ,10);
             }
             return;
         }
         // this code won't execute IF permissions are not allowed, because in the line above there is return statement.
         localizacao.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 //noinspection MissingPermission
                 locationManager.requestLocationUpdates("gps", 5000, 0, listener);
             }
         });
     }
 }
