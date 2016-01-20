LOCAL_PATH := $(call my-dir)  
include $(CLEAR_VARS)  
include ../../sdk/native/jni/OpenCV.mk  
LOCAL_SRC_FILES  := ImageProc.cpp  
LOCAL_SRC_FILES  += Plate_Recognition.cpp
LOCAL_SRC_FILES  += Plate_Segment.cpp
LOCAL_SRC_FILES  += Plate.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_MODULE     := imageproc  
LOCAL_LDLIBS += -llog 
include $(BUILD_SHARED_LIBRARY)  