package com.adidas.sanzalb.blecontroller

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.polidea.rxandroidble.RxBleClient
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.scan.ScanFilter
import com.polidea.rxandroidble.scan.ScanSettings
import rx.Observable
import rx.Subscription
import java.util.concurrent.TimeUnit
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.ParcelUuid
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.content.DialogInterface
import android.provider.Settings
import android.support.v7.app.AlertDialog


class MainActivity : AppCompatActivity() {

    companion object {
        const val ENABLE_BLUETOOTH = 100
        const val ENABLE_LOCATION = 101
    }

    private val rxBleClient: RxBleClient by lazy { RxBleClient.create(this) }

    private var subscription: Subscription? = null

    private val connection: Observable<RxBleConnection> by lazy {
        rxBleClient.scanBleDevices(
                ScanSettings.Builder().build(),
                ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid(BLEConstants.service))
                        .build()
        )
                .take(1)
                .flatMap { it.bleDevice.establishConnection(false) }
                .subscribeOn(Schedulers.io())
                .retry()
                .share()
    }

    private val characteristic by lazy {
        connection.flatMap { it.getCharacteristic(BLEConstants.characteristic) }
                .retry()
                .share()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH)
        } else if (locationPermissionIsNotGranted()) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    ENABLE_LOCATION)
        } else if (locationIsNotEnabled()){
            AlertDialog.Builder(this)
                    .setTitle(R.string.location_error_title)
                    .setMessage(R.string.location_error_message)
                    .setPositiveButton(
                            R.string.location_error_button,
                            { _, _ ->
                                val intent = Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                startActivity(intent)
                            })
                    .show()
        } else {
            startSendingValues()
        }

    }

    private fun locationPermissionIsNotGranted() =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED

    private fun locationIsNotEnabled() =
            !(getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)



    private fun startSendingValues() {
        val timer = Observable.interval(500, TimeUnit.MILLISECONDS)
                .map { (it % 2) * 255 }
                .map {
                    val value = ByteArray(1)
                    value[0] = it.toByte()
                    value
                }

        subscription = Observable.combineLatest(connection, characteristic, timer) { conn, char, time ->
            conn.writeCharacteristic(char, time)
        }
                .observeOn(Schedulers.io())
                .flatMap { it }
                .map { it[0] != 0.toByte() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { txtConsole.setText(
                                if (it)
                                    R.string.led_state_on
                                else
                                    R.string.led_state_off
                        ) }
                )
    }


    override fun onPause() {
        subscription?.unsubscribe()
        super.onPause()
    }
}
