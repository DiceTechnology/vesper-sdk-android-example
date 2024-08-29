package com.vesper.sdk.android.example

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.diceplatform.doris.custom.utils.ScreenUtils

open class BaseActivity : AppCompatActivity() {
    private var orientationEventListener: OrientationEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        renderFullScreen()
        showSystemUi(ScreenUtils.isPortrait(this))
        initOrientationListener()
    }

    private fun initOrientationListener() {
        // Needed to avoid rotating back automatically after the user has manually rotated the
        // player by pressing the "full screen" button.
        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                val isPortrait =
                    orientation > 345 || orientation < 15 || (orientation in 166..194)
                val isLandscape =
                    (orientation in 256..284) || (orientation in 76..104)
                if ((requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && isPortrait) ||
                    (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && isLandscape)
                ) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
            }
        }
        orientationEventListener?.enable()
    }

    private fun renderFullScreen() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun showSystemUi(show: Boolean) {
        val decorView = window.decorView
        val controller = ViewCompat.getWindowInsetsController(decorView) ?: return
        if (show) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        showSystemUi(ScreenUtils.isPortrait(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationEventListener?.disable()
    }
}