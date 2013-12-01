package com.wefeng.upgrade;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Environment;

public class FileUtil{
    private String SDPATH;

    public String getSDPATH(){
        return SDPATH;
    }

    public FileUtil(){
        //SDPATH= Environment.getExternalStorageDirectory()+"/";
        SDPATH = "/cache/";
    }

    public File createSDFile(String fileName) throws IOException{
        File file = new File(SDPATH+fileName);
        file.createNewFile();
        return file;
    }
    public File createSDDir(String dirName) {
        File dir = new File(SDPATH+dirName);
        dir.mkdir();
        return dir;
    }

    public void delFile(String fileName)
    {
        File file = new File(SDPATH+fileName);
        file.delete();
    }

    public boolean isFileExist(String fileName){
        File file = new File(SDPATH+fileName);
        return file.exists();
    }
    public File write2SDFromInput(String path,String fileName,InputStream input){
        File file = null;
        OutputStream output = null;
        try{
            createSDDir(path);
            file = createSDFile(path+fileName);
            output = new FileOutputStream(file);
            byte buffer[] = new byte[4*1024];
            int readLen = 0;

            while((readLen = input.read(buffer))!=-1){
                output.write(buffer, 0, readLen);
            }
            output.flush();
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            try{
                output.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        return file;
    }

}