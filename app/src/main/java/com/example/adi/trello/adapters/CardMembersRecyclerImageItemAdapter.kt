package com.example.adi.trello.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.adi.trello.R
import com.example.adi.trello.databinding.RvItemCardMemberImageBinding
import com.example.adi.trello.models.User

class CardMembersRecyclerImageItemAdapter(
    private val context: Context,
    private val membersList: ArrayList<User>
): RecyclerView.Adapter<CardMembersRecyclerImageItemAdapter.CardMembersImageItemViewHolder>() {

    class CardMembersImageItemViewHolder(binding: RvItemCardMemberImageBinding): RecyclerView.ViewHolder(binding.root) {
        val image = binding.memberIv
        val nameFirstLetter = binding.firstLetterTV
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CardMembersImageItemViewHolder {
        return CardMembersImageItemViewHolder(RvItemCardMemberImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CardMembersImageItemViewHolder, position: Int) {
        val member = membersList[position]
        holder.nameFirstLetter.text = member.name[0].uppercase()
        Glide.with(context)
            .load(member.image)
            .centerCrop()
            .placeholder(R.drawable.transparent)
            .into(holder.image)
    }

    override fun getItemCount(): Int {
        return membersList.size
    }
}