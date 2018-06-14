package team_silatra.camera;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup.LayoutParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by Team Silatra on 06-Mar-18.
 */

public class CameraActivity extends AppCompatActivity{

    private Camera mCamera;
    private CameraPreview mPreview;

    SharedPreferences sharedpreferences;
    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String IP1 = "ip1Key";
    public static final String IP2 = "ip2Key";
    public static final String IP3 = "ip3Key";
    public static final String IP4 = "ip4Key";
    public static final String Port = "portKey";

    public static String results;

    private boolean hasFlash;
    private boolean isFlashOn;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //To enable fullscreen mode -
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.camera_activity);

        //LayoutInflater is used. The camera_overlay.xml layout is used over the Camera Preview layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View theInflatedView = inflater.inflate(R.layout.camera_overlay, null);
        LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        this.addContentView(theInflatedView, layoutParamsControl);

        // To check if the device has flash
        hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Add a listener to the Capture button
        Button captureButton = (Button) theInflatedView.findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // get an image from the camera
                    //mCamera.takePicture(null, null, mPicture);
                    //new UDPSendPicture().execute();
                    //new UDPReceiveText().execute();

                        new UDPReceiveText().execute();
                        new UDPSendPicture().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        );

        // Flash Toggle Button
        Button toggleFlash = theInflatedView.findViewById(R.id.flashToggle);
        toggleFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!hasFlash){
                    Toast.makeText(getApplicationContext(),"Your device doesn't have Flashlight!",Toast.LENGTH_LONG).show();
                }
                else if(isFlashOn){
                    Camera.Parameters params = mCamera.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(params);
                    mCamera.startPreview();
                    isFlashOn = false;
                }
                else{
                    Camera.Parameters params = mCamera.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(params);
                    mCamera.startPreview();
                    isFlashOn = true;
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mCamera.release();
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(mCamera ==null)
        {
            setContentView(R.layout.camera_activity);

            // Create an instance of Camera
            mCamera = getCameraInstance();
            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(mPreview);

            //LayoutInflater is used. The camera_overlay.xml layout is used over the Camera Preview layout
            LayoutInflater inflater = LayoutInflater.from(this);
            View theInflatedView = inflater.inflate(R.layout.camera_overlay, null);
            LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
            this.addContentView(theInflatedView, layoutParamsControl);

            // Add a listener to the Capture button
            Button captureButton = (Button) theInflatedView.findViewById(R.id.button_capture);
            captureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new UDPReceiveText().execute();
                            new UDPSendPicture().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }
            );

            // Flash Toggle Button
            Button toggleFlash = theInflatedView.findViewById(R.id.flashToggle);
            toggleFlash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!hasFlash){
                        Toast.makeText(getApplicationContext(),"Your device doesn't have Flashlight!",Toast.LENGTH_LONG).show();
                    }
                    else if(isFlashOn){
                        Camera.Parameters params = mCamera.getParameters();
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        mCamera.setParameters(params);
                        mCamera.startPreview();
                        isFlashOn = false;
                    }
                    else{
                        Camera.Parameters params = mCamera.getParameters();
                        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(params);
                        mCamera.startPreview();
                        isFlashOn = true;
                    }
                }
            });
        }
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(mCamera!=null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
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

    private class UDPSendPicture extends AsyncTask<Void,Void,Void>{

        private DatagramSocket udpSocket;
        private InetAddress serverAddr;
        private int port;
        @Override
        public Void doInBackground(Void ...params){
            byte[] buffer = new byte[65507]; //Buffer

            sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);   //Shared Preferences

            try {
                udpSocket = new DatagramSocket();
                serverAddr = InetAddress.getByName(sharedpreferences.getString(IP1,null)+"."+
                        sharedpreferences.getString(IP2,null)+"."+
                        sharedpreferences.getString(IP3,null)+"."+
                        sharedpreferences.getString(IP4,null));
                port = sharedpreferences.getInt(Port,0);
            }catch (UnknownHostException e){
                Log.e("Wrong IP:", "Error:", e);
            }catch(SocketException e) {
                Log.e("Socket Open:", "Error:", e);
            }


            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                int width,height;
                byte[] imageBytes;

                @Override
                public void onPreviewFrame(byte[] imgBytes, Camera camera) {
                    DatagramPacket packet;
                    try{
                        Camera.Parameters parameters = camera.getParameters();
                        width = parameters.getPreviewSize().width;
                        height = parameters.getPreviewSize().height;
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        YuvImage yuvImage = new YuvImage(imgBytes, ImageFormat.NV21, width, height, null);
                        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, out);
                        imageBytes = out.toByteArray();
                        Log.d("Size of byte array",""+imageBytes.length);

                        packet = new DatagramPacket(imageBytes, imageBytes.length, serverAddr, port);
                        Log.e("Size of packet",""+packet.getLength());
                        udpSocket.send(packet);
                        Log.d("Transmission", "Message Sent successfully");
                    }catch (NetworkOnMainThreadException e){
                        Toast.makeText(CameraActivity.this, "NetworkOnMainThreadException", Toast.LENGTH_SHORT).show();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
            return null;
        }
    }

    private class UDPReceiveText extends AsyncTask<Void,String,Void>{

        String results;

        private DatagramSocket udpSocket;
        private DatagramPacket udpPacket;
        //private InetAddress serverAddr;
        private int port;

        @Override
        protected Void doInBackground(Void ...params){
            byte[] buffer = new byte[100]; //Buffer
            sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);   //Shared Preferences

            try{
                /*serverAddr = InetAddress.getByName(sharedpreferences.getString(IP1,null)+"."+
                        sharedpreferences.getString(IP2,null)+"."+
                        sharedpreferences.getString(IP3,null)+"."+
                        sharedpreferences.getString(IP4,null));*/
                //port = sharedpreferences.getInt(Port,0);
                port = 49160;
                udpSocket = new DatagramSocket(port);
                udpSocket.setBroadcast(true);
                udpPacket = new DatagramPacket(buffer,buffer.length);

                while(true)
                {
                    try
                    {
                        udpSocket.receive(udpPacket);
                        byte[] result=udpPacket.getData();
                        String strResult = (new String(result, StandardCharsets.UTF_8)).trim();
                        Log.d("Udp tutorial","Length of message received:" + strResult.length());
                        Log.d("Udp tutorial","message received:" + strResult);
                        publishProgress(strResult);
                    }
                    catch (SocketException e) {
                        Log.e("Socket Open:", "Error:", e);
                    }
                }
            }/*catch (UnknownHostException e){
                Log.e("Wrong IP:", "Error:", e);
            }catch (SocketException e){
                Log.e("Socket Open:", "Error:", e);
            }*/catch (IOException e){
                e.printStackTrace();
            }finally {
                udpSocket.close();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
            View theInflatedView = inflater.inflate(R.layout.camera_overlay, null);
            TextView txt = (TextView)theInflatedView.findViewById(R.id.textview_output);
            txt.setText(values[0]+"");
        }
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
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
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
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

}
