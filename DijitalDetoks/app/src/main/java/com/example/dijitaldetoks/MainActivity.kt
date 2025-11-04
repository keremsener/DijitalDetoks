package com.example.dijitaldetoks

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.AppOpsManager
import android.os.Build
import com.example.dijitaldetoks.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.os.CountDownTimer

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var countdownTimer: CountDownTimer? = null
    private var timeRemainingMillis: Long = 0

    private val usageUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateUiTime()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkUsageAccessPermission()
        updateUiTime()
    }

    override fun onResume() {
        super.onResume()
        checkUsageAccessPermission()
        updateUiTime()

        val filter = IntentFilter(Constants.ACTION_USAGE_UPDATE)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RECEIVER_NOT_EXPORTED
        } else {
            0
        }

        ContextCompat.registerReceiver(this, usageUpdateReceiver, filter, flags)
    }

    override fun onPause() {
        unregisterReceiver(usageUpdateReceiver)
        super.onPause()
    }

    private fun checkOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this)
        }
        return true
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Lütfen Dijital Detoks için 'Diğer Uygulamalar Üzerinde Çizme' iznini verin.", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkUsageAccessPermission() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )

        if (mode != AppOpsManager.MODE_ALLOWED) {
            binding.textViewStatus.text = "Durum: 1. İzin (Kullanım Verisi) gerekiyor."
            binding.buttonSettings.text = "Kullanım İznini Aç"
            binding.buttonSettings.setOnClickListener { requestUsageAccessPermission() }
            return
        }

        if (!checkOverlayPermission()) {
            binding.textViewStatus.text = "Durum: 2. İzin ('Üzerinde Çizme') gerekiyor."
            binding.buttonSettings.text = "Üzerinde Çizme İznini Aç"
            binding.buttonSettings.setOnClickListener { requestOverlayPermission() }
            return
        }

        if (!isAccessibilityServiceEnabled()) {
            binding.textViewStatus.text = "Durum: 3. İzin (Erişilebilirlik Servisi) gerekiyor."
            binding.buttonSettings.text = "Erişilebilirlik İznini Aç"
            binding.buttonSettings.setOnClickListener { requestAccessibilityPermission() }
            return
        }

        startTrackingService()
        binding.textViewStatus.text = "Durum: TÜM İZİNLER TAMAM. İzleme Aktif."
        binding.buttonSettings.text = "Ayarlar / Süreyi Sıfırla"
        binding.buttonSettings.setOnClickListener { startSettingsActivity() }
    }

    private fun requestUsageAccessPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun startTrackingService() {
        val intent = Intent(this, TrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        binding.textViewStatus.text = "Durum: Gerçek Zamanlı İzleme Aktif. Sayaç Düşüyor..."
    }

    private fun startSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = packageName + "/" + AppBlockerService::class.java.canonicalName
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(serviceName) == true
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Lütfen Dijital Detoks için Erişilebilirlik Servisini AÇIN.", Toast.LENGTH_LONG).show()
    }

    private fun updateUiTime() {
        val usedTimeMillis = TimeManager.getUsedTime(this)
        val limitTimeMillis = TimeManager.getLimitTime(this)
        val remainingMillis = limitTimeMillis - usedTimeMillis
        val isLimitExceeded = remainingMillis <= 0
        countdownTimer?.cancel()

        if (isLimitExceeded) {
            binding.textViewStatus.text = "SÜRE DOLDU! Instagram Kilitli."
            binding.textViewCountdown.text = "00:00"
            return
        }

        timeRemainingMillis = remainingMillis
        countdownTimer = object : CountDownTimer(timeRemainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                val formattedTime = String.format("%02d:%02d", minutes, seconds)
                binding.textViewCountdown.text = formattedTime
            }

            override fun onFinish() {
                updateUiTime()
            }
        }.start()

        binding.textViewStatus.text = "Durum: TÜM İZİNLER TAMAM. İzleme Aktif."
    }
}
