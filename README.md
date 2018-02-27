# PoC of BLE communication

Just a sample of how to communicate with an HM-10 device

In the other side there is an Arduino connected to the BLE module. It writes the value 
received via BLE into an analog pin that has a led connected (0 => led is off; 255 => led
is fully illuminated; intermediate values => less light)

NOTE: if the connection is lost the app will try to reconnect but the UI **do not** show any
information about the disconnection 

### Technologies used/needed

- Device with Android 4.3 or greater (min version that supports BLE)
- [RxJava](http://reactivex.io/) + [RxAndroidBle](https://github.com/Polidea/RxAndroidBle)
- Kotlin
- An HM-10 module connected to an Arduino (or similar)