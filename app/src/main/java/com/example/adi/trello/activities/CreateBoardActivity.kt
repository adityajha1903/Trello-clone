package com.example.adi.trello.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.adi.trello.R
import com.example.adi.trello.databinding.ActivityCreateBoardBinding
import com.example.adi.trello.firebase.Firestore
import com.example.adi.trello.models.Board
import com.example.adi.trello.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class CreateBoardActivity : BaseActivity() {

    private var binding: ActivityCreateBoardBinding? = null

    var mSelectedImageFileUri: Uri? = null
    private var mProfileImageUrl: String = ""

    private var mUserName = ""

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
                        .into(binding?.proPicIVBoardActivity!!)
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
                    .into(binding?.proPicIVBoardActivity!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            mSelectedImageFileUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBoardBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setUpToolBarAndNavClick()

        if (intent.hasExtra(Constants.NAME)) {
            mUserName = intent.getStringExtra(Constants.NAME)!!
        }

        binding?.proPicIVBoardActivity?.setOnClickListener {
            showImageSelectingDialog(this)
        }

        binding?.createBoardBtn?.setOnClickListener {
            if (binding?.boardNameEditText?.text?.isNotEmpty()!! && mSelectedImageFileUri != null) {
                uploadUserImage()
            } else {
                showErrorSnackBar(resources.getString(R.string.please_fill_all_the_required_fields))
            }
        }
    }

    private fun setUpToolBarAndNavClick() {
        setSupportActionBar(binding?.toolBarBoardActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_24)
        binding?.toolBarBoardActivity?.setNavigationOnClickListener { finish() }
        supportActionBar?.title = resources.getString(R.string.create_board)
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
    }

    fun boardCreatedSuccessfully() {
        hideProgressDialog()
        finish()
    }

    private fun createBoard() {
        showProgressDialog(resources.getString(R.string.please_wait))

        val assignedUserArrayList = ArrayList<String>()
        assignedUserArrayList.add(Firestore().getCurrentUserId())

        val board = Board(
            binding?.boardNameEditText?.text.toString(),
            mProfileImageUrl,
            mUserName,
            assignedUserArrayList
        )

        MainActivity.newBoardAdded = true
        Firestore().createBoard(this, board)
    }

    private fun uploadUserImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        if (mSelectedImageFileUri != null) {
            val sRef: StorageReference =
                FirebaseStorage.getInstance().reference.child(
                    "BOARD_IMAGE" + System.currentTimeMillis() + "." + getFileExtension(mSelectedImageFileUri))

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener { taskSnapshot ->
                Log.i("Firebase image url", taskSnapshot.metadata?.reference?.downloadUrl.toString())

                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                    Log.i("Download image URL", uri.toString())
                    mProfileImageUrl = uri.toString()
                    hideProgressDialog()
                    createBoard()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                hideProgressDialog()
            }
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}