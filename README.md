# AndroidStorageDemo
 Android文件系统详解

总所周知Android上的存储权限一直在更改，从Android增加file provider,到Android10增加分区存储，Google对于存储权限管理越来越严格。我们聊一下Android上的存储Api兼容性适配。

## 1. 应用存储空间

应用保存数据的方式有如下：
- 文件和媒体数据可以保存在“应用专属存储空间”和“公共存储空间之中”
- 短数据或者偏好设置可以通过sharePreference保存
- 数据库


#### 外部存储
以前的手机是存在SDcard的，但目前很多手机都取消了SDcard,Android上引入了映射机制来创建虚拟的SDcard,我们通过文件管理器看到的路径`storage/emulated/0`就是虚拟SDcard，也就是我们俗称的“外部存储空间”或者“公共存储空间”

> app申请的读写权限请求都是申请的外部存储空间权限

#### 内部存储

内存存储也就是本app的专享目录，其他app是无法访问的，适合存储敏感文件。
通过系统api访问到的路径：`/data/user/0/app_packageName/...`
对应的真是目录：`/data/date/app_packageName/...`

#### 分区存储
分区存储实际上就是外部存储空间中建一个app对应目录，本app无须申请权限就可以访问，如果申请读写权限，意味着申请外部空间所有访问权限，**所以分区存储目录是有可能被其他app访问到的。** [官网](https://developer.android.com/training/data-storage?hl=zh-cn#scoped-storage)对它的介绍

## 2.权限变更记录

#### Android6.0引入动态权限

申请读写权限需要在Manifest.xml中申明：

```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```
Android6.0之前只需申明就可以进行文件读写，Android6.0之后需要进行动态权限申请，也就是要用户同意了才行。权限申请回调很烦人，可以去Github上看一下RxPermission,EasyPermission之类的库。


#### Android7.0权限变更

自从Android7.0开始之后，禁止使用  `file://`这类型的URI,尝试直接使用这种URI会触发` FileUriExposedException`,建议使用[FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider?hl=en)来创建`content://`这类型URI

#### Android10尝试引入分区存储

上面已经讲述了分区存储其实就是外部存储空间的app目录，本app无须权限就可以访问。
在Android10上可以禁用：`android:requestLegacyExternalStorage="true"`,但是在Android11上就不管用了哦，系统会自动忽略啊哈哈


> Google首次尝试引入分区存储，我当时听了人都傻了=.=  真能折腾啊

## 3. 使用FileProvider

#### Setup1：

在res目录下新建xml目录，在xml目录下新建filepaths.xml文件


```
<?xml version="1.0" encoding="utf-8"?>
<paths>

    <!--1、对应内部内存卡根目录：Context.getFileDir()-->
    <files-path
        name="int_root"
        path="/" />

    <!--2、对应应用默认缓存根目录：Context.getCacheDir()-->
    <cache-path
        name="app_cache"
        path="/" />

    <!--3、对应外部内存卡根目录：Environment.getExternalStorageDirectory()-->
    <external-path
        name="ext_root"
        path="/" />

    <!--4、对应外部内存卡根目录下的APP公共目录：Context.getExternalFileDir(String)-->
    <external-files-path
        name="ext_pub"
        path="/" />

    <!--5、对应外部内存卡根目录下的APP缓存目录：Context.getExternalCacheDir()-->
    <external-cache-path
        name="ext_cache"
        path="/" />

    <!--6、对应外部内存卡根目录下的APP缓存目录：Context.getExternalMediaDirs()-->
    <external-media-path
        name="ext_media"
        path="/"/>

</paths>

```

#### Setup2：

在AndroidManifest.xml中申明：


```
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:requestLegacyExternalStorage="true"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.AndroidStorageDemo">

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="com.alexlu.androidstorage.fileProvider"
            android:enabled="true"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/filepaths" />
        </provider>

    </application>


```
注意这里的`android:authorities="com.alexlu.androidstorage.fileProvider"`修改为自己的包名，

#### Setup3：

使用的时候注意与配置文件中注册的包名一致：


```
val file = File(xxpath)
val uri = FileProvider.getUriForFile(context, "com.alexlu.androidstorage.fileProvider", file);
```


#### 为什么要建一个xml文件？

其实就是将以前的`file://`解析成自定义的名称，xml中的name参数就是你自定义的路径名称
path填写文件夹名称，如果为空或者`/`，代表所有路径


#### xml中不同方法的意义

以下是一一对应的：
```
<files-path/> --> Context.getFilesDir()
<cache-path/> --> Context.getCacheDir()
<external-path/> --> Environment.getExternalStorageDirectory()
<external-files-path/> --> Context.getExternalFilesDir(String)
<external-cache-path/> --> Context.getExternalCacheDir()
<external-media-path/> --> Context.getExternalMediaDirs()
```
我们看一下不同Api获取的路径是什么样子的：

```
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

```
以上都标明了其输出路径，`storage/emulated/0`代表的就是外部存储空间，`/data/user/0`代表的就是内存存储空间，包含包名说明就是app私有目录或者分区存储目录。

我们通过以上方法就可以愉快的创建文件啦

## 使用示例：

#### 使用内部私有目录

获取内存私有目录下的cache文件路径
```
    fun getAppCachePath(context: Context, subDir:String?=null):String{
        val path = StringBuilder(context.cacheDir.absolutePath)
        subDir?.let {
            path.append(File.separator).append(it).append(File.separator)
        }
        val dir = File(path.toString())
        if (!dir.exists()) dir.mkdir()
        return path.toString()
    }

```

获取内部私有目录下的files文件路径

```
    fun getAppFilePath(context: Context, subDir:String?=null): String {
        val path = StringBuilder(context.filesDir.absolutePath)
        subDir?.let {
            path.append(File.separator).append(it).append(File.separator)
        }
        val dir = File(path.toString())
        if (!dir.exists()) dir.mkdir()
        return path.toString()
    }
```
可以选择创建子目录，其中`subDis`代表子目录文件夹名称

我们保存两张图片到其目录下：

```
        //创建私有目录cache目录下文件
        val file = File(FileUtil.getAppCachePath(this),"${System.currentTimeMillis()}.jpg")

        //创建私有目录files->image目录下文件
        val file2 = File(FileUtil.getAppFilePath(this,"image"),"${System.currentTimeMillis()}.jpg")

        OperatePicUtil.saveBitmap2File(this,bitmap,file)
        OperatePicUtil.saveBitmap2File(this,bitmap,file2)
```
（OperatePicUtil工具类可以查看[Demo](https://github.com/xluu233/AndroidStorageDemo)）

我们在Android12虚拟机上可以看到保存没有问题：

<img src="https://github.com/xluu233/AndroidStorageDemo/image/test.jpg" width = "200" div align=right />


![image](https://github.com/xluu233/AndroidStorageDemo/image/test.jpg)

#### 使用外部公共目录

> `Environment.getExternalStorageDirectory()`在Android10之后标识为废弃，意思就是这个API很好用但是我不想让你用，实际测试在Android12上依旧有用。
```
    /**
     * TODO 外部目录-Pictures
     * @param subDir 子目录文件夹名称
     * @return
     */
    fun getExternalPicturesPath(subDir:String?=null): String{
        val path = StringBuilder(Environment.getExternalStorageDirectory().absolutePath)
            .append(File.separator)
            .append(Environment.DIRECTORY_PICTURES)
        subDir?.let {
            path.append(File.separator).append(it).append(File.separator)
        }
        val dir = File(path.toString())
        if (!dir.exists()) dir.mkdir()
        return path.toString()
    }

    /**
     * TODO 外部目录-Download
     * @param subDir 子目录文件夹名称
     * @return
     */
    fun getExternalDownloadPath(subDir:String?=null): String{
        val path = StringBuilder(Environment.getExternalStorageDirectory().absolutePath)
            .append(File.separator)
            .append(Environment.DIRECTORY_DOWNLOADS)
        subDir?.let {
            path.append(File.separator).append(it).append(File.separator)
        }
        val dir = File(path.toString())
        if (!dir.exists()) dir.mkdir()
        return path.toString()
    }
```

#### 使用分区存储目录

接口定义：分别获取file,cache,media目录，type代表子目录名称，可以为null

```
    @Override
    public File getExternalFilesDir(String type) {
        return mBase.getExternalFilesDir(type);
    }

    @Override
    public File[] getExternalFilesDirs(String type) {
        return mBase.getExternalFilesDirs(type);
    }

    @Override
    public File getExternalCacheDir() {
        return mBase.getExternalCacheDir();
    }

    @Override
    public File[] getExternalCacheDirs() {
        return mBase.getExternalCacheDirs();
    }

    @Override
    public File[] getExternalMediaDirs() {
        return mBase.getExternalMediaDirs();
    }

```

获取分区存储目录：
```
        /**
     * TODO 分区存储-File目录
     * @param context
     * @param subDir 子目录文件夹名称
     * @return
     */
    fun getExternalAppFilePath(context: Context,subDir: String?=null):String{
        val path = context.getExternalFilesDir(subDir)?.absolutePath
        val dir = File(path.toString())
        if (!dir.exists()) dir.mkdir()
        return path.toString()
    }

```
保存图片到分区存储目录下：

```
        val file = File(FileUtil.getExternalAppFilePath(this),"${System.currentTimeMillis()}.jpg")
        PicturesUtil.saveBitmap2File(context = this,bitmap = bitmap,file = file)

```
我们可以打印文件的路径为：

```
        /sdcard/Android/data/com.alexlu.androidstorage/files
```

**App设置界面中的“清除存储空间”和“清除缓存”**

- 清除存储空间：清除app所有保存的文件和偏好设置，数据库等信息，但是不包括外部公共目录的文件，相当于App卸载重新安装了
- 清除缓存：清除`外部分区存储`和`内部私有存储`中的 `cache` 目录



## 其他文件操作

比如其他类型的文件写入，保存文档、音频文件，插入图片到系统相册。具体用法请查看[Github Demo](https://github.com/xluu233/AndroidStorageDemo),如有错误，请大家指出，欢迎大家点个Star