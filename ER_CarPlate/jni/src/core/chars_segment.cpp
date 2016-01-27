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

  //ï¼è½¦ç‰Œé¢œè‰²åˆ¤æ–­å‚æ•°

  m_ColorThreshold = DEFAULT_COLORTHRESHOLD;
  m_BluePercent = DEFAULT_BLUEPERCEMT;
  m_WhitePercent = DEFAULT_WHITEPERCEMT;

  m_debug = DEFAULT_DEBUG;
}

//! å­—ç¬¦å°ºå¯¸éªŒè¯

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

//! ×Ö·ûÔ¤´¦Àí

Mat CCharsSegment::preprocessChar(Mat in) {
  // Remap image
  int h = in.rows;
  int w = in.cols;

  //Í³Ò»Ã¿¸ö×Ö·ûµÄ´óĞ¡

  int charSize = CHAR_SIZE;

  Mat transformMat = Mat::eye(2, 3, CV_32F);
  int m = max(w, h);
  transformMat.at<float>(0, 2) = float(m / 2 - w / 2);
  transformMat.at<float>(1, 2) = float(m / 2 - h / 2);

  Mat warpImage(m, m, in.type());
  warpAffine(in, warpImage, transformMat, warpImage.size(), INTER_LINEAR,
             BORDER_CONSTANT, Scalar(0));

  //£¡ ½«ËùÓĞµÄ×Ö·ûµ÷Õû³ÉÍ³Ò»µÄ³ß´ç

  Mat out;
  resize(warpImage, out, Size(charSize, charSize));

  return out;
}

//! ×Ö·û·Ö¸îÓëÅÅĞò

int CCharsSegment::charsSegment(Mat input, vector<Mat>& resultVec) {
  if (!input.data) return 0x01;

  int w = input.cols;
  int h = input.rows;

  Mat tmpMat = input(Rect_<double>(w * 0.1, h * 0.1, w * 0.8, h * 0.8));

  // ÅĞ¶Ï³µÅÆÑÕÉ«ÒÔ´ËÈ·ÈÏthreshold·½·¨

  Color plateType = getPlateType(tmpMat, true);

  Mat input_grey;
  cvtColor(input, input_grey, CV_BGR2GRAY);

  Mat img_threshold;

  // ¶şÖµ»¯  
  // ¸ù¾İ³µÅÆµÄ²»Í¬ÑÕÉ«Ê¹ÓÃ²»Í¬µÄãĞÖµÅĞ¶Ï·½·¨  
  // TODO£ºÊ¹ÓÃMSERÀ´ÌáÈ¡ÕâĞ©ÂÖÀª


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

  // È¥³ı³µÅÆÉÏ·½µÄÁø¶¤ÒÔ¼°ÏÂ·½µÄºáÏßµÈ¸ÉÈÅ  
  // ²¢ÇÒÒ²ÅĞ¶ÏÁËÊÇ·ñÊÇ³µÅÆ  
  // ²¢ÇÒÔÚ´Ë¶Ô×Ö·ûµÄÌø±ä´ÎÊıÒÔ¼°×Ö·ûÑÕÉ«ËùÕ¼µÄ±ÈÖØ×öÁËÊÇ·ñÊÇ³µÅÆµÄÅĞ±ğÌõ¼ş  
  // Èç¹û²»ÊÇ³µÅÆ£¬·µ»ØErrorCode=0x02


  if (!clearLiuDing(img_threshold)) return 0x02;

  // ÔÚ¶şÖµ»¯Í¼ÏñÖĞÌáÈ¡ÂÖÀª

  Mat img_contours;
  img_threshold.copyTo(img_contours);

  vector<vector<Point> > contours;
  findContours(img_contours,
               contours,               // a vector of contours
               CV_RETR_EXTERNAL,       // retrieve the external contours
               CV_CHAIN_APPROX_NONE);  // all pixels of each contours

  vector<vector<Point> >::iterator itc = contours.begin();
  vector<Rect> vecRect;

  // ½«²»·ûºÏÌØ¶¨³ß´çµÄ×Ö·û¿éÅÅ³ı³öÈ¥

  while (itc != contours.end()) {
    Rect mr = boundingRect(Mat(*itc));
    Mat auxRoi(img_threshold, mr);

    if (verifyCharSizes(auxRoi)) vecRect.push_back(mr);
    ++itc;
  }

   // Èç¹ûÕÒ²»µ½ÈÎºÎ×Ö·û¿é£¬Ôò·µ»ØErrorCode=0x03

  if (vecRect.size() == 0) return 0x03;

  // ¶Ô·ûºÏ³ß´çµÄÍ¼¿é°´ÕÕ´Ó×óµ½ÓÒ½øĞĞÅÅĞò;  
  // Ö±½ÓÊ¹ÓÃstlµÄsort·½·¨£¬¸üÓĞĞ§ÂÊ

  vector<Rect> sortedRect(vecRect);
  std::sort(sortedRect.begin(), sortedRect.end(),
            [](const Rect& r1, const Rect& r2) { return r1.x < r2.x; });

  size_t specIndex = 0;

   // »ñµÃÌØÊâ×Ö·û¶ÔÓ¦µÄRectt,ÈçËÕAµÄ"A"

  specIndex = GetSpecificRect(sortedRect);

  // ¸ù¾İÌØ¶¨RectÏò×ó·´ÍÆ³öÖĞÎÄ×Ö·û  
  // ÕâÑù×öµÄÖ÷ÒªÔ­ÒòÊÇ¸ù¾İfindContours·½·¨ºÜÄÑ²¶×½µ½ÖĞÎÄ×Ö·ûµÄ×¼È·Rect£¬Òò´Ë½öÄÜ  
  // ÍË¹ıÌØ¶¨Ëã·¨À´Ö¸¶¨

  Rect chineseRect;
  if (specIndex < sortedRect.size())
    chineseRect = GetChineseRect(sortedRect[specIndex]);
  else
    return 0x04;

  //ĞÂ½¨Ò»¸öÈ«ĞÂµÄÅÅĞòRect  
  //½«ÖĞÎÄ×Ö·ûRectµÚÒ»¸ö¼Ó½øÀ´£¬ÒòÎªËü¿Ï¶¨ÊÇ×î×ó±ßµÄ  
  //ÆäÓàµÄRectÖ»°´ÕÕË³ĞòÈ¥6¸ö£¬³µÅÆÖ»¿ÉÄÜÊÇ7¸ö×Ö·û£¡ÕâÑù¿ÉÒÔ±ÜÃâÒõÓ°µ¼ÖÂµÄ¡°1¡±×Ö·û

  vector<Rect> newSortedRect;
  newSortedRect.push_back(chineseRect);
  RebuildRect(sortedRect, newSortedRect, specIndex);

  if (newSortedRect.size() == 0) return 0x05;

  // ¿ªÊ¼½ØÈ¡Ã¿¸ö×Ö·û

  for (size_t i = 0; i < newSortedRect.size(); i++) {
    Rect mr = newSortedRect[i];

    // Mat auxRoi(img_threshold, mr);

    // Ê¹ÓÃ»Ò¶ÈÍ¼À´½ØÈ¡Í¼¿é£¬È»ºóÒÀ´Î¶ÔÃ¿¸öÍ¼¿é½øĞĞ´ó½òãĞÖµÀ´¶şÖµ»¯

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

     // ¹éÒ»»¯´óĞ¡

    newRoi = preprocessChar(newRoi);

    // Ã¿¸ö×Ö·ûÍ¼¿éÊäÈëµ½ÏÂÃæµÄ²½Öè½øĞĞ´¦Àí

    resultVec.push_back(newRoi);
  }

  return 0;
}

