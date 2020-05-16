package kr.ac.inu.deepect.arnavigation;

import com.google.ar.sceneform.Node;

import kr.ac.inu.deepect.arnavigation.rendering.LocationNode;
import kr.ac.inu.deepect.arnavigation.rendering.LocationNodeRender;

/**
 * Created by John on 02/03/2018.
 */

public class LocationMarker {

    // Location in real-world terms
    public double longitude;
    public double latitude;

    // Location in AR terms
    public LocationNode anchorNode;

    // Node to render
    public Node node;

    public Node nodeToLook;

    // Called on each frame if not null
    private LocationNodeRender renderEvent;
    private float scaleModifier = 1F;
    private float height = 0F;
    private int onlyRenderWhenWithin = Integer.MAX_VALUE;
    private ScalingMode scalingMode = ScalingMode.FIXED_SIZE_ON_SCREEN;
    private float gradualScalingMinScale = 0.8F;
    private float gradualScalingMaxScale = 1.4F;
    private boolean isAtCameraPosition = false;
    private DirectionMode directionMode = DirectionMode.NONE;

    public LocationMarker(double latitude, double longitude, Node node) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.node = node;
    }

    public DirectionMode getDirectionMode() { return directionMode; }

    public void lookCamera(boolean b) { directionMode = DirectionMode.LOOK_CAMERA; }

    public void setLookNode(Node node) {
        directionMode = DirectionMode.LOOK_NODE;
        nodeToLook = node;
    }

    public void setAtCameraPosition(boolean b) { isAtCameraPosition = b; }

    public boolean isAtCameraPosition() { return isAtCameraPosition; }

    public float getGradualScalingMinScale() {
        return gradualScalingMinScale;
    }

    public void setGradualScalingMinScale(float gradualScalingMinScale) {
        this.gradualScalingMinScale = gradualScalingMinScale;
    }

    public float getGradualScalingMaxScale() {
        return gradualScalingMaxScale;
    }

    public void setGradualScalingMaxScale(float gradualScalingMaxScale) {
        this.gradualScalingMaxScale = gradualScalingMaxScale;
    }

    /**
     * Only render this marker when within [onlyRenderWhenWithin] metres
     *
     * @return - metres or -1
     */
    public int getOnlyRenderWhenWithin() {
        return onlyRenderWhenWithin;
    }

    /**
     * Only render this marker when within [onlyRenderWhenWithin] metres
     *
     * @param onlyRenderWhenWithin - metres
     */
    public void setOnlyRenderWhenWithin(int onlyRenderWhenWithin) {
        this.onlyRenderWhenWithin = onlyRenderWhenWithin;
    }

    /**
     * Height based on camera height
     *
     * @return - height in metres
     */
    public float getHeight() {
        return height;
    }

    /**
     * Height based on camera height
     *
     * @param height - height in metres
     */
    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * How the markers should scale
     *
     * @return - ScalingMode
     */
    public ScalingMode getScalingMode() {
        return scalingMode;
    }

    /**
     * Whether the marker should scale, regardless of distance.
     *
     * @param scalingMode - ScalingMode.X
     */
    public void setScalingMode(ScalingMode scalingMode) {
        this.scalingMode = scalingMode;
    }

    /**
     * Scale multiplier
     *
     * @return - multiplier
     */
    public float getScaleModifier() {
        return scaleModifier;
    }

    /**
     * Scale multiplier
     *
     * @param scaleModifier - multiplier
     */
    public void setScaleModifier(float scaleModifier) {
        this.scaleModifier = scaleModifier;
    }

    /**
     * Called on each frame
     *
     * @return - LocationNodeRender (event)
     */
    public LocationNodeRender getRenderEvent() {
        return renderEvent;
    }

    /**
     * Called on each frame.
     */
    public void setRenderEvent(LocationNodeRender renderEvent) {
        this.renderEvent = renderEvent;
    }

    public enum ScalingMode {
        FIXED_SIZE_ON_SCREEN,
        NO_SCALING,
        GRADUAL_TO_MAX_RENDER_DISTANCE,
        GRADUAL_FIXED_SIZE
    }

    public enum DirectionMode {
        NONE,
        LOOK_CAMERA,
        LOOK_NODE
    }

    public double getLongitude() { return longitude; }

    public double getLatitude() { return latitude; }
}