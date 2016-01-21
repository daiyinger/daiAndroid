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
import android.provider.MediaStore.Images.ImageColumns;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
	private String path = null; //SDCARD 根目录
	String imgpath = null;
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
	    //将汽车完整图像加载程序中并进行显示
		 bmp = BitmapFactory.decodeResource(getResources(), R.drawable.plate_locate);  
	     imageView.setImageBitmap(bmp);
	     path = Environment.getExternalStorageDirectory().getAbsolutePath();//获取跟目录 
	     imgpath = path+"/plate_locate.jpg";
	     System.out.println(path);
	}

	//OpenCV类库加载并初始化成功后的回调函数，在此我们不进行任何操作  
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
                imgpath = getRealFilePath(getApplicationContext(), uri);
            } catch (FileNotFoundException e) {  
                	//Log.e("Exception", e.getMessage(),e);  
            }  
	    }
   }
   
   /**
    * Try to return the absolute file path from the given Uri
    *
    * @param context
    * @param uri
    * @return the file path or null
    */
   public static String getRealFilePath( final Context context, final Uri uri ) {
       if ( null == uri ) return null;
       final String scheme = uri.getScheme();
       String data = null;
       if ( scheme == null )
           data = uri.getPath();
       else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
           data = uri.getPath();
       } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
           Cursor cursor = context.getContentResolver().query( uri, new String[] { ImageColumns.DATA }, null, null, null );
           if ( null != cursor ) {
               if ( cursor.moveToFirst() ) {
                   int index = cursor.getColumnIndex( ImageColumns.DATA );
                   if ( index > -1 ) {
                       data = cursor.getString( index );
                   }
               }
               cursor.close();
           }
       }
       return data;
   }
   
   private class MyTask extends AsyncTask<String, Integer, String> {  
       //onPreExecute方法用于在执行后台任务前做一些UI操作  
       @Override  
       protected void onPreExecute() {  
       }  
         
       //doInBackground方法内部执行后台任务,不可在此方法内修改UI  
       @Override  
       protected String doInBackground(String... params) {
    	   try {  
				String svmpath = path+"/svm.xml";
				String annpath = path+"/ann.xml";
			    System.out.println("entering the jni");
			    SendMsgText("正在识别.....",1);
			    Thread.sleep(100);
			    String result = null;
			    byte[] resultByte =CarPlateDetection.ImageProc(imgpath, svmpath, annpath);
			    System.out.println(result);
			    if(resultByte != null)
			    {
			    	result = new String(resultByte,"GBK");				   
   	 		   		SendMsgText(result,1);
   	 		   		SendMsgText(result,2);
			    }
			    else
			    {
			    	SendMsgText("识别失败!",1);
			    }
          } 
    	  catch (Exception e) {  
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
		  //通过OpenCV引擎服务加载并初始化OpenCV类库，所谓OpenCV引擎服务即是  
       //OpenCV_2.4.3.2_Manager_2.4_*.apk程序包，存在于OpenCV安装包的apk目录中  
       OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);  
	}
}
