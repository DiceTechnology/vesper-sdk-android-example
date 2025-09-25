package com.vesper.sdk.android.example

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import com.diceplatform.doris.custom.ui.utils.ScreenUtils
import com.diceplatform.doris.custom.ui.view.DorisOutput
import com.diceplatform.doris.custom.ui.view.DorisViewEvent
import com.diceplatform.doris.custom.ui.view.viewmodels.state.DisplayType
import com.diceplatform.doris.entity.DorisAdEvent
import com.diceplatform.doris.entity.DorisPlayerEvent
import com.diceplatform.doris.sdk.playback.ApiConfig
import com.diceplatform.doris.sdk.playback.AuthManager
import com.diceplatform.doris.sourceresolver.ResolvableSource
import com.vesper.sdk.android.PlayerManager
import com.vesper.sdk.android.VesperSdk
import com.vesper.sdk.android.config.UserInterfaceConfig
import com.vesper.sdk.android.config.VesperSdkConfig
import com.vesper.sdk.android.error.VesperSdkError

class MainActivity : AppCompatActivity(), DorisOutput {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var rootView: RelativeLayout
    private lateinit var orientationHelper: OrientationHelper

    private var vesperSdk: VesperSdk? = null
    private var playerManager: PlayerManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        orientationHelper = OrientationHelper(this)
        rootView = findViewById(R.id.root_view)
        rootView.keepScreenOn = true

        findViewById<View>(R.id.to_mini_player_display_type).setOnClickListener {
            orientationHelper.hardLockToPortrait()
            playerManager?.getUiManager()?.getUiManagerConfig()?.displayType =
                DisplayType.MINIPLAYER
            resizePlayer()
        }
        findViewById<View>(R.id.to_mini_bar_display_type).setOnClickListener {
            orientationHelper.hardLockToPortrait()
            playerManager?.getUiManager()?.getUiManagerConfig()?.displayType = DisplayType.MINIBAR
            resizePlayer()
        }
        findViewById<View>(R.id.to_naked_display_type).setOnClickListener {
            orientationHelper.hardLockToPortrait()
            playerManager?.getUiManager()?.getUiManagerConfig()?.displayType = DisplayType.NAKED
            resizePlayer()
        }
        findViewById<View>(R.id.to_regular_display_type).setOnClickListener {
            orientationHelper.lockToPortrait()
            playerManager?.getUiManager()?.getUiManagerConfig()?.displayType = DisplayType.REGULAR
            resizePlayer()
        }

        // Initial sizing of the player view
        resizePlayer(ScreenUtils.isPortrait(this))

        // Handle edge-to-edge overlaps
        handleEdgeToEdgeOverlap()

        // Setup the SDK
        setupVesperSdk()
    }

    private fun setupVesperSdk() {
        val apiConfig = ApiConfig(
            /* realm */ "dce.vespersdk",
            ApiConfig.Env.PRODUCTION,
            /* apiKey */ "API_KEY_HERE"
        )

        val authManager = object : AuthManager {
            override fun getAuthToken(callback: AuthManager.AuthTokenCallback) {
                callback.onResult("AUTH_TOKEN_HERE")
            }

            override fun getRefreshToken(callback: AuthManager.RefreshTokenCallback) {
                callback.onResult("REFRESH_TOKEN_HERE")
            }

            override fun refreshAuthToken(
                authToken: String,
                callback: AuthManager.AuthTokenCallback
            ) {
                callback.onResult("NEW_AUTH_TOKEN_HERE")
            }
        }

        val sdkConfig = VesperSdkConfig.Builder()
            .setApiConfig(apiConfig)
            .setAuthManager(authManager)
            .build()

        vesperSdk = VesperSdk(this, sdkConfig)
        vesperSdk?.createPlayerManager(
            this,
            uiConfig = UserInterfaceConfig.default,
            output = this,
            result = object : VesperSdk.Result {
                override fun onSuccess(playerManager: PlayerManager) {
                    this@MainActivity.playerManager = playerManager
                    attachPlayerView(playerManager)
                    loadVideo(playerManager)
                }

                override fun onError(error: VesperSdkError) {
                    Log.d(TAG, "Init error: $error")
                }
            }
        )
    }

    private fun attachPlayerView(playerManager: PlayerManager) {
        rootView.addView(
            playerManager.getUiManager().getPlayerView(),
            RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun loadVideo(playerManager: PlayerManager) {
        val resolvableSource = ResolvableSource.Builder()
            .setId("CONTENT_ID_HERE")
            .setIsLive(false)
            .build()

        playerManager.load(resolvableSource, object : PlayerManager.Listener {
            override fun onError(error: VesperSdkError) {
                Log.d(TAG, "Error while loading: $error")
            }
        })
    }

    override fun onPlayerEvent(event: DorisPlayerEvent) {
        Log.d(TAG, "onPlayerEvent: $event")
    }

    override fun onAdEvent(event: DorisAdEvent) {
        Log.d(TAG, "onAdEvent: $event")
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onViewEvent(event: DorisViewEvent) {
        Log.d(VesperSdk.TAG, "onViewEvent: $event")
        when (event) {
            is DorisViewEvent.FullScreenOffButtonTap -> {
                orientationHelper.lockToPortrait()
                playerManager?.getUiManager()?.getUiManagerConfig()?.displayType =
                    DisplayType.REGULAR
            }

            is DorisViewEvent.FullScreenOnButtonTap -> {
                orientationHelper.lockToLandscape()
                playerManager?.getUiManager()?.getUiManagerConfig()?.displayType = DisplayType.MAX
            }

            is DorisViewEvent.ExpandButtonTap -> {
                orientationHelper.lockToPortrait()
                playerManager?.getUiManager()?.getUiManagerConfig()?.displayType =
                    DisplayType.REGULAR
                resizePlayer()
            }

            else -> {}
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        playerManager?.onPictureInPictureModeChanged(isInPictureInPictureMode)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        playerManager?.enterPictureInPicture()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val displayType = if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
            DisplayType.REGULAR else DisplayType.MAX
        resizePlayer(displayType == DisplayType.REGULAR)
        playerManager?.getUiManager()?.getUiManagerConfig()?.displayType = displayType
    }

    override fun onStart() {
        super.onStart()
        orientationHelper.start()
    }

    override fun onStop() {
        super.onStop()
        orientationHelper.stop()
    }

    private fun resizePlayer(portrait: Boolean = ScreenUtils.isPortrait(this)) {
        val layoutParams = rootView.layoutParams as ViewGroup.MarginLayoutParams
        val displayType = playerManager?.getUiManager()?.getUiManagerConfig()?.displayType
        if (displayType == DisplayType.NAKED) {
            layoutParams.apply {
                width = ScreenUtils.dpToPx(application, 240F)
                height = ScreenUtils.dpToPx(application, 135F)
            }
        } else if (displayType == DisplayType.MINIPLAYER) {
            layoutParams.apply {
                width = ScreenUtils.dpToPx(application, 185F)
                height = ScreenUtils.dpToPx(application, 105F)
            }
        } else if (displayType == DisplayType.MINIBAR) {
            layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ScreenUtils.dpToPx(application, 65F)
            }
        } else if (portrait) {
            layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ScreenUtils.screenWidth * 9 / 16
            }
        } else {
            layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
    }

    private fun handleEdgeToEdgeOverlap() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top
            }
            handleImmersiveMode()
            insets
        }
    }

    private fun handleImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val orientation = resources.configuration.orientation

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}