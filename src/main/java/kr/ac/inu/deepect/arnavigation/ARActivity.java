package kr.ac.inu.deepect.arnavigation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.skt.Tmap.TMapPoint;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import kr.ac.inu.deepect.R;
import kr.ac.inu.deepect.arnavigation.navigation.GpsManager;
import kr.ac.inu.deepect.arnavigation.rendering.LocationNode;
import kr.ac.inu.deepect.arnavigation.rendering.LocationNodeRender;
import kr.ac.inu.deepect.arnavigation.utils.ARLocationPermissionHelper;

public class ARActivity extends AppCompatActivity {
    private boolean installRequested;
    private boolean hasFinishedLoading[] = { false, false };

    private RelativeLayout container;

    private ArFragment fragment;

    private ArSceneView arSceneView;

    // Our ARCore-Location scene
    private LocationScene locationScene;
    // Renderables for this example
    private ModelRenderable arrowRenderable;
    private ModelRenderable myArrowRenderable;
    private ModelRenderable targetRenderable;

    private static final String TAG = "LocationActivity";

    private static TMapPoint destination;
    // private static int middleLength;

    public static void setDestination(@NotNull TMapPoint dest) {
        destination = dest;
    }

    private static class LatLonDesc {
        private double latitude;
        private double longitude;
        private String description;

        LatLonDesc(double latitude, double longitude, String description) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.description = description;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public String getDescription() {
            return description;
        }
    }

    private static List<LatLonDesc> middleNodes = null;

    private static List<String> descriptions = null;

    private int descIndex = 0;

    public static void clearMiddleNodes() {
        if (middleNodes != null) {
            middleNodes = null;
        }
        middleNodes = new ArrayList<LatLonDesc>();
    }

    public static void setMiddleNodes(@NotNull double lat, double lon, String desc) {
        LatLonDesc node = new LatLonDesc(lat, lon, desc);
        middleNodes.add(node);
    }

    public void clearDescriptions() {
        if (descriptions != null) {
            descriptions = null;
        }
        descriptions = new ArrayList<String>();
    }

    public GpsManager gpsMan;

    private boolean doesBlink = true;

    /*
    private LatLon points[] = {
            new LatLon(37.488760, 126.704996),
            new LatLon(37.487507, 126.705703),
            new LatLon(37.487132, 126.705671),
            new LatLon(37.487055, 126.707355),
            new LatLon(37.486502, 126.707634),
            new LatLon(37.484612, 126.707891),
            new LatLon(37.483718, 126.707237),
            new LatLon(37.483505, 126.707613)
    };
    */

