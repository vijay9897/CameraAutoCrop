package com.vijay.autocropdemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.AsyncTask
import com.android.example.cameraxbasic.utils.RealPathUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ImageProcessingTask(
    @SuppressLint("StaticFieldLeak") private val context: Context,
    private val inputUri: Uri?,
    private val outputPath: String,
    private val outputFileName: String,
    private val mOnImageProcessingListener: OnImageProcessingListener,
    private val frameRect: RectF,
    private val targetRect: Rect
) : AsyncTask<Void?, Void?, File?>() {
    private var errMsg: String? = null

    override fun doInBackground(vararg params: Void?): File? {
        var file: File? = null
        try {
            if (inputUri == null) return null
            val realPath = RealPathUtil.getRealPathFromURI(context, inputUri) ?: return null
            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true
            val checkFile = File(realPath)
            val bmp = BitmapFactory.decodeFile(realPath)

            //reverse rotate image of there is any rotation on decoding
            val rotatedBmp = bmp.checkRotation(realPath)

            //applying crop
            val croppedImage = rotatedBmp.cropBitmap()

            //compressing bitmap after cropping
            val byteArray = croppedImage.compress(Bitmap.CompressFormat.WEBP, 90)

//            // Do not allow processed images
//            if (checkFile.name.startsWith("inventory_")) {
//                errMsg = "Duplicate image not allowed. Please select another image"
//                return null
//            }
            val destDir = File(outputPath)
            if (!destDir.exists()) destDir.mkdirs()
            file = File(destDir, "$outputFileName.jpeg")
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(file)
                fos.write(byteArray)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (fos != null) try {
                    fos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: ArithmeticException) {
            e.printStackTrace()
        }
        return file
    }

    private fun Bitmap.checkRotation(filePath: String): Bitmap {
        val exif = ExifInterface(filePath)
        val orientation: Int = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
        val matrix = Matrix()
        when (orientation) {
            6 -> matrix.postRotate(90F)
            3 -> matrix.postRotate(180F)
            8 -> matrix.postRotate(270F)
        }
        return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true); // rotating bitmap
    }

    private fun Bitmap.cropBitmap(): Bitmap {
        val scaledBitmap = Bitmap.createScaledBitmap(this, targetRect.width(), targetRect.height(), false)
        return Bitmap.createBitmap(
            scaledBitmap,
            frameRect.left.toInt(),
            frameRect.top.toInt(),
            frameRect.width().toInt(),
            frameRect.height().toInt()
        )
    }

    private fun Bitmap.compress(format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 100): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(format, quality, stream)
        return stream.toByteArray()
    }

    override fun onPostExecute(result: File?) {
        if (result != null) {
            mOnImageProcessingListener.onImageProcessingSuccess(result.absolutePath /*, request_code*/)
        } else {
            if (errMsg == null || errMsg!!.isEmpty()) errMsg =
                "Failed to access image. Please try a different image."
            mOnImageProcessingListener.onImageProcessingFailure(errMsg)
        }
    }

    interface OnImageProcessingListener {
        fun onImageProcessingSuccess(path: String)
        fun onImageProcessingFailure(err: String?)
    }
}