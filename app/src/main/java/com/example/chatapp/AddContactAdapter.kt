package com.example.chatapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class AddContactAdapter(
    private val context: Context,
    private val userList: ArrayList<Users>
) : RecyclerView.Adapter<AddContactAdapter.AddContactViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(user: Users)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    inner class AddContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTxt: TextView = itemView.findViewById(R.id.usernameTxt)
        val emailTxt: TextView = itemView.findViewById(R.id.emailTxt) // ✅ ADD THIS
        val profileImage: ShapeableImageView = itemView.findViewById(R.id.profileImage)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(userList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddContactViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.add_contacts_user_layout, parent, false)
        return AddContactViewHolder(view)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: AddContactViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.usernameTxt.text = currentUser.name
        holder.emailTxt.text = currentUser.email // ✅ SET THE EMAIL TEXT

        val bitmap = currentUser.uid?.let { ImageUtils.loadProfileImage(it, context) }
        if (bitmap != null) {
            holder.profileImage.setImageBitmap(bitmap)
        } else {
            holder.profileImage.setImageResource(R.drawable.default_image_icon)
        }
    }
}