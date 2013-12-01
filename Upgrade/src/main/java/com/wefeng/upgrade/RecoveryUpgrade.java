package com.wefeng.upgrade;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Administrator on 13-11-17.
 */
public class RecoveryUpgrade {

    public final static String UPGRADE_MODE_FORCE = "force";
    public final static String UPGRADE_MODE_MANUAL = "manual";
    public final static String mUpgradeConfigUrl = "http://oss.aliyuncs.com/myche/system_upgrade_config.xml";

    static {
        System.loadLibrary("system_upgrade");
    }

    private static String TAG = "RecoveryUpgrade";
    public static int CHECK_SPACE = 1000*30;
    private static String mUpgradeFile = "/upgrade.zip";
    private static String mRecoveryUpdateFile = "/update.zip";

    public static void reboot(Context context)
    {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if(nativeInstallPackageEx(context, mUpgradeFile))
        {
            pm.reboot("recovery");
        }
        else
        {
            Log.e(TAG, "check /cache/upgrade.zip fail");
        }
    }
    public static native boolean nativeInstallPackageEx(Context context,
                                                         String path);


    public void delUpgradeFile()
    {
        FileUtil fileUtil = new FileUtil();
        if(fileUtil.isFileExist(mUpgradeFile))
        {
            fileUtil.delFile(mUpgradeFile);
        }
        if(fileUtil.isFileExist(mRecoveryUpdateFile))
        {
            fileUtil.delFile(mRecoveryUpdateFile);
        }

    }
    public static void start(final Context context)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                RecoveryUpgrade upgrade = new RecoveryUpgrade();

                upgrade.delUpgradeFile();

                while(true)
                {
                    int ret = upgrade.downLoadUpgrade(null);

                    if(ret == 0)
                    {
                        if(mUpgradeMode == UpgradeMode.UPGRADE_MODE_FORCE_E)
                        {
                            Log.i(TAG, "reboot");
                            reboot(context);
                            break;
                        }
                        else
                        {
                            Intent intent = new Intent("com.wefeng.upgrade.MainActivity");
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            context.startActivity(intent);
                            break;
                        }
                    }

                    try {
                        Thread.sleep(RecoveryUpgrade.CHECK_SPACE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private static UpgradeMode mUpgradeMode = UpgradeMode.UPGRADE_MODE_NONE_E;

    private int downLoadUpgrade(String urlStr)
    {
        URL url= null;
        InputStream stream = null;
        UpgradeMode isUpgrade  = UpgradeMode.UPGRADE_MODE_NONE_E;

        mUpgradeMode = UpgradeMode.UPGRADE_MODE_NONE_E;

        if(urlStr == null)
        {
            urlStr = mUpgradeConfigUrl;
        }

        try {
            url = new URL(urlStr);

            HttpURLConnection conn=(HttpURLConnection)url.openConnection();

            stream = conn.getInputStream();

            Log.i(TAG, "start parse upgrade config");

            isUpgrade = parseUpgradeConfig(stream, Integer.valueOf(Build.VERSION.INCREMENTAL));

            mUpgradeMode = isUpgrade;
            if(isUpgrade != UpgradeMode.UPGRADE_MODE_NONE_E)
            {
                Log.i(TAG, "start down upgrade file");
                return downFile(mUpgradeUrl,"/","upgrade.zip");
                //return downFile(mUpgradeConfigUrl,"/","upgrade.zip");
            }
            else
            {
                return 1;//needn't to upgrade
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    private String mUpgradeUrl = null;
    private enum UpgradeMode
    {
        UPGRADE_MODE_NONE_E,
        UPGRADE_MODE_FORCE_E,
        UPGRADE_MODE_MANUAL_E,
    }

    private String getString(String pro)
    {
        Class<?> systemProperties = null;
        try {
            systemProperties = Class.forName("android.os.SystemProperties");
            Method getMode = systemProperties.getMethod("get", String.class, String.class);

            return (String)getMode.invoke(null, pro, "");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return "";
    }
    public UpgradeMode parseUpgradeConfig(InputStream inputStream, int SysVersion) throws Exception
    {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, "UTF-8");
        UpgradeMode isUpgrade = UpgradeMode.UPGRADE_MODE_NONE_E;
        Boolean parseUpgradeTag  = false;

        int event = parser.getEventType();
        while(event!=XmlPullParser.END_DOCUMENT){
            switch(event)
            {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    //Log.i(TAG, "found xml tag");
                    if("upgrade".equals(parser.getName()))
                    {
                        //Log.i(TAG, "found upgrade tag");
                        parseUpgradeTag = true;
                        //<upgrade version="2" versionCode="TestVersion" hw="new" mode="force" patchCode="none"><![CDATA[http://oss.aliyuncs.com/myche/upgrade.zip]]></upgrade>
                        int version = Integer.valueOf(parser.getAttributeValue(0));
                        String versionCode = parser.getAttributeValue(1);
                        String hw = parser.getAttributeValue(2);
                        String upgradeMode = parser.getAttributeValue(3);
                        String patchCode = parser.getAttributeValue(4);

                        String localHw = getString("ro.product.firmware");
                        if(!hw.equalsIgnoreCase(localHw))
                        {
                            Log.i(TAG, "hw error " + "network " + hw + " local hw " + localHw);
                            break;
                        }

                        if(SysVersion<version && UPGRADE_MODE_FORCE.equals(upgradeMode))
                        {
                            Log.i(TAG, "found force mode");
                            isUpgrade = UpgradeMode.UPGRADE_MODE_FORCE_E;
                        }
                        else if(SysVersion<version && UPGRADE_MODE_MANUAL.equals(upgradeMode))
                        {
                            Log.i(TAG, "found manual mode");
                            isUpgrade = UpgradeMode.UPGRADE_MODE_MANUAL_E;
                        }
                    }
                    break;
                case XmlPullParser.TEXT:
                {
                    if(parseUpgradeTag)
                    {
                        String text = parser.getText();
                        mUpgradeUrl = text;
                    }
                    break;
                }
                case XmlPullParser.END_TAG:
                    parseUpgradeTag = false;
                    break;
            }
            event = parser.next();
        }

        //Log.i(TAG, "upgrae");

        return isUpgrade;
    }

    public InputStream getInputStreamFromUrl(String urlStr) throws MalformedURLException,IOException{
        URL url = new URL(urlStr);
        HttpURLConnection urlCon =(HttpURLConnection)url.openConnection();
        InputStream inputStream = urlCon.getInputStream();
        return inputStream;

    }

    /**
     * 下载文件并写SD卡
     * @param urlStr
     * @param path
     * @param fileName
     * @return 0-success,-1-fail,1-existed
     */
    public int downFile(String urlStr,String path,String fileName){
        InputStream inputStream= null;
        try{
            FileUtil fileUtil = new FileUtil();
            if(fileUtil.isFileExist(path+fileName))
            {
                Log.i(TAG, "upgrade file exist, fail");
                return 1;
            }
            else
            {
                inputStream = getInputStreamFromUrl(urlStr);
                File resultFile = fileUtil.write2SDFromInput(path, fileName, inputStream);
                if(resultFile == null)
                {
                    Log.i(TAG, "download upgrade file fail");
                    return -1;
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }finally{
            try{
                if(inputStream != null)
                {
                    inputStream.close();
                }
            }catch(Exception e){
                e.printStackTrace();
            }

        }

        Log.i(TAG,"download upgrade file success");
        return 0;
    }
}
