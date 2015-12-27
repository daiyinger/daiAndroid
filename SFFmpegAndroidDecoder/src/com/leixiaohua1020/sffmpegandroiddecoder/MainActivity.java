/**
 * 最简单的基于FFmpeg的视频解码器-安卓
 * Simplest FFmpeg Android Decoder
 * 
 * 雷霄骅 Lei Xiaohua
 * leixiaohua1020@126.com
 * 中国传媒大学/数字电视技术
 * Communication University of China / Digital TV Technology
 * http://blog.csdn.net/leixiaohua1020
 * 
 * 本程序是安卓平台下最简单的基于FFmpeg的视频解码器。它可以将输入的视频数据解码成YUV像素数据。
 * 
 * This software is the simplest decoder based on FFmpeg in Android. It can decode video stream
 * to raw YUV data.
 * 
 */
package com.leixiaohua1020.sffmpegandroiddecoder;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	boolean init = true;
	boolean recv_run_flag = false;
	String ip_str=null,port_str=null;
	TcpThread tcphandle;
	TextView text1;
	ImageView images;
	Bitmap bitmap;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        final TextView text = (TextView) this.findViewById(R.id.text_label1);
        text1 = (TextView) this.findViewById(R.id.text_info);
        Button startButton = (Button) this.findViewById(R.id.button_start);
		final EditText urlEdittext_input= (EditText) this.findViewById(R.id.input_url);
		final EditText urlEdittext_output= (EditText) this.findViewById(R.id.output_url);
		images = (ImageView) this.findViewById(R.id.imageView1);
		startButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0){

				String folderurl=Environment.getExternalStorageDirectory().getPath();
				
				String urltext_input=urlEdittext_input.getText().toString();
		        String inputurl=folderurl+"/"+urltext_input;
		              
		        Log.i("inputurl",inputurl);
		        int res;
		        if(init)
		        {
		        	decode(folderurl+"/"+"555.h264");
			        init = false;
			            
		        }
		        ip_str = urlEdittext_input.getText().toString();
		        port_str = urlEdittext_output.getText().toString();
		        tcphandle = new TcpThread();
		        recv_run_flag = true;
		        tcphandle.start();
			}
		});
    }
    public Handler mHandler=new Handler()  
    {  
        public void handleMessage(Message msg)  
        {  
            switch(msg.what)  
            {  
	            case 1: 
	            	//Toast.makeText(getApplicationContext(), (String)msg.obj, Toast.LENGTH_SHORT).show();
	            	text1.setText((String)msg.obj);
	                break; 
	            case 2: 
	            	if(msg.obj != null)
	            		images.setImageBitmap((Bitmap)msg.obj);
	                break;
	            default:  
	                break;            
            }    
            //super.handleMessage(msg);  
        }  
    };  
    public void SendMsgText(String str)
    {
    	Message message=new Message(); 
        message.what=1; 
        message.obj = str;
        mHandler.sendMessage(message);
    }
    public class  TcpThread extends Thread 
    {  
    	public void run()
	    {
			Socket socket;
	    	//创建一个Socket对象，指定服务器端的IP地址和端口号
		    try {
		    	if(ip_str == null || ip_str == "")
				{
		    		socket = new Socket("103.44.145.243",22175);//"192.168.1.45",10025
				}
		    	else
		    	{
		    		
		    		socket = new Socket(ip_str,Integer.parseInt(port_str));
		    		SendMsgText("connect");
		    	}
		    	//socket = new Socket("172.27.98.8",10025);//"192.1
		    	InputStream inr = socket.getInputStream();
				byte [] recv_buf = new byte[1024*100];
				byte [] frame_buf = new byte[1024*100];
				int rgbdata[] = new int[320*240*3];
				int len;
				int readPos = 0;
				int pos = 0;
				int init_flag = 0;
				int cycle = 0;
				int info = 0;
				int frame = 0;
				//String folderurl=Environment.getExternalStorageDirectory().getPath();
				
				while(recv_run_flag)
				{
		           len = inr.read(recv_buf);
		           if(len <= 0)
		           {
		                SendMsgText("Recv Error!");
				        recv_run_flag = false;
		           }
		           else
		           {
		        	   readPos = 0;
		        	   if(init_flag == 0)
		        	   {
		        		   init_flag = 1;
		        		   frame_buf[pos++] = recv_buf[readPos++];
		        		   frame_buf[pos++] = recv_buf[readPos++];
		        		   frame_buf[pos++] = recv_buf[readPos++];
		        		   frame_buf[pos++] = recv_buf[readPos++];
		        		   if (frame_buf[pos-4] != 0xFF || frame_buf[pos-3] != 0xFF || frame_buf[pos-2] != 0x55 || frame_buf[pos-1] != 0xAA)
		        		   {
								
		        		   }
		        		   pos = 0;
		        		   cycle = 4;
		        	   }
		        	   else
		        	   {
		        		   cycle = 0;
		        	   }
		        	   for(;cycle < len;cycle++)
		        	   {
		        		   frame_buf[pos++] = recv_buf[readPos++];
		        		   if(pos >= 4)
		        		   {
		        			   if (frame_buf[pos-4] != (byte)0xFF || frame_buf[pos-3] !=  (byte)0xFF || frame_buf[pos-2] !=  (byte)0x55 || frame_buf[pos-1] !=  (byte)0xAA)
			        		   {
			        			   info = 0;
			        		   }
			        		   else
			        		   {
			        			   info = 1;
			        		   }
		        		   }
		        		   else
		        		   {
		        			   info = 0;
		        		   }
		        		   if(info == 1)
		        		   {
		        			   byte retData[];
		        			   frame++;
		        			   SendMsgText("framerate "+frame);
		        			   
		        			    if((retData = decodeOneFrameExt(frame_buf,pos-4)) != null)
			   			        {
		        				   	//if(frame < 200)
		        				   	//writeSDFile(folderurl+"/test.yuv",rets);

		        			    	int []rgbdatas = null;
		        			    	rgbdatas = rgb2ARGB(retData,320,240);
		        			    	if(rgbdata != null)
		        			    	{
		        			    		bitmap = Bitmap.createBitmap(rgbdatas, 320, 240,Config.ARGB_8888);
		        			    	}
			   						
		        			    	if(bitmap != null)
		        			    	{
		        			    		Message message = new Message(); 
				   			        	message.what = 2; 
				   			        	message.obj = bigImg(bitmap);
					        			mHandler.sendMessage(message);
		        			    	}
			   			        }

		        			    pos = 0;
		        		   }
		        	   }
		           }
				}
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				SendMsgText("Recv Len UnknownHostException");
				recv_run_flag = false;
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				SendMsgText("Recv IOException");
				recv_run_flag = false; 
				e.printStackTrace();
			}
		    
	    }
    }
    
    private static Bitmap bigImg(Bitmap bitmap) {
		  Matrix matrix = new Matrix(); 
		  matrix.postScale(1.5f,1.5f); //长和宽放大缩小的比例
		  Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
		  return resizeBmp;
	 }

    
  //写文件
    public void writeSDFile(String fileName, byte [] bytes) 
    {  
            File file = new File(fileName); 
     
            FileOutputStream fos;
			try {
				fos = new FileOutputStream(file,true);       
				fos.write(bytes);
				fos.close();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}          
    }
    
    public static int[] rgb2ARGB(byte[] rgb, int width, int height){
		int ret[] = new int[width*height];
		int line_len = width*3;
		int pos = 0;
		for(int line =0; line < height ; line++)
		{
			for(int i=0; i < line_len; i += 3)
			{
				ret[pos++] = 0xFF000000|(((int)rgb[line*line_len+i]<<16)&0xFF0000)|
						(((int)rgb[line*line_len+i+1]<<8)&0xFF00)|(((int)rgb[line*line_len+i+2])&0xFF);
			}
		}
		return ret;
	}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    //JNI
    public native int decode(String inputurl);
    public native byte [] readFrame();
    public native byte [] decodeOneFrameExt(byte data[], int len);
    
    static{
    	System.loadLibrary("avutil-54");
    	System.loadLibrary("swresample-1");
    	System.loadLibrary("avcodec-56");
    	System.loadLibrary("avformat-56");
    	System.loadLibrary("swscale-3");
    	System.loadLibrary("postproc-53");
    	System.loadLibrary("avfilter-5");
    	System.loadLibrary("avdevice-56");
    	System.loadLibrary("sffdecoder");
    }
}
