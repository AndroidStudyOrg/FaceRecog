package org.shop.face_recognition

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtil {
    fun checkPermission(context: Context, permissionList: List<String>): Boolean {
        permissionList.forEach {
            /**
             *  기존에 권한을 받았는지, 아니면 권한 자체가 없는지 여부를 판단
             */
            if (ContextCompat.checkSelfPermission(
                    context,
                    it
                ) == PackageManager.PERMISSION_DENIED
            ) {
                return false
            }
        }
        return true
    }

    fun requestPermission(activity: Activity, permissionList: List<String>) {
        ActivityCompat.requestPermissions(activity, permissionList.toTypedArray(), 1)
    }
}