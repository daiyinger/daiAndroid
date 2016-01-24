package com.daiyinger.carplate;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;

public class PlaneUtil {

	void saveBmpToJpg(String fileName, Bitmap bitmap)
	{
		 String sdStatus = Environment.getExternalStorageState();  
         if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) { // 检测sd是否可用  
             return;  
         } 
         FileOutputStream imgout = null;  
  

         try {  
        	 imgout = new FileOutputStream(fileName);  
             bitmap.compress(Bitmap.CompressFormat.JPEG, 100, imgout);// 把数据写入文件  
         } catch (FileNotFoundException e) {  
             e.printStackTrace();  
         } finally {  
             try {  
            	 //imgout.flush();  
            	 imgout.close();  
             } catch (IOException e) {  
                 e.printStackTrace();  
             }  
         }  
	}
	
	/**
	 * 获取版本号
	 * @return 当前应用的版本号
	 */
	public static String getVersion(Context context) {
	    try {
	        PackageManager manager = context.getPackageManager();
	        PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
	        String version = info.versionName;
	        return version;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return "";
	    }
	}
	public static void copyBigDataToSD(Context context, String srcName, String strOutFileName) throws IOException 
    {  
        InputStream myInput;  
        OutputStream myOutput = new FileOutputStream(strOutFileName);  
        myInput = context.getAssets().open(srcName);  
        byte[] buffer = new byte[1024];  
        int length = myInput.read(buffer);
        while(length > 0)
        {
            myOutput.write(buffer, 0, length); 
            length = myInput.read(buffer);
        }
        
        myOutput.flush();  
        myInput.close();  
        myOutput.close();
    }  
}
