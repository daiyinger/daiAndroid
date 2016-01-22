#include<com_daiyinger_carplate_CarPlateDetection.h>
#include <vector>
#include "easypr.h"
#include "stdlib.h"
#include "stdio.h"
#include "unistd.h"
//#include "config.h"
using namespace easypr;


char* kDefaultSvmPath; 
char* kDefaultAnnPath;

using std::string;  
using std::vector;

char* jstring2str(JNIEnv* env, jstring jstr) {
	char* rtn = NULL;
	jclass clsstring = env->FindClass("java/lang/String");
	jstring strencode = env->NewStringUTF("GB2312");
	jmethodID mid = env->GetMethodID(clsstring, "getBytes",
			"(Ljava/lang/String;)[B");
	jbyteArray barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
	jsize alen = env->GetArrayLength(barr);
	jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE);
	if (alen > 0) {
		rtn = (char*) malloc(alen + 1);
		memcpy(rtn, ba, alen);
		rtn[alen] = 0;
	}
	env->ReleaseByteArrayElements(barr, ba, 0);
	return rtn;
}


JNIEXPORT jbyteArray JNICALL Java_com_daiyinger_carplate_CarPlateDetection_ImageProc
  (JNIEnv *env, jclass obj, jstring logpath, jstring imgpath, jstring svmpath,
		jstring annpath){
	CPlateRecognize pr;
	FILE *fp = NULL;
	int i;
	char* log = jstring2str(env,logpath);
	char* img = jstring2str(env,imgpath);
	char* svm = jstring2str(env,svmpath);
	char* ann = jstring2str(env,annpath);
	
	fp = fopen(log,"a+");	//"/sdcard/ai/ai_log.txt","w+");
	LOGD("fp %x\r\n",(unsigned int)fp);
	fprintf(fp,"\r\n============= start ===========\r\n");
	
	LOGD("enter 0");
	LOGD("%s\n%s\n%s",img,svm,ann);
	fprintf(fp,"%s\r\n%s\r\n%s\r\n",img,svm,ann);
	
	LOGD("enter 16");
	vector < string > plateVec;
	LOGD("enter 2");
	//int count = pr.plateRecognize(src, plateVec);
	try
	{
		plateVec = easypr::api::plate_recognize(img, svm, ann);
	}
	catch(...)
	{
		
	}
	string str = "0";
	LOGD("enter 3");
	usleep(10);
	if (plateVec.size() > 0) 
	{
		str = plateVec[0];
		for(i = 0; i< plateVec.size(); i++)
		{
			fprintf(fp, "%s\r\n", plateVec[i].c_str());
		}
	}
	else
	{
		fprintf(fp,"æœªèƒ½è¯†åˆ«!\r\n");	//Ê¶±ðÊ§°Ü UTF-8±àÂë
		fclose(fp);
		return NULL;
	}
	LOGD("enter 4");
	char *result = new char[str.length() + 1];
	strcpy(result, str.c_str());
	jbyte *by = (jbyte*) result;
	jbyteArray jarray = env->NewByteArray(strlen(result));
	env->SetByteArrayRegion(jarray, 0, strlen(result), by);
	fclose(fp);
	return jarray;
}
