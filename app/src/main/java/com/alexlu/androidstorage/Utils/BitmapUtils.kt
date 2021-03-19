package com.alexlu.androidstorage.Utils

import android.graphics.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

object BitmapUtils {

    /**
     * 网络url转bitmap
     */
    public fun getBitmap(url: String?): Bitmap? {
        var bm: Bitmap? = null
        try {
            val iconUrl = URL(url)
            val conn: URLConnection = iconUrl.openConnection()
            val http: HttpURLConnection = conn as HttpURLConnection
            val length: Int = http.getContentLength()
            conn.connect()
            // 获得图像的字符流
            val `is`: InputStream = conn.getInputStream()
            val bis = BufferedInputStream(`is`, length)
            bm = BitmapFactory.decodeStream(bis)
            bis.close()
            `is`.close() // 关闭流
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bm
    }


    /**
     * 按比例缩放图片
     *
     * @param origin 原图
     * @param ratio  比例
     * @return 新的bitmap
     */
    fun scaleBitmap(origin: Bitmap?, ratio: Float): Bitmap? {
        if (origin == null) {
            return null
        }
        val width = origin.width
        val height = origin.height
        val matrix = Matrix()
        matrix.preScale(ratio, ratio)
        val newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false)
        return newBM
    }


    /**
     * 根据宽高压缩图片
     */
    fun scaledBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap? {
        val m = Matrix()
        m.setRectToRect(RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            Matrix.ScaleToFit.CENTER)
        if (!bitmap.isRecycled){
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        }
        return null
    }


}