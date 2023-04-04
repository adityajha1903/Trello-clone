package com.example.adi.trello.activities

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adi.trello.R
import com.example.adi.trello.adapters.CardMembersRecyclerItemAdapter
import com.example.adi.trello.databinding.ActivityCardDetailsBinding
import com.example.adi.trello.databinding.CardMembersDialogBinding
import com.example.adi.trello.databinding.EditUserDetailBottomSheetLayoutBinding
import com.example.adi.trello.firebase.Firestore
import com.example.adi.trello.models.Board
import com.example.adi.trello.models.Card
import com.example.adi.trello.models.User
import com.example.adi.trello.utils.Constants
import com.example.adi.trello.views.CustomFlag
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class CardDetailsActivity : BaseActivity() {

    private var binding: ActivityCardDetailsBinding? = null

    private lateinit var mBoardDetails: Board
    private var mTaskListPosition = 0
    private var mCardListPosition = 0

    companion object {
        private const val EDIT_CARD_NAME = 1
        private const val ADD_NEW_MEMBER = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardDetailsBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        getIntentData()
        setUpToolBarAndNavOnClick()

        binding?.editCardName?.setOnClickListener {
            showEditCardDetailsDialog(EDIT_CARD_NAME)
        }

        binding?.editDueDate?.setOnClickListener {
            showDatePickerDialog()
        }

        binding?.editLabelColor?.setOnClickListener {
            showColorPickerDialog()
        }

        binding?.addMember?.setOnClickListener {
            showEditCardDetailsDialog(ADD_NEW_MEMBER)
        }

        binding?.showMembers?.setOnClickListener {
            showMembersListDialog()
        }
        binding?.tvMemberList?.setOnClickListener {
            showMembersListDialog()
        }
        binding?.showMembersList?.setOnClickListener {
            showMembersListDialog()
        }
    }

    private fun showDatePickerDialog() {
        val c = Calendar.getInstance()
        val rnYear = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
            val sDayOfMonth = if (dayOfMonth < 10) "0$dayOfMonth" else dayOfMonth
            val sMonthOfYear = if (monthOfYear + 1 < 1) "0${monthOfYear + 1}" else "${monthOfYear + 1}"

            val selectedDate = "$sDayOfMonth/$sMonthOfYear/$year"
            binding?.tvDueDate?.text = selectedDate

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val theDate = sdf.parse(selectedDate)
            val time = theDate?.time
            if (time != null) {
                mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].dueDate = time
                updateCardDetails()
            }
        }, rnYear, month, day).show()
    }

    private fun showColorPickerDialog() {
        val dialog = ColorPickerDialog.Builder(this)
            .setTitle("Select color")
            .setPreferenceName("MyColorPickerDialog")
            .setPositiveButton(resources.getString(R.string.confirm),
                ColorEnvelopeListener { envelope, _ ->
                    binding?.tvColorHashCode?.setBackgroundColor(Color.parseColor("#" + envelope.hexCode))
                    mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].labelColor = "#" + envelope.hexCode
                    updateCardDetails()
                })
            .setNegativeButton(resources.getString(R.string.cancel)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .attachAlphaSlideBar(true)
            .attachBrightnessSlideBar(true)
            .setBottomSpace(12)

        dialog.colorPickerView.flagView = CustomFlag(this, R.layout.layout_flag)
        dialog.show()
    }

    private fun showMembersListDialog() {
        val dialogBinding = CardMembersDialogBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)

        showProgressDialog()
        Firestore().getAssignedMembersListDetails(this, mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].assignedTo, dialogBinding, dialog)
    }

    fun setUpMembers(userList: ArrayList<User>, dialogBinding: CardMembersDialogBinding, dialog: Dialog) {
        hideProgressDialog()
        dialogBinding.rvCardMembers.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvCardMembers.adapter = CardMembersRecyclerItemAdapter(this, userList)
        dialog.show()
    }

    private fun showEditCardDetailsDialog(field: Int) {
        val dialogBinding = EditUserDetailBottomSheetLayoutBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this, R.style.BottomSheetStyle)
        dialog.setContentView(dialogBinding.root)
        when (field) {
            EDIT_CARD_NAME -> {
                dialogBinding.dialogTitleTv.text = resources.getString(R.string.enter_card_name)
                dialogBinding.editText.setText(binding?.tvCardName?.text)
                dialogBinding.editText.inputType = InputType.TYPE_CLASS_TEXT
                dialogBinding.saveBtn.setOnClickListener {
                    if (dialogBinding.editText.text.isNotEmpty()) {
                        binding?.tvCardName?.text = dialogBinding.editText.text
                        updateCardDetails()
                    } else {
                        showErrorSnackBar(resources.getString(R.string.please_fill_all_the_required_fields))
                    }
                    dialog.dismiss()
                }
            }
            ADD_NEW_MEMBER -> {
                dialogBinding.dialogTitleTv.text = resources.getString(R.string.enter_members_email)
                dialogBinding.editText.setText(binding?.tvCardName?.text)
                dialogBinding.editText.hint = resources.getString(R.string.email)
                dialogBinding.editText.setHintTextColor(resources.getColor(R.color.white2, null))
                dialogBinding.editText.setText("")
                dialogBinding.editText.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT
                dialogBinding.saveBtn.setOnClickListener {
                    val newEmail = dialogBinding.editText.text.toString()
                    if (newEmail.isNotEmpty()) {
                        showProgressDialog()
                        Firestore().getMemberDetail(this, newEmail)
                    } else {
                        showErrorSnackBar(resources.getString(R.string.please_fill_all_the_required_fields))
                    }
                    dialog.dismiss()
                }
            }
        }
        dialogBinding.cancelBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    fun memberDetails(user: User) {
        hideProgressDialog()
        if (mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].assignedTo.contains(user.id)) {
            showErrorSnackBar(resources.getString(R.string.member_already_assigned))
        } else if (!mBoardDetails.assignedTo.contains(user.id)) {
            showErrorSnackBar(resources.getString(R.string.first_add_this_member_to_the_board))
        } else {
            mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].assignedTo.add(user.id)
            updateCardDetails()
        }
    }

    private fun getIntentData() {
        if (intent.hasExtra(Constants.BOARD_DETAILS)) {
            mBoardDetails = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Constants.BOARD_DETAILS, Board::class.java)!!
            } else {
                intent.getParcelableExtra(Constants.BOARD_DETAILS)!!
            }
        }
        if (intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)) {
            mTaskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION, -1)
        }
        if (intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)) {
            mCardListPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION, -1)
        }
    }

    private fun setUpToolBarAndNavOnClick() {
        setSupportActionBar(binding?.toolBar)
        binding?.toolBar?.setNavigationIcon(R.drawable.ic_baseline_arrow_back_ios_24)
        binding?.toolBar?.setNavigationOnClickListener {
            finish()
        }
        val cardName = mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].name
        supportActionBar?.title = cardName
        binding?.tvCardName?.text = cardName
        val labelColor: String = mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].labelColor
        if (labelColor.isNotEmpty()) {
            binding?.tvColorHashCode?.setBackgroundColor(Color.parseColor(labelColor))
        }
        val dueDate = mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].dueDate
        if (dueDate > 0) {
            val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val selectedDate = simpleDateFormat.format(Date(dueDate))
            binding?.tvDueDate?.text = selectedDate
        }
        showProgressDialog()
        Firestore().getUserName(this, mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].createdBy)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete_card -> {
                showAlertDialogForDeletingCard()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAlertDialogForDeletingCard() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete ${mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].name}")
        builder.setIcon(R.drawable.ic_baseline_warning_24)
        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            mBoardDetails.taskList[mTaskListPosition].cards.removeAt(mCardListPosition)
            mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)
            showProgressDialog()
            Firestore().addUpdateTaskList(this, mBoardDetails, true)
        }
        builder.setNegativeButton("No") { dialog, _ ->
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

    fun addUpdateTaskListSuccess(closeActivity: Boolean) {
        hideProgressDialog()
        TaskListActivity.updateNeeded = true
        if (closeActivity) {
            finish()
        }
    }

    private fun updateCardDetails() {
        val cardName = binding?.tvCardName?.text.toString()
        val createdBy = mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].createdBy
        val assignedTo = mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].assignedTo
        val labelColor = mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].labelColor
        val dueDate = mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition].dueDate
        val newCard = Card(cardName, createdBy, assignedTo, labelColor, dueDate)

        mBoardDetails.taskList[mTaskListPosition].cards[mCardListPosition] = newCard
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)
        showProgressDialog()
        Firestore().addUpdateTaskList(this, mBoardDetails)
    }

    fun setCreatedByName(name: String) {
        hideProgressDialog()
        binding?.tvCreatedBy?.text = name
    }
}