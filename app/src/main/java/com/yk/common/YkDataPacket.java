package com.yk.common;

import android.graphics.Bitmap;

public final class YkDataPacket {
    public static boolean isLibraryLoaded = false;
    static {
        try {
            System.loadLibrary("ykDataPacket");
            System.loadLibrary("YKImageProc_java");
            isLibraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            isLibraryLoaded = false;
            android.util.Log.e("YkDataPacket", "Failed to load native libraries (likely running on 32-bit device)", e);
        }
    }

    private YkDataPacket() {
    }

    public static native byte[] getPaperTypeCommand();

    public static native byte[] getPrintLabelWithTypeAndPaperCommand(
            String printerModel,
            Bitmap bitmap,
            int printerType,
            int density,
            boolean first,
            boolean last,
            boolean cut,
            float paperWidthMm,
            float paperHeightMm,
            float dieCuttingMm,
            float printX,
            float printY,
            int rotateDegreeType,
            float horizontalOffset,
            float verticalOffset
    );
}
