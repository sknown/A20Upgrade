package com.wefeng.upgrade;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.drm.DrmStore;
import android.util.Log;
import android.widget.Toast;

public class CheckUpgradeBroadCast extends BroadcastReceiver {
    private String TAG="CheckUpgrade";

    /*
     * 接收静态注册广播的BroadcastReceiver，
     * step1:要到AndroidManifest.xml这里注册消息
     *         <receiver android:name="clsReceiver2">
                <intent-filter>
                    <action
                        android:name="com.testBroadcastReceiver.Internal_2"/>
                </intent-filter>
            </receiver>
        step2:定义消息的字符串
        step3:通过Intent传递消息来驱使BroadcastReceiver触发
     */

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.i(TAG, "ACTION_BOOT_COMPLETED");
        if(action == Intent.ACTION_BOOT_COMPLETED)
        {
            RecoveryUpgrade.start(context);
        }

    }
}