    private String generateFilename() {
        String date =
                new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(new Date());
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + File.separator + "Sceneform/" + date + "_screenshot.jpg";
    }

    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {

        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void takePhoto() {
        Toast.makeText(this, "사진 처리 중입니다. 잠시만 기다려주세요.", Toast.LENGTH_LONG).show();
        final String filename = generateFilename();
        ArSceneView view = arSceneView;

        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(ARActivity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
////                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
////                        "성공적으로 캡쳐되었습니다.", Snackbar.LENGTH_LONG);
////                snackbar.setAction("사진 보기", v -> {
////                    File photoFile = new File(filename);
////
////                    Uri photoURI = FileProvider.getUriForFile(ARActivity.this,
////                            ARActivity.this.getPackageName() + ".ar.codelab.name.provider",
////                            photoFile);
////                    Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
////                    intent.setDataAndType(photoURI, "image/*");
////                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
////                    startActivity(intent);
//                });
//                snackbar.show();

                File photoFile = new File(filename);

                Uri photoURI = FileProvider.getUriForFile(ARActivity.this,
                            ARActivity.this.getPackageName() + ".ar.codelab.name.provider",
                            photoFile);
                Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                intent.setDataAndType(photoURI, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } else {
                Toast toast = Toast.makeText(ARActivity.this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ar_main);
        container = (RelativeLayout)findViewById(R.id.ar_container);
//        fragment = (ArFragment)
//                getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        arSceneView = findViewById(R.id.ar_scene_view);
        ViewRenderable roadsignLayoutRenderables[] = new ViewRenderable[middleNodes.size()-1];
        clearDescriptions();
        Toast.makeText(this, "거리가 멀어 보이지 않는 지표가 있을 수 있습니다.", Toast.LENGTH_LONG).show();

        for (int i = 0; i < middleNodes.size(); i++) {
            String desc = middleNodes.get(i).getDescription();
            desc = desc.replace(" 을 ", "를(을) ");
            descriptions.add(desc);
            Log.d(TAG, "kmyLog, desc : " + i + ", " + desc);
        }

        TextView descView = findViewById(R.id.descView);
        TextView descIndexView = findViewById(R.id.descIndexView);
        TextView correctionView = findViewById(R.id.correctionView);
        correctionView.setVisibility(View.INVISIBLE);

        descIndexView.setText(String.valueOf(descIndex + 1));
        descView.setText(descriptions.get(descIndex));

        Button btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (descIndex == descriptions.size() - 1) {
                    Toast.makeText(ARActivity.this, "마지막 지표입니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                else if (descIndex == descriptions.size()-2) {
                    descIndex++;
                    descIndexView.setVisibility(View.INVISIBLE);
                    descView.setText(descriptions.get(descIndex));
                }
                else {
                    descIndex++;
                    descIndexView.setText(String.valueOf(descIndex + 1));
                    descView.setText(descriptions.get(descIndex));
                }
            }
        });

        Button btnPrevious = findViewById(R.id.btnPrevious);
        btnPrevious.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (descIndex == 0) {
                    Toast.makeText(ARActivity.this, "첫번째 지표입니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                else if (descIndex == descriptions.size()-1) {
                    descIndexView.setVisibility(View.VISIBLE);
                    descIndex--;
                    descIndexView.setText(String.valueOf(descIndex + 1));
                    descView.setText(descriptions.get(descIndex));
                }
                else {
                    descIndex--;
                    descIndexView.setText(String.valueOf(descIndex + 1));
                    descView.setText(descriptions.get(descIndex));
                }
            }
        });

        Button btnCapture = findViewById(R.id.btnCapture);
        btnCapture.setVisibility(View.INVISIBLE);
        btnCapture.setOnClickListener(view -> takePhoto());

        Button btnReturn = findViewById(R.id.btnReturn);
        btnReturn.setVisibility(View.INVISIBLE);

        Button btnCorrection = findViewById(R.id.btnCorrection);
        btnCorrection.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                doesBlink = false;
                descView.setVisibility(View.INVISIBLE);
                descIndexView.setVisibility(View.INVISIBLE);
                btnNext.setVisibility(View.INVISIBLE);
                btnPrevious.setVisibility(View.INVISIBLE);
                btnCorrection.setVisibility(View.INVISIBLE);
                correctionView.setVisibility(View.VISIBLE);
                btnReturn.setVisibility(View.VISIBLE);
                btnCapture.setVisibility(View.VISIBLE);
            }
        });

        btnReturn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                doesBlink = true;
                descView.setVisibility(View.VISIBLE);
                descIndexView.setVisibility(View.VISIBLE);
                btnNext.setVisibility(View.VISIBLE);
                btnPrevious.setVisibility(View.VISIBLE);
                btnCorrection.setVisibility(View.VISIBLE);
                correctionView.setVisibility(View.INVISIBLE);
                btnReturn.setVisibility(View.INVISIBLE);
                btnCapture.setVisibility(View.INVISIBLE);
            }
        });

                // Build a renderable from a 2D View.
        // sceneform의 모든 build() 메소드는 CompleableFuture를 반환한다
