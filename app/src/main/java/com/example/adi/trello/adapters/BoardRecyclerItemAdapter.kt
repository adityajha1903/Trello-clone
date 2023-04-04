package com.example.adi.trello.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.adi.trello.R
import com.example.adi.trello.databinding.RvItemBoardBinding
import com.example.adi.trello.models.Board

open class BoardRecyclerItemAdapter(
    private val context: Context,
    private val boardList: ArrayList<Board>,
    private val itemTouchListener: (position: Int, board: Board) -> Unit
): RecyclerView.Adapter<BoardRecyclerItemAdapter.BoardViewHolder>() {

    class BoardViewHolder(binding: RvItemBoardBinding): RecyclerView.ViewHolder(binding.root) {
        val boardImage = binding.boardIv
        val boardName = binding.boardNameTv
        val createdBy = binding.createdByNameTv
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        return BoardViewHolder(RvItemBoardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        val board = boardList[position]
        Glide.with(context)
            .load(board.image)
            .centerCrop()
            .placeholder(R.drawable.transparent)
            .into(holder.boardImage)
        holder.boardName.text = board.name
        holder.createdBy.text = board.createdBy
        holder.itemView.setOnClickListener {
            itemTouchListener.invoke(position, board)
        }
    }

    override fun getItemCount(): Int {
        return boardList.size
    }

}