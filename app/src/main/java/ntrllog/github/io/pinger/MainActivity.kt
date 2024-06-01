package ntrllog.github.io.pinger

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {
    private var continuousMode = false
    private var currentlyRunning = false
    private var dualMode = false
    private var dualSet = 1 // keep track of which ping period is going to be active for dual mode (either 1 or 2)

    private lateinit var statusText: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var stopButton: ImageButton

    private lateinit var handler: Handler

    private lateinit var soundPool: SoundPool
    private var restPeriodSound: Int = 0
    private var startPeriodSound: Int = 0
    private var pingPeriodStartSound: Int = 0
    private var pingPeriodEndSound: Int = 0
    private var dualPeriodStartSound: Int = 0

    private var currCountdown: Int = 0 // keep track of how many seconds are left when paused

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        setStatusText("")

        playPauseButton = findViewById(R.id.play_pause_button)
        playPauseButton.setOnClickListener {
            toggleAction()
        }

        stopButton = findViewById(R.id.stop_button)
        stopButton.setOnClickListener {
            stopIt()
        }

        handler = Handler(Looper.getMainLooper())

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build()
            )
            .build()
        startPeriodSound = soundPool.load(this, R.raw.sound28, 1)
        pingPeriodStartSound = soundPool.load(this, R.raw.cling_1, 1)
        pingPeriodEndSound = soundPool.load(this, R.raw.sound52, 1)
        restPeriodSound = soundPool.load(this, R.raw.beep_3, 1)
        dualPeriodStartSound = soundPool.load(this, R.raw.sound99, 1)

        val continuousModeSwitch = findViewById<SwitchMaterial>(R.id.continuous_mode_switch)
        val restPeriodLinearLayout = findViewById<LinearLayout>(R.id.linear_layout_rest_period)
        val dualModeSwitch = findViewById<SwitchMaterial>(R.id.dual_mode_switch)
        val dualPeriodLinearLayout = findViewById<LinearLayout>(R.id.linear_layout_dual_period)
        continuousModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            continuousMode = isChecked
            restPeriodLinearLayout.visibility =
                if (isChecked) LinearLayout.VISIBLE else LinearLayout.INVISIBLE
        }
        dualModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            dualMode = isChecked
            dualPeriodLinearLayout.visibility =
                if (isChecked) LinearLayout.VISIBLE else LinearLayout.INVISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.explanation -> {
                startActivity(Intent(this, ExplanationActivity::class.java))
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }

    private fun toggleAction() {
        hideKeyboard()
        if (currentlyRunning) {
            pauseIt()
        } else {
            startIt()
        }
    }

    private fun stopIt() {
        currentlyRunning = false
        setStatusText("")
        playPauseButton.setImageResource(R.drawable.baseline_play_circle_outline_150)
        playPauseButton.visibility = LinearLayout.VISIBLE
        stopButton.visibility = LinearLayout.INVISIBLE
        currCountdown = 0
    }

    private fun pauseIt() {
        currentlyRunning = false
        playPauseButton.setImageResource(R.drawable.baseline_play_circle_outline_150)
    }

    private fun startIt() {
        currentlyRunning = true
        dualSet = 1
        playPauseButton.setImageResource(R.drawable.baseline_pause_circle_outline_150)
        stopButton.visibility = LinearLayout.VISIBLE
        val pingPeriodSeconds = getEditTextValue(R.id.ping_period)
        if (pingPeriodSeconds == 0) {
            handler.post {
                Toast.makeText(this, "Ping period must be greater than 0!", Toast.LENGTH_SHORT)
                    .show()
            }
            stopIt()
        } else {
            if (currCountdown > 0) {
                resumePingPeriod()
            } else {
                startStartPeriod()
            }
        }
    }

    private fun startStartPeriod() {
        val startPeriodSeconds = getEditTextValue(R.id.start_period)
        var countdown = startPeriodSeconds

        if (countdown in 1..3) {
            playSound(startPeriodSound)
        }

        playPauseButton.visibility = LinearLayout.INVISIBLE

        setStatusText(getString(R.string.start_period_message, countdown))

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!currentlyRunning) { // stop button is pressed
                    return
                } else {
                    --countdown
                    if (countdown in 1..3) {
                        playSound(startPeriodSound)
                    }
                    setStatusText(getString(R.string.start_period_message, countdown))
                    if (countdown <= 0) {
                        startPingPeriod()
                        return
                    }
                    // reschedule the task
                    handler.postDelayed(this, 1000L)
                }
            }
        }, 1000L)
    }

    private fun startPingPeriod() {
        val pingPeriodSeconds = getEditTextValue(R.id.ping_period)
        var countdown = pingPeriodSeconds

        playSound(pingPeriodStartSound)

        playPauseButton.visibility = LinearLayout.VISIBLE

        setStatusText(getString(R.string.ping_period_message, countdown))

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!currentlyRunning) { // stop button is pressed
                    return
                } else {
                    --countdown
                    currCountdown = countdown
                    setStatusText(getString(R.string.ping_period_message, countdown))
                    if (countdown <= 0) {
                        playSound(pingPeriodEndSound)
                        if (dualMode && dualSet == 1) {
                            startDualPeriod()
                        } else if (continuousMode) {
                            startRestPeriod()
                        } else {
                            stopIt()
                        }
                        if (dualMode) {
                            dualSet = (dualSet % 2) + 1 // toggle between 1 and 2
                        }
                        return
                    }
                    // reschedule the task
                    handler.postDelayed(this, 1000L)
                }
            }
        }, 1000L)
    }

    private fun startRestPeriod() {
        val restPeriodSeconds = getEditTextValue(R.id.rest_period)
        var countdown = restPeriodSeconds

        if (countdown in 1..5) {
            playSound(restPeriodSound)
        }

        playPauseButton.visibility = LinearLayout.INVISIBLE

        setStatusText(getString(R.string.rest_period_message, countdown))

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!currentlyRunning) { // stop button is pressed
                    return
                } else {
                    --countdown
                    if (countdown in 1..5) {
                        playSound(restPeriodSound)
                    }
                    setStatusText(getString(R.string.rest_period_message, countdown))
                    if (countdown <= 0) {
                        startPingPeriod()
                        return
                    }
                    // reschedule the task
                    handler.postDelayed(this, 1000L)
                }
            }
        }, 1000L)
    }

    private fun resumePingPeriod() {
        val pingPeriodSeconds = currCountdown
        var countdown = pingPeriodSeconds

        setStatusText(getString(R.string.ping_period_message, countdown))

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!currentlyRunning) { // stop button is pressed
                    return
                } else {
                    --countdown
                    currCountdown = countdown
                    setStatusText(getString(R.string.ping_period_message, countdown))
                    if (countdown <= 0) {
                        playSound(pingPeriodEndSound)
                        if (dualMode && dualSet == 1) {
                            startDualPeriod()
                        } else if (continuousMode) {
                            startRestPeriod()
                        } else {
                            stopIt()
                        }
                        if (dualMode) {
                            dualSet = (dualSet % 2) + 1 // toggle between 1 and 2
                        }
                        return
                    }
                    // reschedule the task
                    handler.postDelayed(this, 1000L)
                }
            }
        }, 1000L)
    }

    private fun startDualPeriod() {
        val dualPeriodSeconds = getEditTextValue(R.id.dual_period)
        var countdown = dualPeriodSeconds

        if (countdown in 1..5) {
            playSound(dualPeriodStartSound)
        }

        playPauseButton.visibility = LinearLayout.INVISIBLE

        setStatusText(getString(R.string.dual_period_message, countdown))

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!currentlyRunning) { // stop button is pressed
                    return
                } else {
                    --countdown
                    setStatusText(getString(R.string.dual_period_message, countdown))
                    if (countdown <= 0) {
                        if (dualSet == 2) {
                            startPingPeriod()
                        } else {
                            startRestPeriod()
                        }
                        return
                    }
                    // reschedule the task
                    handler.postDelayed(this, 1000L)
                }
            }
        }, 1000L)
    }

    private fun playSound(soundId: Int) {
        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    private fun setStatusText(message: String) {
        statusText.text = message
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40F)
    }

    private fun getEditTextValue(id: Int): Int =
        findViewById<EditText>(id).text.toString().toIntOrNull() ?: 0

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}
