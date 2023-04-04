package com.example.adi.trello.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.adi.trello.R
import com.example.adi.trello.databinding.RvItemMemberBinding
import com.example.adi.trello.models.User

class MemberRecyclerItemAdapter(
    private val context: Context,
    private val membersList: ArrayList<User>
): RecyclerView.Adapter<MemberRecyclerItemAdapter.MemberViewHolder>() {

    class MemberViewHolder(binding: RvItemMemberBinding): RecyclerView.ViewHolder(binding.root) {
        val image = binding.memberIv
        val name = binding.memberNameTv
        val email = binding.emailTv
        val nameFirstLetter = binding.firstLetterTV
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        return MemberViewHolder(RvItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = membersList[position]

        holder.name.text = member.name
        holder.email.text = member.email
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