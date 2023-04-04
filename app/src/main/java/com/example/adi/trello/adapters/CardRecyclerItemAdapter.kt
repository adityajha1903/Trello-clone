package com.example.adi.trello.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adi.trello.activities.TaskListActivity
import com.example.adi.trello.databinding.RvItemCardBinding
import com.example.adi.trello.firebase.Firestore
import com.example.adi.trello.models.Card

class CardRecyclerItemAdapter(
    private val context: Context,
    private val cardList: ArrayList<Card>,
    private val cardOnClick: (position: Int, card: Card) -> Unit
): RecyclerView.Adapter<CardRecyclerItemAdapter.CardRecyclerViewHolder>() {

    class CardRecyclerViewHolder(binding: RvItemCardBinding): RecyclerView.ViewHolder(binding.root) {
        val label = binding.viewLabelColor
        val cardName = binding.tvCardName
        val rvMemberImage = binding.rvCardMemberImage
        val view = binding.cardTouchView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardRecyclerViewHolder {
        return CardRecyclerViewHolder(RvItemCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CardRecyclerViewHolder, position: Int) {
        val card = cardList[position]
        holder.cardName.text = card.name

        if (card.labelColor.isNotEmpty()) {
            holder.label.visibility = View.VISIBLE
            holder.label.setBackgroundColor(Color.parseColor(card.labelColor))
        } else {
            holder.label.visibility = View.GONE
        }

        holder.view.setOnClickListener {
            cardOnClick.invoke(position, card)
        }

        holder.rvMemberImage.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        holder.rvMemberImage.setHasFixedSize(true)

        val newContext = context as TaskListActivity
        Firestore().getCardAssignedMembersListDetails(newContext, card.assignedTo, holder)
    }

    override fun getItemCount(): Int {
        return cardList.size
    }
}