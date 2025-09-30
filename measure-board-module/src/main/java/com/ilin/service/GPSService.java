package com.ilin.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;


public class GPSService extends Service {
    private LocationManager lm;
    private MyLocationListener listener;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("GPSService.onCreate");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("GPSService.onCreate 没有权限");
            stopSelf();
            return;
        }

        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null) {
            System.out.println("GPSService.onCreate lm == null");
            stopSelf();
            return;
        }
        // 注册监听位置服务
        // 给位置提供者设置条件
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider = lm.getBestProvider(criteria, true);
        if (provider == null) {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) provider = LocationManager.GPS_PROVIDER;
            else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) provider = LocationManager.NETWORK_PROVIDER;
            else { stopSelf(); return; }
        }

        listener = new MyLocationListener();
        lm.requestLocationUpdates(provider, 0, 0, listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 取消监听位置服务
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        lm.removeUpdates(listener);
        ls = null;
        listener = null;
    }

    public static interface Ls{
        void onResult(Location location);
    }

    static Ls ls;

    public static void setLs(Ls ls) {
        GPSService.ls = ls;
    }

    class MyLocationListener implements LocationListener {
        /**
         * 当位置改变的时候回调
         */
        @Override
        public void onLocationChanged(Location location) {
            String longitude = "j:" + location.getLongitude() + "\n";
            String latitude = "w:" + location.getLatitude() + "\n";
            String accuracy = "a" + location.getAccuracy() + "\n";
            System.out.println("MyLocationListener.onLocationChanged long = " + longitude);
            System.out.println("MyLocationListener.onLocationChanged latitude = " + latitude);
            System.out.println("MyLocationListener.onLocationChanged accuracy = " + accuracy);
            if (ls != null)
                ls.onResult(location);
        }

        /**
         * 当状态发生改变的时候回调 开启--关闭 ；关闭--开启
         */
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
        }

        /**
         * 某一个位置提供者可以使用了
         */
        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        /**
         * 某一个位置提供者不可以使用了
         */
        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }
    }
}