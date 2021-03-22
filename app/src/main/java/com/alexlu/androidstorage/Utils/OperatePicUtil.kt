package com.venpoo.whalemuse.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.alexlu.androidstorage.APP_NAME
import com.alexlu.androidstorage.Utils.BitmapUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.*
import kotlin.concurrent.thread


object OperatePicUtil {


    /**
     * TODO 保存图片到系统相册
     * @param context
     * @param picUrl 图片链接
     */
    @SuppressLint("CheckResult")
    fun savePicByUrl(context: Context, picUrl: String){
        Glide.with(context).asBitmap().load(picUrl).into(object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                Observable.create(ObservableOnSubscribe<Boolean> {
                    val destFile = createSaveFile(
                        context,
                        "${System.currentTimeMillis()}.jpg"
                    )
                    saveFile(
                        context,
                        resource,
                        destFile
                    )

                    it.onNext(true)
                    it.onComplete()
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (it) {
                            //toast("保存相册成功")
                        }
                    }

            }
        })
    }

    /**
     * TODO 保存Bitmap到系统相册
     *
     * @param context
     * @param bitmap
     */
    @SuppressLint("CheckResult")
    fun savePicByBitmap(context: Context, bitmap: Bitmap){
        thread(name = "savePic") {
            val destFile = createSaveFile(
                context,
                "${System.currentTimeMillis()}.jpg"
            )
            saveFile(
                context,
                bitmap,
                destFile
            )
        }
    }



    /**
     * TODO 批量保存图片，适配AndroidQ以上，建议使用此方法
     *
     * @param context
     * @param picList
     * @param name
     */
    @SuppressLint("CheckResult")
    fun saveListUrl(context: Context, picList: List<String>, name: String? = System.currentTimeMillis().toString()){
        thread {
            for (i in picList.size-1 downTo 0){
                val picUrl = picList[i]
                val bitmap = BitmapUtils.getBitmap(picUrl)

                bitmap?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveBitAndroidQ(context,bitmap,"${name}_${i + 1}.png")
                    }else{
                        //saveBit(context,bitmap,"${name}_${i + 1}.png")
                        val destFile = createSaveFile(context, "${name}_${i + 1}.png")
                        saveFile(context, bitmap, destFile)
                    }

                }
            }
        }
    }

    /**
     * TODO 保存图片（只使用于AndroidQ以下）
     * 需要使用 android:requestLegacyExternalStorage="true"
     * @param context
     * @param bitmap
     * @param fileName
     */
    private fun saveBit(context: Context, bitmap: Bitmap, fileName: String) {
        val filePath = "${Environment.getExternalStorageDirectory().absolutePath}/" + "${Environment.DIRECTORY_PICTURES}"
        //1.创建文件夹
        val storagefile = File(filePath, APP_NAME)
        if (!storagefile.exists()) {
            storagefile.mkdirs()
        }
        //2.创建文件
        val file = File(storagefile, fileName)
        //3.写入文件
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
        //4.通知系统图库更新
        val value = ContentValues()
        value.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        value.put(MediaStore.Images.Media.DATA, file.absolutePath)
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, value)
    }


    /**
     * TODO 创建需要保存的文件
     *  这里统一采用Android10之前的传统存储方式，保存在公共存储 Pictures/app_name 下面
     */
    private fun createSaveFile(context: Context, fileName: String): File {
        val filePath = "${Environment.getExternalStorageDirectory().absolutePath}/" + "${Environment.DIRECTORY_PICTURES}"
        //创建文件夹
        val file = File(filePath, APP_NAME)
        if (!file.exists()) {
            file.mkdirs()
        }
        //创建文件
        return File(file, fileName)
    }

    /**
     * TODO 保存File并刷新相册
     *
     * @param context
     * @param bitmap
     * @param file
     */
    private fun saveFile(context: Context, bitmap: Bitmap, file: File) {
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
     * TODO MediaStore插入图片到公共目录
     *
     * @param bitmap
     * @param displayName
     */
    fun saveBitAndroidQ(context: Context,bitmap: Bitmap,fileName: String) {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
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
            }
        }
    }

    private fun getAppPicturePath(): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // full path
            "${Environment.getExternalStorageDirectory().absolutePath}/" + "${Environment.DIRECTORY_PICTURES}/${APP_NAME}/"
        } else {
            // relative path
            "${Environment.DIRECTORY_PICTURES}/${APP_NAME}/"
        }
    }

}