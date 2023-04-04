package com.example.adi.trello.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.adi.trello.R
import com.example.adi.trello.firebase.Firestore

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val intent = Intent(this, IntroActivity::class.java)
        Handler(Looper.getMainLooper()).postDelayed({
            val userId = Firestore().getCurrentUserId()
            if (userId.isNotEmpty()) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(intent)
            }
            finish()
        }, 2000)
    }
}