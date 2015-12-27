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

#ifdef DECODE_YUV
#include <stdio.h>
#include <time.h> 

#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include "libavutil/log.h"

#ifdef ANDROID
#include <jni.h>
#include <android/log.h>
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "(>_<)", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "(^_^)", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("(>_<) " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("(^_^) " format "\n", ##__VA_ARGS__)
#endif


//Output FFmpeg's av_log()
void custom_log(void *ptr, int level, const char* fmt, va_list vl){
	FILE *fp=fopen("/storage/emulated/0/av_log.txt","a+");
	if(fp){
		vfprintf(fp,fmt,vl);
		fflush(fp);
		fclose(fp);
	}
}

AVFormatContext	*pFormatCtx;
int				i, videoindex;
AVCodecContext	*pCodecCtx;
AVCodec			*pCodec;
AVFrame	*pFrame,*pFrameYUV;
uint8_t *out_buffer;
AVPacket *packet;
struct SwsContext *img_convert_ctx;

JNIEXPORT jint JNICALL Java_com_leixiaohua1020_sffmpegandroiddecoder_MainActivity_decode
  (JNIEnv *env, jobject obj, jstring input_jstr, jstring output_jstr)
{
	/*AVFormatContext	*pFormatCtx;
	int				i, videoindex;
	AVCodecContext	*pCodecCtx;
	AVCodec			*pCodec;
	AVFrame	*pFrame,*pFrameYUV;
	uint8_t *out_buffer;
	AVPacket *packet;*/
	int y_size;
	int ret, got_picture;

	FILE *fp_yuv;
	int frame_cnt;
	clock_t time_start, time_finish;
	double  time_duration = 0.0;

	char input_str[500]={0};
	char output_str[500]={0};
	char info[1000]={0};
	sprintf(input_str,"%s",(*env)->GetStringUTFChars(env,input_jstr, NULL));
	sprintf(output_str,"%s",(*env)->GetStringUTFChars(env,output_jstr, NULL));

	//FFmpeg av_log() callback
	av_log_set_callback(custom_log);
	
	av_register_all();
	avformat_network_init();
	pFormatCtx = avformat_alloc_context();

	if(avformat_open_input(&pFormatCtx,input_str,NULL,NULL)!=0){
		LOGE("Couldn't open input stream.\n");
		return -1;
	}
	if(avformat_find_stream_info(pFormatCtx,NULL)<0){
		LOGE("Couldn't find stream information.\n");
		return -1;
	}
	videoindex=-1;
	for(i=0; i<pFormatCtx->nb_streams; i++) 
		if(pFormatCtx->streams[i]->codec->codec_type==AVMEDIA_TYPE_VIDEO){
			videoindex=i;
			break;
		}
	if(videoindex==-1){
		LOGE("Couldn't find a video stream.\n");
		return -1;
	}
	pCodecCtx=pFormatCtx->streams[videoindex]->codec;
	pCodec=avcodec_find_decoder(pCodecCtx->codec_id);
	if(pCodec==NULL){
		LOGE("Couldn't find Codec.\n");
		return -1;
	}
	if(avcodec_open2(pCodecCtx, pCodec,NULL)<0){
		LOGE("Couldn't open codec.\n");
		return -1;
	}
	
	pFrame=av_frame_alloc();
	pFrameYUV=av_frame_alloc();
	out_buffer=(uint8_t *)av_malloc(avpicture_get_size(PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height));
	avpicture_fill((AVPicture *)pFrameYUV, out_buffer, PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height);
	packet=(AVPacket *)av_malloc(sizeof(AVPacket));

	img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt,
	pCodecCtx->width, pCodecCtx->height, PIX_FMT_YUV420P, SWS_BICUBIC, NULL, NULL, NULL);


	sprintf(info,   "[Input     ]%s\n", input_str);
	sprintf(info, "%s[Output    ]%s\n",info,output_str);
	sprintf(info, "%s[Format    ]%s\n",info, pFormatCtx->iformat->name);
	sprintf(info, "%s[Codec     ]%s\n",info, pCodecCtx->codec->name);
	sprintf(info, "%s[Resolution]%dx%d\n",info, pCodecCtx->width,pCodecCtx->height);

	av_init_packet(packet);
	return 0;

  fp_yuv=fopen(output_str,"wb+");
  if(fp_yuv==NULL){
		printf("Cannot open output file.\n");
		return -1;
	}

	frame_cnt=0;
	time_start = clock();
	while(av_read_frame(pFormatCtx, packet)>=0){
		if(packet->stream_index==videoindex){
			ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
			if(ret < 0){
				LOGE("Decode Error.\n");
				return -1;
			}
			if(got_picture){
				return 0;
				sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height,
					pFrameYUV->data, pFrameYUV->linesize);

				y_size=pCodecCtx->width*pCodecCtx->height;
				fwrite(pFrameYUV->data[0],1,y_size,fp_yuv);    //Y
				fwrite(pFrameYUV->data[1],1,y_size/4,fp_yuv);  //U
				fwrite(pFrameYUV->data[2],1,y_size/4,fp_yuv);  //V
				//Output info
				char pictype_str[10]={0};
				switch(pFrame->pict_type){
					case AV_PICTURE_TYPE_I:sprintf(pictype_str,"I");break;
				  case AV_PICTURE_TYPE_P:sprintf(pictype_str,"P");break;
					case AV_PICTURE_TYPE_B:sprintf(pictype_str,"B");break;
					default:sprintf(pictype_str,"Other");break;
				}
				LOGI("Frame Index: %5d. Type:%s",frame_cnt,pictype_str);
				frame_cnt++;
			}
		}
		av_free_packet(packet);
	}
	//flush decoder
	//FIX: Flush Frames remained in Codec
	while (1) {
		ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
		if (ret < 0)
			break;
		if (!got_picture)
			break;
		sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height,
			pFrameYUV->data, pFrameYUV->linesize);
		int y_size=pCodecCtx->width*pCodecCtx->height;
		fwrite(pFrameYUV->data[0],1,y_size,fp_yuv);    //Y
		fwrite(pFrameYUV->data[1],1,y_size/4,fp_yuv);  //U
		fwrite(pFrameYUV->data[2],1,y_size/4,fp_yuv);  //V
		//Output info
		char pictype_str[10]={0};
		switch(pFrame->pict_type){
			case AV_PICTURE_TYPE_I:sprintf(pictype_str,"I");break;
		  case AV_PICTURE_TYPE_P:sprintf(pictype_str,"P");break;
			case AV_PICTURE_TYPE_B:sprintf(pictype_str,"B");break;
			default:sprintf(pictype_str,"Other");break;
		}
		LOGI("Frame Index: %5d. Type:%s",frame_cnt,pictype_str);
		frame_cnt++;
	}
	time_finish = clock();
	time_duration=(double)(time_finish - time_start);

	sprintf(info, "%s[Time      ]%fms\n",info,time_duration);
	sprintf(info, "%s[Count     ]%d\n",info,frame_cnt);

	sws_freeContext(img_convert_ctx);

	fclose(fp_yuv);

	av_frame_free(&pFrameYUV);
	av_frame_free(&pFrame);
	avcodec_close(pCodecCtx);
	avformat_close_input(&pFormatCtx);

	return 0;
}

