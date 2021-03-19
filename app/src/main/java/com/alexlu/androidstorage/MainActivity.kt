package com.alexlu.androidstorage

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.alexlu.androidstorage.Utils.BitmapUtils
import com.alexlu.androidstorage.Utils.OperatePicUtil
import com.alexlu.androidstorage.Utils.PdfUtils
import com.alexlu.androidstorage.databinding.ActivityMainBinding
import com.tbruyelle.rxpermissions3.RxPermissions
import java.io.File
import java.io.IOException
import java.io.InputStream



const val APP_NAME = "ABC"


class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private lateinit var bitmap:Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getAsset()
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
     * 保存图片
     * 外部存储-公共空间
     * 比如：sdcard/Pictures/
     */
    fun savePic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            //Android Q以上不需要权限，默认保存在Picture目录下，无法操作外部存储公共空间，即无法自行创建文件夹
            //OperatePicUtil.instance.savePicByBitmap(this,bitmap,false)

            //既然传统的File形势无法创建文件，我们可以利用MediaScope插入到公共文件夹
            OperatePicUtil.saveBitmapToPicturePublicFolder(this,bitmap)
            //以上就是将图片保存到 Pictures/AABBCC 目录下

        }else{
            RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe {
                if (it){
                    OperatePicUtil.savePicByBitmap(this,bitmap,false)
                }else{
                    //权限被拒绝
                }
            }
        }

    }

    /**
     * 保存图片
     * 外部存储-私有空间
     * 比如：sdcard/Andord/data/packageName/files/Pictures
     */
    fun savePic2(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            //理论上应该保存在私有目录下，这里有个bug,在私有目录和Picture目录下都保存了
            OperatePicUtil.savePicByBitmap(this,bitmap,true)
        }else{
            RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe {
                if (it){
                    OperatePicUtil.savePicByBitmap(this,bitmap,true)
                }else{
                    //权限被拒绝
                }
            }
        }

    }


    /**
     * 保存PDF文件到 Download/ABC 文件夹下
     */
    fun savePDF(view: View) {
        val list = arrayListOf<Bitmap>()
        list.add(bitmap)
        list.add(bitmap)
        list.add(bitmap)

        RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe {
            if (it){
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    PdfUtils.saveBitmapForPdf(list,"${System.currentTimeMillis()}.pdf",this)
                }else{

                }
            }else{
                //权限被拒绝
            }
        }
    }


}