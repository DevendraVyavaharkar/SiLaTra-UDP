package team_silatra.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup.LayoutParams;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

/**
 * Created by Team Silatra on 06-Mar-18.
 */

public class CameraActivity extends AppCompatActivity{

    private Camera mCamera;
    private CameraPreview mPreview;
    Button captureButton,signModeButton,gestureModeButton;
    TextView OPTextView, rttTextView, imageSizeTextView, fpsTextView;

    SharedPreferences sharedpreferences;
    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String IP1 = "ip1Key";
    public static final String IP2 = "ip2Key";
    public static final String IP3 = "ip3Key";
    public static final String IP4 = "ip4Key";
    public static final String Port = "portKey";
    public static final String DirectConnection = "directConnectionKey";


    InetAddress serverAddr;
    int port;
    boolean directConnectionEnabled;
    Socket tcpSocket;
    String serverUrl;


    private boolean hasFlash;
    private boolean isFlashOn;
    private boolean isTransmiting;
    private boolean connectionStateChanging = false;


    public TCPReceiveText tcpReceive;
    public TCPSendPicture tcpSend;


    RequestQueue queue;
    public String lastTxt = "";
    TextToSpeech textToSpeech;

    TimeTracker timeTracker = new TimeTracker();

    int countSent = 0, countReceived = 0;

