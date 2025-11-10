package com.example.smartcampus

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ChatMessage(val message: String, val isUser: Boolean, val locationName: String? = null)

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_USER) R.layout.item_chat_bubble_user else R.layout.item_chat_bubble_bot
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.message

        if (!message.isUser && message.locationName != null) {
            holder.mapButton?.visibility = View.VISIBLE
            holder.mapButton?.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, MapActivity::class.java).apply {
                    putExtra("SEARCH_QUERY", message.locationName)
                }
                context.startActivity(intent)
            }
        } else {
            holder.mapButton?.visibility = View.GONE
        }
    }

    override fun getItemCount() = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.chatMessageTextView)
        val mapButton: Button? = itemView.findViewById(R.id.viewOnMapButton)
    }
}