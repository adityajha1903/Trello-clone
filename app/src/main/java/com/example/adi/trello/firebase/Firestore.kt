package com.example.adi.trello.firebase

import android.app.Activity
import android.app.Dialog
import android.util.Log
import com.example.adi.trello.R
import com.example.adi.trello.activities.*
import com.example.adi.trello.adapters.CardRecyclerItemAdapter
import com.example.adi.trello.databinding.CardMembersDialogBinding
import com.example.adi.trello.models.Board
import com.example.adi.trello.models.User
import com.example.adi.trello.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.skydoves.colorpickerview.kotlin.colorPickerDialog

class Firestore {

    private val mFirestore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo: User) {
        mFirestore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.userRegisteredSuccess()
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e("registerUser", "registerUser: $e")
            }
    }

    fun loadUserData(activity: Activity, readBoardList: Boolean = false) {
        mFirestore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .get()
            .addOnSuccessListener { document ->
                val loggedUser = document.toObject(User::class.java)
                if (loggedUser != null) {
                    when (activity) {
                        is LogInActivity -> {
                            activity.logInSuccess()
                        }
                        is MainActivity -> {
                            activity.updateNavigationUserDetail(loggedUser, readBoardList)
                        }
                        is MyProfileActivity -> {
                            activity.setUserData(loggedUser)
                        }
                    }
                }
            }.addOnFailureListener { e ->
                when (activity) {
                    is LogInActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e("logInUser", "logInUser: $e")
            }
    }

    fun createBoard(activity: CreateBoardActivity, board: Board) {
        mFirestore.collection(Constants.BOARDS)
            .document()
            .set(board, SetOptions.merge())
            .addOnSuccessListener {
                activity.boardCreatedSuccessfully()
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating a board", e)
            }
    }

    fun updateUserProfileData(activity: Activity, userHashMap: HashMap<String, Any>) {
        mFirestore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .update(userHashMap)
            .addOnSuccessListener {
                when (activity) {
                    is MyProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MainActivity -> {
                        activity.tokenUpdateSuccess()
                    }
                }
            }.addOnFailureListener { e ->
                when (activity) {
                    is MyProfileActivity -> {
                        activity.hideProgressDialog()
                        Log.e(activity.javaClass.simpleName, "Error while creating a board", e)
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog()
                        Log.e(activity.javaClass.simpleName, "Error while creating a board", e)
                    }
                }
            }
    }

    fun getBoardsList(activity: MainActivity) {
        mFirestore.collection(Constants.BOARDS)
            .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserId())
            .get()
            .addOnSuccessListener { document ->
                val boardList = ArrayList<Board>()
                for (i in document.documents) {
                    val board = i.toObject(Board::class.java)
                    board?.documentId = i.id
                    boardList.add(board!!)
                }
                activity.populateBoardsListToUi(boardList)
            }.addOnFailureListener { e ->
                Log.e(activity.javaClass.simpleName, e.message, e)
            }
    }

    fun getCurrentUserId(): String {
        val currentUser = FirebaseAuth.getInstance().currentUser
        var userId = ""
        if (currentUser != null) {
            userId = currentUser.uid
        }
        return userId
    }

    fun getBoardDetails(activity: TaskListActivity, boardDocumentId: String) {
        mFirestore.collection(Constants.BOARDS)
            .document(boardDocumentId)
            .get()
            .addOnSuccessListener { document ->
                val board = document.toObject(Board::class.java)
                board?.documentId = document.id
                activity.setBoardDetails(board!!)
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, e.message, e)
            }
    }

    fun getMemberDetail(activity: BaseActivity, email: String) {
        mFirestore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL, email)
            .get()
            .addOnSuccessListener { document ->
                if (document.documents.size > 0) {
                    val user = document.documents[0].toObject(User::class.java)
                    if (user != null) {
                        if (activity is MembersActivity) {
                            activity.memberDetails(user)
                        } else if (activity is CardDetailsActivity) {
                            activity.memberDetails(user)
                        }
                    }
                } else {
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar(activity.resources.getString(R.string.no_such_user_exist))
                }
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, e.message, e)
            }
    }

    fun assignMemberToBoard(activity: MembersActivity, board: Board, member: User) {
        val assignedToHashMap = HashMap<String, Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo

        mFirestore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(assignedToHashMap)
            .addOnSuccessListener {
                activity.memberAssignSuccess(member)
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, e.message, e)
            }
    }

    fun getAssignedMembersListDetails(activity: BaseActivity, assignedTo: ArrayList<String>, dialogBinding: CardMembersDialogBinding? = null, dialog: Dialog? = null) {
        mFirestore.collection(Constants.USERS)
            .whereIn(Constants.USER_ID, assignedTo)
            .get()
            .addOnSuccessListener { document ->
                val userList = ArrayList<User>()
                for (i in document) {
                    val user = i.toObject(User::class.java)
                    userList.add(user)
                }
                if (activity is MembersActivity) {
                    activity.setUpMembers(userList)
                } else if (activity is CardDetailsActivity) {
                    activity.setUpMembers(userList, dialogBinding!!, dialog!!)
                }
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, e.message, e)
            }
    }

    fun addUpdateTaskList(activity: BaseActivity, board: Board, closeActivity: Boolean = false) {
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFirestore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {
                if (activity is TaskListActivity) {
                    activity.addUpdateTaskListSuccess()
                } else if (activity is CardDetailsActivity) {
                    activity.addUpdateTaskListSuccess(closeActivity)
                }
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, e.message,e )
            }
    }

    fun getUserName(context: CardDetailsActivity, documentId: String) {
        mFirestore.collection(Constants.USERS)
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                if (user != null) {
                    context.setCreatedByName(user.name)
                }
            }.addOnFailureListener { e ->
                context.hideProgressDialog()
                Log.e(context.javaClass.simpleName, e.message, e)
            }
    }

    fun getCardAssignedMembersListDetails(activity: BaseActivity, assignedTo: ArrayList<String>, holder: CardRecyclerItemAdapter.CardRecyclerViewHolder) {
        mFirestore.collection(Constants.USERS)
            .whereIn(Constants.USER_ID, assignedTo)
            .get()
            .addOnSuccessListener { document ->
                val userList = ArrayList<User>()
                for (i in document) {
                    val user = i.toObject(User::class.java)
                    userList.add(user)
                }
                if (activity is TaskListActivity) {
                    activity.setUpCardMembersImage(userList, holder)
                }
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, e.message, e)
            }
    }

    fun deleteCard(activity: MainActivity, documentId: String) {
        mFirestore.collection(Constants.BOARDS)
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                activity.deleteBoardSuccess()
            }
            .addOnFailureListener{
                activity.hideProgressDialog()
            }
    }
}