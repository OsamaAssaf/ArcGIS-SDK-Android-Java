package com.assaf.arcgis_sdk;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private MapView mapView;
    private Callout callout;
    private FeatureLayer featureLayer;
    private ArcGISMap map;
    private LocatorTask locatorTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setApiKeyForApp();
        setupMap();
        initSearchView();
    }


    private void setApiKeyForApp() {
        final String esriApiKey = BuildConfig.ARC_GIS_API_KEY;
        ArcGISRuntimeEnvironment.setApiKey(esriApiKey);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupMap() {
        mapView = findViewById(R.id.mapView);
        map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);
        final double latitude = 34.03758;
        final double longitude = -118.702426;
        final double scale = 25000;
        setupFeatureLayer();

        mapView.setMap(map);
        mapView.setViewpoint(new Viewpoint(latitude, longitude, scale));
        callout = mapView.getCallout();
        mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mapView) {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                featureLayer.clearSelection();
                android.graphics.Point screenPoint = new android.graphics.Point(Math.round(motionEvent.getX()),
                        Math.round(motionEvent.getY()));
                int tolerance = 10;

                final ListenableFuture<IdentifyLayerResult> identifyLayerResultFuture = mMapView
                        .identifyLayerAsync(featureLayer, screenPoint, tolerance, false, -1);
                identifyLayerResultFuture.addDoneListener(() -> {
                    try {
                        // get the result from the identify
                        final IdentifyLayerResult identifyLayerResult = identifyLayerResultFuture.get();
                        // create a list of features from the elements of the identify layer result
                        final List<Feature> identifiedFeatures = new ArrayList<>();
                        for (GeoElement geoelement : identifyLayerResult.getElements()) {
                            if (geoelement instanceof Feature) {
                                identifiedFeatures.add((Feature) geoelement);
                            }
                        }

                        // select the features in the feature layer
                        featureLayer.selectFeatures(identifiedFeatures);

                        Toast.makeText(MainActivity.this, identifiedFeatures.size() + " features selected",
                                Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        String error = "Select features failed: " + e.getMessage();
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, error);
                    }
                });

//                return true;
                if (callout.isShowing()) {
                    callout.dismiss();
                }
                // get the point that was clicked and convert it to a point in map coordinates
//                final android.graphics.Point screenPoint = new android.graphics.Point(Math.round(motionEvent.getX()), Math.round(motionEvent.getY()));
                // create a selection tolerance
//                int tolerance = 10;
                // use identifyLayerAsync to get tapped features
                final ListenableFuture<IdentifyLayerResult> identifyLayerResultListenableFuture = mMapView
                        .identifyLayerAsync(featureLayer, screenPoint, tolerance, false, 1);
                identifyLayerResultListenableFuture.addDoneListener(() -> {
                    try {
                        final IdentifyLayerResult identifyLayerResult = identifyLayerResultListenableFuture.get();
                        TextView calloutContent = new TextView(getApplicationContext());
                        calloutContent.setTextColor(Color.BLACK);
                        calloutContent.setSingleLine(false);
                        calloutContent.setVerticalScrollBarEnabled(true);
                        calloutContent.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                        calloutContent.setMovementMethod(new ScrollingMovementMethod());
                        calloutContent.setLines(5);
                        for (GeoElement element : identifyLayerResult.getElements()) {
                            Feature feature = (Feature) element;
                            // create a map of all available attributes as name value pairs
                            Map<String, Object> attr = feature.getAttributes();
                            @SuppressLint("ClickableViewAccessibility") Set<String> keys = attr.keySet();
                            for (String key : keys) {
                                Object value = attr.get(key);
                                // format observed field value as date
                                if (value instanceof GregorianCalendar) {
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
                                    value = simpleDateFormat.format(((GregorianCalendar) value).getTime());
                                }
                                // append name value pairs to text view
                                calloutContent.append(key + " | " + value + "\n");
                            }
                            // center the mapview on selected feature
                            Envelope envelope = feature.getGeometry().getExtent();
                            mMapView.setViewpointGeometryAsync(envelope, 200);
                            // show callout
                            callout.setLocation(envelope.getCenter());
                            callout.setContent(calloutContent);
                            callout.show();
                        }
                    } catch (Exception e1) {
                        Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e1.getMessage());
                    }
                });
                return super.onSingleTapConfirmed(motionEvent);

            }
        });
    }

    private void setupFeatureLayer() {
        ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable("https://services3.arcgis.com/GVgbJbqm8hXASVYi/arcgis/rest/services/Parks_and_Open_Space_Styled/FeatureServer/0");

        featureLayer = new FeatureLayer(serviceFeatureTable);
        final SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1);
        final SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.YELLOW, lineSymbol);
        featureLayer.setRenderer(new SimpleRenderer(fillSymbol));
        map.getOperationalLayers().add(featureLayer);
    }

    private void initSearchView() {
        locatorTask = new LocatorTask("https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer");
        final SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performGeocode(newText);
                return false;
            }
        });
    }


    private void performGeocode(String query) {
        GeocodeParameters geocodeParameters = new GeocodeParameters();
        geocodeParameters.getResultAttributeNames().add("*");
        geocodeParameters.setMaxResults(1);
        geocodeParameters.setOutputSpatialReference(mapView.getSpatialReference());

        final ListenableFuture<List<GeocodeResult>> geocodeResultFuture = locatorTask.geocodeAsync(query, geocodeParameters);

        geocodeResultFuture.addDoneListener(() -> {
            try {
                List<GeocodeResult> geocodeResults = geocodeResultFuture.get();
                if (!geocodeResults.isEmpty()) {
                    displayResult(geocodeResults.get(0));
                } else {
                    Toast.makeText(MainActivity.this, "No results found.", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(MainActivity.class.getSimpleName(), "Error getting result: " + e.getMessage());
            }
        });
    }

    private void displayResult(GeocodeResult geocodeResult) {
        GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
        // clear the overlay of any previous result
        graphicsOverlay.getGraphics().clear();

        // create a graphic to display the address text
        final TextSymbol textSymbol = new TextSymbol(
                18f,
                geocodeResult.getLabel(),
                Color.BLACK,
                TextSymbol.HorizontalAlignment.CENTER,
                TextSymbol.VerticalAlignment.BOTTOM
        );

        final Graphic textGraphic = new Graphic(geocodeResult.getDisplayLocation(), textSymbol);
        graphicsOverlay.getGraphics().add(textGraphic);

        // create a graphic to display the location as a red square
        final SimpleMarkerSymbol simpleMarkerSymbol = new SimpleMarkerSymbol(
                SimpleMarkerSymbol.Style.SQUARE,
                Color.RED,
                12.0f
        );

        final Graphic markerGraphic = new Graphic(geocodeResult.getDisplayLocation(), geocodeResult.getAttributes(), simpleMarkerSymbol);
        graphicsOverlay.getGraphics().add(markerGraphic);
        mapView.setViewpointCenterAsync(geocodeResult.getDisplayLocation());

    }

    @Override
    protected void onPause() {
        mapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mapView.resume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mapView.dispose();
        super.onDestroy();
    }

}