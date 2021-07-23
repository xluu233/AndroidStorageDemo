package com.alexlu.androidstorage.bean

import android.net.Uri
import java.util.*

/**
 * @ClassName MediaStoreImage
 * @Description TODO
 * @Author AlexLu_1406496344@qq.com
 * @Date 2021/7/23 9:45
 */
data class MediaStoreImage(
    val id: Long,
    val name: String,
    val time:String?,
    val contentUri: Uri
)
