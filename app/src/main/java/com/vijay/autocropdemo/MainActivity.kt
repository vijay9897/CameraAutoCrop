package com.vijay.autocropdemo

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.vijay.autocropdemo.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), ImageProcessingTask.OnImageProcessingListener {
    private lateinit var fileName: String
    private lateinit var folderName: String
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        fileName = (intent?.let {
            it.getStringExtra("input_file_name") ?: getInputFileName()
        } ?: getInputFileName()) as String

        folderName = (intent?.let {
            it.getStringExtra("folder_name") ?: "Inventory"
        } ?: "Inventory") as String

        startCamera()
        viewBinding.imageCaptureButton.setOnClickListener {
            takePhoto()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
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
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e("ImageCaptureActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
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
                    Log.e("ImageCaptureActivity", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val inputFileUri = output.savedUri
                    val sourcePath = Objects.requireNonNull(getExternalFilesDir(null)).toString()
                    val outputFilePath = File("$sourcePath/${folderName}")
                    val fileName = fileName

                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    lifecycleScope.launchWhenResumed {
                        ImageProcessingTask(
                            this@MainActivity,
                            inputFileUri,
                            outputFilePath.absolutePath, fileName,
                            this@MainActivity,
                            viewBinding.overlayoutView.getRect(),
                            Rect().apply { viewBinding.viewFinder.getHitRect(this) }
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
        val intent = Intent()
        intent.putExtra("output_file_path", path)
//        setResult(Activity.RESULT_OK, intent)
//        finish()
    }

    override fun onImageProcessingFailure(err: String?) {

    }
}