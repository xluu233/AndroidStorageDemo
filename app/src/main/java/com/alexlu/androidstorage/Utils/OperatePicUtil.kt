package com.alexlu.androidstorage.Utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.alexlu.androidstorage.APP_NAME
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import java.io.*
import kotlin.concurrent.thread



object OperatePicUtil {


    /**
     * TODO 保存图片到系统相册
     * @param context
     * @param picUrl 图片链接
     * @param save2Public 是否保存到外部存储--公共区域
     */
    @SuppressLint("CheckResult")
    fun savePicByUrl(context: Context, picUrl: String,save2Public:Boolean){
        Glide.with(context).asBitmap().load(picUrl).into(object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                val destFile = createSaveFile(
                    context,
                    save2Public,
                    "${System.currentTimeMillis()}.jpg",
                    "AABBCC"
                )
                saveBitmap2SelfDirectroy(
                    context,
                    resource,
                    destFile
                )
            }
        })
    }


    /**
     * TODO 保存Bitmap到系统相册
     * @param context
     * @param picUrl 图片链接
     * @param isUseExternalFilesDir 是否使用getExternalFilesDir,false为保存在sdcard根目录下
     */
    @SuppressLint("CheckResult")
    fun savePicByBitmap(context: Context, bitmap: Bitmap, isUseExternalFilesDir:Boolean=false){
        thread(name = "savePic") {
            val destFile = createSaveFile(
                context,
                isUseExternalFilesDir,
                "${System.currentTimeMillis()}.jpg",
                APP_NAME
            )
            Log.d("save2Public", isUseExternalFilesDir.toString())
            saveBitmap2SelfDirectroy(
                context,
                bitmap,
                destFile
            )
        }
    }


    /**
     * 通知系统相册更新
     */
    private fun refreshSystemPic(context: Context, destFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertPicInAndroidQ(context, destFile)
        } else {
            //通知系统图库更新
            val value = ContentValues()
            value.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            value.put(MediaStore.Images.Media.DATA, destFile.absolutePath)
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, value)
        }
    }

    /**
     * 创建需要保存的文件
     * @param isUseExternalFilesDir 是否使用getExternalFilesDir,false为保存在sdcard根目录下
     * @param fileName 保存文件名
     * @param folderName 保存在sdcard根目录下的文件夹名（isUseExternalFilesDir=false时需要）
     */
    private fun createSaveFile(
        context: Context,
        isUseExternalFilesDir: Boolean,
        fileName: String,
        folderName: String?
    ): File {
        val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || isUseExternalFilesDir) {
            //外部存储私有区域：/storage/emulated/0/Android/data/packageName/files/Pictures
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath!!
        } else {
            //外部存储公共区域：/storage/emulated/0/fileName
            Environment.getExternalStorageDirectory().absolutePath
        }
        return if (isUseExternalFilesDir) {
            File(filePath, fileName)
        } else {
            //创建文件夹
            val file = File(filePath, folderName ?: context.packageName)
            if (!file.exists()) {
                file.mkdirs()
            }
            //写入文件
            File(file, fileName)
        }
    }

    /**
     * 复制文件
     *
     * @param source 输入文件
     * @param target 输出文件
     */
    private fun copy(source: File?, target: File?) {
        var fileInputStream: FileInputStream? = null
        var fileOutputStream: FileOutputStream? = null
        try {
            fileInputStream = FileInputStream(source)
            fileOutputStream = FileOutputStream(target)
            val buffer = ByteArray(1024)
            while (fileInputStream.read(buffer) > 0) {
                fileOutputStream.write(buffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                source?.delete()
                fileInputStream?.close()
                fileOutputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Android Q以后向系统相册插入图片
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertPicInAndroidQ(context: Context, insertFile: File) {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DESCRIPTION, insertFile.name)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, insertFile.name)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.Media.TITLE, "Image.jpg")
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/")

        val external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val resolver: ContentResolver = context.contentResolver
        val insertUri = resolver.insert(external, values)
        val inputStream: BufferedInputStream?
        var os: OutputStream? = null
        try {
            inputStream = BufferedInputStream(FileInputStream(insertFile))
            if (insertUri != null) {
                os = resolver.openOutputStream(insertUri)
            }
            if (os != null) {
                val buffer = ByteArray(1024 * 4)
                var len: Int
                while (inputStream.read(buffer).also { len = it } != -1) {
                    os.write(buffer, 0, len)
                }
                os.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            os?.close()
        }
    }

    /**
     * 保存图片至app私有目录
     */
    private fun saveBitmap2SelfDirectroy(context: Context, bitmap: Bitmap, file: File) {
        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        refreshSystemPic(context, file)
    }


    /**
     * TODO MediaStore插入图片
     *
     * @param bitmap
     * @param displayName
     */
    fun saveBitmapToPicturePublicFolder(
        context: Context,
        bitmap: Bitmap,
    ) {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        val path = getAppPicturePath()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, path)
        } else {
            val fileDir = File(path)
            if (!fileDir.exists()){
                fileDir.mkdir()
            }
            contentValues.put(MediaStore.MediaColumns.DATA, path + "${System.currentTimeMillis()}.jpg")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.also {
            val outputStream = context.contentResolver.openOutputStream(it)
            outputStream?.also { os ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                os.close()
                Toast.makeText(context, "添加图片成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getAppPicturePath(): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // full path
            "${Environment.getExternalStorageDirectory().absolutePath}/" + "${Environment.DIRECTORY_PICTURES}/$APP_NAME/"
        } else {
            // relative path
            "${Environment.DIRECTORY_PICTURES}/$APP_NAME/"
        }
    }

}