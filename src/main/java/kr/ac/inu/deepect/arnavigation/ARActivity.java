package kr.ac.inu.deepect.arnavigation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import com.skt.Tmap.TMapPoint;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

    private static class LatLon {
        private double latitude;
        private double longitude;

        LatLon(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
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
    }

    private static List<LatLon> middleNodes = null;

    public static void clearMiddleNodes() {
        if (middleNodes != null) {
            middleNodes = null;
        }
        middleNodes = new ArrayList<LatLon>();
    }

    public static void setMiddleNodes(@NotNull double lat, double lon) {
        LatLon node = new LatLon(lat, lon);

        middleNodes.add(node);
    }

    public GpsManager gpsMan;

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

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ar_main);
        arSceneView = findViewById(R.id.ar_scene_view);
        ViewRenderable exampleLayoutRenderables[] = new ViewRenderable[middleNodes.size()];

        // Build a renderable from a 2D View.
        // sceneform의 모든 build() 메소드는 CompleableFuture를 반환한다
//        CompletableFuture<ViewRenderable> exampleLayout = // "미래에 처리할 업무(Task)로서,  Task 결과가 완료되었을때 값을 리턴하거나, 다른 Task가 실행되도록 발화(trigger)시키는 Task."
//                ViewRenderable.builder()
//                        .setView(this, R.layout.example_layout)
//                        .build();

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        CompletableFuture<ViewRenderable> exampleFutures[] = new CompletableFuture[middleNodes.size()];
        for (int i = 0; i < exampleFutures.length; i++) {
            exampleFutures[i] = ViewRenderable.builder()
                    .setView(this, R.layout.example_layout)
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
                                    exampleLayoutRenderables[i] = exampleFutures[i].get();
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
//                        LocationMarker prevLocationMarker;
//                        {
//                            Node node = new Node();
//                            prevLocationMarker = createLocationMarker(0, 0, node);
//                            prevLocationMarker.setAtCameraPosition(true);
//                            node.setRenderable(myArrowRenderable);
//                            locationScene.mLocationMarkers.add(prevLocationMarker);
//                        }
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
                        for (int i = 0; i < middleNodes.size(); i++) {
                            final int finalI = i;
                            LatLon point = middleNodes.get(i);
                            Node node = getExampleView(exampleLayoutRenderables[i]);
                            LocationMarker layoutLocationMarker = createLocationMarker(
                                    point.getLatitude(), point.getLongitude(), node);
                            layoutLocationMarker.setOnlyRenderWhenWithin(500);
                            Log.d(TAG, "kmyLog, nodeId : " + layoutLocationMarker.node);


                            layoutLocationMarker.setRenderEvent(new LocationNodeRender() {
                                @Override
                                public void render(LocationNode node) {
                                    ViewRenderable exampleLayoutRenderable = (ViewRenderable)exampleLayoutRenderables[finalI];
                                    Log.d(TAG, "kmyLog, in render nodeId : " + node);
                                    View eView = exampleLayoutRenderable.getView();
                                    TextView distanceTextView = eView.findViewById(R.id.textView2);
                                    int index = finalI + 1;
                                    String indexString = Integer.toString(index);
                                    distanceTextView.setText(indexString);
                                }
                            });
                            // Adding the marker
                            layoutLocationMarker.lookCamera(true);
                            locationScene.mLocationMarkers.add(layoutLocationMarker);
                        }
                        Node node = new Node();
                        LocationMarker locationMarker = createLocationMarker(destination.getLatitude(), destination.getLongitude(), node);
                        node.setRenderable(targetRenderable);
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
    }

    private LocationMarker createLocationMarker(double latitude, double longitude, Node node) {
        LocationMarker marker = new LocationMarker(latitude, longitude, node);
        marker.setHeight(0);
        marker.setScalingMode(LocationMarker.ScalingMode.SIMPLE_SCALING);
        marker.setGradualScalingMaxScale(20F);
        marker.setGradualScalingMinScale(0.5F);
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
}