package jp.ne.sonet.blog.sora0125.gpssample

import android.Manifest
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.location.FusedLocationProviderClient
import android.R.attr.priority
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationResult
import java.text.DateFormat
import java.util.*
//import sun.text.normalizer.UTF16.append
import android.hardware.SensorManager.getAltitude
import android.R.attr.priority
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.LocationSettingsStatusCodes
import android.content.IntentSender
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.ApiException
import android.support.annotation.NonNull
import com.google.android.gms.tasks.OnFailureListener
import android.os.Looper
import android.content.pm.PackageManager
import android.Manifest.permission
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.support.v4.app.ActivityCompat
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.view.View
import android.widget.Button
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task


class LocationActivity : AppCompatActivity() {
    // Fused Location Provider API.
    private var fusedLocationClient: FusedLocationProviderClient? = null

    // Location Settings APIs.
    private var settingsClient: SettingsClient? = null
    private var locationSettingsRequest: LocationSettingsRequest? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private var location: Location? = null

    private var lastUpdateTime: String? = null
    private var requestingLocationUpdates: Boolean? = null
    private val REQUEST_CHECK_SETTINGS = 0x1
    private var priority = 0
    private var textView: TextView? = null
    private var textLog: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        priority = 0

        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()

        textView = findViewById(R.id.text_view)
        textLog = "onCreate()\n"
        textView?.text = textLog

        // 測位開始
        val buttonStart = findViewById<Button>(R.id.button_start)
        buttonStart.setOnClickListener { startLocationUpdates() }

        // 測位終了
        val buttonStop = findViewById<Button>(R.id.button_stop)
        buttonStop.setOnClickListener    { stopLocationUpdates() }

    }

    // locationのコールバックを受け取る
    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                location = locationResult!!.lastLocation

                lastUpdateTime = DateFormat.getTimeInstance().format(Date())
                updateLocationUI()
            }
        }
    }

    private fun updateLocationUI() {
        // getLastLocation()からの情報がある場合のみ
        if (location != null) {
            val fusedName = arrayOf("Latitude", "Longitude", "Accuracy", "Altitude", "Speed", "Bearing")

            val latitude = location?.latitude ?: 0.toDouble()
            val longitude = location?.longitude ?: 0.toDouble()
            val accuracy = location?.accuracy?.toDouble() ?: 0.toDouble()
            val altitude = location?.altitude ?: 0.toDouble()
            val speed = location?.speed?.toDouble() ?: 0.toDouble()
            val bearing = location?.bearing?.toDouble() ?: 0.toDouble()

            val fusedData = doubleArrayOf(
                latitude,
                longitude,
                accuracy,
                altitude,
                speed,
                bearing
            )

            val strBuf = StringBuilder("---------- UpdateLocation ---------- \n")

            for (i in fusedName.indices) {
                strBuf.append(fusedName[i])
                strBuf.append(" = ")
                strBuf.append(fusedData[i].toString())
                strBuf.append("\n")
            }

            strBuf.append("Time")
            strBuf.append(" = ")
            strBuf.append(lastUpdateTime)
            strBuf.append("\n")

            textLog += strBuf
            textView?.text = textLog
        }

    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()

        if (priority == 0) {
            // 高い精度の位置情報を取得したい場合
            // インターバルを例えば5000msecに設定すれば
            // マップアプリのようなリアルタイム測位となる
            // 主に精度重視のためGPSが優先的に使われる
            locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        } else if (priority == 1) {
            // バッテリー消費を抑えたい場合、精度は100mと悪くなる
            // 主にwifi,電話網での位置情報が主となる
            // この設定の例としては　setInterval(1時間)、setFastestInterval(1分)
            locationRequest?.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        } else if (priority == 2) {
            // バッテリー消費を抑えたい場合、精度は10kmと悪くなる
            locationRequest?.priority = LocationRequest.PRIORITY_LOW_POWER

        } else {
            // 受け身的な位置情報取得でアプリが自ら測位せず、
            // 他のアプリで得られた位置情報は入手できる
            locationRequest?.priority = LocationRequest.PRIORITY_NO_POWER
        }

        // アップデートのインターバル期間設定
        // このインターバルは測位データがない場合はアップデートしません
        // また状況によってはこの時間よりも長くなることもあり
        // 必ずしも正確な時間ではありません
        // 他に同様のアプリが短いインターバルでアップデートしていると
        // それに影響されインターバルが短くなることがあります。
        // 単位：msec
        locationRequest?.interval = 60000
        // このインターバル時間は正確です。これより早いアップデートはしません。
        // 単位：msec
        locationRequest?.fastestInterval = 5000

    }

    // 端末で測位できる状態か確認する。wifi, GPSなどがOffになっているとエラー情報のダイアログが出る
    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        locationRequest?.let {locationRequest ->
            builder.addLocationRequest(locationRequest)
            locationSettingsRequest = builder.build()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int, data: Intent?) {
        when (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> Log.i("debug", "User agreed to make required location settings changes.")
                Activity.RESULT_CANCELED -> {
                    Log.i("debug", "User chose not to make required location settings changes.")
                    requestingLocationUpdates = false
                }
            }// Nothing to do. startLocationupdates() gets called in onResume again.
        }
    }

    // FusedLocationApiによるlocation updatesをリクエスト
    private fun startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        settingsClient?.checkLocationSettings(locationSettingsRequest)
            ?.addOnSuccessListener(this,
                OnSuccessListener {
                    Log.i("debug", "All location settings are satisfied.")

                    // パーミッションの確認
                    if (ActivityCompat.checkSelfPermission(
                            this@LocationActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this@LocationActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return@OnSuccessListener
                    }
                    fusedLocationClient?.requestLocationUpdates(
                        locationRequest, locationCallback, Looper.myLooper()
                    )
                })
            ?.addOnFailureListener(this) { e ->
                val statusCode = (e as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.i(
                            "debug",
                            "Location settings are not satisfied. Attempting to upgrade " + "location settings "
                        )
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the
                            // result in onActivityResult().
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(
                                this@LocationActivity,
                                REQUEST_CHECK_SETTINGS
                            )

                        } catch (sie: IntentSender.SendIntentException) {
                            Log.i("debug", "PendingIntent unable to execute request.")
                        }

                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage =
                            "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                        Log.e("debug", errorMessage)
                        Toast.makeText(
                            this@LocationActivity,
                            errorMessage, Toast.LENGTH_LONG
                        ).show()

                        requestingLocationUpdates = false
                    }
                }
            }

        requestingLocationUpdates = true
    }

    private fun stopLocationUpdates() {
        textLog += "onStop()\n"
        textView?.setText(textLog)

        if (requestingLocationUpdates == false) {
            Log.d("debug", "stopLocationUpdates: " +
                    "updates never requested, no-op.")


            return
        }

        fusedLocationClient?.removeLocationUpdates(locationCallback)
                ?.addOnCompleteListener(this) {
                    fun onComplete(task: Task<Void>) {
                        requestingLocationUpdates = false
                    }
                }
    }

    override fun onPause() {
        super.onPause()
        // バッテリー消費を鑑みLocation requestを止める
        stopLocationUpdates()
    }

}
