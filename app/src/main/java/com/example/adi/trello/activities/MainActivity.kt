package com.example.adi.trello.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.adi.trello.R
import com.example.adi.trello.adapters.BoardRecyclerItemAdapter
import com.example.adi.trello.databinding.ActivityMainBinding
import com.example.adi.trello.firebase.Firestore
import com.example.adi.trello.models.Board
import com.example.adi.trello.models.User
import com.example.adi.trello.utils.Constants
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var binding: ActivityMainBinding? = null

    private lateinit var mUser: User

    private lateinit var mSharedPreferences: SharedPreferences

    private lateinit var mBoardList: ArrayList<Board>

    companion object {
        var userDataChanged = true
        var newBoardAdded = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setSupportActionBar(binding?.includedAppBar?.toolBar)
        binding?.includedAppBar?.toolBar?.setNavigationIcon(R.drawable.ic_baseline_menu_24)
        binding?.includedAppBar?.toolBar?.setNavigationOnClickListener {
            toggleDrawer()
        }
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                backPressed()
            }
        } else {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPressed()
                }
            })
        }

        binding?.includedAppBar?.addBoardFloatingActionButton?.setOnClickListener {
            val intent = Intent(this, CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUser.name)
            startActivity(intent)
        }

        itemTouchHelper.attachToRecyclerView(binding?.includedAppBar?.includedMainContent?.rvBoardList)
        mSharedPreferences = getSharedPreferences(Constants.TRELLO_PREFERENCES, Context.MODE_PRIVATE)

        binding?.navView?.setNavigationItemSelectedListener(this)
    }

    override fun onResume() {
        if (userDataChanged || newBoardAdded) {
            val tokenUpdated =  mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)
            if (tokenUpdated) {
                showProgressDialog()
                Firestore().loadUserData(this, true)
            } else {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(javaClass.simpleName, "Fetching FCM registration token failed", task.exception)
                        return@OnCompleteListener
                    }
                    val token = task.result
                    updateFCMToken(token)
                })
            }
            userDataChanged = false
            newBoardAdded = false
        }
        super.onResume()
    }

    private fun backPressed() {
        if (binding?.drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            binding?.drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            finish()
        }
    }

    fun populateBoardsListToUi(boardList: ArrayList<Board>) {
        mBoardList = boardList
        val emptyListTv = binding?.includedAppBar?.tvNoBoardsAvailable
        val recyclerView = binding?.includedAppBar?.includedMainContent?.rvBoardList
        if (boardList.isNotEmpty()) {
            emptyListTv?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            recyclerView?.layoutManager = LinearLayoutManager(this)
            recyclerView?.adapter = BoardRecyclerItemAdapter(this, boardList) { _, board ->
                val intent = Intent(this, TaskListActivity::class.java)
                intent.putExtra(Constants.DOCUMENT_ID, board.documentId)
                startActivity(intent)
            }
        } else {
            emptyListTv?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
        }

        hideProgressDialog()
    }

    private fun toggleDrawer() {
        if (binding?.drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            binding?.drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            binding?.drawerLayout?.openDrawer(GravityCompat.START)
        }
    }

    private val itemTouchHelper = ItemTouchHelper(object: SimpleCallback(0, ItemTouchHelper.LEFT){
        val mClearPaint = Paint()

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val documentId = mBoardList[position].documentId
            binding?.includedAppBar?.includedMainContent?.rvBoardList?.adapter?.notifyItemRemoved(position)
            mBoardList.removeAt(position)
            showAlertDialogForDeletingBoards(documentId)
        }

        private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
            c.drawRect(left, top, right, bottom, mClearPaint)
        }

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
            return 0.7f
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {

            mClearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            val deleteDrawable = ContextCompat.getDrawable(applicationContext, R.drawable.ic_baseline_delete_24)
            deleteDrawable?.setTint(resources.getColor(R.color.error_red, null))
            val itemView = viewHolder.itemView
            val isCancelled = (dX == 0f) && !isCurrentlyActive
            if (isCancelled) {
                clearCanvas(c,itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                return
            }

            val margin = itemView.height/4
            val top = itemView.top + margin
            val bottom = itemView.bottom - margin
            val right = itemView.right - margin
            val left = right - bottom + top
            deleteDrawable?.setBounds(left, top, right, bottom)
            deleteDrawable?.draw(c)

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    })

    private fun showAlertDialogForDeletingBoards(documentId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete this board")
        builder.setIcon(R.drawable.ic_baseline_warning_24)
        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            showProgressDialog()
            Firestore().deleteCard(this, documentId)
        }
        builder.setNegativeButton("No") { dialog, _ ->
            showProgressDialog()
            Firestore().loadUserData(this, true)
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_my_profile -> {
                startActivity(Intent(this, MyProfileActivity::class.java))
            }
            R.id.nav_sign_out -> {
                mSharedPreferences.edit().clear().apply()
                FirebaseAuth.getInstance().signOut()
                val i = Intent(this, IntroActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(i)
                finish()
            }
        }
        binding?.drawerLayout?.closeDrawer(GravityCompat.START)
        return true
    }

    fun updateNavigationUserDetail(user: User, readBoardList: Boolean) {
        mUser = user
        val userImageView = binding?.navView?.getHeaderView(0)?.findViewById(R.id.proPicIV) as ImageView
        val userNameTextView = binding?.navView?.getHeaderView(0)?.findViewById(R.id.userNameTV) as TextView
        val userEmailTextView = binding?.navView?.getHeaderView(0)?.findViewById(R.id.userEmailTV) as TextView
        val firstLetterTextView = binding?.navView?.getHeaderView(0)?.findViewById(R.id.firstLetterTV) as TextView
        Glide
            .with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.transparent)
            .into(userImageView)
        userNameTextView.text = user.name
        userEmailTextView.text = user.email
        firstLetterTextView.text = user.name[0].uppercase()

        if (readBoardList) {
            Firestore().getBoardsList(this)
        } else {
            hideProgressDialog()
        }
    }

    fun tokenUpdateSuccess() {
        val editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()

        Firestore().loadUserData(this, true)
    }

    private fun updateFCMToken(token: String) {
        val userHashMap = HashMap<String, Any>()
        userHashMap[Constants.FCM_TOKEN] = token

        showProgressDialog()
        Firestore().updateUserProfileData(this, userHashMap)
    }

    fun deleteBoardSuccess() {
        Firestore().loadUserData(this, true)
    }
}