//        CompletableFuture<ViewRenderable> exampleLayout = // "미래에 처리할 업무(Task)로서,  Task 결과가 완료되었을때 값을 리턴하거나, 다른 Task가 실행되도록 발화(trigger)시키는 Task."
//                ViewRenderable.builder()
//                        .setView(this, R.layout.roadsign_layout)
//                        .build();

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        CompletableFuture<ViewRenderable> exampleFutures[] = new CompletableFuture[middleNodes.size()-1];
        for (int i = 0; i < exampleFutures.length; i++) {
            exampleFutures[i] = ViewRenderable.builder()
                    .setView(this, R.layout.roadsign_layout)
                    .build();
        }
        CompletableFuture<ModelRenderable> arrowFuture = ModelRenderable.builder()
                .setSource(this, R.raw.arrow)
                .build();
        CompletableFuture<ModelRenderable> myArrowFuture = ModelRenderable.builder()
                .setSource(this, R.raw.myarrow)
                .build();
        CompletableFuture<ModelRenderable> targetFuture = ModelRenderable.builder()
                .setSource(this, R.raw.target)
                .build();

        CompletableFuture.allOf(
                arrowFuture,
                myArrowFuture,
                targetFuture)
                .handle(
                        (notUsed, throwable) -> {
                            // When you build a Renderable, Sceneform loads its resources in the background while
                            // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                            // before calling get().

                            if (throwable != null) {
                                DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }

                            try {
                                arrowRenderable = arrowFuture.get();
                                myArrowRenderable = myArrowFuture.get();
                                targetRenderable = targetFuture.get();
                                hasFinishedLoading[0] = true;
                            } catch (InterruptedException | ExecutionException ex) {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }
                            return null;
                        });

        CompletableFuture.allOf(
                exampleFutures)
                .handle(
                        (notUsed, throwable) -> {
                            // When you build a Renderable, Sceneform loads its resources in the background while
                            // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                            // before calling get().

                            if (throwable != null) {
                                DemoUtils.displayError(this, "Unable to load renderables", throwable);
                                return null;
                            }

                            try {
                                for (int i = 0; i < exampleFutures.length; i++) {
                                    roadsignLayoutRenderables[i] = exampleFutures[i].get();
                                }
                                hasFinishedLoading[1] = true;
                            } catch (InterruptedException | ExecutionException ex) {
                                DemoUtils.displayError(this, "Unable to load renderables", ex);
                            }
                            return null;
                        });


        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView
                .getScene()
                .addOnUpdateListener(frameTime -> {
                    if (!hasFinishedLoading[0] || !hasFinishedLoading[1]) {
                        return;
                    }
                    if (locationScene == null) {
                        // Adding a simple location marker of a 3D model
                        locationScene = new LocationScene(this, arSceneView);
                        if (locationScene.mLocationMarkers.size() > 0) {
                            locationScene.mLocationMarkersClear();
                        }
                        LocationMarker camera;
                        {
                            Node node = new Node();
                            camera = createLocationMarker(0, 0, node);
                            camera.setAtCameraPosition(true);
                            locationScene.mLocationMarkers.add(camera);
                        }
                        LocationMarker prevLocationMarker = null;
//
//                        for (int i = 0; i < middleNodes.size(); i++) {
//                            // if (gpsMan.getCurrentLocation().getLongitude())
//                            LatLon point = middleNodes.get(i);
//                            /*
//                            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                            Log.i(TAG, "now Lat & Lon : " + location.getLatitude() + ", " + location.getLongitude() + ", Accuracy : " + location.getAccuracy());
//                            Log.i(TAG, "kmyLog2 : " + (SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()));
//                            if (distFrom(location.getLatitude(), location.getLongitude(), point.getLatitude(), point.getLongitude()) > 100)
//                                continue;
//                            */
//                            Node node = new Node();
//                            LocationMarker locationMarker = createLocationMarker(
//                                    point.getLatitude(), point.getLongitude(), node);
//                            // ~m 이내의 마커만 표시.
//                            locationMarker.setOnlyRenderWhenWithin(500);
//                            // 이전 노드가 현재 노드의 방향을 가리키도록 함.
//                            prevLocationMarker.setLookNode(node);
//                            prevLocationMarker = locationMarker;
//
//                            // 타겟 마커는 현재 points의 가장 마지막 원소만 rendering 하도록.
//                            /* node.setRenderable((i < middleNodes.size()) ?
//                                    arrowRenderable : targetRenderable); */
//                            node.setRenderable(arrowRenderable);
//                            locationScene.mLocationMarkers.add(locationMarker);
//                        }
//                        Node node = new Node();
//                        LocationMarker locationMarker = createLocationMarker(destination.getLatitude(), destination.getLongitude(), node);
//                        prevLocationMarker.setLookNode(node);
//                        prevLocationMarker = locationMarker;
//                        node.setRenderable(targetRenderable);
//                        LocationScene.mLocationMarkers.add(locationMarker);
                        for (int i = 0; i < middleNodes.size()-1; i++) {
                            final int finalI = i;
                            LatLonDesc point = middleNodes.get(i);

                            Node node = getExampleView(roadsignLayoutRenderables[i]);
                            LocationMarker layoutLocationMarker = createLocationMarker(
                                    point.getLatitude(), point.getLongitude(), node);
                            layoutLocationMarker.setCameraNode(camera.node);

                            if (prevLocationMarker != null) {
                                prevLocationMarker.setLookNode(node);
                            }
                            prevLocationMarker = layoutLocationMarker;

                            layoutLocationMarker.setScalingMode(LocationMarker.ScalingMode.SIMPLE_SCALING);
                            layoutLocationMarker.setGradualScalingMaxScale(4F);
                            layoutLocationMarker.setGradualScalingMinScale(1F);
                            layoutLocationMarker.setOnlyRenderWhenWithin(400);

                            layoutLocationMarker.setRenderEvent(new LocationNodeRender() {
                                @Override
                                public void render(LocationNode node) {
                                    ViewRenderable roadsignLayoutRendarable = roadsignLayoutRenderables[finalI];
                                    // Log.d(TAG, "kmyLog, in render nodeId : " + node);
                                    double angle = node.getAngle(layoutLocationMarker.cameraNode, layoutLocationMarker.node, layoutLocationMarker.nodeToLook);
                                    View eView = roadsignLayoutRendarable.getView();
                                    TextView roadsignTextView = eView.findViewById(R.id.textView);
                                    roadsignTextView.setTypeface(null, Typeface.BOLD);
                                    int index = finalI + 1;
                                    String indexString = Integer.toString(index);
                                    String arrow;
//                                    if (angle >= 30 && angle < 60)
//                                        arrow = "↗";
//                                    else if (angle <= -30 && angle > -60)
//                                        arrow = "↖";
//                                    else if (angle >= 60 && angle < 120)
//                                        arrow = "→";
//                                    else if (angle <= -60 && angle > -120)
//                                        arrow = "←";
//                                    else if (angle < 30 && angle > -30)
//                                        arrow = "↑";
//                                    else if (angle >= 120 && angle <= 180)
//                                        arrow = "↘";
//                                    else if (angle <= -120 && angle >= -180)
//                                        arrow = "↙";
//                                    else
//                                        arrow = "";
                                    if (angle >= 30)
                                        arrow = "→";
                                    else if (angle <= -30)
                                        arrow = "←";
                                    else
                                        arrow = "↑";
                                    roadsignTextView.setText(indexString + '\n' +  arrow);
                                }
                            });
                            // Adding the marker
                            layoutLocationMarker.lookCamera(true);
                            locationScene.mLocationMarkers.add(layoutLocationMarker);
                        }
                        Node node = new Node();
                        LocationMarker locationMarker = createLocationMarker(destination.getLatitude(), destination.getLongitude(), node);
                        node.setRenderable(targetRenderable);
                        locationMarker.setScalingMode(LocationMarker.ScalingMode.SIMPLE_SCALING);
                        locationMarker.setGradualScalingMaxScale(4F);
                        locationMarker.setGradualScalingMinScale(1F);
                        prevLocationMarker.setLookNode(node);
                        prevLocationMarker = locationMarker;
                        LocationScene.mLocationMarkers.add(locationMarker);
                    }

                    Frame frame = arSceneView.getArFrame();
                    if (frame == null) {
                        return;
                    }
                    if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                        return;
                    }
                    if (locationScene != null) {
                        locationScene.processFrame(frame);
                    }
                });

        // Lastly request CAMERA & fine location permission which is required by ARCore-Location.
        ARLocationPermissionHelper.requestPermission(this);

