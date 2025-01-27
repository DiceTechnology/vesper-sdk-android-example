package com.vesper.sdk.android.example

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.diceplatform.doris.custom.ui.view.DorisOutput
import com.diceplatform.doris.custom.ui.view.DorisViewEvent
import com.diceplatform.doris.custom.utils.ScreenUtils
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

class MainActivity : BaseActivity(), DorisOutput {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var vesperSdk: VesperSdk? = null
    private var playerManager: PlayerManager? = null
    private var rootView: RelativeLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootView = findViewById(R.id.root_view)
        resizePlayer(ScreenUtils.isPortrait(this))

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
        rootView?.addView(
            playerManager.getPlayerView(),
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

    override fun onViewEvent(event: DorisViewEvent) {
        when (event) {
            is DorisViewEvent.FullScreenOffButtonTap -> requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            is DorisViewEvent.FullScreenOnButtonTap -> requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            is DorisViewEvent.BackButtonTap -> finish()

            else -> {}
        }
        Log.d(TAG, "onViewEvent: $event")
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
        resizePlayer(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
    }

    private fun resizePlayer(portrait: Boolean) {
        rootView?.layoutParams?.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = if (portrait) {
                ScreenUtils.getScreenWidth() * 9 / 16
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
    }
}