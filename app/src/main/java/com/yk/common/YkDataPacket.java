package com.yk.common;

import android.graphics.Bitmap;

public final class YkDataPacket {
    static {
        System.loadLibrary("ykDataPacket");
        System.loadLibrary("YKImageProc_java");
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
