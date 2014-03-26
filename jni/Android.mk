LOCAL_PATH	:= $(call my-dir)
#LOCAL_ARM_MODE	:= arm

include $(CLEAR_VARS)
LOCAL_MODULE := xmp-prebuilt
LOCAL_SRC_FILES := ../../../libxmp/obj/local/$(TARGET_ARCH_ABI)/libxmp.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= xmp-jni
LOCAL_CFLAGS    := -O3 -I$(LOCAL_PATH)/../../../libxmp/include \
                   -Wno-int-to-pointer-cast -Wno-pointer-to-int-cast
LOCAL_STATIC_LIBRARIES := xmp-prebuilt
LOCAL_SRC_FILES := xmp-jni.c
#LOCAL_LDLIBS	:= -llog

include $(BUILD_SHARED_LIBRARY)
