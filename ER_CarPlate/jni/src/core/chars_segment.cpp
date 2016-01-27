#include "easypr/core/chars_segment.h"
#include "easypr/util/util.h"
#include "easypr.h"
#include "easypr/config.h"

using namespace std;

namespace easypr {

const float DEFAULT_BLUEPERCEMT = 0.3f;
const float DEFAULT_WHITEPERCEMT = 0.1f;

CCharsSegment::CCharsSegment() {
  m_LiuDingSize = DEFAULT_LIUDING_SIZE;
  m_theMatWidth = DEFAULT_MAT_WIDTH;

  //！车牌颜色判断参数

  m_ColorThreshold = DEFAULT_COLORTHRESHOLD;
  m_BluePercent = DEFAULT_BLUEPERCEMT;
  m_WhitePercent = DEFAULT_WHITEPERCEMT;

  m_debug = DEFAULT_DEBUG;
}

//! 字符尺寸验证

bool CCharsSegment::verifyCharSizes(Mat r) {
  // Char sizes 45x90
  float aspect = 45.0f / 90.0f;
  float charAspect = (float)r.cols / (float)r.rows;
  float error = 0.7f;
  float minHeight = 10.f;
  float maxHeight = 35.f;
  // We have a different aspect ratio for number 1, and it can be ~0.2
  float minAspect = 0.05f;
  float maxAspect = aspect + aspect * error;
  // area of pixels
  int area = cv::countNonZero(r);
  // bb area
  int bbArea = r.cols * r.rows;
  //% of pixel in area
  int percPixels = area / bbArea;

  if (percPixels <= 1 && charAspect > minAspect && charAspect < maxAspect &&
      r.rows >= minHeight && r.rows < maxHeight)
    return true;
  else
    return false;
}

//! �ַ�Ԥ����

Mat CCharsSegment::preprocessChar(Mat in) {
  // Remap image
  int h = in.rows;
  int w = in.cols;

  //ͳһÿ���ַ��Ĵ�С

  int charSize = CHAR_SIZE;

  Mat transformMat = Mat::eye(2, 3, CV_32F);
  int m = max(w, h);
  transformMat.at<float>(0, 2) = float(m / 2 - w / 2);
  transformMat.at<float>(1, 2) = float(m / 2 - h / 2);

  Mat warpImage(m, m, in.type());
  warpAffine(in, warpImage, transformMat, warpImage.size(), INTER_LINEAR,
             BORDER_CONSTANT, Scalar(0));

  //�� �����е��ַ�������ͳһ�ĳߴ�

  Mat out;
  resize(warpImage, out, Size(charSize, charSize));

  return out;
}

//! �ַ��ָ�������

int CCharsSegment::charsSegment(Mat input, vector<Mat>& resultVec) {
  if (!input.data) return 0x01;

  int w = input.cols;
  int h = input.rows;

  Mat tmpMat = input(Rect_<double>(w * 0.1, h * 0.1, w * 0.8, h * 0.8));

  // �жϳ�����ɫ�Դ�ȷ��threshold����

  Color plateType = getPlateType(tmpMat, true);

  Mat input_grey;
  cvtColor(input, input_grey, CV_BGR2GRAY);

  Mat img_threshold;

  // ��ֵ��  
  // ���ݳ��ƵĲ�ͬ��ɫʹ�ò�ͬ����ֵ�жϷ���  
  // TODO��ʹ��MSER����ȡ��Щ����


  if (BLUE == plateType) {
    // cout << "BLUE" << endl;
    img_threshold = input_grey.clone();

    int w = input_grey.cols;
    int h = input_grey.rows;
    Mat tmp = input_grey(Rect_<double>(w * 0.1, h * 0.1, w * 0.8, h * 0.8));
    int threadHoldV = ThresholdOtsu(tmp);

    threshold(input_grey, img_threshold, threadHoldV, 255, CV_THRESH_BINARY);

  } else if (YELLOW == plateType) {
    // cout << "YELLOW" << endl;
    img_threshold = input_grey.clone();
    int w = input_grey.cols;
    int h = input_grey.rows;
    Mat tmp = input_grey(Rect_<double>(w * 0.1, h * 0.1, w * 0.8, h * 0.8));
    int threadHoldV = ThresholdOtsu(tmp);
    utils::imwrite(path.defaultImgPath + "inputgray2.jpg", input_grey);

    threshold(input_grey, img_threshold, threadHoldV, 255,
              CV_THRESH_BINARY_INV);

  } else if (WHITE == plateType) {
    // cout << "WHITE" << endl;

    threshold(input_grey, img_threshold, 10, 255,
              CV_THRESH_OTSU + CV_THRESH_BINARY_INV);
  } else {
    // cout << "UNKNOWN" << endl;
    threshold(input_grey, img_threshold, 10, 255,
              CV_THRESH_OTSU + CV_THRESH_BINARY);
  }

  // ȥ�������Ϸ��������Լ��·��ĺ��ߵȸ���  
  // ����Ҳ�ж����Ƿ��ǳ���  
  // �����ڴ˶��ַ�����������Լ��ַ���ɫ��ռ�ı��������Ƿ��ǳ��Ƶ��б�����  
  // ������ǳ��ƣ�����ErrorCode=0x02


  if (!clearLiuDing(img_threshold)) return 0x02;

  // �ڶ�ֵ��ͼ������ȡ����

  Mat img_contours;
  img_threshold.copyTo(img_contours);

  vector<vector<Point> > contours;
  findContours(img_contours,
               contours,               // a vector of contours
               CV_RETR_EXTERNAL,       // retrieve the external contours
               CV_CHAIN_APPROX_NONE);  // all pixels of each contours

  vector<vector<Point> >::iterator itc = contours.begin();
  vector<Rect> vecRect;

  // ���������ض��ߴ���ַ����ų���ȥ

  while (itc != contours.end()) {
    Rect mr = boundingRect(Mat(*itc));
    Mat auxRoi(img_threshold, mr);

    if (verifyCharSizes(auxRoi)) vecRect.push_back(mr);
    ++itc;
  }

   // ����Ҳ����κ��ַ��飬�򷵻�ErrorCode=0x03

  if (vecRect.size() == 0) return 0x03;

  // �Է��ϳߴ��ͼ�鰴�մ����ҽ�������;  
  // ֱ��ʹ��stl��sort����������Ч��

  vector<Rect> sortedRect(vecRect);
  std::sort(sortedRect.begin(), sortedRect.end(),
            [](const Rect& r1, const Rect& r2) { return r1.x < r2.x; });

  size_t specIndex = 0;

   // ��������ַ���Ӧ��Rectt,����A��"A"

  specIndex = GetSpecificRect(sortedRect);

  // �����ض�Rect�����Ƴ������ַ�  
  // ����������Ҫԭ���Ǹ���findContours�������Ѳ�׽�������ַ���׼ȷRect����˽���  
  // �˹��ض��㷨��ָ��

  Rect chineseRect;
  if (specIndex < sortedRect.size())
    chineseRect = GetChineseRect(sortedRect[specIndex]);
  else
    return 0x04;

  //�½�һ��ȫ�µ�����Rect  
  //�������ַ�Rect��һ���ӽ�������Ϊ���϶�������ߵ�  
  //�����Rectֻ����˳��ȥ6��������ֻ������7���ַ����������Ա�����Ӱ���µġ�1���ַ�

  vector<Rect> newSortedRect;
  newSortedRect.push_back(chineseRect);
  RebuildRect(sortedRect, newSortedRect, specIndex);

  if (newSortedRect.size() == 0) return 0x05;

  // ��ʼ��ȡÿ���ַ�

  for (size_t i = 0; i < newSortedRect.size(); i++) {
    Rect mr = newSortedRect[i];

    // Mat auxRoi(img_threshold, mr);

    // ʹ�ûҶ�ͼ����ȡͼ�飬Ȼ�����ζ�ÿ��ͼ����д����ֵ����ֵ��

    Mat auxRoi(input_grey, mr);
    Mat newRoi;

    if (BLUE == plateType) {
      /* img_threshold = auxRoi.clone();
       int w = input_grey.cols;
       int h = input_grey.rows;
       Mat tmp = input_grey(Rect_<double>(w * 0.1, h * 0.1, w * 0.8, h * 0.8));
       int threadHoldV = ThresholdOtsu(tmp);*/

      threshold(auxRoi, newRoi, 5, 255, CV_THRESH_BINARY + CV_THRESH_OTSU);
    } else if (YELLOW == plateType) {
      threshold(auxRoi, newRoi, 5, 255, CV_THRESH_BINARY_INV + CV_THRESH_OTSU);

    } else if (WHITE == plateType) {
      threshold(auxRoi, newRoi, 5, 255, CV_THRESH_OTSU + CV_THRESH_BINARY_INV);
    } else {
      threshold(auxRoi, newRoi, 5, 255, CV_THRESH_OTSU + CV_THRESH_BINARY);
    }

     // ��һ����С

    newRoi = preprocessChar(newRoi);

    // ÿ���ַ�ͼ�����뵽����Ĳ�����д���

    resultVec.push_back(newRoi);
  }

  return 0;
}

//! 根据特殊车牌来构造猜测中文字符的位置和大小

Rect CCharsSegment::GetChineseRect(const Rect rectSpe) {
  int height = rectSpe.height;
  float newwidth = rectSpe.width * 1.15f;
  int x = rectSpe.x;
  int y = rectSpe.y;

  int newx = x - int(newwidth * 1.15);
  newx = newx > 0 ? newx : 0;

  Rect a(newx, y, int(newwidth), height);

  return a;
}

//! 找出指示城市的字符的Rect，例如苏A7003X，就是"A"的位置

int CCharsSegment::GetSpecificRect(const vector<Rect>& vecRect) {
  vector<int> xpositions;
  int maxHeight = 0;
  int maxWidth = 0;

  for (size_t i = 0; i < vecRect.size(); i++) {
    xpositions.push_back(vecRect[i].x);

    if (vecRect[i].height > maxHeight) {
      maxHeight = vecRect[i].height;
    }
    if (vecRect[i].width > maxWidth) {
      maxWidth = vecRect[i].width;
    }
  }

  int specIndex = 0;
  for (size_t i = 0; i < vecRect.size(); i++) {
    Rect mr = vecRect[i];
    int midx = mr.x + mr.width / 2;

    //如果一个字符有一定的大小，并且在整个车牌的1/7到2/7之间，则是我们要找的特殊字符
    //当前字符和下个字符的距离在一定的范围内

    if ((mr.width > maxWidth * 0.8 || mr.height > maxHeight * 0.8) &&
        (midx < int(m_theMatWidth / 7) * 2 &&
         midx > int(m_theMatWidth / 7) * 1)) {
      specIndex = i;
    }
  }

  return specIndex;
}

//! 这个函数做两个事情
//  1.把特殊字符Rect左边的全部Rect去掉，后面再重建中文字符的位置。
//  2.从特殊字符Rect开始，依次选择6个Rect，多余的舍去。

int CCharsSegment::RebuildRect(const vector<Rect>& vecRect,
                               vector<Rect>& outRect, int specIndex) {
  int count = 6;
  for (size_t i = specIndex; i < vecRect.size() && count; ++i, --count) {
    outRect.push_back(vecRect[i]);
  }

  return 0;
}

}
