package com.example.chatapp

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var userRecyclerView: RecyclerView
    private lateinit var contactList: ArrayList<Users>
    private lateinit var adapter: UserAdapter
    private lateinit var moreOptionsBtn: ImageButton
    private lateinit var newContactBtn: MaterialButton
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private var profileImageView: ImageView? = null
    private var currentUserUid: String = ""
    private lateinit var contactSearchTxt: EditText
    private lateinit var fullContactList: ArrayList<Users>

    // Handles the change profile image on main screen
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val selectedImageUri: Uri? = result.data?.data
            selectedImageUri?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                profileImageView?.setImageBitmap(bitmap)
                mAuth.currentUser?.uid?.let {
                    ImageUtils.saveProfileImage(it, bitmap, this)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().reference
        currentUserUid = mAuth.currentUser?.uid ?: ""
        userRecyclerView = findViewById(R.id.userRecyclerView)
        moreOptionsBtn = findViewById(R.id.moreOptionsBtn)
        newContactBtn = findViewById(R.id.newContactBtn)
        contactSearchTxt = findViewById(R.id.contactSearchTxt)

        contactList = ContactStorage.loadContactsForUser(this, currentUserUid)
        fullContactList = ArrayList(contactList)

        adapter = UserAdapter(this, contactList)
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = adapter

        // Search for added contacts with live search
        contactSearchTxt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                val filteredList = if (query.isEmpty()) {
                    fullContactList
                } else {
                    fullContactList.filter {
                        it.name?.contains(query, ignoreCase = true) == true
                    }
                }
                contactList.clear()
                contactList.addAll(filteredList)
                adapter.notifyDataSetChanged()
            }
        })

        // Handles deletion of saved contacts by long click
        adapter.setOnItemLongClickListener { position ->
            val userToRemove = contactList[position]
            AlertDialog.Builder(this)
                .setTitle("Remove Contact")
                .setMessage("You are about to remove ${userToRemove.name} from your contact list. Are you sure you want to continue?")
                .setPositiveButton("REMOVE") { _, _ ->
                    // Search for added contacts
                    fullContactList.remove(userToRemove)
                    contactList.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    ContactStorage.saveContactsForUser(this, currentUserUid, fullContactList)
                }
                .setNegativeButton("CANCEL", null)
                .show()
        }

        moreOptionsBtn.setOnClickListener { showMoreOptionsPopup() }
        newContactBtn.setOnClickListener { showAddContactsPopup() }

        fetchOnlineStatusAndUpdateContacts()
    }

    // Monitors the online status of the user
    private fun fetchOnlineStatusAndUpdateContacts() {
        mDbRef.child("user").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedList = ArrayList<Users>()
                val currentTime = System.currentTimeMillis()

                for (user in contactList) {
                    val userSnapshot = snapshot.child(user.uid ?: "")
                    val lastSeen = userSnapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                    user.userOnlineStatus = currentTime - lastSeen < 60_000
                    updatedList.add(user)
                }

                updatedList.sortWith(compareByDescending<Users> { it.userOnlineStatus }.thenBy { it.name?.lowercase() })
                contactList.clear()
                contactList.addAll(updatedList)

                fullContactList.clear()
                fullContactList.addAll(updatedList)

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Pops-up the more options window when the more options button is clicked
    private fun showMoreOptionsPopup() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.more_options_activity, null)
        val profileImage = dialogView.findViewById<ImageView>(R.id.profileImage)
        val loggedInUsernameTxt = dialogView.findViewById<TextView>(R.id.loggedInUsernameTxt)
        val editImageBtn = dialogView.findViewById<Button>(R.id.editImageBtn)
        val logoutBtn = dialogView.findViewById<Button>(R.id.logoutBtn)
        val closeBtn = dialogView.findViewById<ImageButton>(R.id.closeBtn)

        val changeNameBtn = dialogView.findViewById<Button>(R.id.changeNameBtn)

        // Delete Account Function
        val deleteAccountBtn = dialogView.findViewById<Button>(R.id.deleteAccountBtn)
        deleteAccountBtn.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_account, null)
            val passwordInput = dialogView.findViewById<EditText>(R.id.passwordConfirmationTxt)
            val confirmDeleteBtn = dialogView.findViewById<Button>(R.id.confirmAccountDeletionBtn)
            val cancelBtn = dialogView.findViewById<Button>(R.id.cancelAccountDeletionBtn)

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            cancelBtn.setOnClickListener {
                dialog.dismiss()
            }

            confirmDeleteBtn.setOnClickListener {
                val password = passwordInput.text.toString().trim()
                val user = mAuth.currentUser

                if (user != null && password.isNotEmpty()) {
                    // Require the user to authenticate themselves by entering their password before proceeding
                    val credential = EmailAuthProvider.getCredential(user.email ?: "", password)

                    user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            mDbRef.child("user").child(user.uid).removeValue()
                            user.delete().addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to delete account: ${deleteTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "The operation cannot be completed as the password entered is incorrect", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Your password is required in order to proceed with the operation", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }

        // Changes the name of the user
        changeNameBtn.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_name, null)
            val nameEditTxt = dialogView.findViewById<EditText>(R.id.nameEditTxt)
            val confirmNameChangeBtn = dialogView.findViewById<Button>(R.id.confirmNameChangeBtn)

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            confirmNameChangeBtn.setOnClickListener {
                val newName = nameEditTxt.text.toString().trim()
                if (newName.isNotEmpty()) {
                    mAuth.currentUser?.uid?.let { uid ->
                        mDbRef.child("user").child(uid).child("name").setValue(newName)
                            .addOnSuccessListener {
                                loggedInUsernameTxt.text = newName
                                dialog.dismiss()
                            }
                    }
                } else {
                    Toast.makeText(this, "Name can't be empty", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.show()
        }

        profileImageView = profileImage

        // Start of code for change profile image functionality
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        alertDialog.window?.setGravity(Gravity.CENTER)

        mAuth.currentUser?.uid?.let { uid ->
            val bitmap = ImageUtils.loadProfileImage(uid, this)
            bitmap?.let { profileImage.setImageBitmap(it) }

            mDbRef.child("user").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(Users::class.java)
                    loggedInUsernameTxt.text = user?.name ?: "User"
                }

                override fun onCancelled(error: DatabaseError) {
                    loggedInUsernameTxt.text = "Unknown"
                }
            })
        }

        editImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            imagePickerLauncher.launch(intent)
        }
        // End of code for change profile image functionality

        // Logs the user out when log-out button is pressed
        logoutBtn.setOnClickListener {
            val uid = mAuth.currentUser?.uid
            if (uid != null) {
                val updates = HashMap<String, Any>()
                updates["lastSeen"] = System.currentTimeMillis()
                mDbRef.child("user").child(uid).updateChildren(updates).addOnCompleteListener {
                    mAuth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            } else {
                mAuth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        closeBtn.setOnClickListener { alertDialog.dismiss() }
        alertDialog.show()
    }

    // Pop-up the full user list window when the add contacts button is clicked
    private fun showAddContactsPopup() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.add_contacts_layout, null)
        val closeBtn = dialogView.findViewById<ImageButton>(R.id.closeBtn)
        val addContactsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.addContactsUserRecyclerView)
        val searchTxt = dialogView.findViewById<TextView>(R.id.searchTxt)

        val allUsersList = ArrayList<Users>()
        val addContactsAdapter = AddContactAdapter(this, allUsersList)

        addContactsRecyclerView.layoutManager = LinearLayoutManager(this)
        addContactsRecyclerView.adapter = addContactsAdapter

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        alertDialog.window?.setGravity(Gravity.CENTER)

        val fullUserList = ArrayList<Users>()

        mDbRef.child("user").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsersList.clear()
                fullUserList.clear()
                for (postSnapshot in snapshot.children) {
                    val user = postSnapshot.getValue(Users::class.java)
                    if (user != null && user.uid != mAuth.currentUser?.uid) {
                        allUsersList.add(user)
                        fullUserList.add(user)
                    }
                }
                addContactsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        searchTxt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                val filteredList = if (query.isEmpty()) {
                    fullUserList
                } else {
                    fullUserList.filter {
                        it.name?.contains(query, ignoreCase = true) == true
                    }
                }
                allUsersList.clear()
                allUsersList.addAll(filteredList)
                addContactsAdapter.notifyDataSetChanged()
            }
        })

        addContactsAdapter.setOnItemClickListener(object : AddContactAdapter.OnItemClickListener {
            override fun onItemClick(user: Users) {
                if (!contactList.any { it.uid == user.uid }) {
                    contactList.add(user)
                    adapter.notifyItemInserted(contactList.size - 1)
                    fullContactList.add(user)
                    ContactStorage.saveContactsForUser(this@MainActivity, currentUserUid, fullContactList)
                }
                alertDialog.dismiss()
            }
        })

        closeBtn.setOnClickListener { alertDialog.dismiss() }
        alertDialog.show()
    }
}