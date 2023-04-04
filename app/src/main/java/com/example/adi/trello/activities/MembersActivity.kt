package com.example.adi.trello.activities

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adi.trello.R
import com.example.adi.trello.adapters.MemberRecyclerItemAdapter
import com.example.adi.trello.databinding.ActivityMembersBinding
import com.example.adi.trello.databinding.DialogSearchMemberBinding
import com.example.adi.trello.fcm.MyFirebaseMessagingService
import com.example.adi.trello.firebase.Firestore
import com.example.adi.trello.models.Board
import com.example.adi.trello.models.User
import com.example.adi.trello.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MembersActivity : BaseActivity() {

    private var binding: ActivityMembersBinding? = null

    private var mBoardDetails: Board? = null
    private lateinit var mAssignedMembersList: ArrayList<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMembersBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setUpToolBarAndNavOnClick()

        if (intent.hasExtra(Constants.BOARD_DETAILS)) {
            mBoardDetails = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Constants.BOARD_DETAILS, Board::class.java)
            } else {
                intent.getParcelableExtra(Constants.BOARD_DETAILS)
            }
        }

        val assignedTo = mBoardDetails?.assignedTo
        showProgressDialog()
        if (assignedTo != null) {
            Firestore().getAssignedMembersListDetails(this, assignedTo)
        }
    }

    private fun setUpToolBarAndNavOnClick() {
        setSupportActionBar(binding?.toolBar)
        binding?.toolBar?.setNavigationIcon(R.drawable.ic_baseline_arrow_back_ios_24)
        supportActionBar?.title = resources.getString(R.string.members)
        binding?.toolBar?.setNavigationOnClickListener {
            finish()
        }
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

    fun setUpMembers(membersList: ArrayList<User>) {
        hideProgressDialog()
        mAssignedMembersList = membersList
        binding?.rvMembersList?.layoutManager = LinearLayoutManager(this)
        binding?.rvMembersList?.adapter = MemberRecyclerItemAdapter(this, membersList)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_member, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_member -> {
                showDialogSearchMember()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDialogSearchMember() {
        val dialog = Dialog(this)
        val dialogBinding = DialogSearchMemberBinding.inflate(layoutInflater)
        val layoutParams = LinearLayout.LayoutParams(resources.displayMetrics.widthPixels - 32, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.gravity = Gravity.CENTER
        layoutParams.setMargins(20, 20, 20, 20)
        dialogBinding.ll.layoutParams = layoutParams
        dialog.setContentView(dialogBinding.root)
        dialogBinding.btnCancelMember.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.btnAddMember.setOnClickListener {
            val email = dialogBinding.etEmail.text.toString()
            if (email.isNotEmpty()) {
                showProgressDialog()
                Firestore().getMemberDetail(this, email)
            } else {
                showErrorSnackBar(resources.getString(R.string.please_fill_all_the_required_fields))
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    fun memberDetails(user: User) {
        if (mBoardDetails?.assignedTo?.contains(user.id)!!) {
            hideProgressDialog()
            showErrorSnackBar(resources.getString(R.string.member_already_assigned))
        } else {
            mBoardDetails?.assignedTo?.add(user.id)
            Firestore().assignMemberToBoard(this, mBoardDetails!!, user)
        }
    }

    fun memberAssignSuccess(user: User) {
        hideProgressDialog()
        TaskListActivity.updateNeeded = true
        mAssignedMembersList.add(user)
        setUpMembers(mAssignedMembersList)

        lifecycleScope.launch(Dispatchers.IO) {
            val result = sendNotificationToUser(mBoardDetails?.name!!, user.fcmToken)
            Log.d(MyFirebaseMessagingService.TAG, result)
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

    private fun sendNotificationToUser(boardName: String, token: String): String {
        var result: String

        var connection: HttpURLConnection? = null
        try {
            val url = URL(Constants.FCM_BASE_URL)
            connection = url.openConnection() as HttpURLConnection

            connection.doOutput = true
            connection.doInput = true
            connection.instanceFollowRedirects = false
            connection.requestMethod = Constants.POST
            connection.setRequestProperty(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
            connection.setRequestProperty(Constants.CHARSET, Constants.UTF_8)
            connection.setRequestProperty(Constants.ACCEPT, Constants.APPLICATION_JSON)
            connection.setRequestProperty(Constants.FCM_AUTHORIZATION, "${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}")
            connection.useCaches = false

            val wr = DataOutputStream(connection.outputStream)

            val jsonRequest = JSONObject()
            val dataObject = JSONObject()
            dataObject.put(Constants.FCM_TITLE, "Assigned to the Board $boardName")
            dataObject.put(Constants.FCM_BODY, "You have been assigned to the new board by ${mAssignedMembersList[0].name}")

            jsonRequest.put(Constants.FCM_TO, token)
            jsonRequest.put(Constants.FCM_NOTIFICATION, dataObject)
            jsonRequest.put(Constants.FCM_PRIORITY, Constants.FCM_HIGH)

            val body = "{\"to\": \"${token}\",\"notification\": {\"title\": \"Assigned to the Board $boardName\",\"body\": \"You have been assigned to the new board by ${mAssignedMembersList[0].name}\"},\"priority\": \"high\"}"
            wr.writeBytes(jsonRequest.toString())
            wr.flush()
            wr.close()

            val httpResult: Int = connection.responseCode

            if (httpResult == HttpURLConnection.HTTP_OK) {

                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val sb = StringBuilder()
                var line: String?
                try {
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line + "\n")
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                result = sb.toString()
            } else {
                result = connection.responseMessage
            }

        } catch (e: SocketTimeoutException) {
            result = "Connection Timeout"
        } catch (e: Exception) {
            result = "Error : " + e.message
        } finally {
            connection?.disconnect()
        }

        return result
    }
}