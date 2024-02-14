package org.shop.face_recognition

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

class Camera(private val context: Context) : ActivityCompat.OnRequestPermissionsResultCallback {
    private val preview by lazy {
        Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
    }

    private val cameraSelector by lazy {
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
    }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView

    private var cameraExecutor = Executors.newSingleThreadExecutor()

    // FrameLayout이 Layout Param으로 들어옴
    fun initCamera(layout: ViewGroup) {
        previewView = PreviewView(context)
        layout.addView(previewView)
        permissionCheck(context)
    }

    private fun permissionCheck(context: Context) {
        val permissionList = listOf(Manifest.permission.CAMERA)

        /**
         *  권한이 없으면 requestPermission
         *  있으면 openPreview
         */
        if (!PermissionUtil.checkPermission(context, permissionList)) {
            PermissionUtil.requestPermission(context as Activity, permissionList)
        } else {
            openPreview()
        }
    }

    private fun openPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(context).also { providerFuture ->
            providerFuture.addListener({}, ContextCompat.getMainExecutor(context))
        }
    }

    private fun startPreview(context: Context) {
        val cameraProvider = cameraProviderFuture.get()
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, preview)
        } catch (e: Exception) {
            e.stackTrace
        }
    }

    /**
     *  권한이 획득되었을 때 이 부분을 Activity쪽에서 request해서 여기서 preview를 띄울 수 있도록
     *  권한 요청을 하고, 권한이 없으면 아래 함수로 들어와서 권한을 획득하면 preview를 띄우고 없으면 Toast 띄우고 종료
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        var flag = true
        if (grantResults.isNotEmpty()) {
            for ((i, _) in permissions.withIndex()) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    flag = false
                }
            }
            if (flag) {
                openPreview()
            } else {
                Toast.makeText(context, "권한을 허용해야합니다.", Toast.LENGTH_SHORT).show()
                (context as Activity).finish()
            }
        }
    }
}