//        btnCapture.setOnClickListener(new Button.OnClickListener() {
//            public void onClick(View v) {
//                arSceneView.buildDrawingCache();
//                Bitmap captureView = arSceneView.getDrawingCache();
//                FileOutputStream fos;
//                try {
//                    fos = new FileOutputStream(Environment.getExternalStorageDirectory().toString()+"/capture.jpeg");
//                    captureView.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//                Toast.makeText(getApplicationContext(), "캡쳐 후 서버로 전송되었습니다.", Toast.LENGTH_LONG).show();
//            }
//        });
    }

    private LocationMarker createLocationMarker(double latitude, double longitude, Node node) {
        LocationMarker marker = new LocationMarker(latitude, longitude, node);
        marker.setHeight(0);
        return marker;
    }

    private Node getExampleView(ViewRenderable exampleLayoutRenderable) {
        Node base = new Node();
        base.setRenderable(exampleLayoutRenderable);
        Log.d(TAG, "kmyLog : " + base.getRenderable());
        Context c = this;
        // Add  listeners etc here
        View eView = exampleLayoutRenderable.getView();
        eView.setOnTouchListener((v, event) -> {
            Toast.makeText(
                    c, "Location marker touched.", Toast.LENGTH_LONG)
                    .show();
            return false;
        });
        return base;
    }

    /***
     * Example Node of a 3D model
     *
     * @return
     */

    /**
     * Make sure we call locationScene.resume();
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        if (arSceneView.getSession() == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                // installRequested라는 추가 기능을 요청하는 생성자.
                // DemoUtils에서 정의한 자체적 생성자를 사용하여 Session을 생성 중.
                Session session = DemoUtils.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    Config config = session.getConfig();
                    config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
                    session.configure(config);
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                DemoUtils.handleSessionException(this, e);
            }
        }

        try {
            // resume()은 onResume()에서 호출되어야 한다.
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) { // 카메라를 열 수 없을 경우의 예외 처리.
            DemoUtils.displayError(this, "Unable to get camera", ex);
            finish();
        }
    }

    /**
     * Make sure we call locationScene.pause();
     */
    @Override
    public void onPause() {
        super.onPause();

        if (locationScene != null) {
            locationScene.pause();
        }

        // pause() 메소드는 onPause()에서 호출해야 한다.
        arSceneView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }



