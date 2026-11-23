package com.example.chatapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter(val context: Context, val messageList: ArrayList<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val itemReceived = 1
    val itemSent = 2


    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessageTxt = itemView.findViewById<TextView>(R.id.sentMessageTxt)
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receivedMessageTxt = itemView.findViewById<TextView>(R.id.receivedMessageTxt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            // Inflate received holder
            val view: View = LayoutInflater.from(context).inflate(R.layout.received_layout, parent, false)
            return ReceivedViewHolder(view)
        } else {
            // Inflate sent
            val view: View = LayoutInflater.from(context).inflate(R.layout.sent_layout, parent, false)
            return SentViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val currentMessage = messageList[position]

        if (holder.javaClass == SentViewHolder::class.java) {
            // SentViewHolder
            val viewHolder = holder as SentViewHolder
            holder.sentMessageTxt.text = currentMessage.message
        } else {
            // ReceivedViewHolder
            val viewHolder = holder as ReceivedViewHolder
            holder.receivedMessageTxt.text = currentMessage.message
        }
    }

    override fun getItemViewType(position: Int): Int {

        val currentMessage = messageList[position]

        if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId)) {
            return itemSent
        } else {
            return itemReceived
        }

    }

}