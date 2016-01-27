#include "easypr/core/chars_recognise.h"
#include "easypr/util/util.h"
#include "easypr/config.h"
#include "easypr.h"

#define CHAR_DEBUG

namespace easypr {

#ifdef CHAR_DEBUG
  static int cnt = 0;
  static int plan_cnt = 0;
#endif

CCharsRecognise::CCharsRecognise() 
{
	cnt = 0 ; 
	plan_cnt = 0;
	m_charsSegment = new CCharsSegment(); 
}

CCharsRecognise::~CCharsRecognise() { SAFE_RELEASE(m_charsSegment); }
void makeString(std::string &string , int cnt)
{
  std::stringstream ss;
  std::string cnt_s;
  ss << cnt;
  ss >> cnt_s;
  string += cnt_s+".jpg";
}
int CCharsRecognise::charsRecognise(Mat plate, std::string& plateLicense) {
  std::vector<Mat> matChars;
#ifdef CHAR_DEBUG
  std::string planepath = path.defaultImgPath + "plane_";
  makeString(planepath , plan_cnt);
  LOGD("%s",planepath.c_str());
  utils::imwrite(planepath, plate);
  plan_cnt++;
#endif
  int result = m_charsSegment->charsSegment(plate, matChars);
  if (result == 0) {
    for (auto block : matChars) {
#ifdef CHAR_DEBUG
	  
	  std::string imgpath = path.defaultImgPath + "char";
	  makeString(imgpath , cnt);
	  LOGD("%s",imgpath.c_str());
	  utils::imwrite(imgpath, block);
	  cnt++;
#endif
      auto character = CharsIdentify::instance()->identify(block);
      plateLicense.append(character.second);
    }
  }

  if (plateLicense.size() < 7) {
    return -1;
  }

  return result;

}
}