package com.ilin.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.ilin.util.Config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/***
 * 接收司机数据及司机设置
 */
public class DriverSetReceiver extends BroadcastReceiver {
    /**司机被选择广播*/
    private final String ACT_DRIVER_SET = "act.driver.selected";
    /**收到司机数据广播*/
    private final String ACT_DRIVERS_DATA = "act.drivers.data";
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String act = intent.getAction();

        if (ACT_DRIVER_SET.equals(act)) {
            // 保存数据
            String selId = intent.getStringExtra("driverId");
            if (!TextUtils.isEmpty(selId)) {
                SharedPreferences pref = ctx.getSharedPreferences(Config.PREF, Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = pref.edit();
                edit.putString("selectedId", selId);
                edit.apply();
            }
        }
        else if (ACT_DRIVERS_DATA.equals(act)) {
            //这里只保存名字和收款码URL， key (driverId): val ({"name":, "payImg":})
            String drivers = intent.getStringExtra("drivers");
            try {
                JSONArray arr = new JSONArray(drivers);
                int size = arr.length();
                SharedPreferences pref = ctx.getSharedPreferences(Config.PREF, Context.MODE_PRIVATE);
                if (size == 0) {
                    SharedPreferences.Editor edit = pref.edit();
                    edit.putString("selectedId", "");
                    edit.apply();
                }
                else {
                    SharedPreferences.Editor edit = pref.edit();
                    for(int i=0; i<size; i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String driverId = (String)obj.remove("driverId");
                        edit.putString(driverId, obj.toString());
                    }
                    edit.apply();
                }
            }
            catch (JSONException e) {
                Log.e("jsonArrParseError.", e.getLocalizedMessage());
            }
        }
        else if ("ilincar.landeng.LED_ON".equals(act)) {
            Intent actIntent = new Intent("act.pay.img.need.show");
            actIntent.addCategory("android.intent.category.DEFAULT");
            actIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            actIntent.setClassName("com.ex.myapplication", "com.ex.myapplication.QrActivity");
            ctx.startActivity(actIntent);
        }
    }
}