//    private void blink() {
//        final Handler handler = new Handler();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                int timeToBlink = 800;
//                try {Thread.sleep(timeToBlink);}
//                catch (Exception e) {}
//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            TextView correctionView = (TextView) findViewById(R.id.correctionView);
//                            if (!doesBlink) {
//                                if (correctionView.getVisibility() == View.VISIBLE)
//                                    correctionView.setVisibility(View.INVISIBLE);
//                                else
//                                    correctionView.setVisibility(View.VISIBLE);
//                                blink();
//                            }
//                            else {
//                                correctionView.setVisibility(View.INVISIBLE);
//                            }
//                        }
//                    });
//            }
//        }).start();
//    }

};

//    public static double distFrom(double nowLat, double nowLon, double destLat, double destLon) {
//        double earthRadius = 6371000; //meters
//        double dLat = Math.toRadians(destLat-nowLat);
//        double dLng = Math.toRadians(destLon-nowLon);
//        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
//                Math.cos(Math.toRadians(nowLat)) * Math.cos(Math.toRadians(destLat)) *
//                        Math.sin(dLng/2) * Math.sin(dLng/2);
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
//        double dist = (double) (earthRadius * c);
//
//        Log.i(TAG, "kmyLog, Distance = " + String.valueOf(dist));
//        return dist;
//    }
