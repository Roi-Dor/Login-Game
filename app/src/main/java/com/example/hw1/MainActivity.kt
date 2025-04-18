package com.example.hw1

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.hardware.Camera
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // ────────────────────────────────────────────────────────────────────────────
    // Views & locks
    // ────────────────────────────────────────────────────────────────────────────
    private lateinit var lock1: ImageView
    private lateinit var lock2: ImageView
    private lateinit var lock3: ImageView
    private lateinit var lock4: ImageView
    private lateinit var root: ViewGroup

    private lateinit var stepReceiver: BroadcastReceiver
    private lateinit var motionLock: MotionLock
    private val locksSatisfied = BooleanArray(4)

    // Run‑away bookkeeping
    private val runAwayAttempts = IntArray(4)
    private val MAX_RUNAWAY_STEPS = 20

    // ────────────────────────────────────────────────────────────────────────────
    // Permissions & contracts
    // ────────────────────────────────────────────────────────────────────────────
    private val REQ_ACTIVITY_REC = 1001
    private val REQ_CAMERA       = 1002

    /** Opens the back camera via ACTION_IMAGE_CAPTURE */
    private val cameraIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == RESULT_OK) {
                val bmp = res.data?.extras?.get("data") as? Bitmap
                if (bmp != null) {
                    checkIfCat(bmp)
                } else {
                    Toast.makeText(this, "No picture captured", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    // ────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        root = findViewById(android.R.id.content)

        if (hasActivityRecPermission()) startStepService() else askActivityRecognitionPermission()

        initViewsAndLocks()
        restoreStepGoalState()
        initBroadcastReceiver()
    }

    override fun onResume() {
        super.onResume()
        motionLock.start()
        ContextCompat.registerReceiver(
            this, stepReceiver,
            IntentFilter("com.example.STEP_GOAL_REACHED"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        restoreStepGoalState()
    }

    override fun onPause() {
        super.onPause()
        motionLock.stop()
        unregisterReceiver(stepReceiver)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Permission helpers
    // ────────────────────────────────────────────────────────────────────────────
    private fun hasActivityRecPermission() =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

    private fun askActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                REQ_ACTIVITY_REC
            )
        }
    }

    override fun onRequestPermissionsResult(
        code: Int, perms: Array<out String>, results: IntArray
    ) {
        super.onRequestPermissionsResult(code, perms, results)

        when (code) {
            REQ_ACTIVITY_REC ->
                if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED)
                    startStepService()
                else
                    Toast.makeText(
                        this,
                        "Physical‑activity permission is required for walk‑to‑unlock",
                        Toast.LENGTH_LONG
                    ).show()

            REQ_CAMERA ->
                if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED)
                    launchBackCamera()
                else
                    Toast.makeText(
                        this,
                        "Camera permission is required for cat‑photo unlock",
                        Toast.LENGTH_LONG
                    ).show()
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Step‑counter service & receiver
    // ────────────────────────────────────────────────────────────────────────────
    private fun startStepService() {
        Log.i("MainActivity", "Starting foreground step‑counter service")
        ContextCompat.startForegroundService(
            this, Intent(this, StepCounterService::class.java)
        )
    }

    private fun initBroadcastReceiver() {
        stepReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                if (i?.action == "com.example.STEP_GOAL_REACHED") {
                    locksSatisfied[3] = true
                    updateLockUI(3)
                    checkAllLocks()
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // UI helpers
    // ────────────────────────────────────────────────────────────────────────────
    private fun initViewsAndLocks() {
        lock1 = findViewById(R.id.lock1)
        lock2 = findViewById(R.id.lock2)
        lock3 = findViewById(R.id.lock3)
        lock4 = findViewById(R.id.lock4)

        motionLock = MotionLock(this) { unlocked ->
            locksSatisfied[2] = unlocked
            updateLockUI(2)
            checkAllLocks()
        }

        lock1.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
            } else {
                launchBackCamera()
            }
        }

        lock2.setOnClickListener { toggleLock(1, lock2) }
        addRunAwayBehavior(lock2, 1)
    }

    private fun launchBackCamera() {
        val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            // Hints for using the back lens; ignored gracefully on older devices
            putExtra("android.intent.extras.CAMERA_FACING", Camera.CameraInfo.CAMERA_FACING_BACK)
            putExtra("android.intent.extras.LENS_FACING_BACK", 1)
            putExtra("android.intent.extra.USE_FRONT_CAMERA", false)
        }
        cameraIntentLauncher.launch(camIntent)
    }

    private fun toggleLock(index: Int, v: ImageView) {
        locksSatisfied[index] = !locksSatisfied[index]
        updateLockUI(index)
        checkAllLocks()
    }

    private fun updateLockUI(index: Int) {
        val img = when (index) { 0 -> lock1; 1 -> lock2; 2 -> lock3; 3 -> lock4; else -> return }
        img.setImageResource(
            if (locksSatisfied[index]) R.drawable.ic_unlocked else R.drawable.ic_lock
        )
    }

    private fun restoreStepGoalState() {
        locksSatisfied[3] = getSharedPreferences(
            "step_lock_prefs", MODE_PRIVATE
        ).getBoolean("goal_done", false)

        updateLockUI(3)
        checkAllLocks()
    }

    private fun checkAllLocks() {
        if (locksSatisfied.all { it }) {
            stopService(Intent(this, StepCounterService::class.java))
            startActivity(Intent(this, SuccessActivity::class.java))
            finish()
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Run‑away lock helpers
    // ────────────────────────────────────────────────────────────────────────────
    @SuppressLint("ClickableViewAccessibility")
    private fun addRunAwayBehavior(lock: ImageView, idx: Int) {
        lock.setOnTouchListener { v, ev ->
            if (ev.action == MotionEvent.ACTION_DOWN && !locksSatisfied[idx]) {
                runAwayAttempts[idx]++
                if (runAwayAttempts[idx] >= MAX_RUNAWAY_STEPS) {
                    Toast.makeText(this, "I give up", Toast.LENGTH_SHORT).show()
                    locksSatisfied[idx] = true
                    updateLockUI(idx)
                    checkAllLocks()
                    lock.setOnTouchListener(null)
                    false
                } else {
                    if(runAwayAttempts[idx]%5 == 0){
                        Toast.makeText(this, "You'll never catch me", Toast.LENGTH_SHORT).show()
                    }
                    else if(runAwayAttempts[idx]%13 == 0){
                        Toast.makeText(this, "You'll need to be faster than that", Toast.LENGTH_SHORT).show()
                    }
                    moveLockSomewhereElse(v)
                    true
                }
            } else false
        }
    }

    private fun moveLockSomewhereElse(v: View) {
        val maxX = root.width - v.width
        val maxY = root.height - v.height
        if (maxX <= 0 || maxY <= 0) return

        v.animate()
            .x(Random.nextInt(maxX).toFloat())
            .y(Random.nextInt(maxY).toFloat())
            .setDuration(180)
            .start()
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Cat detection
    // ────────────────────────────────────────────────────────────────────────────
    private fun checkIfCat(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                val hasCat = labels.any { it.text.equals("Cat", true) && it.confidence > 0.60 }
                if (hasCat) {
                    locksSatisfied[0] = true
                    updateLockUI(0)
                    checkAllLocks()
                } else {
                    Toast.makeText(this, "That’s not a cat – try again!", Toast.LENGTH_SHORT).show()
                }
                labeler.close()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Unable to analyse image: ${it.localizedMessage}",
                    Toast.LENGTH_LONG).show()
                labeler.close()
            }
    }
}