JNIEXPORT jbyteArray JNICALL Java_com_leixiaohua1020_sffmpegandroiddecoder_MainActivity_decodeOneFrame(JNIEnv *env, jobject obj, jbyteArray arr)
{
    int i;
    int res;
    int got_picture;
    //获取传入数组的长度
    jsize len = (*env)->GetArrayLength(env, arr);

	jbyte *carr;
    carr =  (*env)-> GetByteArrayElements(env, arr, NULL);//env->GetByteArrayElements(arr, 0);   //获得Java数组arr的引用的指针
    if(carr == NULL)
    {
        return 0;
    }

    jbyteArray ret;
    AVPacket *packet = (AVPacket *)av_malloc(sizeof(AVPacket));
    if(av_read_frame(pFormatCtx, packet) >= 0)
    {
    	//在java中申请一块内存  以用来将C的数组传输给java程序
    	ret = (*env)->NewByteArray(env,len);

		res = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
		if(res < 0)
		{
			LOGE("Decode Error.\n");
			(*env)->DeleteLocalRef(env, ret);
			return NULL;
		}
		if(got_picture)
		{
			sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height,
				pFrameYUV->data, pFrameYUV->linesize);

			int y_size=pCodecCtx->width*pCodecCtx->height;
			int pos = 0;
			memcpy(carr, pFrameYUV->data[0],y_size);    //Y
			pos += y_size;
			memcpy(carr+pos, pFrameYUV->data[1],y_size/4);  //U
			pos += y_size/4;
			memcpy(carr+pos, pFrameYUV->data[2],y_size/4);  //V
		}
		else
		{
			(*env)->DeleteLocalRef(env, ret);
			return NULL;
		}
		av_free_packet(packet);
	    //将C的数组拷贝给java中的数组
	    (*env)->SetByteArrayRegion(env,ret,0,len,carr);
	}
    else
    {
    	ret = NULL;
    }

    //env->ReleaseByteArrayElements(arr, carr, 0);
    return ret;
}


