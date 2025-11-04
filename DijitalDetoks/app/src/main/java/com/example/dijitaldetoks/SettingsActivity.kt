package com.example.dijitaldetoks

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.dijitaldetoks.databinding.ActivitySettingsBinding
import android.widget.Toast
import android.content.Intent

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Süre Ayarları"

        setupDurationSlider()
        setupResetButton()
    }

    private fun setupResetButton() {
        binding.buttonResetTime.setOnClickListener {
            TimeManager.resetUsedTime(applicationContext)
            Toast.makeText(this, "Kullanılan süre sıfırlandı!", Toast.LENGTH_SHORT).show()
            val intent = Intent(Constants.ACTION_USAGE_UPDATE)
            sendBroadcast(intent)
        }
    }

    private fun setupDurationSlider() {
        val currentLimitMinutes = (TimeManager.getLimitTime(this) / (60 * 1000L)).toInt()
        binding.seekBarDuration.max = 180
        binding.seekBarDuration.progress = currentLimitMinutes
        binding.textViewCurrentDuration.text = "Şu Anki Limit: ${currentLimitMinutes} dakika"

        binding.seekBarDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val newProgress = if (progress < 5) 5 else progress
                binding.textViewCurrentDuration.text = "Seçilen Limit: ${newProgress} dakika"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val finalDurationMinutes = if (seekBar?.progress ?: 0 < 5) 5 else seekBar?.progress ?: 0
                TimeManager.setLimitTime(applicationContext, finalDurationMinutes)
                Toast.makeText(this@SettingsActivity, "Limit ${finalDurationMinutes} dakikaya ayarlandı.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