    public String recognitionMode = "SIGN";


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //To enable fullscreen mode -
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.camera_activity);

        //LayoutInflater is used. The camera_overlay.xml layout is used over the Camera Preview layout
        LayoutInflater inflater = LayoutInflater.from(this);
        final View theInflatedView = inflater.inflate(R.layout.camera_overlay, null);
        LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        this.addContentView(theInflatedView, layoutParamsControl);

        // To check if the device has flash
        hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        OPTextView = findViewById(R.id.textview_output);
        imageSizeTextView = findViewById(R.id.imageSize_TextView);
        fpsTextView = findViewById(R.id.fps_TextView);
        rttTextView = findViewById(R.id.RTT_TextView);
        signModeButton = findViewById(R.id.signModeButton);
        gestureModeButton = findViewById(R.id.gestureModeButton);

        isTransmiting = false;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //Initialize TTS
        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });


        // Change Media volume instead of ringtone volume when hardware buttons are clicked
        // Reference: https://stackoverflow.com/a/13304713/5370202
        setVolumeControlStream(AudioManager.STREAM_MUSIC);


        //To prevent "at android.os.StrictMode$AndroidBlockGuardPolicy.onNetwork" exception from getting thrown
        //Reference: https://stackoverflow.com/a/22395546/5370202
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);



        //Fetch stored preferences of IP address, port number
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);   //Shared Preferences
        try {
            serverAddr = InetAddress.getByName(sharedpreferences.getString(IP1,null)+"."+
                    sharedpreferences.getString(IP2,null)+"."+
                    sharedpreferences.getString(IP3,null)+"."+
                    sharedpreferences.getString(IP4,null));
            port = sharedpreferences.getInt(Port,0);
            directConnectionEnabled = sharedpreferences.getBoolean(DirectConnection, false);

        } catch (UnknownHostException e) {
            Log.e("SilatraException","Message:"+e.toString());
        }


        updateViewBasedOnPreferences();



        // Instantiate the RequestQueue for http requests.
        queue = Volley.newRequestQueue(this);
        serverUrl ="http:/"+serverAddr+":5000/get-port-number?recognitionMode=";  //serverAddr returns as '/192.168.2.5', hence, string has 'http:/'



        // Add a listener to the Capture button
        captureButton = theInflatedView.findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    captureButton.setEnabled(false);
                    if(!isTransmiting) {
                        connectionStateChanging = true;
                        findViewById(R.id.loaderGif).setVisibility(View.VISIBLE);
                        findViewById(R.id.loaderBg).setVisibility(View.VISIBLE);
                        Toast.makeText(getApplicationContext(),"Establishing connection with server...",Toast.LENGTH_SHORT).show();

                        //This will fetch port no of new server socket created from python server
                        new establishServerConnection().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


                    } else{
                        connectionStateChanging = true;
                        new CloseConnectionAsync().execute();
                        tcpReceive.cancel(true);
                        tcpSend.cancel(true);
                        isTransmiting=false;
                        captureButton.setBackgroundColor(Color.parseColor("#0ba8ef"));
                        captureButton.setText("Capture");
                        captureButton.setEnabled(true);
                        timeTracker.resetTSQ();
                    }
                    changeRecognitionModePanel();
                }
            }
        );


        // Flash Toggle Button
        final FloatingActionButton toggleFlash = theInflatedView.findViewById(R.id.flashToggle);
        toggleFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!hasFlash){
                    Toast.makeText(getApplicationContext(),"Your device doesn't have Flashlight!",
                            Toast.LENGTH_LONG).show();
                }
                else if(isFlashOn){
                    Camera.Parameters params = mCamera.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(params);
                    mCamera.startPreview();
                    toggleFlash.setBackgroundTintList(ColorStateList.valueOf(0x9aeeeeee));
                    isFlashOn = false;
                }
                else{
                    Camera.Parameters params = mCamera.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(params);
                    mCamera.startPreview();
                    toggleFlash.setBackgroundTintList(ColorStateList.valueOf(0x9a4aff41));
                    isFlashOn = true;
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        //Reference: https://stackoverflow.com/a/31596288/5370202
        if(connectionStateChanging){
            Toast.makeText(getApplicationContext(),"Wait for connection state to change",Toast.LENGTH_SHORT).show();
        }
        else{
            super.onBackPressed();
        }
    }

    public void updateViewBasedOnPreferences(){
        if(directConnectionEnabled){
            findViewById(R.id.modeSelector).setVisibility(View.GONE);
        }
    }

    public void changeRecognitionModePanel(){
        if(isTransmiting){
//            ((LinearLayout)findViewById(R.id.modeSelector)).setWeightSum(1);
            if(recognitionMode.equals("SIGN"))
                gestureModeButton.setVisibility(View.GONE);
            else
                signModeButton.setVisibility(View.GONE);

            signModeButton.setEnabled(false);
            gestureModeButton.setEnabled(false);
        }
        else{
//            ((LinearLayout)findViewById(R.id.modeSelector)).setWeightSum(2);
            gestureModeButton.setVisibility(View.VISIBLE);
            signModeButton.setVisibility(View.VISIBLE);

            signModeButton.setEnabled(true);
            gestureModeButton.setEnabled(true);
        }
    }

    public void switchRecognitionMode(View view){

        // Check which button was clicked
        switch(view.getId()) {
            case R.id.signModeButton:
                recognitionMode = "SIGN";
                signModeButton.setTextColor(getResources().getColor(R.color.selectedModeBtnFG));
                signModeButton.setShadowLayer(15,1,1,getResources().getColor(R.color.selectedModeBtnBG));
                gestureModeButton.setTextColor(getResources().getColor(R.color.normalModeBtnFG));
                gestureModeButton.setShadowLayer(5,1,1,getResources().getColor(R.color.normalModeBtnBG));
                break;
            case R.id.gestureModeButton:
                recognitionMode = "GESTURE";
                signModeButton.setTextColor(getResources().getColor(R.color.normalModeBtnFG));
                signModeButton.setShadowLayer(5,1,1,getResources().getColor(R.color.normalModeBtnBG));
                gestureModeButton.setTextColor(getResources().getColor(R.color.selectedModeBtnFG));
                gestureModeButton.setShadowLayer(15,1,1,getResources().getColor(R.color.selectedModeBtnBG));
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if(isTransmiting) {
            new CloseConnectionAsync().execute();
        }
        //mCamera.release();
        timeTracker.resetTSQ();
    }



    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            List<Camera.Size> ls= c.getParameters().getSupportedPreviewSizes();  //Reference: https://stackoverflow.com/a/8385634/5370202
            boolean flagResoExists =false;
            for(Camera.Size s:ls){
                if(s.width == 640 && s.height == 480){
                    flagResoExists = true;
                }
            }

            if(flagResoExists){
                Log.d("Size of image","Size of image set as 640*480");
                Camera.Parameters params = c.getParameters();
                params.setPreviewSize(640,480);
                List<Integer> ls1 = params.getSupportedPreviewFrameRates();

                //params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                c.setParameters(params);
                c.setDisplayOrientation(90);
            }
        }
        catch (Exception e){} // Camera is not available (in use or does not exist)
        return c; // returns null if camera is unavailable
    }

    private class TCPSendPicture extends AsyncTask<Void,Void,Void>{

        DataOutputStream tcpOutput;

//        int imgCtr = 0;
//        int customFrameRate = 30;
//        int frameNoThresh = 30 / customFrameRate;

        @Override
        public Void doInBackground(Void ...params){

            try {
                tcpSocket = new Socket(serverAddr,port);

                /**
                 * Problem being faced was:
                 *  On Marshmallow-device, the writeInt function was sending 4 bytes, but on Nougat-device,
                 *  it was sending only 1 byte. (Checked this from Wireshark).
                 *  Still don't understand the exact problem, but adding BufferedOutputStream,
                 *  fixed it for reasons mentioned in the reference.
                 *
                 * Reference: https://stackoverflow.com/a/39460558/5370202
                 *
                 * Old Code: tcpOutput = new DataOutputStream(tcpSocket.getOutputStream());
                 * New Code: tcpOutput = new DataOutputStream(new BufferedOutputStream(tcpSocket.getOutputStream()));
                 */
                tcpOutput = new DataOutputStream(new BufferedOutputStream(tcpSocket.getOutputStream()));
            }catch (UnknownHostException e){
                Log.e("SilatraWrong IP:", "Error:", e);
            }catch(SocketException e) {
                Log.e("SilatraSocket Open:", "Error:", e);
            }catch(IOException e){
                Log.e("SilatraIOException", "Error:", e);
            }


            tcpReceive = new TCPReceiveText();
            tcpReceive.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                int width,height;
                byte[] imageBytes;

                @Override
                public void onPreviewFrame(byte[] imgBytes, Camera camera) {
                    if(isCancelled())
                        return;

                    try{
                        //Capturing the frame once every five times
//                        imgCtr = (imgCtr + 1)%(frameNoThresh);
//                        if(timeTracker.timestampQueue.size()>4 && (imgCtr) != 0){
//                            return;
//                        }

                        if(timeTracker.timestampQueue.size()>4){
                            return;
                        }

                        Camera.Parameters parameters = camera.getParameters();
                        width = parameters.getPreviewSize().width;
                        height = parameters.getPreviewSize().height;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        YuvImage yuvImage = new YuvImage(imgBytes, ImageFormat.NV21, width, height, null);
                        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, out);
                        imageBytes = out.toByteArray();
                        Log.d("SilatraSize of byte arr",""+imageBytes.length);

                        tcpOutput.writeInt(imageBytes.length); //Send image size
                        Log.d("SilatraSize of Image",""+imageBytes.length);
                        tcpOutput.write(imageBytes,0,imageBytes.length); //Send image
                        tcpOutput.flush();


                        //Record start of RTT measurement
                        Long tsLong = System.currentTimeMillis();
                        fpsTextView.setText(timeTracker.addNewStartTimestamp(tsLong)+" fps");


                        imageSizeTextView.setText(imageBytes.length+" B");
                        countSent++;

                        Log.d("SilatraTransmission", "Message Sent successfully");
                        Thread.sleep(100);
                    } catch (NetworkOnMainThreadException |IOException | InterruptedException e){
                        //Toast.makeText(CameraActivity.this, "NetworkOnMainThreadException", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            });
            return null;
        }


    }

    private class TCPReceiveText extends AsyncTask<Void,String,Void>{

        String results;

        InputStream in;
        BufferedReader br;

        @Override
        protected Void doInBackground(Void ...params){

            try{
                in = tcpSocket.getInputStream();
                br = new BufferedReader(new InputStreamReader(in));
                Log.d("SilatraTCP Input","Connected to Socket's input stream");

                while(true)
                {
                    if(isCancelled())
                        break;
                    try
                    {
                        Log.d("SilatraReceiver","Waiting to get input");
                        results=br.readLine();
                        Log.d("SilatraMessage Received",""+results);
                        if(results!= null){
                            Log.d("SilatraTCP Message Rec","message received:" + results);
                            publishProgress(results);
                         }
                         else{
                            Log.d("SilatraTCP Message Rec","Empty message received");
                            publishProgress("No Sign Detected");
                        }
                    }
                    catch (SocketException e) {
                        Log.e("Socket Open:", "Error:", e);
                        break;
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            Log.d("Silatra",values[0]);
            if(!lastTxt.equals(values[0]+"")){
                OPTextView.setText(values[0]+"");
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && (!(values[0]+"").equals("--") ) ){
                    textToSpeech.speak(values[0] + "", TextToSpeech.QUEUE_FLUSH, null, null);
                }
                lastTxt = values[0]+"";
            }



            //Record end of RTT measurement
            Long tsLong = System.currentTimeMillis();
            Long tsEnd = timeTracker.fetchRTT(tsLong);
            if(tsEnd!=null){
                rttTextView.setText("RTT:"+tsEnd+" ms");
            }
            Log.d("SilatraQSize",timeTracker.timestampQueue.size()+"");

            countReceived++;

        }

    }

    private class CloseConnectionAsync extends AsyncTask<Void,String,Void>{

        @Override
        protected Void doInBackground(Void ...params){
            try{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.loaderGif).setVisibility(View.VISIBLE);
                        findViewById(R.id.loaderBg).setVisibility(View.VISIBLE);
                    }
                });
                /**
                 * Problem being faced was:
                 *  On Marshmallow-device, the writeInt function was sending 4 bytes, but on Nougat-device,
                 *  it was sending only 1 byte. (Checked this from Wireshark).
                 *  Still don't understand the exact problem, but adding BufferedOutputStream,
                 *  fixed it for reasons mentioned in the reference.
                 *
                 * Reference: https://stackoverflow.com/a/39460558/5370202
                 *
                 * Old Code: new DataOutputStream(tcpSocket.getOutputStream()).writeInt(0);
                 * New Code: new DataOutputStream(new BufferedOutputStream(tcpSocket.getOutputStream())).writeInt(0);
                 */
                new DataOutputStream(new BufferedOutputStream(tcpSocket.getOutputStream())).writeInt(0);
                Log.d("Silatra","Sent quit indication");
                Log.d("SilatraQSize",timeTracker.timestampQueue.size()+"");
                Log.d("Silatra","Sent:"+countSent+", Received:"+countReceived);

            }catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new Timer().schedule(new TimerTask(){

                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageSizeTextView.setText("Size: (Bytes)");
                            fpsTextView.setText("X fps");
                            rttTextView.setText("RTT: (ms)");
                            OPTextView.setText("--");
                            findViewById(R.id.loaderGif).setVisibility(View.GONE);
                            findViewById(R.id.loaderBg).setVisibility(View.GONE);
                        }
                    });

                    tcpReceive.cancel(true);
                    tcpSend.cancel(true);
                    connectionStateChanging = false;

                }
            },1000);

        }
    }

    private class establishServerConnection extends AsyncTask<Void,String,Void>{
        @Override
        protected Void doInBackground(Void ...params){

            if(directConnectionEnabled){  // This branch will be executed if direct Connection has been enabled by app user

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tcpSend = new TCPSendPicture();
                        tcpSend.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                        //Change local properties to indicate start of transmission
                        isTransmiting=true;
                        captureButton.setBackgroundColor(Color.RED);
                        captureButton.setText("Stop");

                        findViewById(R.id.loaderGif).setVisibility(View.GONE);
                        findViewById(R.id.loaderBg).setVisibility(View.GONE);
                        captureButton.setEnabled(true);

                        Toast.makeText(getApplicationContext(),"Connection established with server",Toast.LENGTH_SHORT).show();
                        connectionStateChanging = false;
                    }
                });


            }
            else{  // This branch will be executed if direct Connection has not been enabled by app user
                // Request a string response from the provided URL.
                final StringRequest stringRequest = new StringRequest(Request.Method.GET, serverUrl+recognitionMode,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.d("Silatra","Server response is: "+response);
                                port = Integer.parseInt(response);

                                //Reference: http://envyandroid.com/android-delayed-tasks/
                                //This delay is added so as to compensate for the time required by the server to actually start the server socket
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                tcpSend = new TCPSendPicture();
                                                tcpSend.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                                                //Change local properties to indicate start of transmission
                                                isTransmiting=true;
                                                captureButton.setBackgroundColor(Color.RED);
                                                captureButton.setText("Stop");

                                                findViewById(R.id.loaderGif).setVisibility(View.GONE);
                                                findViewById(R.id.loaderBg).setVisibility(View.GONE);
                                                captureButton.setEnabled(true);

                                                Toast.makeText(getApplicationContext(),"Connection established with server",Toast.LENGTH_LONG).show();

                                                connectionStateChanging = false;

                                            }
                                        });

                                    }
                                },5000);
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.loaderGif).setVisibility(View.GONE);
                                findViewById(R.id.loaderBg).setVisibility(View.GONE);
                                captureButton.setEnabled(true);
                                Log.d("Silatra",error+"");

                                Toast.makeText(getApplicationContext(),"Unreachable Server!",Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                });
                // Add the request to the RequestQueue.
                queue.add(stringRequest);
            }

            return null;
        }
    }

}

class TimeTracker{
    public Queue<Long> timestampQueue = new LinkedList<Long>();
    public Long lastTS = -1L;

    public String addNewStartTimestamp(Long start){
        String fps = "X";
        timestampQueue.add(start);
        if(lastTS!=-1L){
            fps =  String.format("%.2f", 1000/(double)(start - lastTS));
        }
        lastTS = start;
        return fps;
    }
    
    
    synchronized public Long fetchRTT(Long end){
        Long start = timestampQueue.poll();
        if(start == null){
            return null;
        }
        return end-start;
    }
    
    synchronized public void resetTSQ(){
        timestampQueue = new LinkedList<Long>();
    }
}

/** A basic Camera preview class */
class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        Camera.Parameters parameters=mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); //This is for auto-focus
        parameters.setPictureSize(640,480);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        // mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.e(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

}
