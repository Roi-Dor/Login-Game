package com.example.hw1

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class MotionLock(
    context: Context,
    private val onStateChanged: (Boolean /*unlocked?*/) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isUnlocked = false
    private var lastMotionTime = 0L

    // ↓ tweak these two to taste
    private val SHAKE_THRESHOLD_G = 1.5f        // > gravity × 1.5  ⇒ unlock
    private val STILL_TIMEOUT_MS  = 3000L        // 0.8 s of calm     ⇒ lock

    fun start() = sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
    fun stop()  = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()

        if (g > SHAKE_THRESHOLD_G) {
            lastMotionTime = now
            if (!isUnlocked) {
                isUnlocked = true
                onStateChanged(true)          // 🔓
            }
        } else if (isUnlocked && now - lastMotionTime > STILL_TIMEOUT_MS) {
            isUnlocked = false
            onStateChanged(false)             // 🔒
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
