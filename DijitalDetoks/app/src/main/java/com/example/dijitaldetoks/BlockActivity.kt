package com.example.dijitaldetoks

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dijitaldetoks.databinding.ActivityBlockBinding
import androidx.activity.addCallback
import androidx.activity.OnBackPressedCallback

class BlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, true) {
            // Geri tuşu devre dışı
        }

        val usedTime = TimeManager.getUsedTime(this) / (60 * 1000)
        binding.textViewBlockMessage.text =
            "Üzgünüz, 30 dakikalık limitinizi (${usedTime} dakika) aştınız. Lütfen telefondan uzaklaşın!"
    }
}
