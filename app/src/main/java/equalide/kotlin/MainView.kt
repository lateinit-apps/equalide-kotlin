package equalide.kotlin

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.GridLayout
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_view.*
import android.view.Gravity
import android.widget.LinearLayout
import android.util.TypedValue

class MainView : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val colorPicker = findViewById(R.id.color_picker) as LinearLayout

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        addColors(colorPicker, 3)
        nav_view.setNavigationItemSelectedListener(this)

        val gridArea = findViewById(R.id.grid) as GridLayout
        gridArea.addOnLayoutChangeListener({v, _, _, _, _, _, _, _, _-> getSize(v)})
    }

    fun getSize(view: View) {
        val height = view.getHeight()
        val width = view.getWidth()
        val dp = TypedValue.COMPLEX_UNIT_DIP
        Log.d("TAG","$height + $width + $dp")
    }

    fun addColors(picker: ViewGroup, numOfColors: Int) {
        for (i in 1..numOfColors) {
            val colorButton = Button(this)
            val params = LinearLayout.LayoutParams(100,100)
            params.gravity = Gravity.BOTTOM
            params.setMargins(1,1,1,1)
            colorButton.setBackgroundResource(R.drawable.primitive_border)
            colorButton.layoutParams = params
            picker.addView(colorButton)
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.actionbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            //R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_camera -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
