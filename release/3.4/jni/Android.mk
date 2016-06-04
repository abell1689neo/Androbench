LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE	:= Interface_JNI
LOCAL_SRC_FILES	:= Interface_JNI.c

include $(BUILD_SHARED_LIBRARY)