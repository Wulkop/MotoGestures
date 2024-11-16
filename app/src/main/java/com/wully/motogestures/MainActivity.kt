package com.wully.motogestures

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

enum class Dimension(val value: Int) {
    X(0),
    Y(1),
    Z(2);

    companion object {
        fun fromInt(value: Int) = entries.first { it.value == value }
    }
}

class MainActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {

    private lateinit var valueText: Array<TextView>
    private lateinit var sliderArray: Array<SeekBar>


    private var myService: BackgroundService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as BackgroundService.LocalBinder
            myService = localBinder.getService()
            isBound = true


            for ((i, slider) in sliderArray.withIndex()) {
                if (isBound) {
                    slider.progress = (myService?.threshold?.get(i)?.times(10))?.toInt() ?: 5
                }

            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            myService = null
        }
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, BackgroundService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sliderArray = arrayOf(
            findViewById(R.id.SliderX),
            findViewById(R.id.SliderY),
            findViewById(R.id.SliderZ)
        )
        valueText =
            arrayOf(findViewById(R.id.ValueX), findViewById(R.id.ValueY), findViewById(R.id.ValueZ))

        for (slider in sliderArray) {
            slider.setOnSeekBarChangeListener(this)
        }

    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

        val index = sliderArray.indexOf(seekBar)
        val value = progress / 10.0F
        if (isBound && fromUser) {
            myService?.setThreshold(index, value)
        }

        valueText[index].text = "%.2f".format(value)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }
}