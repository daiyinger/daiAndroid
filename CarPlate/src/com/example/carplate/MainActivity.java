package com.example.carplate;

import java.io.File;
import java.io.FileNotFoundException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private ImageView imageView = null;  
	private Bitmap bmp = null; 
	private Button btnTrain = null;
	private Button btnPic = null;
	private TextView m_text = null;
	private String path = null; //SDCARD ��Ŀ¼
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		imageView = (ImageView) findViewById(R.id.image_view);  
		m_text = (TextView) findViewById(R.id.myshow);
		btnTrain = (Button) findViewById(R.id.btn_plate);
		btnPic = (Button) findViewById(R.id.btn_pick);
		
		btnTrain.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				 new MyTask().execute();
			}});
		btnPic.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				  
				startActivityForResult(i, 1);
			}});
	    //����������ͼ����س����в�������ʾ
		 bmp = BitmapFactory.decodeResource(getResources(), R.drawable.test);  
	     imageView.setImageBitmap(bmp);
	     path = Environment.getExternalStorageDirectory().getAbsolutePath();//��ȡ��Ŀ¼ 
	     System.out.println(path);
	}

	//OpenCV�����ز���ʼ���ɹ���Ļص��������ڴ����ǲ������κβ���  
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {  
       @Override  
       public void onManagerConnected(int status) {  
           switch (status) {  
               case LoaderCallbackInterface.SUCCESS:{  
                   System.loadLibrary("imageproc");  
               } break;  
               default:{  
                   super.onManagerConnected(status);  
               } break;  
           }  
       }  
   };  
   
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	 
	    if (requestCode == 1 && resultCode == RESULT_OK && null != data) {
	    	Uri uri = data.getData();
	    	ContentResolver cr = this.getContentResolver();  
            try {  
            	bmp = BitmapFactory.decodeStream(cr.openInputStream(uri));  
                imageView.setImageBitmap(bmp);
            } catch (FileNotFoundException e) {  
                	//Log.e("Exception", e.getMessage(),e);  
            }  
	    }
   }
   
   private class MyTask extends AsyncTask<String, Integer, String> {  
       //onPreExecute����������ִ�к�̨����ǰ��һЩUI����  
       @Override  
       protected void onPreExecute() {  
       }  
         
       //doInBackground�����ڲ�ִ�к�̨����,�����ڴ˷������޸�UI  
       @Override  
       protected String doInBackground(String... params) {  
           try {  
        	   System.out.println("entering the jni");
    	 	   SendMsgText("����ʶ��.....",1);
    	 	   int w = bmp.getWidth();
    	 	   int h = bmp.getHeight();
    	 	   int[] pixels = new int[w * h];
    	 	   String result=null;
    	 	   bmp.getPixels(pixels, 0, w, 0, 0, w, h);
    		   Thread.sleep(100);
    	 	   result=CarPlateDetection.ImageProc(pixels, w, h,path);
    	 	   System.out.println(result);
    	 	   if(result != null)
    	 	   {
    	 		   SendMsgText(result,1);
        	 	   SendMsgText(result,2);
    	 	   }
    	 	   else
    	 	   {
    	 		  SendMsgText("ʶ��ʧ��!",1);
    	 	   }
           } catch (Exception e) {  
        	   SendMsgText("entering the detect error",2);
           }
		return null;
       }  
   }
   public Handler mHandler=new Handler()  
   {  
       public void handleMessage(Message msg)  
       {  
           switch(msg.what)  
           {  
	            case 1: 
	            	//Toast.makeText(getApplicationContext(), (String)msg.obj, Toast.LENGTH_SHORT).show();
	            	m_text.setText((String)msg.obj);
	                break; 
	            case 2: 
	            	Toast.makeText(getApplicationContext(), (String)msg.obj, Toast.LENGTH_SHORT).show();
	                break;
	            default:  
	                break;            
           }    
           super.handleMessage(msg);  
       }  
   }; 
  
   public void SendMsgText(String str, int id)
   {
   		Message message=new Message(); 
   		message.what=id; 
   		message.obj = str;
   		mHandler.sendMessage(message);
   }
   
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		  //ͨ��OpenCV���������ز���ʼ��OpenCV��⣬��νOpenCV���������  
       //OpenCV_2.4.3.2_Manager_2.4_*.apk�������������OpenCV��װ����apkĿ¼��  
       OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);  
	}
}
