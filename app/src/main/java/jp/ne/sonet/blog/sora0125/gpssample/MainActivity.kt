package jp.ne.sonet.blog.sora0125.gpssample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import jp.ne.sonet.blog.sora0125.gpssample.LocationActivity
import jp.ne.sonet.blog.sora0125.gpssample.R

import jp.ne.sonet.blog.sora0125.gpssample.R.layout.activity_main

class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "onCreate()")

        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission()
        } else {
            locationActivity()
        }
    }

    // 位置情報許可の確認
    fun checkPermission() {
        // 既に許可している
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            locationActivity()
        } else {
            requestLocationPermission()
        }// 拒否していた場合
    }

    // 許可を求める
    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION
            )

        } else {
            val toast = Toast.makeText(
                this,
                "許可されないとアプリが実行できません", Toast.LENGTH_SHORT
            )
            toast.show()

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION
            )

        }
    }

    // 結果の受け取り
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (requestCode == REQUEST_PERMISSION) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationActivity()

            } else {
                // それでも拒否された時の対応
                val toast = Toast.makeText(
                    this,
                    "これ以上なにもできません", Toast.LENGTH_SHORT
                )
                toast.show()
            }
        }
    }

    // Intent でLocation
    private fun locationActivity() {
        val intent = Intent(application, LocationActivity::class.java)
        startActivity(intent)
    }
}