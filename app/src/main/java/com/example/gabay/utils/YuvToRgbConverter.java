package com.example.gabay.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

public class YuvToRgbConverter {
    private final RenderScript rs;
    private final ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Allocation inputAllocation, outputAllocation;

    public YuvToRgbConverter(Context context) {
        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    public void yuvToRgb(Image image, Bitmap outputBitmap) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Invalid image format");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        //byte[] yuvBytes = ImageUtils.imageToByteArray(image);

        if (inputAllocation == null) {
           // Type.Builder inputType = new Type.Builder(rs, Element.U8(rs)).setX(yuvBytes.length);
            //inputAllocation = Allocation.createTyped(rs, inputType.create(), Allocation.USAGE_SCRIPT);

            Type.Builder outputType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            outputAllocation = Allocation.createTyped(rs, outputType.create(), Allocation.USAGE_SCRIPT);
        }

        //inputAllocation.copyFrom(yuvBytes);
        yuvToRgbIntrinsic.setInput(inputAllocation);
        yuvToRgbIntrinsic.forEach(outputAllocation);
        outputAllocation.copyTo(outputBitmap);
    }
}