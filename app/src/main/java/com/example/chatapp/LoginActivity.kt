package com.example.chatapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var emailTxt: EditText
    private lateinit var passwordTxt: EditText
    private lateinit var loginBtn: Button
    private lateinit var signupBtn: Button

    // Firebase
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailTxt = findViewById(R.id.emailTxt)
        passwordTxt = findViewById(R.id.passwordTxt)
        loginBtn = findViewById(R.id.loginBtn)
        signupBtn = findViewById(R.id.signupBtn)

        mAuth = FirebaseAuth.getInstance()

        // When signupBtn is clicked, window is directed to the SignUpActivity class
        signupBtn.setOnClickListener {
            val signUpIntent = Intent(this, SignUpActivity::class.java)
            startActivity(signUpIntent)
        }

        loginBtn.setOnClickListener {
            val email = emailTxt.text.toString().trim()
            val password = passwordTxt.text.toString().trim()

            // Method that will pass the email and the password
            login (email, password)
        }

        loginBtn.setOnClickListener {
            val email = emailTxt.text.toString().trim()
            val password = passwordTxt.text.toString().trim()

            // Input validation before calling login()
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

            // Method that will pass the email and the password
            login(email, password)
        }
    }



    // Method login with parameters: email of type string, and password of type string
    private fun login (email: String, password: String) {
        // Method logs-in the existing user
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Successful login
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "The email or password you entered is incorrect", Toast.LENGTH_LONG).show()
                }
            }
    }
}