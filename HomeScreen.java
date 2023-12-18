package com.example.herewegothesis

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
//import android.support.v4.app.FragmentActivity
//import android.support.v4.content.ContextCompat

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager

import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech

import android.widget.Button
import android.widget.EditText

import android.content.Context
import android.content.Intent
import android.location.Criteria


import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
//import android.util.Log
//import android.view.KeyEvent
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // Constants and request codes
    private val DESTINATION_SPEECH_REQUEST_CODE = 1001
    private val CONFIRMATION_SPEECH_REQUEST_CODE = 1002

    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1


    var startTime: Long = 0
    var isCounting = false


    private var volumeUpDownPressed = false
    private var timer: Timer? = null

    private var isVolumeUpPressed = false


    private lateinit var speechRecognizer: SpeechRecognizer


    // Declare UI elements
    private lateinit var destinationEditText: EditText

    //private lateinit var startButton: Button
    private lateinit var emailButton: Button

    // Text-to-Speech engine
    private lateinit var textToSpeech: TextToSpeech

    // Google Map variables
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var currentLatLng: Location
    private lateinit var destinationLatLng: LatLng
    private lateinit var currentLocation: Location
    private lateinit var context: Context

    /*private val VOLUME_HOLD_DURATION = 3000 // 3 seconds
    private var volumeDownPressTime: Long = 0
    private var volumeUpPressTime: Long = 0*/

    //private var destinationToConfirm: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        destinationEditText = findViewById(R.id.destinationEditText)
        // startButton = findViewById(R.id.startButton)
        emailButton = findViewById(R.id.emailButton)



        volumeControlStream = AudioManager.STREAM_MUSIC


        // Initialize location provider client and map
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize location manager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set language for Text-to-Speech
                val result = textToSpeech.setLanguage(Locale.US)

                // Check if language is supported
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS Language not supported", Toast.LENGTH_SHORT).show()
                } else {
                    // Start welcome message
                    welcomeMessage()
                }
            } else {
                Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
            }
        }
        // Start Speech Recognition after a 5sec delay
        startDestinationSpeechRecognition()

        // Check and request location permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }

        // Set click listener for email button
        emailButton.setOnClickListener {
            sendEmailAlert()
        }

        /*startButton.setOnClickListener {
            // Add your code for the start button click event
        }*/
    }

    //Welcome Message using TTS
    private fun welcomeMessage() {
        val welcome = "Welcome to Here We GO. Please say your destination to start."
        textToSpeech.speak(welcome, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    //Send Email Alert with current location
    private fun sendEmailAlert() {
        //Check location permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Get latitude and longitude
                    val latitude = location.latitude
                    val longitude = location.longitude

                    // Create maps URL and email message
                    val mapsUrl = "https://www.google.com/maps?q=$latitude,$longitude"
                    val message = "Hi\n\nHelp me!! I'm at $mapsUrl\n\nRegards\nJ"

                    // Recipient email and subject
                    val recipientEmail = "joshnadevi12@gmail.com"
                    val subject = "Emergency Alert"

                    // Create email intent
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, recipientEmail)
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, message)
                    }

                    // Check if there is an email app on the device
                    if (intent.resolveActivity(packageManager) != null) {
                        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("joshnarakonda@gmail.com"))
                        startActivity(intent)

                        // Delay the instruction by 2 seconds
                        Handler().postDelayed({
                            val emailInstruction =
                                "Click on send on the top right of the screen to send."
                            textToSpeech.speak(
                                emailInstruction,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        }, 2000)
                    } else {
                        // Display a message if no email app is found
                        Toast.makeText(
                            this,
                            "No email app found on the device",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }


    // Start Speech Recognition after a 5 sec delay
    private fun startDestinationSpeechRecognition() {
        Handler().postDelayed({
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the destination")
            startActivityForResult(intent, DESTINATION_SPEECH_REQUEST_CODE)
        }, 4600)
    }

    private fun startConfirmationSpeechRecognition() {
        Handler().postDelayed({
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "yes or record")
            startActivityForResult(intent, CONFIRMATION_SPEECH_REQUEST_CODE)
        }, 5200)
    }

    private fun showConfirmationMessage(destination: String) {
        val confirmationMessage =
            "Your destination is $destination. Please say 'yes' to confirm or 'record' to re-record."
        textToSpeech.speak(confirmationMessage, TextToSpeech.QUEUE_FLUSH, null, null)
        startConfirmationSpeechRecognition()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                DESTINATION_SPEECH_REQUEST_CODE -> {
                    val results =
                        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (!results.isNullOrEmpty()) {
                        val destination = results[0]
                        destinationEditText.setText(destination)
                        showConfirmationMessage(destination)
                    }
                }
                CONFIRMATION_SPEECH_REQUEST_CODE -> {
                    val results =
                        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (!results.isNullOrEmpty()) {
                        val userResponse = results[0].toLowerCase(Locale.getDefault())
                        when {
                            userResponse.contains("yes") -> {
                                // Start navigation
                                startNavigation()
                            }
                            userResponse.contains("record") -> {
                                // Allow the user to re-record destination
                                startDestinationSpeechRecognition()
                            }
                            else -> {
                                // Handle unrecognized response, prompt again
                                showConfirmationMessage(destinationEditText.text.toString())
                            }
                        }
                    }
                }
            }
        }
    }

    //Start Navigation with Google Maps
    private fun startNavigation() {
        val navigatingMessage = "Click bottom left of your screen to start your navigation."
        textToSpeech.speak(navigatingMessage, TextToSpeech.QUEUE_FLUSH, null, null)

        Handler().postDelayed({
            val destination = destinationEditText.text.toString()
            val mapsUrl =
                "https://www.google.com/maps/dir/?api=1&mode=walking&destination=" + Uri.encode(
                    destination
                )
            val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
            startActivity(mapsIntent)
        }, 2000)
    }


    // Callback when the map is ready
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        // Get last known location and move camera to that location
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        val location = locationManager.getLastKnownLocation(
            locationManager.getBestProvider(criteria, false)
                .toString()
        )
        if (location != null) {
            val latLng = LatLng(location.latitude, location.longitude)
            googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Current Location")
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }
}





