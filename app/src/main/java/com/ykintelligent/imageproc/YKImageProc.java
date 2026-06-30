package com.ykintelligent.imageproc;

import android.graphics.Bitmap;
import android.graphics.Rect;

public final class YKImageProc {
    private YKImageProc() {
    }

    public static final class catResult {
        public Bitmap bitmap;
        public int index;
    }

    public static final class arResult {
        public Bitmap bitmap;
        public float x;
        public float y;
        public float scale;
        public float rotation;
    }

    public static native Bitmap T82TEST(Bitmap bitmap, int mode);
    public static native catResult catMistakeShots(Bitmap[] bitmaps, int width, int height, boolean a, boolean b, boolean c, boolean d);
    public static native void filterEnhanceDocument(Bitmap bitmap);
    public static native void filterEnhanceDocumentLVDIE(Bitmap bitmap);
    public static native void filterEnhanceGamma(Bitmap bitmap, float gamma);
    public static native void filterEnhanceLaplace(Bitmap bitmap, int level);
    public static native void filterEnhanceLog(Bitmap bitmap, int level);
    public static native void filterEqualizeHist(Bitmap bitmap);
    public static native void filterEqualizeHistClahe(Bitmap bitmap, int level);
    public static native void filterEqualizeHistSingleChannel(Bitmap bitmap);
    public static native Bitmap filterForA4(Bitmap bitmap, int width, int height);
    public static native Bitmap filterMistakeImage(Bitmap src, Bitmap mask, int mode, float value, int color, boolean flag);
    public static native void filterSketchReduceBackground(Bitmap bitmap, boolean flag, int value);
    public static native void filterSketchStreak(Bitmap bitmap, int value);
    public static native void filterTextErodeProc(Bitmap bitmap, int value);
    public static native Bitmap filterToggleMistakeImage(Bitmap a, Bitmap b, Bitmap c);
    public static native Bitmap generateMaze(int width, int height, boolean flag, int value);
    public static native Bitmap generateSudoku(int a, int b, int c);
    public static native boolean getLowQualityStatus();
    public static native Bitmap imageCropAroundJava(Bitmap bitmap, int value);
    public static native Bitmap imageCropEdgeJava(Bitmap bitmap, int width, int height);
    public static native Bitmap imageCropInRectJava(Bitmap bitmap, Rect rect);
    public static native Bitmap imageInvert(Bitmap bitmap, boolean flag);
    public static native Bitmap imageMapGrayToColor(Bitmap bitmap, int a, int b, int c);
    public static native void imageMixGlobal(Bitmap bitmap, float[] values);
    public static native void imageMixGlobalThresh(Bitmap bitmap, float[] values, int threshold, boolean flag);
    public static native void imageMixGlobal_TEST(Bitmap bitmap, float[] values);
    public static native void imageMixGlobal_TEST(Bitmap bitmap, float[] values, int mode);
    public static native Bitmap mergeTwoImgs(Bitmap a, Bitmap b);
    public static native Bitmap mistakeShotPostProc(Bitmap bitmap, int mode);
    public static native Bitmap multiFocusFusion(Bitmap a, Bitmap b, int mode);
    public static native Bitmap preMistakeShot(Bitmap bitmap);
    public static native void previewImageProcJava(Bitmap bitmap, int mode);
    public static native Bitmap printDocHD(Bitmap a, Bitmap b, int c, int d, int e);
    public static native Bitmap printDocumentImg(Bitmap bitmap, int width, int height);
    public static native void printImageProcJava(Bitmap bitmap);
    public static native void printImageProcT2WJava(Bitmap bitmap, int mode);
    public static native void printImageProcTransfer(Bitmap bitmap);
    public static native void printImageUniversal(Bitmap bitmap, int a, int b, int c);
    public static native void printPhotoProcJava(Bitmap bitmap);
    public static native void printPhotoProcQ2Java(Bitmap bitmap);
    public static native void printTextFastProcJava(Bitmap bitmap, int value);
    public static native void printTextImageProcJava(Bitmap bitmap);
    public static native void printTextProcJava(Bitmap bitmap, int a, float b, int c);
    public static native Bitmap removalHwPostProc(Bitmap a, Bitmap b);
    public static native Bitmap removalHwPreProc(Bitmap bitmap);
    public static native Bitmap resizeBorder(Bitmap bitmap, int a, int b, int c, int d, int e, int f, float g);
    public static native Bitmap setBackgroundWhite(Bitmap bitmap);
    public static native void setLowQuality(boolean enabled);
    public static native void tattooAr_clear();
    public static native arResult tattooAr_render(Bitmap bitmap, float value);
    public static native void tattooAr_setSkipFrameParam(int value);
    public static native void tattooAr_setTattoo(Bitmap bitmap);
    public static native Bitmap tattooPreview_blendTattoo(Bitmap bitmap, float a, float b, float c, float d, float e, boolean f);
    public static native void tattooPreview_clearBackground();
    public static native Bitmap tattooPreview_distortAndBlendTattoo(Bitmap bitmap, float a, float b, float c, float d, float e, boolean f);
    public static native Bitmap tattooPreview_distortTattoo(Bitmap bitmap, float a, float b, float c, float d, float e, boolean f);
    public static native Bitmap tattooPreview_preProcTattoo(Bitmap bitmap);
    public static native void tattooPreview_setBackground(Bitmap bitmap);
    public static native Bitmap tattoo_filterLineArt(Bitmap bitmap, int value);
    public static native Bitmap tattoo_filterLineArtNew(Bitmap bitmap, int a, int b, int c);
    public static native Bitmap tattoo_genHalftone(Bitmap a, Bitmap b, boolean c, boolean d, int e, boolean f);
    public static native Bitmap tattoo_genMask(Bitmap bitmap);
    public static native Bitmap toggleMisImg_initMask(int width, int height);
    public static native Bitmap toggleMisImg_setEraseArea(Bitmap a, Bitmap b);
    public static native Bitmap toggleMisImg_setInvertArea(Bitmap a, Bitmap b);
    public static native Bitmap xprinterProc(Bitmap bitmap, int width, int height);
}
