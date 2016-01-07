/**
 * ��򵥵Ļ���FFmpeg����Ƶ������-��׿
 * Simplest FFmpeg Android Decoder
 * 
 * ������ Lei Xiaohua
 * leixiaohua1020@126.com
 * �й���ý��ѧ/���ֵ��Ӽ���
 * Communication University of China / Digital TV Technology
 * http://blog.csdn.net/leixiaohua1020
 * 
 * �������ǰ�׿ƽ̨����򵥵Ļ���FFmpeg����Ƶ�������������Խ��������Ƶ���ݽ����YUV�������ݡ�
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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

	static boolean init = true;
	static boolean recv_run_flag = false;
	static boolean decode_flag = true;
	static String ip_str=null,port_str=null;
	static TcpThread tcphandle;
	static TextView text1;
	static ImageView images;
	static Bitmap bitmap;
	PowerManager powerManager = null;  
    WakeLock wakeLock = null; 
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text1 = (TextView) this.findViewById(R.id.text_info);
        Button startButton = (Button) this.findViewById(R.id.button_start);
		final Button decodeOnOffBtn = (Button) this.findViewById(R.id.decodeOnOff);
        final EditText urlEdittext_input= (EditText) this.findViewById(R.id.input_url);
		final EditText urlEdittext_output= (EditText) this.findViewById(R.id.output_url);
		images = (ImageView) this.findViewById(R.id.imageView1);
		
		powerManager = (PowerManager)this.getSystemService(this.POWER_SERVICE);  
        wakeLock = this.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
		
		decodeOnOffBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				decode_flag = !decode_flag;
				if(decode_flag)
				{
					decodeOnOffBtn.setText("On");
				}
				else
				{
					decodeOnOffBtn.setText("Off");
				}
			}
			
			
		});
		startButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0){

				String folderurl=Environment.getExternalStorageDirectory().getPath();
				
				String urltext_input=urlEdittext_input.getText().toString();
		        String inputurl=folderurl+"/"+urltext_input;
		              
		        Log.i("inputurl",inputurl);
		        //int res;
		        if(init)
		        {
		        	decode("");
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
    
    @Override  
    protected void onResume() {  
        super.onResume();  
        wakeLock.acquire();  
    }  

    @Override  
    protected void onPause() {  
        super.onPause();  
        wakeLock.release();  
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
        			writeSDString("bitmap msg rx ok");
	            	if(msg.obj != null)
	            	{
	            		images.setImageBitmap((Bitmap)msg.obj);
	            	}
	            	else
	            	{
	            		writeSDString("bitmap msg null");
	            	}
	                break;
	            case 3:
	            	
	            	break;
	            default:  
	                break;            
            }    
            super.handleMessage(msg);  
        }  
    };  
    public void SendMsgText(int id, String str)
    {
    	Message message=new Message(); 
        message.what=id; 
        message.obj = str;
        mHandler.sendMessage(message);
    }
    public class  TcpThread extends Thread 
    {  
		Socket socket = null;
		InputStream inr = null;
		OutputStream out = null;
		int connectCnt = 0;
		int readDataOKFlag = 0;
    	public void run()
	    {
	    	//����һ��Socket����ָ���������˵�IP��ַ�Ͷ˿ں�
		    try {
		    	if(ip_str == null || ip_str == "")
				{
		    		SendMsgText(1,"��ȷ����Ƶ��������IP�Ͷ˿���Ϣ");
		    		return;
				}
		    	else
		    	{
		    		socket = new Socket();
		    		connectFunc();
		    	}
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
				writeSDString("tcp thread start!");
				while(recv_run_flag)
				{
		           len = inr.read(recv_buf);
		           if(len <= 0)
		           {
		                //SendMsgText(1,"Recv Error!" + " last Frame "+frame);
		                writeSDString("Recv Error!" + " last Frame "+frame);
				        if((connectCnt > 3) || (readDataOKFlag > 0))
		                {
				        	recv_run_flag = false;
		                }
				        else
				        {
				        	Thread.sleep(1000);
				        	connectFunc();
				        }
		           }
		           else
		           {
		        	   readDataOKFlag = 1;
		        	   readPos = 0;
		        	   if(init_flag == 0)
		        	   {
		        		   init_flag = 1;
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
		        			   if(pos == 4)	//��� pos �պõ���4  ��û���յ���Ч����
		        			   {
			        			   pos = 0;
			        			   continue;
		        			   }
		        			   frame++;
		        			   SendMsgText(1,"framerate "+frame);
		        			   writeSDString("framerate "+frame);
		        			   if(decode_flag)
		        			   {
			        			   	byte retData[];
			        			    if((retData = decodeOneFrameExt(frame_buf,pos-4)) != null)
				   			        {
			        			    	rgb2ARGB(rgbdata, retData, 320, 240);
			        			    	if(rgbdata != null)
			        			    	{
			        			    		bitmap = Bitmap.createBitmap(rgbdata, 320, 240,Config.ARGB_8888);
			        			    	}
				   						
			        			    	if(bitmap != null)
			        			    	{
			        			    		Message message = new Message(); 
					   			        	message.what = 2; 
					   			        	message.obj = bigImg(bitmap);
						        			mHandler.sendMessage(message);
						        			writeSDString("decode ok "+frame);
			        			    	}
				   			        }
			        			    else
			        			    {
				        				   writeSDString("decode err "+frame);
			        			    }
		        			   }
		        			   else
		        			   {
		        				   SendMsgText(3,"framerate "+frame);
		        			   }
		        			   pos = 0;
		        		   }
		        	   }
		           }
				}
				//�ͷų���
				//wakeLock.release(); 
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				writeSDString("Recv Len UnknownHostException");
				recv_run_flag = false;
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				writeSDString("Recv IOException");
				recv_run_flag = false; 
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    public void connectFunc()
	    {
	    	try
    		{
	    		connectCnt++;
				SendMsgText(1,"connect "+connectCnt);
    			writeSDString("connect "+connectCnt);
				socket.connect(new InetSocketAddress(ip_str,Integer.parseInt(port_str)), 5000);
				inr = socket.getInputStream();
				out = socket.getOutputStream();
	    		out.write(0x01);
    		}
    		catch (IllegalArgumentException e){
    			// TODO Auto-generated catch block
				writeSDString("connect IllegalArgumentException "+connectCnt);
				e.printStackTrace();
    		}
	    	catch (IOException e) {
				// TODO Auto-generated catch block
				writeSDString("connect IOException "+connectCnt);
				e.printStackTrace();
			} 
	    }
    }
    
    private static Bitmap bigImg(Bitmap bitmap) {
		  Matrix matrix = new Matrix(); 
		  matrix.postScale(1.5f,1.5f); //���Ϳ�Ŵ���С�ı���
		  Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
		  return resizeBmp;
	 }

    
    //д�ļ�
    public void writeSDFile(byte [] bytes) 
    {  
    		String fileName = Environment.getExternalStorageDirectory().getPath();
            File file = new File(fileName+"/video_debug.dat"); 
     
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
    
    //д�ļ�
    public void writeSDString(String str) 
    {  
    		String fileName = Environment.getExternalStorageDirectory().getPath();
    		File file = new File(fileName+"/video_debug.txt");  
     
            FileOutputStream fos;
			try {
				fos = new FileOutputStream(file,true);
				str += "\n";
				fos.write(str.getBytes());
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
    
    public static void rgb2ARGB(int argb[], byte[] rgb, int width, int height){
		int line_len = width*3;
		int pos = 0;
		for(int line =0; line < height ; line++)
		{
			for(int i=0; i < line_len; i += 3)
			{
				argb[pos++] = 0xFF000000|(((int)rgb[line*line_len+i]<<16)&0xFF0000)|
						(((int)rgb[line*line_len+i+1]<<8)&0xFF00)|(((int)rgb[line*line_len+i+2])&0xFF);
			}
		}
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
