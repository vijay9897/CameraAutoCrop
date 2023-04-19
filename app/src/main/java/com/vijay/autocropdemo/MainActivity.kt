package com.vijay.autocropdemo

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.provider.MediaStore
import android.view.OrientationEventListener
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.android.example.cameraxbasic.utils.RealPathUtil
import com.vijay.autocropdemo.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), ImageProcessingTask.OnImageProcessingListener {
    private lateinit var captureFileName: String
    private lateinit var resultFileName: String
    private lateinit var destinationPath: String
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var showOverlay = true
    private var isButtonVisible = true
    private var camera: Camera? = null
    private var flashLightOn = false
    private lateinit var cameraExecutor: ExecutorService
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera Permission is Required", Toast.LENGTH_SHORT).show()
            requestPermission()
        }
    }
    companion object {
        /*Filename that will be clicked by camera*/
        const val EXTRA_FILENAME = "file_name"
        /*Filename that be used for cropped image. That will be final*/
        const val EXTRA_RESULT_FILENAME = "result_file_name"
        /*Path to store final cropped and compressed image*/
        const val EXTRA_OUTPUT_PATH = "output_path"
        /*if cropping needs to be applied*/
        const val EXTRA_CROP_NEEDED = "crop"

        const val RESULT_OUTPUT_PATH = "output_path"
        const val EXTRA_ASSET_META_INFO = "asset_meta_info"

        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private fun requestPermission() {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        captureFileName = (intent?.let {
            it.getStringExtra(EXTRA_FILENAME) ?: getInputFileName()
        } ?: getInputFileName()) as String
        destinationPath = (intent?.let {
            it.getStringExtra(EXTRA_OUTPUT_PATH) ?: ""
        }) ?: ""
        resultFileName = (intent?.let {
            it.getStringExtra(EXTRA_RESULT_FILENAME) ?: ""
        }) ?: ""
        showOverlay = (intent?.getBooleanExtra(EXTRA_CROP_NEEDED, true)) ?: true

        viewBinding.cameraCaptureButton.setOnClickListener {
            takePhoto()
        }
        viewBinding.flashlight.setOnClickListener {
            camera?.let { camera ->
                flashLightOn = !flashLightOn
                camera.cameraControl.enableTorch(flashLightOn)
                if (flashLightOn) {
                    (it as AppCompatImageView).setImageResource(R.drawable.flashlight_off)
                } else {
                    (it as AppCompatImageView).setImageResource(R.drawable.flashlight_on)
                }
            }
        }
        viewBinding.squareOverlay.isVisible = showOverlay
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermission()
        }
    }

    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    override fun onStop() {
        super.onStop()
        camera?.cameraControl?.enableTorch(flashLightOn)
        orientationEventListener.disable()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder()
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, captureFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraDemo-Image")
//            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    exc.printStackTrace()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val inputFileUri = output.savedUri
                    lifecycleScope.launchWhenResumed {
                        ProcessImageTask(
                            RealPathUtil.getRealPathFromURI(this@MainActivity, inputFileUri!!),
                            destinationPath,
                            resultFileName,
                            this@MainActivity,
                            viewBinding.squareOverlay.getRect(),
                            Rect().apply { viewBinding.viewFinder.getHitRect(this) },
                            cropImage = showOverlay
                        ).execute()
                    }
                }
            }
        )
    }

    private fun getInputFileName(): String {
        return SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis()) ?: ""
    }

    override fun onImageProcessingSuccess(path: String) {
        val intent = Intent().apply {
            putExtra(RESULT_OUTPUT_PATH, path)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onImageProcessingFailure(err: String?) {

    }

    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation > 345 || orientation < 15) {
                    if (!isButtonVisible) {
                        isButtonVisible = true
                        toggle(true)
                    }
                } else {
                    if (isButtonVisible) {
                        isButtonVisible = false
                        toggle(false)
                    }
                }
            }
        }
    }

    private fun toggle(show: Boolean) {
        val transition: Transition = Fade()
        transition.duration = 600
        transition.addTarget(viewBinding.cameraCaptureButton)
        transition.addTarget(viewBinding.infoText)
        TransitionManager.beginDelayedTransition(viewBinding.viewFinder, transition)
        viewBinding.cameraCaptureButton.visibility = if (show) View.VISIBLE else View.GONE
        viewBinding.infoText.visibility = if (show) View.GONE else View.VISIBLE
    }
}