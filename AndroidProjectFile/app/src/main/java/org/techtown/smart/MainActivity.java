package org.techtown.smart;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pedro.library.AutoPermissions;
import com.pedro.library.AutoPermissionsListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

//GoogleMap.OnMarkerClickListener
public class MainActivity extends AppCompatActivity implements AutoPermissionsListener {
    private static final String TAG = "MainActivity";
    Handler handler = new Handler();    //스레드 활용을 위한 핸들러
    SupportMapFragment mapFragment; //맵 프래그먼트 객체
    GoogleMap map;
    MarkerOptions myLocationMarker;
    MarkerOptions trashCan;

    public DataInputStream dis;
    public DataOutputStream dos;
    private final String Server_ip = "192.168.0.19";
    private final int port = 5001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //위치 데이터를 받아오기 위한 위험권한 요청
        AutoPermissions.Companion.loadAllPermissions(this, 101);

        //Map 프래그먼트 객체 참조 및 getMapAsync() 메소드 호출.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(googleMap -> {
            Log.d(TAG, "GoogleMap is ready.");

            map = googleMap;

            try {
                map.setMyLocationEnabled(true);
            } catch(SecurityException e) {e.printStackTrace();}

            //map.setOnMarkerClickListener(this);
        });

        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestMyLocation();
            }
        });

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpsClient gc = new gpsClient();
                gc.start();
            }
        });

        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capaClient cc = new capaClient();
                cc.start();
            }
        });
    }

    private void requestMyLocation() {
        //위치 관리자 객체(LocationManager) 참조
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            long minTime = 10000;   //업데이트에 필요한 최소 시간(여기서는 10초)
            float minDistance = 0;  //업데이트에 필요한 최소 거리(여기서는 0)

            //위치 관리자 객체를 통해 10초마다 위치 업데이트 요청
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            showCurrentLocation(location);   //현재 위치를 나타내는 함수 호출 (location 객체를 파라미터로 받음)
                        }
                        @Override
                        public void onProviderDisabled(String provider) {
                        }
                        @Override
                        public void onProviderEnabled(String provider) {
                        }
                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }
                    });

            //최근 확인한 위치 정보 저장
            Location lastLocation = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lastLocation != null) { //최근 위치 정보가 있다면
                showCurrentLocation(lastLocation);   //현재 위치를 나타내는 함수 호출 (location 객체를 파라미터로 받음)
            }

        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }


    //현재 위치를 나타내는 함수. (Location 객체를 Parameter로 받음)
    private void showCurrentLocation(Location location){
        LatLng curPoint = new LatLng(location.getLatitude(), location.getLongitude());  //LatLng 객체로 현재 위치 저장
        try {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 15));

            showMyLocationMarker(location);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void TrashcanLocation(LatLng curpoint){
        //LatLng 객체로 현재 위치 저장
        //map.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 15)); //현재 위치로 지도를 이동시키는 건 굳이 안해도 될듯.
        TrashCanMarker(curpoint); //현재 위치에 마커 표시
    }

    //마커 표시 함수
    private void showMyLocationMarker(Location location){
        if (myLocationMarker == null){  //마커가 설정이 안되있을 시
            myLocationMarker = new MarkerOptions(); //마커 옵션 생성
            myLocationMarker.position(new LatLng(location.getLatitude(), location.getLongitude())); //마커 위치는 현재 위치
            myLocationMarker.title("내 위치\n");
            myLocationMarker.snippet("GPS로 확인한 위치");
            myLocationMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.mylocation)); //마커 아이콘 설정
            map.addMarker(myLocationMarker);    //Map에 마커 추가
        } else {    //마커가 설정되어있을 경우
            myLocationMarker.position(new LatLng(location.getLatitude(), location.getLongitude()));    //위치만 업데이트
        }
    }

    private void TrashCanMarker(LatLng curpoint){
        if (trashCan== null){  //마커가 설정이 안되있을 시
            trashCan = new MarkerOptions(); //마커 옵션 생성
            trashCan.position(curpoint); //마커 위치는 현재 위치
            trashCan.title("휴지통\n");
            trashCan.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_trashcan)); //휴지통 마커 아이콘 설정
            map.addMarker(trashCan);
        } else { //마커가 설정되어있을 경우
            trashCan.position(curpoint);   //위치만 업데이트
        }
    }

    class gpsClient extends Thread {
        LatLng trash;
        public void run() {
            try {
                //라즈베리파이 서버를 통해 GPS 데이터 얻어옴
                Socket sock = new Socket(Server_ip, port);  //소켓 설정, ip는 휴지통의 ip
                Log.d(TAG, "소켓 연결 완료");

                String data = "GPS";

                dos = new DataOutputStream(sock.getOutputStream());
                dis = new DataInputStream(sock.getInputStream());
                dos.writeUTF(data);
                dos.flush();

                Log.d(TAG, "데이터 요청함");

                String gps_data;
                gps_data = dis.readLine();

                Log.d(TAG, "전송받은 데이터: " + gps_data);
                String[] location = gps_data.split(",");

                trash = new LatLng(Double.parseDouble(location[0]),Double.parseDouble(location[1]));
                sock.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            handler.post(new Runnable(){
                @Override
                public void run(){
                    Log.d(TAG, "받은 데이터: " + trash);
                    TrashcanLocation(trash);
                }
            });
        }
    }

    class capaClient extends Thread {
        String capacity;
        public void run() {
            try {
                //휴지통 내부 용량을 확인하는 코드, 소켓 통신으로 내부 용량 상태를 얻어옴
                Socket sock = new Socket(Server_ip, port);
                Log.d(TAG, "소켓 연결 완료");

                String data = "Capa";

                dos = new DataOutputStream(sock.getOutputStream());
                dis = new DataInputStream(sock.getInputStream());
                dos.writeUTF(data);
                dos.flush();

                Log.d(TAG, "데이터 요청함");
                Thread.sleep(2000);

                capacity = dis.readLine();
                Log.d(TAG, "받은 데이터: " + capacity);
                sock.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            handler.post(new Runnable(){
                @Override
                public void run(){
                    //토스트 메세지로 현재 용량 표현
                    if(Double.parseDouble(capacity) >= 0 && Double.parseDouble(capacity) <= 30){
                        Toast.makeText(getApplicationContext(), "현재 휴지통 용량: " + capacity + "% (적음)", Toast.LENGTH_LONG).show();
                    }else if(Double.parseDouble(capacity) > 30 && Double.parseDouble(capacity) <= 60 ){
                        Toast.makeText(getApplicationContext(), "현재 휴지통 용량: " + capacity + "% (보통)", Toast.LENGTH_LONG).show();
                    }else if(Double.parseDouble(capacity) > 60){
                        Toast.makeText(getApplicationContext(), "현재 휴지통 용량: " + capacity + "% (많음)", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(getApplicationContext(), "용량 확인 중", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    /*public boolean onMarkerClick(Marker marker){
        capaClient cc = new capaClient();
        cc.start();
        return true;
    }*/

    //위험 권한을 요청하는 메소드, 메소드 호출시 승인, 거부로 나뉨.
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AutoPermissions.Companion.parsePermissions(this, requestCode, permissions, this);
    }

    @Override
    public void onDenied(int requestCode, String[] permissions) {
        Toast.makeText(this, "permissions denide: " + permissions.length, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGranted(int requestCode, String[] permissions) {
        Toast.makeText(this, "permissions granted: " + permissions.length, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (map != null) {
            try {
                map.setMyLocationEnabled(true);
            } catch(SecurityException e) {e.printStackTrace();}
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (map != null) {
            try {
                map.setMyLocationEnabled(false);
            } catch(SecurityException e) {e.printStackTrace();}
        }
    }
}