//! æ ¹æ®ç‰¹æ®Šè½¦ç‰Œæ¥æ„é€ çŒœæµ‹ä¸­æ–‡å­—ç¬¦çš„ä½ç½®å’Œå¤§å°

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

//! æ‰¾å‡ºæŒ‡ç¤ºåŸå¸‚çš„å­—ç¬¦çš„Rectï¼Œä¾‹å¦‚è‹A7003Xï¼Œå°±æ˜¯"A"çš„ä½ç½®

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

    //å¦‚æœä¸€ä¸ªå­—ç¬¦æœ‰ä¸€å®šçš„å¤§å°ï¼Œå¹¶ä¸”åœ¨æ•´ä¸ªè½¦ç‰Œçš„1/7åˆ°2/7ä¹‹é—´ï¼Œåˆ™æ˜¯æˆ‘ä»¬è¦æ‰¾çš„ç‰¹æ®Šå­—ç¬¦
    //å½“å‰å­—ç¬¦å’Œä¸‹ä¸ªå­—ç¬¦çš„è·ç¦»åœ¨ä¸€å®šçš„èŒƒå›´å†…

    if ((mr.width > maxWidth * 0.8 || mr.height > maxHeight * 0.8) &&
        (midx < int(m_theMatWidth / 7) * 2 &&
         midx > int(m_theMatWidth / 7) * 1)) {
      specIndex = i;
    }
  }

  return specIndex;
}

//! è¿™ä¸ªå‡½æ•°åšä¸¤ä¸ªäº‹æƒ…
//  1.æŠŠç‰¹æ®Šå­—ç¬¦Rectå·¦è¾¹çš„å…¨éƒ¨Rectå»æ‰ï¼Œåé¢å†é‡å»ºä¸­æ–‡å­—ç¬¦çš„ä½ç½®ã€‚
//  2.ä»ç‰¹æ®Šå­—ç¬¦Rectå¼€å§‹ï¼Œä¾æ¬¡é€‰æ‹©6ä¸ªRectï¼Œå¤šä½™çš„èˆå»ã€‚

int CCharsSegment::RebuildRect(const vector<Rect>& vecRect,
                               vector<Rect>& outRect, int specIndex) {
  int count = 6;
  for (size_t i = specIndex; i < vecRect.size() && count; ++i, --count) {
    outRect.push_back(vecRect[i]);
  }

  return 0;
}

}
