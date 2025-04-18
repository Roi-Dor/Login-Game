package com.example.hw1

import android.app.Service
import android.content.Intent
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.os.Build
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.hardware.SensorEventListener
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import android.Manifest

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var baseline: Int? = null
    private var goalReached = false
    private val stepGoal = 7

    // Shared‑prefs keys
    companion object {
        private const val PREF = "step_lock_prefs"
        private const val KEY_GOAL_DONE = "goal_done"
        private const val KEY_BASELINE = "baseline"
        private const val NOTIF_ID = 42
        private const val CHANNEL_ID = "step_lock_fg"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()


        startForeground(
            NOTIF_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_walk)
                .setContentTitle("Counting your steps")
                .setOngoing(true)
                .build()
        )

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        val prefs = getSharedPreferences(PREF, MODE_PRIVATE)
        goalReached = prefs.getBoolean(KEY_GOAL_DONE, false)
        baseline = prefs.getInt(KEY_BASELINE, -1).takeIf { it != -1 }
    }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }

        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        return START_STICKY
    }


    // ─────────────────────────────────────────────────────────────────────────────

    override fun onSensorChanged(ev: SensorEvent) {
        if (goalReached || ev.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        val totalSinceBoot = ev.values[0].toInt()
        if (baseline == null) {
            baseline = totalSinceBoot
            saveBaseline(baseline!!)
            return
        }
        val walked = totalSinceBoot - baseline!!

        Log.d("StepService", "walked=$walked  totalSinceBoot=$totalSinceBoot")


        if (walked >= stepGoal) {
            markGoalDone()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ─────────────────────────────────────────────────────────────────────────────

    private fun markGoalDone() {
        goalReached = true
        getSharedPreferences(PREF, MODE_PRIVATE)
            .edit() { putBoolean(KEY_GOAL_DONE, true) }


        sendBroadcast(Intent("com.example.STEP_GOAL_REACHED"))

        stopSelf()
    }

    private fun saveBaseline(b: Int) =
        getSharedPreferences(PREF, MODE_PRIVATE)
            .edit() { putInt(KEY_BASELINE, b) }

    // ─────────────────────────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return

        val ch = NotificationChannel(
            CHANNEL_ID,
            "Step‑Lock Service",
            NotificationManager.IMPORTANCE_LOW
        )
        ch.description = "Counts steps in the background so your lock can open"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(ch)
    }


    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
