package com.android.tcl.scenerecognition.utils;

import com.android.tcl.scenerecognition.common.ResultData;

public class TfliteUtils {
    private static final String NATIVE_LIBNAME = "native-lib";
    private static final String TFLITE_LIBNAME = "tflite_c";

    private TfliteUtils(){
    }

    public static native int runTfliteModel(byte[] buffer, ResultData result);

    static {
        System.loadLibrary(NATIVE_LIBNAME);
        System.loadLibrary(TFLITE_LIBNAME);
    }
}
