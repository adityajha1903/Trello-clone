package com.example.adi.trello.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adi.trello.R
import com.example.adi.trello.adapters.CardMembersRecyclerImageItemAdapter
import com.example.adi.trello.adapters.CardRecyclerItemAdapter
import com.example.adi.trello.adapters.TaskRecyclerItemAdapter
import com.example.adi.trello.databinding.ActivityTaskListBinding
import com.example.adi.trello.firebase.Firestore
import com.example.adi.trello.models.Board
import com.example.adi.trello.models.Card
import com.example.adi.trello.models.Task
import com.example.adi.trello.models.User
import com.example.adi.trello.utils.Constants

class TaskListActivity : BaseActivity() {

    private var binding: ActivityTaskListBinding? = null

    private lateinit var mBoardDetails: Board

    private var boardDocumentId = ""

    companion object {
        var updateNeeded: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setUpToolBarAndNavOnClick()

        if (intent.hasExtra(Constants.DOCUMENT_ID)) {
            boardDocumentId = intent.getStringExtra(Constants.DOCUMENT_ID)!!
        }

        showProgressDialog(resources.getString(R.string.please_wait))
        Firestore().getBoardDetails(this, boardDocumentId)
    }

    override fun onResume() {
        if (updateNeeded) {
            showProgressDialog(resources.getString(R.string.please_wait))
            Firestore().getBoardDetails(this, boardDocumentId)
            updateNeeded = false
        }
        super.onResume()
    }

    fun cardDetails(taskListPosition: Int, cardPosition: Int) {
        val intent = Intent(this, CardDetailsActivity::class.java)
        intent.putExtra(Constants.BOARD_DETAILS, mBoardDetails)
        intent.putExtra(Constants.TASK_LIST_ITEM_POSITION, taskListPosition)
        intent.putExtra(Constants.CARD_LIST_ITEM_POSITION, cardPosition)
        startActivity(intent)
    }

    private fun setUpToolBarAndNavOnClick() {
        setSupportActionBar(binding?.toolBar)
        binding?.toolBar?.setNavigationIcon(R.drawable.ic_baseline_arrow_back_ios_24)
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

    fun setBoardDetails(board: Board) {
        hideProgressDialog()
        mBoardDetails = board
        supportActionBar?.title = board.name

        val lastTask = Task(resources.getString(R.string.add_list))
        board.taskList.add(lastTask)

        binding?.rvTaskList?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding?.rvTaskList?.setHasFixedSize(true)
        binding?.rvTaskList?.adapter = TaskRecyclerItemAdapter(this, board.taskList, mBoardDetails)
    }

    fun createTaskList(taskListName: String) {
        val task = Task(taskListName, Firestore().getCurrentUserId())
        mBoardDetails.taskList.add(0, task)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)

        showProgressDialog(resources.getString(R.string.please_wait))
        Firestore().addUpdateTaskList(this, mBoardDetails)
    }

    fun deleteTaskList(position: Int) {
        mBoardDetails.taskList.removeAt(position)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)

        showProgressDialog(resources.getString(R.string.please_wait))
        Firestore().addUpdateTaskList(this, mBoardDetails)
    }

    fun updateTaskList(position: Int, listName: String, task: Task) {
        val newTask = Task(listName, task.createdBy, task.cards)
        mBoardDetails.taskList[position] = newTask
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)

        showProgressDialog(resources.getString(R.string.please_wait))
        Firestore().addUpdateTaskList(this, mBoardDetails)
    }

    fun addUpdateTaskListSuccess() {
        Firestore().getBoardDetails(this, mBoardDetails.documentId)
    }

    fun addCardToTaskList(position: Int, cardName: String) {
        val cardAssignedUsersList = ArrayList<String>()
        cardAssignedUsersList.add(Firestore().getCurrentUserId())

        val card = Card(cardName, Firestore().getCurrentUserId(), cardAssignedUsersList)

        val cardsList = mBoardDetails.taskList[position].cards
        cardsList.add(card)
        val task = Task(mBoardDetails.taskList[position].title, mBoardDetails.taskList[position].createdBy, cardsList)

        mBoardDetails.taskList[position] = task
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)

        showProgressDialog(resources.getString(R.string.please_wait))
        Firestore().addUpdateTaskList(this, mBoardDetails)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_members, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_members -> {
                val intent = Intent(this, MembersActivity::class.java)
                intent.putExtra(Constants.BOARD_DETAILS, mBoardDetails)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    fun setUpCardMembersImage(userList: ArrayList<User>, holder: CardRecyclerItemAdapter.CardRecyclerViewHolder) {
        holder.rvMemberImage.adapter = CardMembersRecyclerImageItemAdapter(this, userList)
    }
}