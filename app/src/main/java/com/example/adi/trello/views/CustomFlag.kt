package com.example.adi.trello.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView
import com.example.adi.trello.R
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.flag.FlagView

@SuppressLint("ViewConstructor")
class CustomFlag(
    context: Context?,
    layout: Int
) : FlagView(context, layout) {

    private val textView = findViewById<TextView>(R.id.flag_color_code)
    private val alphaTileView = findViewById<LinearLayout>(R.id.flag_color_layout)

    @SuppressLint("SetTextI18n")
    override fun onRefresh(colorEnvelope: ColorEnvelope?) {
        val hexCode = "#" + colorEnvelope?.hexCode
        textView.text = hexCode
        alphaTileView.setBackgroundColor(Color.parseColor(hexCode))
    }
}