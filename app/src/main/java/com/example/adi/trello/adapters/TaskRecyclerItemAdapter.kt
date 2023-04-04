package com.example.adi.trello.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adi.trello.R
import com.example.adi.trello.activities.TaskListActivity
import com.example.adi.trello.databinding.RvItemTaskBinding
import com.example.adi.trello.firebase.Firestore
import com.example.adi.trello.models.Board
import com.example.adi.trello.models.Task
import java.util.*
import kotlin.collections.ArrayList

class TaskRecyclerItemAdapter(
    private val context: Context,
    private val taskList: ArrayList<Task>,
    private val board: Board
): RecyclerView.Adapter<TaskRecyclerItemAdapter.TaskRecyclerViewHolder>() {

    private var mPositionDraggedFrom = -1
    private var mPositionDraggedTo = -1

    class TaskRecyclerViewHolder(binding: RvItemTaskBinding): RecyclerView.ViewHolder(binding.root) {
        val tvAddTaskList = binding.tvAddTaskList
        val cvAddTaskListName = binding.cvAddTaskListName
        val ibCloseListName = binding.ibCloseListName
        val etTaskListName = binding.etTaskListName
        val ibDoneListName = binding.ibDoneListName
        val llTaskItem = binding.llTaskItem
        val llTitleView = binding.llTitleView
        val tvTaskListTitle = binding.tvTaskListTitle
        val ibEditList = binding.ibEditList
        val ibDeleteListName = binding.ibDeleteListName
        val cvEditTaskListName = binding.cvEditTaskListName
        val ibCloseEditableView = binding.ibCloseEditableView
        val etEditTaskListName = binding.etEditTaskListName
        val ibDoneEditListName = binding.ibDoneEditListName
        val rvCardList = binding.rvCardList
        val cvAddCard = binding.cvAddCard
        val ibCloseCardName = binding.ibCloseCardName
        val etCardName = binding.etCardName
        val ibDoneCardName = binding.ibDoneCardName
        val tvAddCard = binding.tvAddCard
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskRecyclerViewHolder {
        val binding = RvItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val layoutParams = LinearLayout.LayoutParams((parent.width * 0.7).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(15.toDp().toPx(), 15.toDp().toPx(), 40.toDp().toPx(), 15.toDp().toPx())
        binding.root.layoutParams = layoutParams
        return TaskRecyclerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskRecyclerViewHolder, position: Int) {
        val task = taskList[position]
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (position == taskList.size - 1) {
            holder.tvAddTaskList.visibility = View.VISIBLE
             holder.llTaskItem.visibility = View.GONE
        } else {
            holder.tvAddTaskList.visibility = View.GONE
            holder.llTaskItem.visibility = View.VISIBLE
            holder.llTitleView.visibility = View.VISIBLE
        }

        holder.tvTaskListTitle.text = task.title

        holder.tvAddTaskList.setOnClickListener {
            holder.tvAddTaskList.visibility = View.GONE
            holder.cvAddTaskListName.visibility = View.VISIBLE
        }
        holder.ibCloseListName.setOnClickListener {
            holder.tvAddTaskList.visibility = View.VISIBLE
            holder.cvAddTaskListName.visibility = View.GONE
            holder.etTaskListName.setText("")
            inputMethodManager.hideSoftInputFromWindow(holder.etTaskListName.windowToken, 0)
        }
        holder.ibDoneListName.setOnClickListener {
            val listName = holder.etTaskListName.text.toString()

            if (listName.isNotEmpty()) {
                if (context is TaskListActivity) {
                    context.createTaskList(listName)
                }
            } else {
                holder.tvAddTaskList.visibility = View.VISIBLE
                holder.cvAddTaskListName.visibility = View.GONE
                holder.etTaskListName.setText("")
                inputMethodManager.hideSoftInputFromWindow(holder.etTaskListName.windowToken, 0)
                if (context is TaskListActivity) {
                    context.showErrorSnackBar(context.resources.getString(R.string.cant_create_empty_task))
                }
            }
        }

        holder.ibEditList.setOnClickListener {
            holder.tvAddCard.visibility = View.VISIBLE
            holder.cvAddCard.visibility = View.GONE
            holder.etCardName.setText("")
            inputMethodManager.hideSoftInputFromWindow(holder.etCardName.windowToken, 0)
            holder.etEditTaskListName.setText(task.title)
            holder.llTitleView.visibility = View.GONE
            holder.cvEditTaskListName.visibility = View.VISIBLE
        }
        holder.ibCloseEditableView.setOnClickListener {
            holder.llTitleView.visibility = View.VISIBLE
            holder.cvEditTaskListName.visibility = View.GONE
            holder.etEditTaskListName.setText("")
            inputMethodManager.hideSoftInputFromWindow(holder.etEditTaskListName.windowToken, 0)
        }
        holder.ibDoneEditListName.setOnClickListener {
            val listName = holder.etEditTaskListName.text.toString()

            if (listName.isNotEmpty()) {
                if (context is TaskListActivity) {
                    context.updateTaskList(position, listName, task)
                }
            } else {
                holder.llTitleView.visibility = View.VISIBLE
                holder.cvEditTaskListName.visibility = View.GONE
                holder.etEditTaskListName.setText("")
                inputMethodManager.hideSoftInputFromWindow(holder.etEditTaskListName.windowToken, 0)
                if (context is TaskListActivity) {
                    context.showErrorSnackBar(context.resources.getString(R.string.cant_create_empty_task))
                }
            }
        }
        holder.ibDeleteListName.setOnClickListener {
            showAlertDialogForDeletingTaskList(position, task.title)
        }

        holder.tvAddCard.setOnClickListener {
            holder.llTitleView.visibility = View.VISIBLE
            holder.cvEditTaskListName.visibility = View.GONE
            holder.etEditTaskListName.setText("")
            inputMethodManager.hideSoftInputFromWindow(holder.etEditTaskListName.windowToken, 0)
            holder.tvAddCard.visibility = View.GONE
            holder.cvAddCard.visibility = View.VISIBLE
        }

        holder.ibCloseCardName.setOnClickListener {
            holder.tvAddCard.visibility = View.VISIBLE
            holder.cvAddCard.visibility = View.GONE
            holder.etCardName.setText("")
            inputMethodManager.hideSoftInputFromWindow(holder.etCardName.windowToken, 0)
        }

        holder.ibDoneCardName.setOnClickListener {
            val cardName = holder.etCardName.text.toString()
            holder.tvAddCard.visibility = View.VISIBLE
            holder.cvAddCard.visibility = View.GONE
            holder.etCardName.setText("")
            inputMethodManager.hideSoftInputFromWindow(holder.etCardName.windowToken, 0)
            if (cardName.isNotEmpty()) {
                if (context is TaskListActivity) {
                    context.addCardToTaskList(position, cardName)
                }
            } else {
                holder.tvAddCard.visibility = View.VISIBLE
                holder.cvAddCard.visibility = View.GONE
                holder.etCardName.setText("")
                inputMethodManager.hideSoftInputFromWindow(holder.etCardName.windowToken, 0)
                if (context is TaskListActivity) {
                    context.showErrorSnackBar(context.resources.getString(R.string.cant_create_empty_card))
                }
            }
        }

        holder.rvCardList.layoutManager = LinearLayoutManager(context)
        holder.rvCardList.setHasFixedSize(true)
        holder.rvCardList.adapter = CardRecyclerItemAdapter(context, task.cards) { cardPosition, _ ->
            if (context is TaskListActivity) {
                context.cardDetails(position, cardPosition)
            }
        }
        setUpItemTouchHelper(position, holder)
    }

    private fun setUpItemTouchHelper(taskListPosition: Int, holder: TaskRecyclerViewHolder) {
        ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            @SuppressLint("ClickableViewAccessibility")
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val draggedPosition = viewHolder.adapterPosition
                val targetPosition = target.adapterPosition

                if (mPositionDraggedFrom == -1) {
                    mPositionDraggedFrom = draggedPosition
                }
                mPositionDraggedTo = targetPosition
                holder.rvCardList.adapter?.notifyItemMoved(draggedPosition, targetPosition)
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                val size = board.taskList[taskListPosition].cards.size
                if (mPositionDraggedFrom in 0 until size && mPositionDraggedTo in 0 until size) {
                    Collections.swap(board.taskList[taskListPosition].cards, mPositionDraggedFrom, mPositionDraggedTo)
                    val activity = context as TaskListActivity
                    activity.showProgressDialog()
                    board.taskList.removeAt(board.taskList.size - 1)
                    Firestore().addUpdateTaskList(activity, board)
                }
                mPositionDraggedFrom = -1
                mPositionDraggedTo = -1
                super.clearView(recyclerView, viewHolder)
            }

        }).attachToRecyclerView(holder.rvCardList)
    }

    private fun showAlertDialogForDeletingTaskList(position: Int, title: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete $title")
        builder.setIcon(R.drawable.ic_baseline_warning_24)
        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            if (context is TaskListActivity) {
                context.deleteTaskList(position)
            }
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    private fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()

    private fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
}