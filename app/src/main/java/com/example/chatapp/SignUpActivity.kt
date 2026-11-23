package com.example.chatapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var nameTxt: EditText
    private lateinit var emailTxt: EditText
    private lateinit var passwordTxt: EditText
    private lateinit var signupBtn: Button
    private lateinit var goBackToLoginBtn: Button
    private lateinit var uploadImageBtn: Button
    private lateinit var profileImage: ImageView

    // Firebase
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference

    // Image functionality
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        nameTxt = findViewById(R.id.nameTxt)
        emailTxt = findViewById(R.id.emailTxt)
        passwordTxt = findViewById(R.id.passwordTxt)
        signupBtn = findViewById(R.id.signupBtn)
        goBackToLoginBtn = findViewById(R.id.goBackToLoginBtn)
        uploadImageBtn = findViewById(R.id.uploadImageBtn)
        profileImage = findViewById(R.id.profileImage)

        mAuth = FirebaseAuth.getInstance()

        // Image Functionality
        val imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let {
                    profileImage.setImageURI(it)
                    selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                }
            }
        }

        uploadImageBtn.setOnClickListener { // Image Functionality - Upload button click handler
            val intent = Intent(Intent.ACTION_GET_CONTENT) // Image Functionality - Create intent
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/jpg"))
            imagePickerLauncher.launch(intent) // Image Functionality - Launch window image picker
        }

        // When goBackToLoginBtn is clicked, window is directed to the Login page
        goBackToLoginBtn.setOnClickListener {
            val goBackIntent = Intent(this, LoginActivity::class.java)
            startActivity(goBackIntent)
        }

        signupBtn.setOnClickListener {
            val name = nameTxt.text.toString().trim()
            val email = emailTxt.text.toString().trim()
            val password = passwordTxt.text.toString().trim()

            // Input validation before calling signUp()
            if (name.isEmpty()) {
                nameTxt.error = "Name is required"
                nameTxt.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                emailTxt.error = "Email is required"
                emailTxt.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordTxt.error = "Password is required"
                passwordTxt.requestFocus()
                return@setOnClickListener
            }

            // Calls signUp method and passes name, email and password
            signUp(name, email, password)
        }
    }

    // Method login with parameters: email of type string, and password of type string
    private fun signUp(name: String, email: String, password: String) {
        // Method creates a new user
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = mAuth.currentUser?.uid!!
                    // Adds user to database ***add profile image when implemented to the program***
                    addUserToDatabase(name, email, uid) // "!!" - null safe

                    // Image Functionality — Save image locally with UID
                    selectedBitmap?.let { // Image Functionality - Save image if selected
                        ImageUtils.saveProfileImage(uid, it, this) // Image Functionality - Centralized helper call
                    }


                    // Toast to inform the user that the account is created successfully
                    Toast.makeText(this@SignUpActivity, "Account created successfully", Toast.LENGTH_SHORT).show()
                    // Redirects to login page
                    val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@SignUpActivity, "An error occurred", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Method to add user to database
    private fun addUserToDatabase(name: String, email: String, uid: String) {
        mDbRef = FirebaseDatabase.getInstance().getReference()

        // Adds nodes from uid
        mDbRef.child("user").child(uid).setValue(Users(name, email, uid))
    }

}