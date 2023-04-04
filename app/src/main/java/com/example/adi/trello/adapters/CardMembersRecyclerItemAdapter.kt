package com.example.adi.trello.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.adi.trello.R
import com.example.adi.trello.databinding.RvItemCardMemberBinding
import com.example.adi.trello.models.User

class CardMembersRecyclerItemAdapter(
    private val context: Context,
    private val membersList: ArrayList<User>
): RecyclerView.Adapter<CardMembersRecyclerItemAdapter.CardMemberItemViewHolder>() {

    class CardMemberItemViewHolder(binding: RvItemCardMemberBinding): RecyclerView.ViewHolder(binding.root) {
        val memberName = binding.tvMemberName
        val nameFirstLetter = binding.firstLetterTV
        val image = binding.memberIv
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardMemberItemViewHolder {
        return CardMemberItemViewHolder(RvItemCardMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CardMemberItemViewHolder, position: Int) {
        val member = membersList[position]

        holder.memberName.text = member.name
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