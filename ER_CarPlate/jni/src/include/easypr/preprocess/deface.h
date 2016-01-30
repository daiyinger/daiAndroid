//  ����������
//
//  Created by zhou shiwei on 15/1/20.
//  Copyright (c) 2015�� zhou shiwei. All rights reserved.

//	������ʶ���ļ�
//	�ڷ������ݵ�general_test���ݼ������������ķ�����ͼ����д���
//	EasyPR��Դ��Ŀ�ǳ�ע�ر���ͼƬ�м�ʻ�˵���˽

#ifndef EASYPR_CORE_DEFACE_H_
#define EASYPR_CORE_DEFACE_H_

#include <opencv2/opencv.hpp>

namespace easypr {

namespace preprocess {

int deface();

cv::Mat detectAndMaskFace(cv::Mat& img, cv::CascadeClassifier& cascade,
                          double scale);
}
}

#endif  // EASYPR_CORE_DEFACE_H_
