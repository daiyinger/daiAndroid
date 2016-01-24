package com.daiyinger.carplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
    private int window_width, window_height;// 控件宽度
	private int state_height;// 状态栏的高度
    private DragImageView  mZoomView = null;
    private ViewTreeObserver viewTreeObserver;
	private Bitmap bmp = null; 
	private Button btnTrain = null;
	private Button btnPic = null;
	private TextView m_text = null;
	private String path = null; //SDCARD 根目录
	String imgpath = null;
	boolean selected_img_flag = false;
	@SuppressWarnings({ "deprecation" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		m_text = (TextView) findViewById(R.id.myshow);
		btnTrain = (Button) findViewById(R.id.btn_plate);
		btnPic = (Button) findViewById(R.id.btn_pick);
		
		mZoomView = (DragImageView)findViewById(R.id.imageview);  
		
		btnTrain.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				 if(selected_img_flag == false)
				 {
					 File file=new File(imgpath);
					 if(!file.exists())
					 {
						 SendMsgText("未选择图片 且默认路径 "+imgpath+"图片不存在!", 2);
						 return;
					 }
					 bmp = getLoacalBitmap(imgpath);
					 mZoomView.setImageBitmap(bmp);
					 selected_img_flag = true;
				 }
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
		 bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ai);  
		 mZoomView.setImageBitmap(bmp);
		 /** 获取可見区域高度 **/
		 WindowManager manager = getWindowManager();
		 window_width = manager.getDefaultDisplay().getWidth();
		 window_height = manager.getDefaultDisplay().getHeight();
		 mZoomView.setmActivity(this);//注入Activity.
		 /** 测量状态栏高度 **/
		 viewTreeObserver = mZoomView.getViewTreeObserver();
		 viewTreeObserver
				.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

					@Override
					public void onGlobalLayout() {
						if (state_height == 0) {
							// 获取状况栏高度
							Rect frame = new Rect();
							getWindow().getDecorView()
									.getWindowVisibleDisplayFrame(frame);
							state_height = frame.top;
							mZoomView.setScreen_H(window_height);
							mZoomView.setScreen_W(window_width);
						}

					}
				});
	     path = Environment.getExternalStorageDirectory().getAbsolutePath();//获取跟目录 
	     imgpath = path+"/ai/plate_locate.jpg";
	     System.out.println(path);
	     InitEnv();
	}
	
	//查询xml资源是否存在 如果不存在则从assets进行拷贝
	@SuppressLint("ShowToast")
	void InitEnv()
	{
		 try {
			 String lastVersion = null;
			 SharedPreferences sharedPreferences;
	         sharedPreferences = getSharedPreferences("info",Activity.MODE_PRIVATE); 
	         if(sharedPreferences.contains("version") == true)
	         {    	
	        	lastVersion = sharedPreferences.getString("version","0.0");
	         }
			 String curVersion = PlaneUtil.getVersion(getApplicationContext());
			 if(!curVersion.equals(lastVersion))
			 {
				 String sdpath = Environment.getExternalStorageDirectory().getAbsolutePath();
				 File dir = new File(sdpath + "/ai");
				 if(!dir.isDirectory())
				 {
					 dir.mkdir();
				 }
				 PlaneUtil.copyBigDataToSD(getApplicationContext(),
						 "ann.xml",sdpath + "/ai/ann.xml");
				 PlaneUtil.copyBigDataToSD(getApplicationContext(),
						 "svm.xml",sdpath + "/ai/svm.xml");
				 PlaneUtil.copyBigDataToSD(getApplicationContext(),
						 "plate_locate.jpg",sdpath + "/ai/plate_locate.jpg");
				 dir = new File(sdpath + "/ai/etc/");
				 if(!dir.isDirectory())
				 {
					 dir.mkdir();
				 }
				 PlaneUtil.copyBigDataToSD(getApplicationContext(),
						 "province_mapping",sdpath + "/ai/etc/province_mapping");
				 
				 SharedPreferences.Editor editor = sharedPreferences.edit(); 
		     	 //用putString的方法保存数据 
		     	 editor.putString("version",curVersion);
		     	 editor.commit(); 
			 }
		 } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		 }
	}
	
	
	 /**
	    * 加载本地图片
	    * @param url
	    * @return
	    */
    public static Bitmap getLoacalBitmap(String url) {
         try {
              FileInputStream fis = new FileInputStream(url);
              return BitmapFactory.decodeStream(fis);  ///把流转化为Bitmap图片        

           } catch (FileNotFoundException e) {
              e.printStackTrace();
              return null;
         }
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
	 
	    if (resultCode == RESULT_OK && null != data) {
	    	if(requestCode == 1)
	    	{
		    	Uri uri = data.getData();
		    	ContentResolver cr = this.getContentResolver();  
	            try {  
	            	bmp = BitmapFactory.decodeStream(cr.openInputStream(uri));  
	            	mZoomView.setImageBitmap(bmp);
	                imgpath = getRealFilePath(getApplicationContext(), uri);
	            } catch (FileNotFoundException e) {  
	                	//Log.e("Exception", e.getMessage(),e);  
	            }  
	    	}
	    	else if(requestCode == 2)
	    	{
	    	
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
    		    String resultImgDirPath = path +"/ai/tmp/";
    		   	String logpath = path+"/ai/ai_log.log";
				String svmpath = path+"/ai/svm.xml";
				String annpath = path+"/ai/ann.xml";
				String imagepath =  new String(imgpath.getBytes(),"gbk");
			    System.out.println("entering the jni");
			    SendMsgText("正在识别.....",1);
			    Thread.sleep(100);
			    String result = null;
			    
			    byte[] resultByte =CarPlateDetection.ImageProc(path, logpath, imagepath, svmpath, annpath);
			    System.out.println(result);
			    if(resultByte != null)
			    {
			    	bmp = BitmapFactory.decodeFile(resultImgDirPath+"result.jpg");
	                SendMsgRefresh(3);
			    	result = new String(resultByte,"UTF-8");				   
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
	            	m_text.setText((String)msg.obj);
	                break; 
	            case 2: 
	            	Toast.makeText(getApplicationContext(), (String)msg.obj, Toast.LENGTH_SHORT).show();
	                break;
	            case 3:
	            	mZoomView.setImageBitmap(bmp);
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
   
   public void SendMsgRefresh(int id)
   {
   		Message message=new Message(); 
   		message.what=id; 
   		message.obj = null;
   		mHandler.sendMessage(message);
   }
   
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		//通过OpenCV引擎服务加载并初始化OpenCV类库，所谓OpenCV引擎服务即是  
		//OpenCV_2.4.3.2_Manager_2.4_*.apk程序包，存在于OpenCV安装包的apk目录中  
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);  
	}
	
  	@Override
  	public boolean onOptionsItemSelected(MenuItem item) {
  		File root;
	    Uri uri;
	    Intent intent;
	  	switch (item.getItemId()) {
	
	  		case R.id.action_settings:
	  			//intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);  
                //startActivityForResult(intent, 2); 
	  			break;
	  		case R.id.action_view_image:
	  			root = new File(Environment.getExternalStorageDirectory().getPath()
	  					+ "/ai/tmp/result.jpg");
			    uri = Uri.fromFile(root);
			    intent = new Intent();
			    intent.setAction(android.content.Intent.ACTION_VIEW);
			    intent.setDataAndType(uri , "image/*"); 
			    startActivity(intent);
	  			break;
	  		case R.id.action_about:
	  			Toast.makeText(getApplicationContext(),"daiyinger", Toast.LENGTH_SHORT).show();
	  			break;
	  		default:
	  			break;
	  	}
	  	return super.onOptionsItemSelected(item);
  	}	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
}
