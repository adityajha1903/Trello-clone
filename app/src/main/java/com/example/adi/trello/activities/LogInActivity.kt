package com.example.adi.trello.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import com.example.adi.trello.R
import com.example.adi.trello.databinding.ActivityLogInBinding
import com.example.adi.trello.firebase.Firestore
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LogInActivity : BaseActivity() {

    private var binding: ActivityLogInBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolBar)
        supportActionBar?.title = ""
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            })
        }

        binding?.backBtn?.setOnClickListener { finish() }

        binding?.changeToSignUpBtn?.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        binding?.logInBtn?.setOnClickListener {
            logInUser()
        }

    }

    private fun logInUser() {
        val email = binding?.emailET?.text.toString().trim { it <= ' ' }
        val password = binding?.passwordET?.text.toString().trim { it <= ' ' }

        if (validateForm(email, password)) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Firestore().loadUserData(this)
                    } else {
                        hideProgressDialog()
                        try {
                            throw task.exception!!
                        } catch (e: FirebaseAuthInvalidUserException) {
                            showErrorSnackBar(resources.getString(R.string.no_such_user_exist))
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            showErrorSnackBar(resources.getString(R.string.wrong_email_or_password_added))
                        } catch (e: FirebaseNetworkException) {
                            showErrorSnackBar(resources.getString(R.string.network_unavailable))
                        } catch (e: Exception) {
                            showErrorSnackBar(e.message.toString())
                        }
                    }
                }
        } else {
            showErrorSnackBar(resources.getString(R.string.please_fill_all_the_required_fields))
        }
    }

    private fun validateForm(email: String, password: String): Boolean {
        return email.isNotEmpty() && password.isNotEmpty()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    fun logInSuccess() {
        hideProgressDialog()
        val i = Intent(this, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        MainActivity.newBoardAdded = true
        MainActivity.userDataChanged = true
        startActivity(i)
        finish()
    }
}