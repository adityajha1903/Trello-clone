package com.example.adi.trello.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.adi.trello.R
import com.example.adi.trello.databinding.ActivityMyProfileBinding
import com.example.adi.trello.databinding.EditUserDetailBottomSheetLayoutBinding
import com.example.adi.trello.firebase.Firestore
import com.example.adi.trello.models.User
import com.example.adi.trello.utils.Constants
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException
import kotlin.collections.HashMap

class MyProfileActivity : BaseActivity() {

    companion object {
        private const val EDIT_NAME = -1
        private const val EDIT_MOBILE = -2
    }

    private var binding: ActivityMyProfileBinding? = null

    var mSelectedImageFileUri: Uri? = null
    private var mProfileImageUrl: String = ""
    private lateinit var mUserDetails: User

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null && result.data?.data != null) {
                mSelectedImageFileUri = result.data?.data
                try {
                    Glide
                        .with(this)
                        .load(mSelectedImageFileUri)
                        .centerCrop()
                        .placeholder(R.drawable.transparent)
                        .into(binding?.proPicIVProfileActivity!!)

                    uploadUserImage()
                } catch (e: IOException) {
                    e.printStackTrace()
                    mSelectedImageFileUri = null
                }
            }
        }

    val openCameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it) {
            try {
                Glide
                    .with(this)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.transparent)
                    .into(binding?.proPicIVProfileActivity!!)

                uploadUserImage()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            mSelectedImageFileUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setSupportActionBar(binding?.toolBarProfileActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_24)
        binding?.toolBarProfileActivity?.setNavigationOnClickListener { finish() }
        supportActionBar?.title = resources.getString(R.string.my_profile)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            })
        }

        binding?.proPicFrameProfileActivity?.setOnClickListener {
            showImageSelectingDialog(this)
        }

        binding?.editUserName?.setOnClickListener {
            editUser(EDIT_NAME)
        }

        binding?.editUserMobile?.setOnClickListener {
            editUser(EDIT_MOBILE)
        }

        showProgressDialog(resources.getString(R.string.please_wait))
        Firestore().loadUserData(this)
    }

    private fun editUser(field: Int) {
        val dialogBinding = EditUserDetailBottomSheetLayoutBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this, R.style.BottomSheetStyle)
        dialog.setContentView(dialogBinding.root)

        when (field) {
            EDIT_NAME -> {
                dialogBinding.dialogTitleTv.text = resources.getString(R.string.enter_your_name)
                dialogBinding.editText.inputType = InputType.TYPE_CLASS_TEXT
                dialogBinding.editText.setText(binding?.userNameTVProfileActivity?.text)
            }
            EDIT_MOBILE -> {
                dialogBinding.dialogTitleTv.text = resources.getString(R.string.enter_your_mobile_number)
                dialogBinding.editText.inputType = InputType.TYPE_CLASS_NUMBER
                dialogBinding.editText.setText(binding?.userMobileTVProfileActivity?.text)
            }
        }

        dialogBinding.cancelBtn.setOnClickListener { dialog.dismiss() }
        dialogBinding.saveBtn.setOnClickListener {
            when (field) {
                EDIT_NAME -> {
                    binding?.userNameTVProfileActivity?.text = dialogBinding.editText.text
                }
                EDIT_MOBILE -> {
                    binding?.userMobileTVProfileActivity?.text = dialogBinding.editText.text
                }
            }
            dialog.dismiss()
            updateUserProfileData()
        }
        dialog.show()
    }

    fun setUserData(user: User) {
        mUserDetails = user

        Glide
            .with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.transparent)
            .into(binding?.proPicIVProfileActivity!!)

        binding?.userNameTVProfileActivity?.text = user.name
        binding?.userEmailTVProfileActivity?.text = user.email
        binding?.firstLetterTVProfileActivity?.text = user.name[0].uppercase()
        if (user.mobile != 0L) {
            binding?.userMobileTVProfileActivity?.text = user.mobile.toString()
        }

        hideProgressDialog()
    }

    private fun uploadUserImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        if (mSelectedImageFileUri != null) {
            val sRef: StorageReference =
                FirebaseStorage.getInstance().reference.child(
                    "USER_IMAGE" + System.currentTimeMillis() + "." + getFileExtension(mSelectedImageFileUri))

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener { taskSnapshot ->
                Log.i("Firebase image url", taskSnapshot.metadata?.reference?.downloadUrl.toString())

                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                    Log.i("Download image URL", uri.toString())
                    mProfileImageUrl = uri.toString()
                    hideProgressDialog()
                    updateUserProfileData()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                hideProgressDialog()
            }

        }
    }

    private fun updateUserProfileData() {
        showProgressDialog(resources.getString(R.string.please_wait))
        val userHashMap = HashMap<String, Any>()
        var anyChangesMade = false

        if (mProfileImageUrl.isNotEmpty() && mProfileImageUrl != mUserDetails.image) {
            userHashMap[Constants.IMAGE] = mProfileImageUrl
            anyChangesMade = true
        }

        if (binding?.userNameTVProfileActivity?.text != mUserDetails.name) {
            userHashMap[Constants.NAME] = binding?.userNameTVProfileActivity?.text.toString()
            anyChangesMade = true
        }

        if (binding?.userMobileTVProfileActivity?.text != mUserDetails.mobile.toString() && binding?.userMobileTVProfileActivity?.text?.isNotEmpty() == true) {
            userHashMap[Constants.MOBILE] = binding?.userMobileTVProfileActivity?.text.toString().toLong()
            anyChangesMade = true
        } else {
            userHashMap[Constants.MOBILE] = 0
        }

        if (anyChangesMade) {
            MainActivity.userDataChanged = true
            Firestore().updateUserProfileData(this, userHashMap)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}