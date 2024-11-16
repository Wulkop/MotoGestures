package com.wully.motogestures

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Binder
import android.os.Build
import android.os.CombinedVibration
import android.os.IBinder
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat


class BackgroundService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var cameraManager: CameraManager
    private lateinit var vibratorManager: VibratorManager

    private lateinit var cameraId: String
    private var gyroSensor: Sensor? = null
    private lateinit var vibrationEffect: CombinedVibration
    lateinit var threshold: FloatArray
    private lateinit var lastTimeThreshold: LongArray

    private var flashLightActive = false


    companion object {
        var isRunning = false
        private const val PREFS_NAME = "ServicePreferences"
        private const val FLOAT_KEYX = "ThresholdX"
        private const val FLOAT_KEYY = "ThresholdY"
        private const val FLOAT_KEYZ = "ThresholdZ"

        private const val CHANNEL_ID = "VibrationServiceChannel"

        private const val MAX_TIME_INTERVAL_MILLIS = 300
        //private const val MIN_TIME_INTERVAL_MILLIS = 100
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BackgroundService = this@BackgroundService
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()
        Log.d("MyBackgroundService", "Service has been created")
        isRunning = true

        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList[0]
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibrationEffect = CombinedVibration.createParallel(
            VibrationEffect.createOneShot(
                200,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )

        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (gyroSensor != null) {
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI)
        }

        threshold = floatArrayOf(5F, 5F, 5F)
        threshold[0] = sharedPreferences.getFloat(FLOAT_KEYX, 5.0f)
        threshold[1] = sharedPreferences.getFloat(FLOAT_KEYY, 5.0f)
        threshold[2] = sharedPreferences.getFloat(FLOAT_KEYZ, 5.0f)

        lastTimeThreshold = longArrayOf(0L, 0L, 0L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vibration Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)

        }
        // Foreground Notification
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vibration Service")
            .setContentText("Service is running in background")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MyBackgroundService", "Service has been started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("MyBackgroundService", "Service has been stopped")
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_GYROSCOPE) {

            val currentTime = System.currentTimeMillis()

            for (i in 0..2) {
                //ValueTextArray[i].text = "%.2f".format(event.values[i])
                if (event.values[i] >= threshold[i]) {
                    val timeDiff = currentTime - lastTimeThreshold[i]
                    if (timeDiff < MAX_TIME_INTERVAL_MILLIS) {
                        triggerGesture(Dimension.fromInt(i))
                    }
                    lastTimeThreshold[i] = currentTime
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @RequiresApi(Build.VERSION_CODES.S)
    private fun triggerGesture(dim: Dimension) {
        when (dim) {
            Dimension.X -> {

            }

            Dimension.Y -> {

            }

            Dimension.Z -> {
                vibratorManager.vibrate(vibrationEffect)
                flashLightActive = !flashLightActive
                cameraManager.setTorchMode(cameraId, flashLightActive)
            }
        }

    }

    fun setThreshold(index: Int, value: Float) {
        threshold[index] = value

        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putFloat(FLOAT_KEYX, threshold[0])
            putFloat(FLOAT_KEYY, threshold[1])
            putFloat(FLOAT_KEYZ, threshold[2])
            apply()
        }
    }
}
