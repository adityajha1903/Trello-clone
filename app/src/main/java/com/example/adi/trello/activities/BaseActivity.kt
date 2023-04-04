package com.example.adi.trello.activities

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.adi.trello.R
import com.example.adi.trello.databinding.ImageSourceBottomSheetDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.*

open class BaseActivity: AppCompatActivity() {

    private lateinit var mProgressDialog: Dialog

    private var currentActivityInUse: Activity? = null

    fun showProgressDialog(txt: String = resources.getString(R.string.please_wait)) {
        mProgressDialog = Dialog(this)
        mProgressDialog.setContentView(R.layout.progress_dialog_layout)
        val text = mProgressDialog.findViewById<TextView>(R.id.txtTv)
        text.text = txt
        mProgressDialog.setCancelable(false)
        mProgressDialog.show()
    }

    fun hideProgressDialog() {
        if (mProgressDialog.isShowing) {
            mProgressDialog.dismiss()
        }
    }

    fun showErrorSnackBar(message: String) {
        val snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.grey, null))
        snackBar.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
        snackBar.show()
    }

    fun showImageSelectingDialog(activity: Activity) {
        currentActivityInUse = activity
        val dialogBinding = ImageSourceBottomSheetDialogBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this, R.style.BottomSheetStyle)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.closeDialogBtn.setOnClickListener { dialog.dismiss() }
        dialogBinding.cameraBtn.setOnClickListener {
            askPermissionForCamera(activity)
            dialog.dismiss()
        }
        dialogBinding.galleryBtn.setOnClickListener {
            askPermissionForReadingGallery(activity)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun askPermissionForCamera(activity: Activity) =
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            pickImageUsingCamera(activity)
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showRequestPermissionRationaleAlertDialog()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), CAMERA_PERMISSION_REQ_CODE
                )
            }
        }

    private fun askPermissionForReadingGallery(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            pickImageUsingGallery(activity)
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showRequestPermissionRationaleAlertDialog()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), READ_PERMISSION_REQ_CODE
                )
            }
        }
    }

    private fun showRequestPermissionRationaleAlertDialog() {
        AlertDialog.Builder(this).setMessage("" +
                "It looks like you have turned off permission required " +
                "for this feature. It can be enabled under the " +
                "Application Settings")
            .setPositiveButton("SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") {dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun pickImageUsingCamera(activity: Activity) {
        val imageFile = File(applicationContext.filesDir, "${UUID.randomUUID()}.jpg")
        when (activity) {
            is MyProfileActivity -> {
                activity.mSelectedImageFileUri = FileProvider.getUriForFile(applicationContext, "com.example.adi.trello.fileProvider", imageFile)
                activity.openCameraLauncher.launch(activity.mSelectedImageFileUri)
            }
            is CreateBoardActivity -> {
                activity.mSelectedImageFileUri = FileProvider.getUriForFile(applicationContext, "com.example.adi.trello.fileProvider", imageFile)
                activity.openCameraLauncher.launch(activity.mSelectedImageFileUri)
            }
        }
    }

    private fun pickImageUsingGallery(activity: Activity) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        when (activity) {
            is MyProfileActivity -> {
                activity.openGalleryLauncher.launch(intent)
            }
            is CreateBoardActivity -> {
                activity.openGalleryLauncher.launch(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQ_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (currentActivityInUse != null) {
                        pickImageUsingGallery(currentActivityInUse!!)
                        currentActivityInUse = null
                    }
                }
            }
            READ_PERMISSION_REQ_CODE -> {
                if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    if (currentActivityInUse != null) {
                        pickImageUsingGallery(currentActivityInUse!!)
                        currentActivityInUse = null
                    }
                }
            }
        }
    }

    fun getFileExtension(uri: Uri?): String? {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri!!))
    }

    companion object {
        private const val READ_PERMISSION_REQ_CODE = 1
        private const val CAMERA_PERMISSION_REQ_CODE = 2
    }
}