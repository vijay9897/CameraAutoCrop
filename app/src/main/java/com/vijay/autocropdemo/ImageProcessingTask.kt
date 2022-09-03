package com.vijay.autocropdemo

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.*
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
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
            var scaledBitmap: Bitmap? = null
            if (inputUri == null) return null
            val realPath = RealPathUtil.getRealPathFromURI(context, inputUri)
                ?: //                DMSLogger.INSTANCE.logAppException("image_processing", new IllegalArgumentException(), new HashMap<String, String>() {{
//                    put("uri", inputUri.toString());
//                    put("error_msg", "realPath null");
//                }}, 0);
                return null
            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true
            var bmp = BitmapFactory.decodeFile(realPath)
            val checkFile = File(realPath)

            val croppedImage = cropBitmap(bmp)
            val stream = ByteArrayOutputStream()
            croppedImage?.compress(Bitmap.CompressFormat.WEBP, 80, stream)
            var byteArray = stream.toByteArray()
            // Do not allow processed images
            if (checkFile.name.startsWith("inventory_")) {
                errMsg = "Duplicate image not allowed. Please select another image"
                return null
            }
            val destDir = File(outputPath)
            if (!destDir.exists()) destDir.mkdirs()
            file = File(destDir, outputFileName + "test" + ".webp")
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
//            var actualHeight = bmOptions.outHeight
//            var actualWidth = bmOptions.outWidth
//            val maxHeight = 1024.0f
//            val maxWidth = 768.0f
//            var imgRatio = (actualWidth / actualHeight).toFloat()
//            val maxRatio = maxWidth / maxHeight
//            if (actualHeight > maxHeight || actualWidth > maxWidth) {
//                if (imgRatio < maxRatio) {
//                    imgRatio = maxHeight / actualHeight
//                    actualWidth = (imgRatio * actualWidth).toInt()
//                    actualHeight = maxHeight.toInt()
//                } else if (imgRatio > maxRatio) {
//                    imgRatio = maxWidth / actualWidth
//                    actualHeight = (imgRatio * actualHeight).toInt()
//                    actualWidth = maxWidth.toInt()
//                } else {
//                    actualHeight = maxHeight.toInt()
//                    actualWidth = maxWidth.toInt()
//                }
//            }
//            bmOptions.inSampleSize = calculateInSampleSize(bmOptions, actualWidth, actualHeight)
//            bmOptions.inJustDecodeBounds = false
//            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
//                bmOptions.inPurgeable = true
//                bmOptions.inInputShareable = true
//            }
//            bmOptions.inTempStorage = ByteArray(16 * 1024)
//            try {
//                //load the bitmap from its path
//                bmp = BitmapFactory.decodeFile(realPath, bmOptions)
//            } catch (ex: OutOfMemoryError) {
////                DMSLogger.INSTANCE.logAppException("image_processing", ex, new HashMap<String, String>() {{
////
////                }}, 0);
//                ex.printStackTrace()
//                //GlobalMethods.logCrashlyticsException(ex);
//            }
//            try {
//                if (actualWidth > 0 && actualHeight > 0) scaledBitmap =
//                    Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
//            } catch (ex: OutOfMemoryError) {
////                DMSLogger.INSTANCE.logAppException("image_processing", ex, new HashMap<String, String>() {{
////                    put("error_msg", "OOM when creating scaledBitmap");
////                }}, 0);
//                ex.printStackTrace()
//                //GlobalMethods.logCrashlyticsException(ex);
//            }
//            val ratioX = actualWidth / bmOptions.outWidth.toFloat()
//            val ratioY = actualHeight / bmOptions.outHeight.toFloat()
//            val middleX = actualWidth / 2.0f
//            val middleY = actualHeight / 2.0f
//            val scaleMatrix = Matrix()
//            scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
//            if (scaledBitmap != null) {
//                val canvas = Canvas(scaledBitmap)
//                canvas.setMatrix(scaleMatrix)
//                canvas.drawBitmap(
//                    bmp, middleX - bmp.width / 2, middleY - bmp.height / 2, Paint(
//                        Paint.FILTER_BITMAP_FLAG
//                    )
//                )
//                val exif: ExifInterface
//                try {
//                    exif = ExifInterface(realPath)
//                    val orientation = exif.getAttributeInt(
//                        ExifInterface.TAG_ORIENTATION,
//                        ExifInterface.ORIENTATION_NORMAL
//                    )
//                    val matrix = Matrix()
//                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) matrix.postRotate(90f) else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) matrix.postRotate(
//                        180f
//                    ) else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) matrix.postRotate(
//                        270f
//                    )
//                    scaledBitmap = Bitmap.createBitmap(
//                        scaledBitmap,
//                        0,
//                        0,
//                        scaledBitmap.width,
//                        scaledBitmap.height,
//                        matrix,
//                        true
//                    )
//                } catch (ex: IOException) {
////                    DMSLogger.INSTANCE.logAppException("image_processing", ex, new HashMap<String, String>() {{
////                    }}, 0);
//                    ex.printStackTrace()
//                    //GlobalMethods.logCrashlyticsException(e);
//                }
//                val bytes = ByteArrayOutputStream()
//                scaledBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
//                val outputBitmap =
//                    BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size())
//
//                // Move original to DCIM if camera image
//
//            }
        } catch (e: ArithmeticException) {
            e.printStackTrace()
        }
        return file
    }

    private fun cropBitmap(bitmap: Bitmap): Bitmap {
        Log.d("ImageCropDebug", "${frameRect.left}--${frameRect.top}--${frameRect.right}--${frameRect.bottom}")
        Log.d("ImageCropDebug", "${frameRect.width()}--${frameRect.height()}")
        Log.d("ImageCropDebug", "${targetRect.width()}--${targetRect.height()}")
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetRect.width(), targetRect.height(), false)
        val result = Bitmap.createBitmap(scaledBitmap, frameRect.left.toInt(),frameRect.top.toInt(), frameRect.width().toInt(), frameRect.height().toInt())
        return result
    }

    fun scaleCenterCrop(
        source: Bitmap, newHeight: Int,
        newWidth: Int
    ): Bitmap? {
        val sourceWidth = source.width
        val sourceHeight = source.height
        val xScale = newWidth.toFloat() / sourceWidth
        val yScale = newHeight.toFloat() / sourceHeight
        val scale = Math.max(xScale, yScale)

        // Now get the size of the source bitmap when scaled
        val scaledWidth = scale * sourceWidth
        val scaledHeight = scale * sourceHeight
        val left = (newWidth - scaledWidth) / 2
        val top = (newHeight - scaledHeight) / 2
        val targetRect = RectF(
            left, top, left + scaledWidth, top
                    + scaledHeight
        ) //from ww w  .j a va 2s. co m
        val dest = Bitmap.createBitmap(
            newWidth, newHeight,
            source.config
        )
//        val canvas = Canvas(dest)
//        canvas.drawBitmap(source, null, targetRect, null)
        return dest
    }

    fun Bitmap.compress(format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 100): Bitmap {
        val stream = ByteArrayOutputStream()
        this.compress(format, quality, stream)
        var byteArray = stream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
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

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        val totalPixels = (width * height).toFloat()
        val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }
        return inSampleSize
    }

    interface OnImageProcessingListener {
        fun onImageProcessingSuccess(path: String)
        fun onImageProcessingFailure(err: String?)
    }

    companion object {
        /**
         * Get a file path from a Uri. This will get the the path for Storage Access
         * Framework Documents, as well as the _data field for the MediaStore and
         * other file-based ContentProviders.
         *
         * @param context The context.
         * @param uri     The Uri to query.
         * @author paulburke
         */
        fun getRealPathFromURI(context: Context, uri: Uri): String? {
            val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }

                    // TODO handle non-primary volumes
                } else if (isDownloadsDocument(uri)) {
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                    return getDataColumn(context, contentUri, null, null)
                } else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(
                        split[1]
                    )
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {

                // Return the remote address
                return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                    context,
                    uri,
                    null,
                    null
                )
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
            return null
        }

        /**
         * Get the value of the data column for this Uri. This is useful for
         * MediaStore Uris, and other file-based ContentProviders.
         *
         * @param context       The context.
         * @param uri           The Uri to query.
         * @param selection     (Optional) Filter used in the query.
         * @param selectionArgs (Optional) Selection arguments used in the query.
         * @return The value of the _data column, which is typically a file path.
         */
        fun getDataColumn(
            context: Context, uri: Uri?, selection: String?,
            selectionArgs: Array<String>?
        ): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(
                column
            )
            try {
                cursor = context.contentResolver.query(
                    uri!!, projection, selection, selectionArgs,
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is Google Photos.
         */
        fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri.authority
        }
    }
}