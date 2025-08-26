package com.vesper.sdk.android.example

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import android.view.Surface

class OrientationHelper(
    private val activity: Activity,
    private val unlockDelayMs: Long = 300L // keeps the “unlock” stable
) {

    private var listener: OrientationEventListener? = null
    private var pendingUnlock: Runnable? = null

    private enum class LockMode { NONE, SENSOR_LANDSCAPE, SENSOR_PORTRAIT }

    private var lockMode: LockMode = LockMode.NONE

    fun start() {
        if (listener != null) return
        listener = object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val isPortraitDeg = isPortraitAngle(orientation)
                val isLandscapeDeg = isLandscapeAngle(orientation)

                when (lockMode) {
                    LockMode.SENSOR_LANDSCAPE -> if (isLandscapeDeg) requestUnlockWhenDisplayMatches(
                        targetLandscape = true
                    )

                    LockMode.SENSOR_PORTRAIT -> if (isPortraitDeg) requestUnlockWhenDisplayMatches(
                        targetLandscape = false
                    )

                    LockMode.NONE -> Unit
                }
            }
        }.also { it.enable() }
    }

    fun stop() {
        pendingUnlock?.let { activity.window.decorView.removeCallbacks(it) }
        pendingUnlock = null
        listener?.disable()
        listener = null
    }

    /** Lock to landscape (both 90° and 270° supported) */
    fun lockToLandscape() {
        lockMode = LockMode.SENSOR_LANDSCAPE
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    /** Lock to portrait (0° and 180°) */
    fun lockToPortrait() {
        lockMode = LockMode.SENSOR_PORTRAIT
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    /** Hard lock to portrait. */
    fun hardLockToPortrait() {
        // Immediate portrait, ignore sensor
        lockMode = LockMode.NONE
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // ---- Internals ----

    private fun requestUnlockWhenDisplayMatches(targetLandscape: Boolean) {
        // Replace any previous attempt so we don’t stack unlocks
        pendingUnlock?.let { activity.window.decorView.removeCallbacks(it) }
        val r = Runnable {
            val matches = if (targetLandscape) isDisplayLandscape() else isDisplayPortrait()
            if (matches) {
                lockMode = LockMode.NONE
                // Don’t switch to SENSOR (avoids the portrait flip); hand back to user/system.
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
            }
        }
        pendingUnlock = r
        activity.window.decorView.postDelayed(r, unlockDelayMs)
    }

    // Angle helpers
    private fun isLandscapeAngle(deg: Int): Boolean {
        val d = ((deg % 360) + 360) % 360
        return (d in 60..120) || (d in 240..300)
    }

    private fun isPortraitAngle(deg: Int): Boolean {
        val d = ((deg % 360) + 360) % 360
        return (d in 330..359) || (d in 0..30) || (d in 150..210)
    }

    private fun isDisplayLandscape(): Boolean {
        val r = activity.window.decorView.display?.rotation
        return r == Surface.ROTATION_90 || r == Surface.ROTATION_270
    }

    private fun isDisplayPortrait(): Boolean = !isDisplayLandscape()
}