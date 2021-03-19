package com.alexlu.androidstorage.Utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.alexlu.androidstorage.APP_NAME
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * @ClassName PdfUtils
 * @Description TODO
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/3/19 17:33
 */
object PdfUtils {

    /**
     * TODO 将Bitmap保存为PDF文件在Download目录下
     *
     * @param bitmaps
     * @param fileName 文件名
     * @param context
     */
    fun saveBitmapForPdf(bitmaps: ArrayList<Bitmap>, fileName: String, context: Context) {

        val doc = PdfDocument()
        val pageWidth: Int = PrintAttributes.MediaSize.ISO_A4.getWidthMils() * 72 / 1000
        val scale = pageWidth.toFloat() / bitmaps[0].width.toFloat()
        val pageHeight = (bitmaps[0].height * scale).toInt()
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (i in 0 until bitmaps.size) {
            val newPage = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i).create()
            val page = doc.startPage(newPage)
            val canvas = page.canvas
            canvas.drawBitmap(bitmaps[i], matrix, paint)
            doc.finishPage(page)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,getDownloadPath())
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

            uri?.also {
                val outputStream = context.contentResolver.openOutputStream(it)
                outputStream?.also { os ->
                    try {
                        doc.writeTo(os)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        doc.close()
                        os.close()
                        try {
                            outputStream?.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }

                }
            }

        } else {
            val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (!storageDir?.exists()!!) {
                storageDir.mkdir()
            }
            val file = File(Environment.getExternalStorageDirectory(), fileName)


        }

    }


    private fun getDownloadPath(): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // full path
            "${Environment.getExternalStorageDirectory().absolutePath}/" + "${Environment.DIRECTORY_DOWNLOADS}/$APP_NAME/"
        } else {
            // relative path
            "${Environment.DIRECTORY_DOWNLOADS}/$APP_NAME/"
        }
    }


}