package com.example.adi.trello.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adi.trello.adapters.IntroViewPagerAdapter
import com.example.adi.trello.databinding.ActivityIntroBinding
import com.google.android.material.tabs.TabLayoutMediator

class IntroActivity: AppCompatActivity() {

    private var binding: ActivityIntroBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.introSliderViewPager?.adapter = IntroViewPagerAdapter(applicationContext)
        TabLayoutMediator(binding?.introSliderTabLayout!!, binding?.introSliderViewPager!!) { _, _ -> }.attach()

        binding?.logInBtn?.setOnClickListener {
            startActivity(Intent(this, LogInActivity::class.java))
        }

        binding?.signUpBtn?.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}