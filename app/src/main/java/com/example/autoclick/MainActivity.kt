package com.example.autoclick

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = "1. Neeche button dabao\n2. Accessibility mein 'AutoClick Lite' ON karo\n" +
                "3. Crosshair ko mob/block par rakho (tap screen ke beech hota hai)\n" +
                "4. Button: tap = ON/OFF, long press = TAP/HOLD, drag = move"
            textSize = 16f
        })
        root.addView(Button(this).apply {
            text = "Accessibility Settings kholo"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        setContentView(root)
    }
}
