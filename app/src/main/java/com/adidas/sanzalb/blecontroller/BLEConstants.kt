package com.adidas.sanzalb.blecontroller

import java.util.*

object BLEConstants {
    const val deviceName = "HMSoft"
    val service : UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    val characteristic : UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    val macAddress : String? = null
}