JNIEXPORT jbyteArray JNICALL Java_com_leixiaohua1020_sffmpegandroiddecoder_MainActivity_readFrame(JNIEnv *env, jobject obj)
{
    int i;
    int res;

    jbyteArray ret;

    if(av_read_frame(pFormatCtx, packet) >= 0)
    {
		ret = (*env)->NewByteArray(env,packet->size);
		//将C的数组拷贝给java中的数组
		(*env)->SetByteArrayRegion(env, ret, 0, packet->size, packet->data);
    	av_free_packet(packet);
	}
    else
    {
    	ret = NULL;
    }
    return ret;
}

JNIEXPORT jbyteArray JNICALL Java_com_leixiaohua1020_sffmpegandroiddecoder_MainActivity_decodeOneFrameExt(JNIEnv *env, jobject obj, jbyteArray arr , jint bufLen)
{
    int res;
    int got_picture;

    //获取传入数组的长度
    jsize len = (*env)->GetArrayLength(env, arr);

	jbyte *carr =  (*env)-> GetByteArrayElements(env, arr, NULL);//env->GetByteArrayElements(arr, 0);   //获得Java数组arr的引用的指针
    if(carr == NULL)
    {
        return NULL;
    }

    jbyteArray ret;

    packet->size = bufLen;
    packet->data = carr;
    //if(av_read_frame(pFormatCtx, packet) >= 0)
    {
		res = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
		if(res < 0)
		{
			LOGE("Decode Error.\n");
			return NULL;
		}
		if(got_picture)
		{
			sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height,
				pFrameYUV->data, pFrameYUV->linesize);

			int y_size = pCodecCtx->width*pCodecCtx->height;
	    	//在java中申请一块内存  以用来将C的数组传输给java程序
	    	ret = (*env)->NewByteArray(env,(int)(y_size*1.5));

			int pos = 0;
			(*env)->SetByteArrayRegion(env, ret, pos, y_size, pFrameYUV->data[0]);	 	//Y
			pos += y_size;
			(*env)->SetByteArrayRegion(env, ret, pos, y_size/4, pFrameYUV->data[1]);	//U
			pos += y_size/4;
			(*env)->SetByteArrayRegion(env, ret, pos, y_size/4, pFrameYUV->data[2]);	//V
		}
		else
		{
			(*env)->DeleteLocalRef(env, ret);
			return NULL;
		}
	}
    return ret;
}
JNIEXPORT jintArray JNICALL Java_com_leixiaohua1020_sffmpegandroiddecoder_MainActivity_ccvt_420p_rgb565(JNIEnv *env, jobject obj, jint width, jint height, jbyteArray arr)
{
	int line, col, linewidth;
	int y, u, v, yy, vr, ug, vg, ub;
	int r, g, b;
	const unsigned char *py, *pu, *pv;

	int *dst;




	jbyte *src =  (*env)-> GetByteArrayElements(env, arr, NULL);//env->GetByteArrayElements(arr, 0);   //获得Java数组arr的引用的指针
    if(src == NULL)
    {
        return NULL;
    }

    jintArray ret;
    //在java中申请一块内存  以用来将C的数组传输给java程序
    ret = (*env)->NewIntArray(env,(int)(width*height));
    dst = (int*)malloc(sizeof(int)*width*height);
    if(dst == NULL)
    {
    	(*env)->DeleteLocalRef(env, ret);
    	return NULL;
    }
	linewidth = width >> 1;
	py = src;
	pu = py + (width * height);
	pv = pu + (width * height) / 4;

	y = *py++;
	yy = y << 8;
	u = *pu - 128;
	ug =   88 * u;
	ub = 454 * u;
	v = *pv - 128;
	vg = 183 * v;
	vr = 359 * v;

	for (line = 0; line < height; line++) {
	   for (col = 0; col < width; col++) {
		r = (yy +      vr) >> 8;
		g = (yy - ug - vg) >> 8;
		b = (yy + ub     ) >> 8;

		if (r < 0)   r = 0;
		if (r > 255) r = 255;
		if (g < 0)   g = 0;
		if (g > 255) g = 255;
		if (b < 0)   b = 0;
		if (b > 255) b = 255;
	   *dst++ = (((__u16)r>>3)<<11) | (((__u16)g>>2)<<5) | (((__u16)b>>3)<<0);

		y = *py++;
		yy = y << 8;
		if (col & 1) {
		 pu++;
		 pv++;

		 u = *pu - 128;
		 ug =   88 * u;
		 ub = 454 * u;
		 v = *pv - 128;
		 vg = 183 * v;
		 vr = 359 * v;
		}
	   }
	   if ((line & 1) == 0) { // even line: rewind
		pu -= linewidth;
		pv -= linewidth;
	   }
	}
	(*env)->SetIntArrayRegion(env, ret, 0, sizeof(int)*width*height, dst);	 	//Y
	free(dst);
	return ret;
}

