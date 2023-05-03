package se.ju.student.robomow.ui

import android.app.Dialog
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import se.ju.student.robomow.R
import se.ju.student.robomow.api.RoboMowApi
import se.ju.student.robomow.ui.view.MapView
import se.ju.student.robomow.model.AvoidedCollisions
import se.ju.student.robomow.model.MowSession
import javax.inject.Inject

@AndroidEntryPoint

class MapActivity : AppCompatActivity(), MapView.CollisionAvoidanceListener {
    @Inject
    lateinit var roboMowApi: RoboMowApi
    private lateinit var mapView: MapView
    private lateinit var progressBar: ProgressBar
    private lateinit var informationDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        mapView = findViewById(R.id.map_view)
        progressBar = findViewById(R.id.progress_bar)
        mapView.listener = this
        setupMapInformationDialog()
        val mowSession = getMowSession()
        mapView.setCoordinates(mowSession?.path, mowSession?.avoidedCollisions)
    }

    private fun getMowSession(): MowSession? {
        val mowSession = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("MOW_SESSION", MowSession::class.java)
        } else {
            intent.getParcelableExtra("MOW_SESSION")
        }
        return mowSession
    }

    override fun onCollisionAvoidanceClicked(collision: AvoidedCollisions) {
        lifecycleScope.launch(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            val response = roboMowApi.getImageUrl(collision.imageLink)
            val imageUrl = if (response.isSuccessful) {
                val collisionAvoidanceImage = response.body()
                collisionAvoidanceImage!!.imageURL
            } else {
                "https://cdn.presslabs.com/wp-content/uploads/2018/03/custom-error-pages-825x510.png"
            }
            val imageFragment = CollisionAvoidanceImageFragment().apply {
                arguments = Bundle().apply {
                    putString(CollisionAvoidanceImageFragment.ARG_IMAGE_URL, imageUrl)
                }
            }
            progressBar.visibility = View.GONE
            imageFragment.show(supportFragmentManager, "image_dialog")
        }
    }

    private fun setupMapInformationDialog() {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val hasShownMapOverview = sharedPref.getBoolean(getString(R.string.has_shown_map_overview_key), false)
        if (hasShownMapOverview) {
            return
        }
        informationDialog = Dialog(this)
        informationDialog.setContentView(R.layout.fragment_map_view_information)
        val dismissButton = informationDialog.findViewById<Button>(R.id.dismiss_button)
        dismissButton.setOnClickListener {
            with(sharedPref.edit()) {
                putBoolean(getString(R.string.has_shown_map_overview_key), true)
                apply()
            }
            informationDialog.dismiss()
        }
        informationDialog.show()
    }
}