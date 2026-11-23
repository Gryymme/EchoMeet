package com.example.chatapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.view.animation.AnimationUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ChatActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageButton
    private lateinit var currentUsernameTxt: TextView
    private lateinit var onlineStatusTxt: TextView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBoxTxt: EditText
    private lateinit var sendMessageBtn: MaterialButton
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var profileImage: ShapeableImageView

    private lateinit var mDbRef: DatabaseReference

    var receiverRoom: String? = null
    var senderRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val name = intent.getStringExtra("name")
        val receiverUid = intent.getStringExtra("uid")
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        mDbRef = FirebaseDatabase.getInstance().getReference()

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        backBtn = findViewById(R.id.backBtn)
        currentUsernameTxt = findViewById(R.id.currentUsernameTxt)
        onlineStatusTxt = findViewById(R.id.onlineStatusTxt)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBoxTxt = findViewById(R.id.messageBoxTxt)
        sendMessageBtn = findViewById(R.id.sendMessageBtn)
        profileImage = findViewById(R.id.profileImage)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter

        if (receiverUid != null) {
            val bitmap = ImageUtils.loadProfileImage(receiverUid, this)
            if (bitmap != null) {
                profileImage.setImageBitmap(bitmap)
            } else {
                profileImage.setImageResource(R.drawable.default_image_icon)
            }
        }

        // Firebase message listener
        mDbRef.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        message?.let { messageList.add(it) }
                    }
                    messageAdapter.notifyDataSetChanged()

                    chatRecyclerView.scrollToPosition(messageList.size - 1)

                    val layoutAnimationController = AnimationUtils.loadLayoutAnimation(
                        this@ChatActivity,
                        R.anim.layout_animation_slide_up
                    )
                    chatRecyclerView.layoutAnimation = layoutAnimationController
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        currentUsernameTxt.text = name

        sendMessageBtn.setOnClickListener {
            val messageText = messageBoxTxt.text.toString()
            if (messageText.isNotEmpty()) {
                val timestamp = System.currentTimeMillis()
                val messageObject = Message(messageText, senderUid ?: "unknown_user", timestamp)

                mDbRef.child("chats").child(senderRoom!!).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        mDbRef.child("chats").child(receiverRoom!!).child("messages").push()
                            .setValue(messageObject)
                    }

                messageBoxTxt.setText("")
            }
        }

        // Keyboard adjustment
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val isKeyboardVisible = imeHeight > 0

            val messageContainer = findViewById<View>(R.id.placeHolderForMessageFunction)
            // val chatCardViewContainer = findViewById<View>(R.id.chatRecyclerView)

            messageContainer.translationY = if (isKeyboardVisible) -imeHeight * 0.85f else 0f
            // chatCardViewContainer.translationY = if (isKeyboardVisible) -imeHeight * 0.85f else 0f

            if (isKeyboardVisible && messageList.isNotEmpty()) {
                chatRecyclerView.postDelayed({
                    chatRecyclerView.scrollToPosition(messageList.size - 1)
                }, 100)
            }

            return@setOnApplyWindowInsetsListener insets
        }

        backBtn.setOnClickListener {
            val backBtnIntent = Intent(this@ChatActivity, MainActivity::class.java)
            finish()
            startActivity(backBtnIntent)
        }

        if (receiverUid != null) {
            val statusRef = mDbRef.child("user").child(receiverUid).child("status")
            val lastSeenRef = mDbRef.child("user").child(receiverUid).child("lastSeen")

            statusRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(String::class.java)

                    if (status == "online") {
                        onlineStatusTxt.text = "Online"
                    } else {
                        // Check last seen only if offline
                        lastSeenRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(lastSeenSnapshot: DataSnapshot) {
                                val lastSeenTimestamp = lastSeenSnapshot.getValue(Long::class.java)
                                if (lastSeenTimestamp != null) {
                                    val currentTime = System.currentTimeMillis()
                                    val elapsedTimeMillis = currentTime - lastSeenTimestamp
                                    val elapsedMinutes = elapsedTimeMillis / (60 * 1000)

                                    if (elapsedMinutes <= 60) {
                                        onlineStatusTxt.text = "Online $elapsedMinutes minute${if (elapsedMinutes != 1L) "s" else ""} ago"
                                    } else {
                                        onlineStatusTxt.text = "Offline"
                                    }
                                } else {
                                    onlineStatusTxt.text = "Offline"
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                onlineStatusTxt.text = "Offline"
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onlineStatusTxt.text = ""
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        uid?.let {
            mDbRef.child("user").child(it).child("status").setValue("online")
        }
    }

    override fun onPause() {
        super.onPause()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        uid?.let {
            val currentTime = System.currentTimeMillis()
            mDbRef.child("user").child(it).child("status").setValue("offline")
            mDbRef.child("user").child(it).child("lastSeen").setValue(currentTime)
        }
    }
}