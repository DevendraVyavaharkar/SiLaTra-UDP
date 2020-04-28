# Sign Language Translator (SiLaTra) Client-Side Android Application

This is the client side of the SiLaTra System. This system is targetted towards the hearing and speech impaired community that use sign language for communicating with each other. But when they communicate with other people outside this community, there is a communication gap. This system is an attempt towards bridging this gap.

Currently, the system supports recognition of:
* **33 hand poses (whose recognition needs only 1 frame):**
    * 23 letters (A-Z except H, J. These 2 letters are conveyed through gestures. Hence, wasn't covered. V hand pose is equivalent to 2, hence not counted in letters)
    * 10 digits (0-9)
* **12 Gestures (whose recognition needs a sequence of at least 5 frames):**
    * After
    * All The Best
    * Apple
    * Good Afternoon
    * Good Morning
    * Good Night
    * I Am Sorry
    * Leader
    * Please Give Me Your Pen
    * Strike
    * That is Good
    * Towards
    
## Sign Language Translator (SiLaTra) Server-Side

The server-side of the system can be found here: [Sign Language Translator (SiLaTra) Server-Side](https://github.com/kartik2112/Silatra)

## App Download

You can try out this app for yourself by downloading it from [here](https://github.com/DevendraVyavaharkar/SiLaTra-UDP/releases/download/6.0/Silatra_28_April_2020.apk). This application is working as of April 28, 2020. Let us know in case of any issues.

## Usage

* Provide the required permissions
* Click the 3 dots at the top-right corner and select Settings
* Now, enter the IP Address of the machine hosting the SiLaTra Flask server.
  * Use Direct Connection if you are directly invoking ``Receiver.py`` instead of ``server.py`` on the SiLaTra server. 
  You can find its documentation [here](https://github.com/kartik2112/Silatra). If you do not understand this, keep it switched off.
* Now, click the message icon on the lower-right corner of the App's home screen. This will start your camera. 
You can switch on or off your Flash using the button provided.
* If you have not selected Direct Connection, you can select the recognition mode: **SIGN** or **GESTURE**.
* Now, click Capture button. After some time, the connection will be established and you can start using the application.

## Screenshots

<img src="/Screenshots/Gesture_Good Morning.jpg" width="220px"/> 

## Demo Videos

The Demo Videos of this application can be found here:

* [Video: Android Application GESTURE Recognition Mode](https://drive.google.com/file/d/1YH6i5OYm3zrSTE-fvWF-zeas9-wiPObo/preview)

* [Video: Android Application SIGN Recognition Mode](https://drive.google.com/file/d/1nrDSmnbonpNWM9grgfg7baCJIL-cQAWX/preview)

* [Video: Deprecated Android Application Client Server Screen Recording GESTURE Recognition Mode](https://drive.google.com/file/d/1KgJbSvABfCukhtKDfdMRi8h6vzNzIKTq/preview)
