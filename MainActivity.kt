package com.carlauncher

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {

    data class AppItem(val label: String, val pkg: String, val icon: Drawable)

    private val defaultFavs = setOf(
        "com.google.android.apps.maps",
        "com.google.android.youtube",
        "com.waze",
        "com.spotify.music"
    )

    private lateinit var prefs: SharedPreferences
    private lateinit var grid: GridView
    private var apps = listOf<AppItem>()
    private var favSet = setOf<String>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private val adapter = object : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(p: Int) = apps[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, v: View?, parent: ViewGroup): View {
            val app = apps[p]
            val ctx = this@MainActivity
            val tile = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(16), dp(8), dp(12))
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(if (app.pkg in favSet) 0xFF1F3A5F.toInt() else 0xFF1B2128.toInt())
                }
            }
            tile.addView(ImageView(ctx).apply { setImageDrawable(app.icon) },
                LinearLayout.LayoutParams(dp(72), dp(72)))
            tile.addView(TextView(ctx).apply {
                text = app.label
                setTextColor(Color.WHITE)
                textSize = 15f
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(0, dp(8), 0, 0)
            })
            return tile
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("fav", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0D1117.toInt())
            setPadding(dp(28), dp(16), dp(28), dp(16))
        }
        val clock = TextClock(this).apply {
            format12Hour = "h:mm a"
            format24Hour = "HH:mm"
            textSize = 40f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(12))
        }
        grid = GridView(this).apply {
            numColumns = 6
            verticalSpacing = dp(16)
            horizontalSpacing = dp(16)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            selector = null
            adapter = this@MainActivity.adapter
            setOnItemClickListener { _, _, i, _ -> launch(apps[i].pkg) }
            setOnItemLongClickListener { _, _, i, _ -> toggleFav(apps[i].pkg); refresh(); true }
        }
        root.addView(clock)
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        favSet = prefs.getStringSet("favs", null) ?: defaultFavs
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
            .distinctBy { it.pkg }
            .sortedWith(compareBy({ it.pkg !in favSet }, { it.label.lowercase() }))
        adapter.notifyDataSetChanged()
    }

    private fun toggleFav(pkg: String) {
        val s = favSet.toMutableSet()
        if (!s.add(pkg)) s.remove(pkg)
        prefs.edit().putStringSet("favs", s).apply()
    }

    private fun launch(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }

    override fun onBackPressed() { /* stay on home */ }
}
