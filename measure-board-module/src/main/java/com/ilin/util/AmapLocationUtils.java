package com.ilin.util;

import android.content.Context;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import java.util.HashMap;
import java.util.Map;


/**
 * 通过阿里高德api快速获取定位
 */
public class AmapLocationUtils {

    String TAG = AmapLocationUtils.class.getSimpleName();
    static AmapLocationUtils instance;
    private Context mContext;
    /**位置信息*/
    private AMapLocation mLoc = null;
    private AMapLocationClient mAMapClient;
    private AMapLocationListener mLocationListener;

    public static AmapLocationUtils getInstance(){
        if (instance == null){
            synchronized (AmapLocationUtils.class){
                if (instance == null)
                    instance = new AmapLocationUtils();
            }
        }
        return instance;
    }

    public void init(Context context){
        this.mContext = context;
        initAMap();
    }

    public void setLocationListener(AMapLocationListener mLocationListener) {
        this.mLocationListener = mLocationListener;
    }

    void initAMap() {
        if (mContext == null){
            Log.e(TAG, "initAMap: context is null" );
            return;
        }
        AMapLocationClient.updatePrivacyShow(mContext,true,true);
        AMapLocationClient.updatePrivacyAgree(mContext,true);

        try {
//            mAmapConverter = new CoordinateConverter(mContext);
            AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
            //mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.Sport);
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setNeedAddress(false);
            //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
            mLocationOption.setInterval(3000);
            mLocationOption.setWifiActiveScan(false);
            mLocationOption.setSensorEnable(true);
            //设置是否允许模拟位置,默认为true，允许模拟位置
//            mLocationOption.setMockEnable(true);
            mLocationOption.setLocationCacheEnable(false);
            mLocationOption.setHttpTimeOut(20000);
            mAMapClient = new AMapLocationClient(mContext);
            mAMapClient.setLocationOption(mLocationOption);
            mAMapClient.setLocationListener(location -> {
                int type = 0;
                int errCode = 1;
                if (location != null) {
                    type = location.getLocationType();
                    errCode = location.getErrorCode();
                }
                if (type != 0 && errCode == 0) {
                    if (type > 5) {
                        Log.w(TAG, "onLocation: 未定位 ! type = " + type );
                    }
                    else if (mLocationListener != null){
                        mLocationListener.onLocationChanged(location);
                    }
//                    else {
                        Log.d(TAG, "onLocation.info: " + getInfo(location, true));
//                    }
                }
                else {
                    Log.e(TAG, "onLocation: 定位对象有误 = " + location );
                }
                mLoc = location;
            });
        }
        catch (Exception e){
            FLog.getInstance().log("create amap client error." + e.getMessage());
        }
    }


    public String getInfo(AMapLocation amapLocation, boolean isAll){
        Map<String, Object> map = new HashMap<>();
        /*
         * 1 GPS定位结果  通过设备GPS定位模块返回的定位结果，精度较高，在10米－100米左右
         * 2 前次定位结果 网络定位请求低于1秒、或两次定位之间设备位置变化非常小时返回，设备位移通过传感器感知。
         * 4 缓存定位结果 返回一段时间前设备在同样的位置缓存下来的网络定位结果
         * 5 Wifi定位结果 属于网络定位，定位精度相对基站定位会更好，定位精度较高，在5米－200米之间。
         */
        map.put("locType", amapLocation.getLocationType());//获取当前定位结果来源，如网络定位结果，详见定位类型表
        map.put("lat", amapLocation.getLatitude());//获取纬度
        map.put("long", amapLocation.getLongitude());//获取经度
        map.put("bearing", amapLocation.getBearing());
        map.put("speed", amapLocation.getSpeed());
/*
        amapLocation.getAccuracy();//获取精度信息
        amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
        amapLocation.getCountry();//国家信息
        amapLocation.getProvince();//省信息
        */
        if (isAll){
            String city =  amapLocation.getCity();//城市信息
            String district = amapLocation.getDistrict();//城区信息
            map.put("cityInfo", city + district);
        }
        /*
        amapLocation.getStreet();//街道信息
        amapLocation.getStreetNum();//街道门牌号信息
        amapLocation.getCityCode();//城市编码
        amapLocation.getAdCode();//地区编码
        amapLocation.getAoiName();//获取当前定位点的AOI信息
        amapLocation.getBuildingId();//获取当前室内定位的建筑物Id
        amapLocation.getFloor();//获取当前室内定位的楼层
         */
      //  amapLocation.getGpsAccuracyStatus();//获取GPS的当前状态
        if (isAll){
            map.put("gpsStatus", amapLocation.getGpsAccuracyStatus());
        }
        //获取定位时间
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        Date date = new Date(amapLocation.getTime());
//        df.format(date);

        return map.toString();
    }

    /***
     * 开启定位
     */
    public void startLocation() {
        if (mAMapClient != null) {
            Log.i(TAG,"start amap location");
            mAMapClient.startLocation();
        }
    }

    /***
     * 停止定位
     */
    public void stopLocation() {
        if (mAMapClient != null) {
            Log.i(TAG,"stop amap location");
            // 停止定位后，本地定位服务并不会被销毁
            mAMapClient.stopLocation();
        }
    }

    public void destroy(){
        if (mAMapClient != null){
            // 销毁定位客户端，同时销毁本地定位服务。
            mAMapClient.onDestroy();
        }
    }


}
