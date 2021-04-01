package github.sun5066.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import github.sun5066.camera.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// 그냥주석
class MainActivity : BaseActivity<ActivityMainBinding>() {

    interface ClickListener {
        fun onClickListener()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private var mImageCapture: ImageCapture? = null
    private var mCameraSelector: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var mVideoCapture: VideoCapture
    private lateinit var mOutputDirectory: File
    private lateinit var mCameraExecutor: ExecutorService
    private lateinit var mCameraProvider: ProcessCameraProvider
    private val mCameraProviderFuture by lazy { ProcessCameraProvider.getInstance(this) }

    private val mViewModel: MainViewModel by lazy { ViewModelProvider(this).get(MainViewModel::class.java) }

    @LayoutRes
    override fun getLayoutResourceId(): Int = R.layout.activity_main

    override fun initDataBinding() {
        mBinding.lifecycleOwner = this
        mBinding.viewmodel = mViewModel
    }

    override fun initView() {
        if (allPermissionsGranted()) this.setupCamera()
        else ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        mOutputDirectory = getOutputDirectory()
        mCameraExecutor = Executors.newSingleThreadExecutor()
        mBinding.btnPicture.setOnClickListener {
            this.takePhoto()
        }

        mBinding.btnChangeCamera.setOnClickListener {
            mCameraExecutor.shutdown()
            mCameraSelector =
                if (CameraSelector.LENS_FACING_BACK == mCameraSelector) CameraSelector.LENS_FACING_FRONT
                else CameraSelector.LENS_FACING_BACK
            this.bindCamera()
        }
    }

    private fun takePhoto() {
        val imageCapture = mImageCapture ?: return
        val photoFile = File(
            mOutputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.KOREA)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            })
    }

    private fun setupCamera() {
        mCameraProviderFuture.addListener(Runnable {
            mCameraProvider = mCameraProviderFuture.get()

            this.bindCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera() {
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(mBinding.viewFinder.createSurfaceProvider())
            }

        val cameraSelector = CameraSelector.Builder().requireLensFacing(mCameraSelector).build()
        mImageCapture = ImageCapture.Builder().build()
        mCameraProvider.unbindAll()

        try {
            mCameraProvider.bindToLifecycle(this, cameraSelector, preview, mImageCapture)
        } catch (e: Exception) {
            Log.e(TAG, "${e.message}")
        }
    }

    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir
        else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        mCameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) this.setupCamera()
        else {
            Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}