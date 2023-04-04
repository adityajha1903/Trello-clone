package com.example.adi.trello.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.example.adi.trello.databinding.ActivitySignUpBinding
import com.google.firebase.auth.*
import com.example.adi.trello.R
import com.example.adi.trello.firebase.Firestore
import com.example.adi.trello.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class SignUpActivity : BaseActivity() {

    private var binding: ActivitySignUpBinding? = null

    private lateinit var auth: FirebaseAuth
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                val name = account.displayName
                val email = account.email
                firebaseAuthWithGoogle(account.idToken!!, name!!, email!!)
            } catch (e: ApiException) {
                Log.w("Google authentication", "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, name: String, email: String) {
        val credentials = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credentials)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uId = task.result.user?.uid!!
                    val user = User(id = uId, name = name, email = email)
                    Firestore().registerUser(this, user)
                } else {
                    showErrorSnackBar(task.exception?.message.toString())
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
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

        binding?.changeToLogInBtn?.setOnClickListener {
            startActivity(Intent(this, LogInActivity::class.java))
            finish()
        }

        binding?.signUpBtn?.setOnClickListener {
            registerUser()
        }


        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding?.googleAuthBtn?.setOnClickListener {
            showProgressDialog(getString(R.string.please_wait))
            signIn(googleSignInClient)
        }

    }

    private fun signIn(googleSignInClient: GoogleSignInClient) {
        val intent = googleSignInClient.signInIntent
        resultLauncher.launch(intent)
    }

    private fun registerUser() {
        val name = binding?.nameET?.text.toString().trim{ it <= ' '}
        val email = binding?.emailET?.text.toString().trim{ it <= ' '}
        val password = binding?.passwordET?.text.toString()

        if (validateForm(name, email, password)) {
            showProgressDialog(resources.getString(R.string.please_wait))

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = task.result.user!!
                        val registeredEmail = firebaseUser.email!!
                        val user = User(firebaseUser.uid, name, registeredEmail)
                        Firestore().registerUser(this, user)
                    } else {
                        hideProgressDialog()
                        try {
                            throw task.exception!!
                        } catch (e: FirebaseAuthWeakPasswordException) {
                            showErrorSnackBar("Please create a strong password")
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            showErrorSnackBar("Email address is badly formatted")
                        } catch (e: FirebaseAuthUserCollisionException) {
                            showErrorSnackBar("This user already exist")
                        } catch (e: Exception) {
                            showErrorSnackBar(e.message.toString())
                        }
                    }
            }
        } else {
            showErrorSnackBar(resources.getString(R.string.please_fill_all_the_required_fields))
        }
    }

    fun userRegisteredSuccess() {
        hideProgressDialog()
        val i = Intent(this, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        MainActivity.newBoardAdded = true
        MainActivity.userDataChanged = true
        startActivity(i)
        finish()
    }

    private fun validateForm(name: String, email: String, password: String): Boolean {
        return name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}