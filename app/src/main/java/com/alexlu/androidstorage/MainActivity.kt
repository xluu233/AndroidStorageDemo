package com.alexlu.androidstorage

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.alexlu.androidstorage.util.PdfUtil
import com.alexlu.androidstorage.databinding.ActivityMainBinding
import com.alexlu.androidstorage.util.FileUtil
import com.alexlu.androidstorage.util.MediaStoreUtil
import com.tbruyelle.rxpermissions3.RxPermissions
import com.alexlu.androidstorage.util.PicturesUtil
import java.io.File
import java.io.IOException
import java.io.InputStream



const val TAG = "Android分区存储测试"


class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private lateinit var bitmap:Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE).subscribe {
            if (it){
                Log.d(TAG,"读写权限已获取")
            }
        }
        showPath()
        getAsset()
    }

    //展示不同api获取到的路径
    private fun showPath() {

        Log.d(TAG,Environment.getExternalStorageDirectory().absolutePath)
        ///storage/emulated/0

        Log.d(TAG,Environment.getRootDirectory().absolutePath)
        ///system

        Log.d(TAG,Environment.getDataDirectory().absolutePath)
        ///data

        Log.d(TAG,Environment.getDownloadCacheDirectory().absolutePath)
        ///data/cache

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Log.d(TAG,"getDownloadCacheDirectory():"+Environment.getStorageDirectory().absolutePath)
            ///storage
        }

        Log.d(TAG,this.filesDir.absolutePath)
        ///data/user/0/com.alexlu.androidstorage/files

        Log.d(TAG,this.cacheDir.absolutePath)
        ///data/user/0/com.alexlu.androidstorage/cache

        Log.d(TAG,this.codeCacheDir.absolutePath)
        ///data/user/0/com.alexlu.androidstorage/code_cache

        Log.d(TAG,"externalCacheDir:"+this.externalCacheDir?.absolutePath)
        ///storage/emulated/0/Android/data/com.alexlu.androidstorage/cache

        this.externalMediaDirs.forEach {
            Log.d(TAG,"externalMediaDirs:"+it.absolutePath)
            ///storage/emulated/0/Android/media/com.alexlu.androidstorage
        }

        Log.d(TAG,"getExternalFilesDir:"+this.getExternalFilesDir(null)?.absolutePath)
        ///storage/emulated/0/Android/data/com.alexlu.androidstorage/files


        //预定义的一些常用目录
        Log.d(TAG,Environment.DIRECTORY_PICTURES)
        //Pictures
        Log.d(TAG,Environment.DIRECTORY_DOWNLOADS)
        //Download
        Log.d(TAG,Environment.DIRECTORY_DCIM)
        //DCIM
        Log.d(TAG,Environment.DIRECTORY_MUSIC)
        //Music
        Log.d(TAG,Environment.DIRECTORY_MOVIES)
        //Movies
    }

    private fun getAsset() {
        //这里将一张图片以 AssetFileDescriptor 的形式读取出来
        try {
            val pic = this.assets.openFd("test1.png")
            val `is`: InputStream = pic.createInputStream()
            bitmap = BitmapFactory.decodeStream(`is`)
            `is`.close()
            pic.close()
            binding.imageView.setImageBitmap(bitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    /**
     * TODO 保存图片到内存存储-私有目录下
     */
    fun savePic(view: View) {
        //创建私有目录cache目录下文件
        val file = File(FileUtil.getAppCachePath(this),"${System.currentTimeMillis()}.jpg")

        //创建私有目录files->image目录下文件
        val file2 = File(FileUtil.getAppFilePath(this,"image"),"${System.currentTimeMillis()}.jpg")

        PicturesUtil.saveBitmap2File(bitmap = bitmap,file = file)
        PicturesUtil.saveBitmap2File(bitmap = bitmap,file = file2)


        val uri1:Uri = FileProvider.getUriForFile(this,"com.alexlu.androidstorage.fileProvider",file)
        Log.d(TAG,"uri1:${uri1}")
        //uri1:content://com.alexlu.androidstorage.fileProvider/app_cache/1626865241315.jpg

        val uri2:Uri = Uri.parse(file.absolutePath)
        Log.d(TAG,"uri2:${uri2}")
        //uri2:/data/user/0/com.alexlu.androidstorage/cache/1626865241315.jpg
    }

    /**
     * TODO 保存图片到外部存储-公共目录
     * 一般图片保存在：sdcard/Pictures
     */
    fun savePic2(view: View) {
        //创建私有目录cache目录下文件
        val file = File(FileUtil.getExternalPicturesPath("test"),"${System.currentTimeMillis()}.jpg")
        PicturesUtil.saveBitmap2File(context = this,bitmap = bitmap,file = file,refreshAlbum = true)
    }

    /**
     * TODO 保存图片到外部存储-分区目录
     */
    fun savePic3(view: View) {
        ///sdcard/Android/data/com.alexlu.androidstorage/files
        val file = File(FileUtil.getExternalAppFilePath(this,"这是子目录"),"${System.currentTimeMillis()}.jpg")
        PicturesUtil.saveBitmap2File(context = this,bitmap = bitmap,file = file)

        ///sdcard/Android/data/com.alexlu.androidstorage/cache
        val file2 = File(FileUtil.getExternalAppCachePath(this),"${System.currentTimeMillis()}.jpg")
        PicturesUtil.saveBitmap2File(context = this,bitmap = bitmap,file = file2)
    }


    /**
     * TODO 保存PDF文件
     */
    fun savePDF(view: View) {
        val list = arrayListOf<Bitmap>()
        list.add(bitmap)
        list.add(bitmap)
        list.add(bitmap)

        PdfUtil.saveBitmapForPdf(list,"${System.currentTimeMillis()}.pdf",this)
    }



    fun testMediaStore(view: View) {
        //获取相册图片
/*        val result = MediaStoreUtil.getAlbumList(this)
        result.forEach {
            println("image uri is $it")
        }*/



        MediaStoreUtil.getVideoList(this)

    }


}