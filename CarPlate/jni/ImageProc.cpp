#include<com_example_carplate_CarPlateDetection.h>
#include "Plate.h"
#include "Plate_Segment.h"
#include "Plate_Recognition.h"
#include <android/log.h>
#define LOG_TAG "System.out"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

/*char* jstring2str(JNIEnv* env, jstring jstr)
{
    char*   rtn   =   NULL;
    jclass   clsstring   =   env->FindClass("java/lang/String");
    jstring   strencode   =   env->NewStringUTF("GB2312");
    jmethodID   mid   =   env->GetMethodID(clsstring,   "getBytes",   "(Ljava/lang/String;)[B");
    jbyteArray   barr=   (jbyteArray)env->CallObjectMethod(jstr,mid,strencode);
    jsize   alen   =   env->GetArrayLength(barr);
    jbyte*   ba   =   env->GetByteArrayElements(barr,JNI_FALSE);
    if(alen   >   0)
    {
        rtn   =   (char*)malloc(alen+1);
        memcpy(rtn,ba,alen);
        rtn[alen]=0;
    }
    env->ReleaseByteArrayElements(barr,ba,0);
    return  rtn;
}*/

JNIEXPORT jstring JNICALL Java_com_example_carplate_CarPlateDetection_ImageProc
  (JNIEnv *env, jclass obj, jintArray buf, jint w, jint h,jstring dir){
	jint *cbuf;
    cbuf = env->GetIntArrayElements(buf, NULL);
    //char* path = jstring2str(env,dir);

    Size size;
    size.width = w;
    size.height = h;

    Mat imageData,input;
    imageData = Mat(size, CV_8UC4, (unsigned char*)cbuf);
    input = Mat(size, CV_8UC3);
    cvtColor(imageData,input,CV_BGRA2BGR);

	vector<Plate> posible_regions = segment(input);

	const char strCharacters[] = {'0','1','2','3','4','5','6','7','8','9','B', 'C', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'};
	CvANN_MLP ann;
	//SVM for each plate region to get valid car plates,Read file storage.
	FileStorage fs;
	//strcat(path,"/SVM.xml");
	fs.open("/sdcard/SVM.xml", FileStorage::READ);
	Mat SVM_TrainingData;
	Mat SVM_Classes;
	fs["TrainingData"] >> SVM_TrainingData;
	fs["classes"] >> SVM_Classes;
	if(fs.isOpened())
		LOGD("read success!");

	//Set SVM params
	LOGD("size:%d",SVM_TrainingData.rows);
	SVM_TrainingData.convertTo(SVM_TrainingData, CV_32FC1);
	SVM_Classes.convertTo(SVM_Classes, CV_32FC1);
	CvSVMParams SVM_params;
	SVM_params.svm_type = CvSVM::C_SVC;
	SVM_params.kernel_type = CvSVM::LINEAR; //CvSVM::LINEAR;
	SVM_params.degree = 0;
	SVM_params.gamma = 1;
	SVM_params.coef0 = 0;
	SVM_params.C = 1;
	SVM_params.nu = 0;
	SVM_params.p = 0;
	SVM_params.term_crit = cvTermCriteria(CV_TERMCRIT_ITER, 1000, 0.01);
	LOGD("Everything is ready");
	//Train SVM
	LOGD("START TO ENTER SVM PREDICT");
	CvSVM svmClassifier(SVM_TrainingData, SVM_Classes, Mat(), Mat(), SVM_params);
	//For each possible plate, classify with svm if it's a plate or no
	vector<Plate> plates;
	for(int i=0; i< posible_regions.size(); i++)
	{
		Mat img=posible_regions[i].plateImg;
		Mat p= img.reshape(1, 1);
		p.convertTo(p, CV_32FC1);

		int response = (int)svmClassifier.predict( p );
		if(response==1)
			plates.push_back(posible_regions[i]);
	}
	LOGD("SVM PREDICT FINISH");
	fs.release();
	//Read file storage.
	FileStorage fs2;
	fs2.open("/sdcard/OCR.xml", FileStorage::READ);//storage/
	Mat TrainingData;
	Mat Classes;
	fs2["TrainingDataF15"] >> TrainingData;
	fs2["classes"] >> Classes;
	LOGD("size:%d",TrainingData.rows);
	LOGD("START TO TRAIN MLP");
	//训练神经网络
	train(TrainingData, Classes,&ann,10);
	LOGD("FINISH TRAIN MLP");
	usleep(10);
	if(plates.size() <= 0)
	{
		LOGD("error 0");
		return NULL;
	}
	Mat inputs = plates[0].plateImg;
	LOGD("Before extracting hist and low-resolution image first 0");
	usleep(10);
	Plate mplate;
	LOGD("Before extracting hist and low-resolution image first 1");

	//dealing image and save each character image into vector<CharSegment>
	//Threshold input image
	Mat img_threshold;
	threshold(inputs, img_threshold, 60, 255, CV_THRESH_BINARY_INV);
	
	LOGD("Before extracting hist and low-resolution image first");
	Mat img_contours;
	img_threshold.copyTo(img_contours);
	//Find contours of possibles characters
	vector< vector< Point> > contours;
	findContours(img_contours,
		contours, // a vector of contours
		CV_RETR_EXTERNAL, // retrieve the external contours
		CV_CHAIN_APPROX_NONE); // all pixels of each contours
	//Start to iterate to each contour founded
	vector<vector<Point> >::iterator itc= contours.begin();
	LOGD("Before extracting hist and low-resolution image");
	//Remove patch that are no inside limits of aspect ratio and area.
	while (itc!=contours.end()) {

		//Create bounding rect of object
		Rect mr= boundingRect(Mat(*itc));
		//Crop image
		Mat auxRoi(img_threshold, mr);
		if(verifySizes(auxRoi)){
			auxRoi=preprocessChar(auxRoi);
			LOGD("FINISH extracting features");
			//对每一个小方块，提取直方图特征
			Mat f=features(auxRoi,15);
			//For each segment feature Classify
			LOGD("START TO CLASSIFY IN MLP");
			int character=classify(f,&ann);
			mplate.chars.push_back(strCharacters[character]);
			LOGD("FINISH CLASSIFY");
			mplate.charsPos.push_back(mr);
			//printf("%c ",strCharacters[character]);
		}
		++itc;
	}
	LOGD("FINISH END");
	fs2.release();
	string licensePlate=mplate.str();
	//const char *result;
	//result=licensePlate.c_str();
	env->ReleaseIntArrayElements(buf, cbuf, 0);

	return env->NewStringUTF(licensePlate.c_str());
}

