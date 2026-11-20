package com.focus.launcher

import com.focus.launcher.data.Tip
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.launcher.data.ACTION_DRAWER
import com.focus.launcher.data.ACTION_NOTHING
import com.focus.launcher.data.ACTION_SEARCH
import com.focus.launcher.data.ACTION_WEB_SEARCH
import com.focus.launcher.data.AppEntry
import com.focus.launcher.data.Settings
import com.focus.launcher.service.WeeklyReview
import com.focus.launcher.ui.drawer.AppMenu
import com.focus.launcher.ui.TestTags
import com.focus.launcher.ui.DrawerOrigin
import com.focus.launcher.ui.DrawerPager
import com.focus.launcher.ui.drawer.DrawerScreen
import com.focus.launcher.ui.home.HomeScreen
import com.focus.launcher.ui.home.homeDragging
import com.focus.launcher.ui.home.runHomeAction
import com.focus.launcher.ui.launchApp
import com.focus.launcher.ui.theme.FocusTheme
import com.focus.launcher.ui.theme.LocalFocusColors
import com.focus.launcher.ui.theme.applyFocusWindow
import com.focus.launcher.util.Perms
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** The home screen (page 0) and the drawer (page 1), opening along the chosen swipe's axis. */
class MainActivity : ComponentActivity() {
    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.settings.value.let { applyFocusWindow(it.dark, it.hideStatusBar, it.showWallpaper) }
        // First start ever: the introduction, once. Marked as seen here already, so that pressing
        // Home in the middle of it never brings it back.
        if (savedInstanceState == null && !Graph.state.tutorialSeen) {
            Graph.state.tutorialSeen = true
            startActivity(SettingsActivity.intent(this, "welcome"))
        }
        setContent {
            val settings by Graph.settings.flow.collectAsStateWithLifecycle()
            LaunchedEffect(settings.dark, settings.hideStatusBar, settings.showWallpaper) {
                applyFocusWindow(settings.dark, settings.hideStatusBar, settings.showWallpaper)
            }
            FocusTheme(settings, wallpaper = settings.showWallpaper) { Launcher(settings, homePresses) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The Home button while the launcher is already showing: go back to page 0.
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.tryEmit(Unit)
    }

    override fun onResume() {
        super.onResume()
        // Reapply the window/surface contract on every return, not just a settings change.
        Graph.settings.value.let { applyFocusWindow(it.dark, it.hideStatusBar, it.showWallpaper) }
    }
}

private var lastBackfill = 0L

@Composable
private fun Launcher(settings: Settings, homePresses: Flow<Unit>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val apps by Graph.apps.apps.collectAsStateWithLifecycle()
    val loaded by Graph.apps.loaded.collectAsStateWithLifecycle()
    val today by Graph.usage.today.collectAsStateWithLifecycle()
    val pendingReview by Graph.state.pendingReview.collectAsStateWithLifecycle()
    val tip by Graph.state.tip.collectAsStateWithLifecycle()

    val pager = rememberPagerState { 2 }
    var drawerOrigin by remember { mutableStateOf(DrawerOrigin.RIGHT) }
    var pendingDrawerOpen by remember { mutableStateOf(false) }
    // The drawer counts as open from the moment a swipe is let go towards it: not halfway through
    // the drag, where the finger can still turn back (the keyboard used to pop up and drop again),
    // and not only once the page has settled (the keyboard would come late). So the keyboard
    // rises while the page glides in, and falls while it glides out.
    val pagerDragged by pager.interactionSource.collectIsDraggedAsState()
    val drawerActive by remember { derivedStateOf { if (pagerDragged) pager.settledPage == 1 else pager.targetPage == 1 } }
    var query by remember { mutableStateOf("") }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var wantsSearchFocus by remember { mutableStateOf(false) }
    var resumeCount by remember { mutableIntStateOf(0) }
    var usageAccess by remember { mutableStateOf(Perms.hasUsageAccess()) }
    var setupIncomplete by remember { mutableStateOf(false) }

    // Every return to the launcher: re-read permissions, refresh today's numbers (then keep them
    // ticking once a minute), and see whether a weekly review has come due.
    LifecycleResumeEffect(settings.timersEnabled) {
        resumeCount++
        usageAccess = Perms.hasUsageAccess()
        setupIncomplete = !Perms.isDefaultLauncher(context) || !usageAccess ||
            (settings.timersEnabled && !Perms.isTimerServiceEnabled(context))
        val job = scope.launch {
            WeeklyReview.checkDue()
            val now = System.currentTimeMillis()
            if (usageAccess && now - lastBackfill > 6 * 3_600_000L) {
                lastBackfill = now
                launch { Graph.usage.backfill() }
            }
            while (true) {
                Graph.usage.refreshToday()
                delay(60_000)
            }
        }
        onPauseOrDispose { job.cancel() }
    }

    // Leaving the launcher (an app was opened, the screen went off) always resets it to page 0.
    LifecycleStartEffect(Unit) {
        onStopOrDispose {
            query = ""
            menuApp = null
            wantsSearchFocus = false
            pendingDrawerOpen = false
            scope.launch { pager.scrollToPage(0) }
        }
    }

    // Arrived in the app list, by whichever way: that tip is learnt.
    LaunchedEffect(pager.settledPage) { if (pager.settledPage == 1) Graph.state.did(Tip.SWIPE_LEFT) }

    LaunchedEffect(Unit) {
        homePresses.collect {
            menuApp = null
            query = ""
            pendingDrawerOpen = false
            if (pager.currentPage != 0) pager.animateScrollToPage(0)
        }
    }

    // A launcher is never "backed out of": back only returns from the drawer to home.
    BackHandler {
        pendingDrawerOpen = false
        if (pager.currentPage != 0) scope.launch { pager.animateScrollToPage(0) }
    }

    val launch: (AppEntry) -> Unit = { entry -> launchApp(context, scope, entry) }
    // The drawer can be reached by any swipe; while it is on the drawer side of home the pager
    // keeps its own drag (the page follows the finger). Every other horizontal swipe is read here.
    val drawerIsLeft = settings.swipeLeftAction == ACTION_DRAWER
    val openDrawer by rememberUpdatedState<(Boolean, DrawerOrigin) -> Unit> { focusSearch, origin ->
        wantsSearchFocus = focusSearch
        drawerOrigin = origin
        pendingDrawerOpen = true
    }
    // Run after the new pager orientation is composed, so its very first frame moves on that axis.
    LaunchedEffect(pendingDrawerOpen, drawerOrigin) {
        if (pendingDrawerOpen) {
            pager.animateScrollToPage(1)
            pendingDrawerOpen = false
        }
    }
    LaunchedEffect(pager.settledPage, pendingDrawerOpen) {
        if (pager.settledPage == 0 && !pager.isScrollInProgress && !pendingDrawerOpen) {
            drawerOrigin = DrawerOrigin.RIGHT
        }
    }
    val launchLatest by rememberUpdatedState(launch)
    val background = LocalFocusColors.current.bg

    DrawerPager(
        origin = drawerOrigin,
        state = pager,
        // There is no page to the left of home, so the pager ignores that swipe. Watch it on the
        // way down (Initial pass, nothing consumed) and run the action the user chose instead.
        modifier = Modifier.fillMaxSize().pointerInput(settings.swipeLeftAction, settings.swipeRightAction, apps) {
            val left = settings.swipeLeftAction
            val right = settings.swipeRightAction
            val threshold = 72.dp.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (pager.currentPage != 0 || pager.isScrollInProgress) return@awaitEachGesture
                if (homeDragging.value) return@awaitEachGesture
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (homeDragging.value) break // a fast app is being dragged: no action from a sideways wobble
                    val moved = change.position - down.position
                    val sideways = abs(moved.x) > threshold && abs(moved.x) > abs(moved.y) * 2
                    if (!sideways) continue
                    val spec = if (moved.x > 0) right else left
                    if (moved.x < 0 && drawerIsLeft && drawerOrigin == DrawerOrigin.RIGHT) break
                    if (spec != ACTION_NOTHING) {
                        change.consume()
                        runHomeAction(
                            context, spec, apps, launchLatest,
                            { search -> openDrawer(search, if (moved.x < 0) DrawerOrigin.RIGHT else DrawerOrigin.LEFT) },
                        ) { route -> context.startActivity(SettingsActivity.intent(context, route)) }
                        if (spec == ACTION_WEB_SEARCH) Graph.state.did(Tip.SWIPE_RIGHT)
                        if (spec == ACTION_DRAWER || spec == ACTION_SEARCH) Graph.state.did(Tip.SWIPE_LEFT)
                    }
                    break
                }
            }
        },
        userScrollEnabled = !homeDragging.value &&
            (pager.settledPage != 0 || (drawerIsLeft && drawerOrigin == DrawerOrigin.RIGHT)),
    ) { page ->
        // The page being left fades and sinks back a touch while the other one arrives. Done in
        // graphicsLayer, which runs in the draw phase: the swipe never triggers a recomposition.
        Box(
            Modifier.fillMaxSize().testTag(if (page == 0) TestTags.HOME_PAGE else TestTags.APP_LIST).graphicsLayer {
                val distance = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                // Text on a flat background never overlaps itself, so each draw call can simply be
                // made more transparent. The default would render the whole page into a full-screen
                // off-screen buffer on every frame of the swipe: more memory and GPU for no gain.
                compositingStrategy = CompositingStrategy.ModulateAlpha
                alpha = (1f - distance * 1.2f).coerceIn(0f, 1f)
                val scale = 1f - 0.05f * distance
                scaleX = scale
                scaleY = scale
            }.then(if (page == 1) Modifier.background(background) else Modifier),
        ) {
            if (page == 0) {
                HomeScreen(
                    settings = settings,
                    apps = apps,
                    today = today,
                    usageAccess = usageAccess,
                    setupIncomplete = setupIncomplete,
                    pendingReview = pendingReview,
                    resumeCount = resumeCount,
                    onLaunch = launch,
                    onAppMenu = { Graph.state.did(Tip.APP_MENU); menuApp = it },
                    onOpenDrawer = openDrawer,
                    onOpenSettings = { route -> context.startActivity(SettingsActivity.intent(context, route)) },
                    onOpenReview = { week -> context.startActivity(ReviewActivity.intent(context, week)) },
                )
            } else {
                DrawerScreen(
                    settings = settings,
                    apps = apps,
                    loaded = loaded,
                    today = today,
                    query = query,
                    onQueryChange = { query = it },
                    isActive = drawerActive,
                    wantsSearchFocus = wantsSearchFocus,
                    onSearchFocusHandled = { wantsSearchFocus = false },
                    onLaunch = launch,
                    onAppMenu = { Graph.state.did(Tip.APP_MENU); menuApp = it },
                    hint = tip?.takeIf { it == Tip.APP_MENU }?.let { it.gesture + "  →  " + it.result },
                )
            }
        }
    }

    menuApp?.let { app ->
        AppMenu(
            app = app,
            settings = settings,
            today = today,
            onDismiss = { menuApp = null },
            onOpenSetup = { context.startActivity(SettingsActivity.intent(context, "setup")) },
        )
    }
}
