package com.daiyinger.carplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.http.client.utils.URIUtils;
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
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
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
	
    private int window_width, window_height;// �ؼ����
	private int state_height;// ״̬���ĸ߶�
    private DragImageView  mZoomView = null;
    private ViewTreeObserver viewTreeObserver;
	private Bitmap bmp = null; 
	private Button btnTrain = null;
	private Button btnPic = null;
	private TextView m_text = null;
	private String path = null; //SDCARD ��Ŀ¼
	private Uri imgUri = null;
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
						 SendMsgText("δѡ��ͼƬ ��Ĭ��·�� "+imgpath+"ͼƬ������!", 2);
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
	    //����������ͼ����س����в�������ʾ
		 bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ai);  
		 mZoomView.setImageBitmap(bmp);
		 /** ��ȡ��Ҋ����߶� **/
		 WindowManager manager = getWindowManager();
		 window_width = manager.getDefaultDisplay().getWidth();
		 window_height = manager.getDefaultDisplay().getHeight();
		 mZoomView.setmActivity(this);//ע��Activity.
		 /** ����״̬���߶� **/
		 viewTreeObserver = mZoomView.getViewTreeObserver();
		 viewTreeObserver
				.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

					@Override
					public void onGlobalLayout() {
						if (state_height == 0) {
							// ��ȡ״�����߶�
							Rect frame = new Rect();
							getWindow().getDecorView()
									.getWindowVisibleDisplayFrame(frame);
							state_height = frame.top;
							mZoomView.setScreen_H(window_height);
							mZoomView.setScreen_W(window_width);
						}

					}
				});
	     path = Environment.getExternalStorageDirectory().getAbsolutePath();//��ȡ��Ŀ¼ 
	     imgpath = path+"/ai/plate_locate.jpg";
	     System.out.println(path);
	     InitEnv();
	}
	
	//��ѯxml��Դ�Ƿ���� ������������assets���п���
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
		     	 //��putString�ķ����������� 
		     	 editor.putString("version",curVersion);
		     	 editor.commit(); 
			 }
		 } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		 }
	}
	
	
	 /**
	    * ���ر���ͼƬ
	    * @param url
	    * @return
	    */
    public static Bitmap getLoacalBitmap(String url) {
         try {
              FileInputStream fis = new FileInputStream(url);
              return BitmapFactory.decodeStream(fis);  ///����ת��ΪBitmapͼƬ        

           } catch (FileNotFoundException e) {
              e.printStackTrace();
              return null;
         }
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
	 
	    if (resultCode == RESULT_OK && null != data) {
	    	if(requestCode == 1)
	    	{
		    	Uri uri = data.getData();
		    	imgUri = uri;
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
	    		bmp = BitmapFactory.decodeFile(imgpath); 
	    		//bmp = data.getParcelableExtra("data");
	    		mZoomView.setImageBitmap(bmp);
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
   
   /**
    * �ݹ�ɾ��Ŀ¼�µ������ļ�����Ŀ¼�������ļ�
    * @param dir ��Ҫɾ�����ļ�Ŀ¼
    * @return boolean Returns "true" if all deletions were successful.
    *                 If a deletion fails, the method stops attempting to
    *                 delete and returns "false".
    */
   private static boolean deleteDir(File dir) {
       if (dir.isDirectory()) {
           String[] children = dir.list();
           //�ݹ�ɾ��Ŀ¼�е���Ŀ¼��
           for (int i=0; i<children.length; i++) {
               boolean success = deleteDir(new File(dir, children[i]));
               if (!success) {
                   return false;
               }
           }
       }
       // Ŀ¼��ʱΪ�գ�����ɾ��
       return dir.delete();
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
    		    String resultImgDirPath = path +"/ai/tmp/";
    		   	String logpath = path+"/ai/ai_log.log";
				String svmpath = path+"/ai/svm.xml";
				String annpath = path+"/ai/ann.xml";
				String imagepath =  new String(imgpath.getBytes(),"gbk");
			    System.out.println("entering the jni");
			    SendMsgText("����ʶ��.....",1);
			    Thread.sleep(100);
			    String result = null;
			    if(deleteDir(new File(resultImgDirPath)) != true)
			    {
			    	//SendMsgText("ɾ����ʱ�ļ�ʧ��!",2);
			    }
			    byte[] resultByte =CarPlateDetection.ImageProc(path, logpath, imagepath, svmpath, annpath);
			    if(resultByte != null)
			    {
				    System.out.println(result);
			    	bmp = BitmapFactory.decodeFile(resultImgDirPath + "result.jpg");
	                SendMsgRefresh(3);
			    	result = new String(resultByte,"UTF-8");				   
   	 		   		SendMsgText(result,1);
   	 		   		SendMsgText(result,2);
			    }
			    else
			    {
			    	SendMsgText("ʶ��ʧ��!",1);
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
		//ͨ��OpenCV���������ز���ʼ��OpenCV��⣬��νOpenCV���������  
		//OpenCV_2.4.3.2_Manager_2.4_*.apk�������������OpenCV��װ����apkĿ¼��  
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);  
	}
	
	/**
	 * ����ϵͳͼƬ�༭���вü�
	 */
	public void startPhotoCrop(Uri uri , String output_name) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("scale", true);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(output_name)));
		intent.putExtra("return-data", false);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); // no face detection
		startActivityForResult(intent, 2);
	}
	
	/**
     * ��ת��ϵͳ��ͼ������н�ͼ
     * 
     * @param data
     * @param size
     */ 
    private void startPhotoZoom(Uri data, int size) { 
        Intent intent = new Intent("com.android.camera.action.CROP"); 
        intent.setDataAndType(data, "image/*"); 
        // cropΪtrueʱ��ʾ��ʾ��view���Լ��� 
        intent.putExtra("crop", "true"); 
        intent.putExtra("scale", "true"); 
        // aspectX aspectY �ǿ�ߵı��� 
        intent.putExtra("aspectX", 1); 
        intent.putExtra("aspectY", 1); 
        // outputX,outputY �Ǽ���ͼƬ�Ŀ�� 
        intent.putExtra("outputX", size); 
        intent.putExtra("outputY", size); 
        intent.putExtra("return-data", true); 
        startActivityForResult(intent, 2); 
    } 
    
    public Uri getUriFromPath(String Path)
    {
    	Uri res = null;
        if (path != null) {
            path = Uri.decode(path);
            ContentResolver cr = this.getContentResolver();
            StringBuffer buff = new StringBuffer();
            buff.append("(")
                    .append(Images.ImageColumns.DATA)
                    .append("=")
                    .append("'" + path + "'")
                    .append(")");
            Cursor cur = cr.query(
                    Images.Media.EXTERNAL_CONTENT_URI,
                    new String[] { Images.ImageColumns._ID },
                    buff.toString(), null, null);
            int index = 0;
            for (cur.moveToFirst(); !cur.isAfterLast(); cur
                    .moveToNext()) {
                index = cur.getColumnIndex(Images.ImageColumns._ID);
                // set _id value
                index = cur.getInt(index);
            }
            if (index == 0) {
                //do nothing
            } else {
                Uri uri_temp = Uri
                        .parse("content://media/external/images/media/"
                                + index);
                if (uri_temp != null) {
                    res = uri_temp;
                }
            }
        }
        return res;
    }
	
  	@Override
  	public boolean onOptionsItemSelected(MenuItem item) {
  		File root;
	    Uri uri;
	    Intent intent;
	  	switch (item.getItemId()) {
	
	  		case R.id.action_settings:
	  			if(imgUri != null)
	  				{
  						imgpath = path + "/ai/tmp.jpg";
	  					startPhotoCrop(imgUri, imgpath);//startPhotoZoom(imgUri, 200);
	  				}
	  			else
	  				Toast.makeText(getApplicationContext(),"����ѡ��ͼƬ!", 
	  						Toast.LENGTH_SHORT).show();
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
