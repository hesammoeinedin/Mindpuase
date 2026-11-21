package com.mindpause.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mindpause.domain.CheckInMessage
import com.mindpause.ui.CheckInScreen
import com.mindpause.ui.OverlayUi
import com.mindpause.ui.theme.MindPauseTheme

/** Shows the full-screen check-in as a window over other apps. */
class CheckInOverlay(
    private val context: Context,
    private val onDone: () -> Unit,
    private val onMore: () -> Unit,
) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val ui = mutableStateOf<OverlayUi?>(null)
    private var root: View? = null
    private var owner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = root != null
    val isAsking: Boolean get() = ui.value is OverlayUi.Ask

    fun show(message: CheckInMessage) {
        ui.value = OverlayUi.Ask(message)
        if (root != null) return

        val lifecycleOwner = OverlayLifecycleOwner().also { it.create() }
        val compose = ComposeView(context).apply {
            setContent {
                MindPauseTheme {
                    CheckInScreen(ui = ui.value, onDone = onDone, onMore = onMore)
                }
            }
        }
        val frame = BackAwareFrame(context) { if (isAsking) onMore() }.apply {
            addView(compose)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        try {
            wm.addView(frame, params)
            lifecycleOwner.resume()
            root = frame
            owner = lifecycleOwner
        } catch (_: Exception) {
            // Permission revoked or window refused: skip this check-in.
            lifecycleOwner.destroy()
            ui.value = null
        }
    }

    fun showDone() {
        ui.value = OverlayUi.Done
    }

    fun hide() {
        root?.let { view ->
            try {
                wm.removeView(view)
            } catch (_: Exception) {
            }
        }
        owner?.destroy()
        root = null
        owner = null
        ui.value = null
    }

    @SuppressLint("ViewConstructor")
    private class BackAwareFrame(context: Context, private val onBack: () -> Unit) : FrameLayout(context) {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) onBack()
                return true
            }
            return super.dispatchKeyEvent(event)
        }
    }
}