JNIEXPORT jint JNICALL Java_com_leixiaohua1020_sffmpegandroiddecoder_MainActivity_decodeTest
  (JNIEnv *env, jobject obj, jstring input_jstr, jstring output_jstr)
{
	return 3;
}

#else

/*
 * 最简单的基于FFmpeg的视频解码器-安卓
 *
*/

#include <stdio.h>
#include <time.h>

#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include "libavutil/log.h"

#ifdef ANDROID
#include <jni.h>
#include <android/log.h>
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "(>_<)", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "(^_^)", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("(>_<) " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("(^_^) " format "\n", ##__VA_ARGS__)
#endif


//Output FFmpeg's av_log()
void custom_log(void *ptr, int level, const char* fmt, va_list vl){
	FILE *fp=fopen("/storage/emulated/0/av_log.txt","a+");
	if(fp){
		vfprintf(fp,fmt,vl);
		fflush(fp);
		fclose(fp);
	}
}

AVFormatContext	*pFormatCtx;
AVCodecContext	*pCodecCtx;
AVCodec			*pCodec;
AVFrame	*pFrame,*pFrameRGB;

AVPacket *packet;
struct SwsContext *img_convert_ctx;

JNIEXPORT jint JNICALL Java_com_leixiaohua1020_sffmpegandroiddecoder_MainActivity_decode
  (JNIEnv *env, jobject obj, jstring input_jstr)
{
	int i, videoindex;
	//int y_size;
	int ret;
	uint8_t *out_buffer;

	//FILE *fp_yuv;
	//int frame_cnt;
	//clock_t time_start, time_finish;
	//double  time_duration = 0.0;

	char input_str[500]={0};
	char output_str[500]={0};
	char info[1000]={0};
	sprintf(input_str,"%s",(*env)->GetStringUTFChars(env,input_jstr, NULL));

	//FFmpeg av_log() callback
	av_log_set_callback(custom_log);

	av_register_all();
	avformat_network_init();
	pFormatCtx = avformat_alloc_context();

	if(avformat_open_input(&pFormatCtx,input_str,NULL,NULL) != 0)
	{
		LOGE("Couldn't open input stream.\n");
		return -1;
	}
	if(avformat_find_stream_info(pFormatCtx,NULL) < 0)
	{
		LOGE("Couldn't find stream information.\n");
		return -1;
	}
	videoindex=-1;
	for(i=0; i<pFormatCtx->nb_streams; i++)
		if(pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO)
		{
			videoindex=i;
			break;
		}
	if(videoindex==-1)
	{
		LOGE("Couldn't find a video stream.\n");
		return -1;
	}
	pCodecCtx = pFormatCtx->streams[videoindex]->codec;
	pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
	if(pCodec == NULL){
		LOGE("Couldn't find Codec.\n");
		return -1;
	}
	if(avcodec_open2(pCodecCtx, pCodec,NULL) < 0){
		LOGE("Couldn't open codec.\n");
		return -1;
	}

	pFrame = av_frame_alloc();
	pFrameRGB = av_frame_alloc();

	out_buffer = (uint8_t *)av_malloc(avpicture_get_size(AV_PIX_FMT_RGB24, pCodecCtx->width, pCodecCtx->height));
	avpicture_fill((AVPicture *)pFrameRGB, out_buffer, AV_PIX_FMT_RGB24, pCodecCtx->width, pCodecCtx->height);
	packet = (AVPacket *)av_malloc(sizeof(AVPacket));

	img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt, 
		pCodecCtx->width, pCodecCtx->height, AV_PIX_FMT_RGB24, SWS_BICUBIC, NULL, NULL, NULL);
  
	sprintf(info,   "[Input     ]%s\n", input_str);
	sprintf(info, "%s[Output    ]%s\n",info,output_str);
	sprintf(info, "%s[Format    ]%s\n",info, pFormatCtx->iformat->name);
	sprintf(info, "%s[Codec     ]%s\n",info, pCodecCtx->codec->name);
	sprintf(info, "%s[Resolution]%dx%d\n",info, pCodecCtx->width,pCodecCtx->height);

	av_init_packet(packet);
	return 0;
}

JNIEXPORT jbyteArray JNICALL Java_com_leixiaohua1020_sffmpegandroiddecoder_MainActivity_readFrame(JNIEnv *env, jobject obj)
{
    int i;
    int res;

    jbyteArray ret;

    if(av_read_frame(pFormatCtx, packet) >= 0)
    {
		ret = (*env)->NewByteArray(env,packet->size);
		//将C的数组拷贝给java中的数组
		(*env)->SetByteArrayRegion(env, ret, 0, packet->size, packet->data);
    	av_free_packet(packet);
	}
    else
    {
    	ret = NULL;
    }
    return ret;
}

JNIEXPORT jbyteArray JNICALL Java_com_leixiaohua1020_sffmpegandroiddecoder_MainActivity_decodeOneFrameExt(JNIEnv *env, jobject obj, jbyteArray arr , jint bufLen)
{
    int res;
    int got_picture;

    //获取传入数组的长度
    //jsize len = (*env)->GetArrayLength(env, arr);

	jbyte *carr =  (*env)-> GetByteArrayElements(env, arr, NULL);//env->GetByteArrayElements(arr, 0);   //获得Java数组arr的引用的指针
    if(carr == NULL)
    {
        return NULL;
    }

    jbyteArray ret;

    packet->size = bufLen;
    packet->data = carr;
    //if(av_read_frame(pFormatCtx, packet) >= 0)
    {
		res = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
		if(res < 0)
		{
			LOGE("Decode Error.\n");
			return NULL;
		}
		if(got_picture)
		{
			sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height,
					pFrameRGB->data, pFrameRGB->linesize);

			int y_size = pCodecCtx->width*pCodecCtx->height;
	    	//在java中申请一块内存  以用来将C的数组传输给java程序
	    	ret = (*env)->NewByteArray(env,(int)(y_size*3));

	    	(*env)->SetByteArrayRegion(env, ret, 0, y_size*3, pFrameRGB->data[0]);	//Packed
		}
		else
		{
			return NULL;
		}
	}
    return ret;
}

#endif

