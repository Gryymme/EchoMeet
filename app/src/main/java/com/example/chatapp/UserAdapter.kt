package com.example.chatapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class UserAdapter(
    private val context: Context,
    private val userList: ArrayList<Users>
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    // Long click listener
    private var longClickListener: ((position: Int) -> Unit)? = null

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // ✅ ADDED
    private val mDbRef: DatabaseReference = FirebaseDatabase.getInstance().reference // ✅ ADDED

    // ViewHolder class
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTxt: TextView = itemView.findViewById(R.id.usernameTxt)
        val profileImage: ShapeableImageView = itemView.findViewById(R.id.profileImage)
        val messagePreviewTxt: TextView = itemView.findViewById(R.id.messagePreviewTxt) // ✅ ADDED
        val messageTimestampTxt: TextView = itemView.findViewById(R.id.messageTimestampTxt) // ✅ ADDED

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val selectedUser = userList[position]
                    val intent = Intent(context, ChatActivity::class.java).apply {
                        putExtra("name", selectedUser.name)
                        putExtra("uid", selectedUser.uid)
                    }
                    context.startActivity(intent)
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    longClickListener?.invoke(position)
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.user_layout, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.usernameTxt.text = currentUser.name

        val bitmap = currentUser.uid?.let { ImageUtils.loadProfileImage(it, context) }
        if (bitmap != null) {
            holder.profileImage.setImageBitmap(bitmap)
        } else {
            holder.profileImage.setImageResource(R.drawable.default_image_icon)
        }

        // ✅ ADDED: Load latest message + timestamp
        val chatRoomId = if (currentUserId != null) {
            if (currentUserId < currentUser.uid!!) currentUserId + currentUser.uid
            else currentUser.uid + currentUserId
        } else return

        mDbRef.child("chats").child(chatRoomId).child("messages")
            .orderByChild("timestamp")
            .limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (messageSnap in snapshot.children) {
                        val lastMessage = messageSnap.getValue(Message::class.java)
                        if (lastMessage != null) {
                            holder.messagePreviewTxt.text = lastMessage.message // ✅ Set message preview
                            val timeText = formatTimestamp(lastMessage.timestamp ?: 0L)
                            holder.messageTimestampTxt.text = timeText // ✅ Set formatted timestamp
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Optionally handle error
                }
            })
    }

    // ✅ FIXED: Removed "Yesterday" label; always show time if <24h, else show full date
    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return if (diff < 24 * 60 * 60 * 1000) {
            // Less than 24 hours → Show time (e.g., 5:49 PM)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        } else {
            // 24+ hours → Show actual date (e.g., Mar 21)
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }

    // Public setter method for long click
    fun setOnItemLongClickListener(listener: (position: Int) -> Unit) {
        this.longClickListener = listener
    }
}
