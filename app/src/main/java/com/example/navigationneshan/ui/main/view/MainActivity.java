package com.example.navigationneshan.ui.main.view;

import static com.example.navigationneshan.utils.Utils.convertJSONToModel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.carto.graphics.Color;
import com.carto.styles.AnimationStyle;
import com.carto.styles.AnimationStyleBuilder;
import com.carto.styles.AnimationType;
import com.carto.styles.LineStyle;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.MarkerStyle;
import com.carto.styles.MarkerStyleBuilder;
import com.carto.utils.BitmapUtils;
import com.example.navigationneshan.BuildConfig;
import com.example.navigationneshan.R;
import com.example.navigationneshan.data.model.SearchModel;
import com.example.navigationneshan.databinding.ActivityMainBinding;
import com.example.navigationneshan.ui.main.viewmodel.MyViewModelActivity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.neshan.common.model.LatLng;
import org.neshan.common.utils.PolylineEncoding;
import org.neshan.mapsdk.model.Marker;
import org.neshan.mapsdk.model.Polyline;
import org.neshan.servicessdk.direction.model.DirectionStep;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements IStartNavigate {

    // region variable
    //region Static
    final int REQUEST_CODE = 123;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 3000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 2000;
    private static final float VMap = 0.5f;
    private static final long DELAY_HANDLER_NAVIGATE = 1000;
    //endregion
    private ActivityMainBinding binding;
    private Handler handler;

    private MyViewModelActivity viewModelActivity;

    public ArrayList<LatLng> decodedStepByStepPath;
    public ArrayList<String> listTextAddress;
    public ArrayList<String> listTextDistance;
    public ArrayList<String> listTextDuration;

    private ArrayList<Marker> markers;
    private Marker markerCurrent;
    private Marker markerUser;
    private Marker markerStart;

    private Polyline onMapPolylineLeftOver;
    private Polyline onMapPolylineTotalWay;

    private Location userLocation;
    private Location userLastLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;

    private Typeface typeface;
    //endregion

    // region LifeCycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // init binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setMyClickHandlers(new MyClickHandlers());
        // init  view model
        viewModelActivity = new ViewModelProvider(this).get(MyViewModelActivity.class);
        //set variable
        init();

    }

    @Override
    protected void onStart() {
        super.onStart();
        initLayoutReferences();
        initLocation();
        startReceivingLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
    // endregion

    //region init

    public void init() {
        markers = new ArrayList<>();
        typeface = ResourcesCompat.getFont(MainActivity.this, R.font.vazir_light);

        binding.setAddressText("مقصد را روی نقشه کلیک کنید");
        userLastLocation = new Location(LocationManager.GPS_PROVIDER);
        userLastLocation.setLatitude(0);
        userLastLocation.setLongitude(0);
        SearchBarListeners();
    }

    // endregion

    //region init Map
    private void initMap(Location userLocation) {
        if (userLocation != null) {
            binding.map.moveCamera(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), VMap);
        } else {
            binding.map.moveCamera(new LatLng(35.762234, 51.331743), VMap);
        }
        binding.map.setZoom(14, VMap);
        binding.map.setTilt(90, VMap);
        binding.map.setBearing(0, VMap);

    }
    //endregion

    //region SearchBar

    public void SearchBarListeners() {
        binding.floatingSearchView.setOnQueryChangeListener((oldQuery, newQuery) -> {

            if (!oldQuery.equals("") && newQuery.equals("")) {
                binding.floatingSearchView.clearSuggestions();
            } else {
                if (newQuery.length() > 3) {
                    binding.floatingSearchView.showProgress();
                    viewModelActivity.getSearch(newQuery, new LatLng(userLocation.getLatitude(), userLocation.getLongitude())).observe(MainActivity.this, items -> {
                        binding.floatingSearchView.swapSuggestions(items);
                        binding.floatingSearchView.hideProgress();
                    });
                }

            }
        });

        binding.floatingSearchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
                try {
                    SearchModel.Item model = convertJSONToModel(new JSONObject(searchSuggestion.getBody()));
                    binding.map.moveCamera(new LatLng(model.getLocation().getY(), model.getLocation().getX()), VMap);
                    binding.floatingSearchView.setDismissFocusOnItemSelection(true);
                    binding.floatingSearchView.setDismissOnOutsideClick(true);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onSearchAction(String currentQuery) {
            }
        });
        binding.floatingSearchView.setOnBindSuggestionCallback((suggestionView, leftIcon, textView, item, itemPosition) -> {
            try {
                SearchModel.Item model = convertJSONToModel(new JSONObject(item.getBody()));
                textView.setText(model.getTitle());

                textView.setTypeface(typeface);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }
    //endregion

    // region Routing
    private void initLayoutReferences() {
        // init map
        initMap(null);

        // when long clicked on map, a marker is added in clicked location
        binding.map.setOnMapLongClickListener(latLng -> {
            if (markers.size() < 1) {
                markers.add(addMarkerCurrent(latLng));
                if (markers.size() == 1) {
                    runOnUiThread(() -> {
                        binding.loadingLayout.setVisibility(View.VISIBLE);
                        routingApi();
                    });
                }
            } else {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, getResources().getString(R.string.pross_between_tow_point), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void routingApi() {
        LatLng userLocationTemp = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
        LatLng userCurrentLocationTemp = markers.get(0).getLatLng();
        viewModelActivity.getRootNavigation(userLocationTemp, userCurrentLocationTemp)
                .observeForever(route -> {
                    if (route != null) {
                        // init Lists
                        decodedStepByStepPath = new ArrayList<>();
                        listTextAddress = new ArrayList<>();
                        listTextDistance = new ArrayList<>();
                        listTextDuration = new ArrayList<>();

                        //set points to polyline map
                        int tempSizeTexts = 0;
                        int summarySizeTexts = 0;
                        for (DirectionStep step : route.getLegs().get(0).getDirectionSteps()) {
                            decodedStepByStepPath.addAll(PolylineEncoding.decode(step.getEncodedPolyline()));
                            summarySizeTexts += tempSizeTexts;
                            tempSizeTexts = decodedStepByStepPath.size() - summarySizeTexts;
                            for (int i = 0; i < tempSizeTexts; i++) {
                                listTextAddress.add(step.getInstruction());
                                listTextDistance.add(step.getDistance().getText());
                                listTextDuration.add(step.getDuration().getText());
                            }
                        }

                        //set Polyline
                        onMapPolylineLeftOver = new Polyline(decodedStepByStepPath, getLineStyleLeftOver());
                        onMapPolylineTotalWay = new Polyline(decodedStepByStepPath, getLineStyleTotalWay());
                        binding.map.addPolyline(onMapPolylineTotalWay);
                        binding.map.addPolyline(onMapPolylineLeftOver);

                        // set Map position
                        double centerFirstMarkerX = userLocation.getLatitude();
                        double centerFirstMarkerY = userLocation.getLongitude();
                        double centerFocalPositionX = (centerFirstMarkerX + markers.get(0).getLatLng().getLatitude()) / 2;
                        double centerFocalPositionY = (centerFirstMarkerY + markers.get(0).getLatLng().getLongitude()) / 2;

                        binding.map.moveCamera(new LatLng((centerFirstMarkerX + centerFocalPositionX) / 2,
                                (centerFirstMarkerY + centerFocalPositionY) / 2), VMap);
                        binding.map.setZoom(14, VMap);

                        // init BottomSheet and bundle
                        BottomSheetFragment bottomSheetDialog;
                        bottomSheetDialog = new BottomSheetFragment(MainActivity.this);
                        Bundle bundle = new Bundle();
                        bundle.putString("address", route.getLegs().get(0).getSummary());
                        bundle.putString("distance", route.getLegs().get(0).getDistance().getText());
                        bundle.putString("duration", route.getLegs().get(0).getDuration().getText());
                        bottomSheetDialog.setArguments(bundle);
                        bottomSheetDialog.show(getSupportFragmentManager(), "");
                        binding.loadingLayout.setVisibility(View.GONE);

                        // marker start point
                        addUserMarkerStart(new LatLng(decodedStepByStepPath.get(0).getLatitude(),
                                decodedStepByStepPath.get(0).getLongitude()));

                    } else {
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.not_fond_rout), Toast.LENGTH_LONG).show();
                    }

                });


    }

    // region Marker
    private Marker addMarkerCurrent(LatLng loc) {
        AnimationStyleBuilder animStBl = new AnimationStyleBuilder();
        animStBl.setFadeAnimationType(AnimationType.ANIMATION_TYPE_SMOOTHSTEP);
        animStBl.setSizeAnimationType(AnimationType.ANIMATION_TYPE_SPRING);
        animStBl.setPhaseInDuration(VMap);
        animStBl.setPhaseOutDuration(VMap);
        // marker animation style
        AnimationStyle animSt = animStBl.buildStyle();
        MarkerStyleBuilder markStCr = new MarkerStyleBuilder();
        markStCr.setSize(30f);
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.current_marker)));
        markStCr.setAnimationStyle(animSt);
        MarkerStyle markSt = markStCr.buildStyle();
        markerCurrent = new Marker(loc, markSt);
        binding.map.addMarker(markerCurrent);
        return markerCurrent;
    }

    private void addUserMarker(LatLng loc) {
        if (loc.getLatitude() != userLastLocation.getLatitude() &&
                loc.getLongitude() != userLastLocation.getLongitude()) {
            if (markerUser != null) {
                binding.map.removeMarker(markerUser);
            }
            MarkerStyleBuilder markStCr = new MarkerStyleBuilder();
            markStCr.setSize(30f);
            markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.live_marker)));
            MarkerStyle markSt = markStCr.buildStyle();
            markerUser = new Marker(loc, markSt);
            binding.map.addMarker(markerUser);

            userLastLocation.setLatitude(loc.getLatitude());
            userLastLocation.setLongitude(loc.getLongitude());
        }

    }

    private void addUserMarkerStart(LatLng loc) {
        if (markerStart != null) {
            binding.map.removeMarker(markerStart);
        }
        MarkerStyleBuilder markStCr = new MarkerStyleBuilder();
        markStCr.setSize(30f);
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.start_marker)));
        MarkerStyle markSt = markStCr.buildStyle();
        markerStart = new Marker(loc, markSt);
        binding.map.addMarker(markerStart);

    }
    //endregion

    private LineStyle getLineStyleLeftOver() {
        LineStyleBuilder lineStCr = new LineStyleBuilder();
        lineStCr.setColor(new Color(android.graphics.Color.parseColor("#259CD8")));
        lineStCr.setWidth(5f);
        lineStCr.setStretchFactor(0f);
        return lineStCr.buildStyle();
    }

    private LineStyle getLineStyleTotalWay() {
        LineStyleBuilder lineStCr = new LineStyleBuilder();
        lineStCr.setColor(new Color(android.graphics.Color.parseColor("#c3c3c3")));
        lineStCr.setWidth(10f);
        lineStCr.setStretchFactor(0f);
        return lineStCr.buildStyle();
    }

    //endregion

    //region location

    private void initLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                userLocation = locationResult.getLastLocation();
                onLocationChange();
            }
        };


        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();

    }

    private void startLocationUpdates() {
        settingsClient
                .checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, locationSettingsResponse -> {
                    Log.i("TAG", "All location settings are satisfied.");

                    //noinspection MissingPermission
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

                    onLocationChange();
                })
                .addOnFailureListener(this, e -> {
                    int statusCode = ((ApiException) e).getStatusCode();
                    switch (statusCode) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            Log.i("TAG", "Location settings are not satisfied. Attempting to upgrade " +
                                    "location settings ");
                            try {
                                // Show the dialog by calling startResolutionForResult(), and check the
                                // result in onActivityResult().
                                ResolvableApiException rae = (ResolvableApiException) e;
                                rae.startResolutionForResult(MainActivity.this, REQUEST_CODE);
                            } catch (IntentSender.SendIntentException sie) {
                                Log.i("TAG", "PendingIntent unable to execute request.");
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            String errorMessage = "Location settings are inadequate, and cannot be " +
                                    "fixed here. Fix in Settings.";
                            Log.e("TAG", errorMessage);

                            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }

                    onLocationChange();
                });
    }

    public void stopLocationUpdates() {
        fusedLocationClient
                .removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, task -> {
                });
    }

    public void startReceivingLocationUpdates() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        startLocationUpdates();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            // open device settings when the permission is
                            openSettings();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }

                }).check();
    }

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void onLocationChange() {
        if (userLocation != null) {
            addUserMarker(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()));
        }
    }

    //endregion

    //region navigate map
    @Override
    public void start() {
        visibilityForNavigate(true);
        navigationMode(true);
        final double[] lastBearing = {-1};
        handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                if (decodedStepByStepPath.size() != 0) {
                    double bearingTo;
                    binding.map.removeMarker(markerUser);
                    // is last Point
                    if (decodedStepByStepPath.size() == 1) {
                        double latTemp = decodedStepByStepPath.get(0).getLatitude();
                        double lngTemp = decodedStepByStepPath.get(0).getLongitude();

                        // calculate bearing and set marker
                        bearingTo = bearing(latTemp, lngTemp, latTemp, lngTemp);
                        addUserMarker(new LatLng(latTemp, lngTemp));

                        binding.map.moveCamera(new LatLng(latTemp, lngTemp), VMap);
                        binding.setAddressText(listTextAddress.get(0));
                        binding.setDistanceAndDuration(listTextDistance.get(0) + " " + listTextDuration.get(0));

                    } else {
                        double latPresentTemp = decodedStepByStepPath.get(0).getLatitude();
                        double lngPresentTemp = decodedStepByStepPath.get(0).getLongitude();

                        double latNextTemp = decodedStepByStepPath.get(1).getLatitude();
                        double lngNextTemp = decodedStepByStepPath.get(1).getLongitude();

                        bearingTo = bearing(latPresentTemp, lngPresentTemp, latNextTemp, lngNextTemp);
                        addUserMarker(new LatLng(latNextTemp, lngNextTemp));

                        binding.map.moveCamera(new LatLng(latNextTemp, lngNextTemp), VMap);
                        binding.setAddressText(listTextAddress.get(1));
                        binding.setDistanceAndDuration(listTextDistance.get(1) + " " + listTextDuration.get(1));
                    }

                    // duplicate Lat Lng
                    if (bearingTo == 0) {
                        binding.map.setBearing((int) lastBearing[0], VMap);
                    } else {
                        binding.map.setBearing((int) bearingTo, VMap);
                        lastBearing[0] = bearingTo;
                    }

                    // ready for next point
                    decodedStepByStepPath.remove(0);
                    listTextAddress.remove(0);
                    listTextDistance.remove(0);
                    listTextDuration.remove(0);
                    // update Polyline
                    binding.map.removePolyline(onMapPolylineLeftOver);
                    onMapPolylineLeftOver = new Polyline(decodedStepByStepPath, getLineStyleLeftOver());
                    binding.map.addPolyline(onMapPolylineLeftOver);

                    handler.postDelayed(this, DELAY_HANDLER_NAVIGATE);

                } else {
                    navigationMode(false);
                    visibilityForNavigate(false);
                }
            }
        };
        handler.post(runnable);

    }

    private double bearing(double lat1, double lng1, double lat2, double lng2) {
        double dLon = (lng2 - lng1);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double brng = Math.toDegrees((Math.atan2(y, x)));
        brng = (brng + 360) % 360;
        return brng;
    }

    public void navigationMode(boolean flag) {

        if (flag) {
            stopLocationUpdates();
            binding.btnExit.setVisibility(View.VISIBLE);
            binding.map.moveCamera(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), VMap);
            binding.map.setZoom(20, VMap);
            binding.map.setTilt(30, VMap);
        } else {
            startLocationUpdates();
            markers.clear();
            listTextAddress.clear();
            listTextDistance.clear();
            listTextDuration.clear();
            binding.setAddressText("مقصد را روی نقشه کلیک کنید");
            binding.map.removeMarker(markerUser);
            binding.map.removeMarker(markerStart);
            binding.map.removeMarker(markerCurrent);
            binding.map.removePolyline(onMapPolylineLeftOver);
            binding.map.removePolyline(onMapPolylineTotalWay);
            binding.btnExit.setVisibility(View.GONE);
            initMap(userLocation);
        }
    }


    //endregion

    // region  OnClick
    public class MyClickHandlers {
        public void onClickButtonExit(View view) {
            visibilityForNavigate(false);
            handler.removeCallbacksAndMessages(null);
            binding.map.removeMarker(markerUser);
            binding.map.removeMarker(markerCurrent);
            binding.map.removePolyline(onMapPolylineLeftOver);
            binding.map.removePolyline(onMapPolylineTotalWay);
            binding.setDistanceAndDuration("");
            initMap(userLocation);
            view.setVisibility(View.GONE);
            navigationMode(false);
        }

        public void onClickLayoutLading(View view) {

        }

        public void focusOnUserLocation(View view) {
            if (userLocation != null) {
                binding.map.moveCamera(
                        new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), VMap);
                binding.map.setZoom(15, VMap);
            }
        }
    }


    // endregion

    //region Visibility
    public void visibilityForNavigate(boolean flag) {
        if (flag) {
            binding.setVisibilityRouting(View.VISIBLE);
            binding.setVisibilitySearch(View.GONE);
        } else {
            binding.setVisibilityRouting(View.GONE);
            binding.setVisibilitySearch(View.VISIBLE);
        }

    }
    //endregion
}
