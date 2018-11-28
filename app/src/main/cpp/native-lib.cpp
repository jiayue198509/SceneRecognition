#include <jni.h>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <unistd.h>
#include <vector>
#include "fpi_video_tf_interface.h"

#include <android/log.h>
#include <android/bitmap.h>

#define TAG "TfliteC"
#define LOGI(...)  __android_log_print(ANDROID_LOG_VERBOSE, TAG , __VA_ARGS__);
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG, __VA_ARGS__)

jint run_tflite_main(JNIEnv *env, jbyteArray buffer, jobject result)
{
    LOGI("start load model test_tflite");
    int CreateEngine = fpi_video_mobile_net_create_engine();
    if (CreateEngine) {
        LOGE("CreateEngine Error!");
        return -1;
    }

    //  load input
    /*jchar *input_data_ptr = env->GetCharArrayElements(buffer, nullptr);
    if (input_data_ptr == nullptr) return -1;*/
    jsize length = env->GetArrayLength(buffer);
    unsigned char * uc = (unsigned char *)env->GetByteArrayElements(buffer, 0);
    LOGI("buffer length: %d", length);
    LOGI("result  %d %d" , uc[0], uc[1]);
    LOGI("result  %d %d" , uc[2], uc[3]);
    LOGI("result  %d %d" , uc[4], uc[5]);
    LOGI("result  %d %d" , uc[6], uc[7]);

    struct _ST_video_resultlabels data;
    int ret = fpi_video_mobile_net_classify(uc, length, &data);
    if (ret) {
        LOGE("MobilenetClassify Error!");
        return -1;
    }

    jclass objectClass = (*env).GetObjectClass(result);
    jfieldID precision = (*env).GetFieldID(objectClass,"precision","F");
    jfieldID content = (*env).GetFieldID(objectClass,"scene","Ljava/lang/String;");
    jfieldID label = (*env).GetFieldID(objectClass,"label","I");
    float prec = data.degree;
    int index = data.label;
    jstring sceneContent = (env)->NewStringUTF(data.contentText);
    (*env).SetFloatField(result, precision, prec);
    (*env).SetIntField(result, label, index);
    LOGI("result  %f %d" , prec, index);
    (*env).SetObjectField(result, content, sceneContent);

    //fpi_video_mobile_net_release_source();
    (*env).ReleaseByteArrayElements(buffer, (jbyte *)uc, 0);
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL  Java_com_android_tcl_scenerecognition_utils_TfliteUtils_runTfliteModel
        (JNIEnv *env, jobject obj, jbyteArray buffer, jobject result){
    return run_tflite_main(env, buffer, result);
}
