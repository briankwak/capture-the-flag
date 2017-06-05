/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csm117.ucla.capturetheflag;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewGameActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<LocationSettingsResult>, GoogleMap.OnMapClickListener {

    protected static final String TAG = "NewGameActivity";
    protected static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 0x49;
    private static final float MAP_ZOOM = 15;

    private static final double AREA_WIDTH = 0.0025;
    private static final double AREA_HEIGHT = 0.0025;

    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected LocationSettingsRequest mLocationSettingsRequest;
    protected Location mCurrentLocation;
    protected LatLng mCurrentLatLng;
    private String mLastUpdateTime;

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private GoogleMap mMap;
    private Polygon mRedTeamArea;
    private Polygon mBlueTeamArea;
    private DatabaseReference mDatabase;
    private Marker mMarker;
    private LatLngInterpolator.Linear mInterpolator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buildGoogleApiClient();
        createLocationRequest();
        buildLocationSettingsRequest();

        mDatabase = FirebaseDatabase.getInstance().getReference();

        mInterpolator = new LatLngInterpolator.Linear();
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Uses a {@link LocationSettingsRequest.Builder} to build
     * a {@link LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    protected void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    protected void checkLocationSettings() {
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleApiClient,
                        mLocationSettingsRequest
                );
        result.setResultCallback(this);
    }

    /**
     * The callback invoked when
     * {@link com.google.android.gms.location.SettingsApi#checkLocationSettings(GoogleApiClient,
     * LocationSettingsRequest)} is called. Examines the
     * {@link LocationSettingsResult} object and determines if
     * location settings are adequate. If they are not, begins the process of presenting a location
     * settings dialog to the user.
     */
    @Override
    public void onResult(LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.i(TAG, "All location settings are satisfied.");
                startLocationUpdates();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings ");

                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result
                    // in onActivityResult().
                    status.startResolutionForResult(NewGameActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Log.i(TAG, "PendingIntent unable to execute request.");
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");

                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we
     * just add a marker near Africa.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        if(mMarker == null && mCurrentLocation != null) {
            mMarker = map.addMarker(new MarkerOptions().position(new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude())).title("Marker"));
        }
        mMap.setOnMapClickListener(this);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
        checkLocationSettings();
        if (mCurrentLocation == null) {

            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mCurrentLatLng = new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
            buildRectangle(mCurrentLatLng);


            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLatLng,
                    MAP_ZOOM));
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        }
    }

    protected void startLocationUpdates() {

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,
                    mLocationRequest,
                    this
            );
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mCurrentLatLng = new LatLng(location.getLatitude(),location.getLongitude());
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        if(mMarker == null) {
            mMarker = mMap.addMarker(new MarkerOptions().position(mCurrentLatLng).title("Marker"));
        } else{
            mMarker.setPosition(mCurrentLatLng);
            //MarkerAnimation.animateMarkerToICS(mMarker,new LatLng(location.getLatitude(),location.getLongitude()),mInterpolator);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient,
                this
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /* buncha rectangle nonsense */
    private void buildRectangle(LatLng latLng){
        mRedTeamArea = mMap.addPolygon(new PolygonOptions()
                .addAll(createRectangle(latLng, AREA_WIDTH, AREA_HEIGHT, true))
                .fillColor(Color.argb(32, 255, 0, 0))
                .strokeColor(Color.argb(128, 255, 0, 0))
                .strokeWidth(10));

        mBlueTeamArea = mMap.addPolygon(new PolygonOptions()
                .addAll(createRectangle(latLng, AREA_WIDTH, AREA_HEIGHT, false))
                .fillColor(Color.argb(32, 0, 0, 255))
                .strokeColor(Color.argb(128, 0, 0, 255))
                .strokeWidth(10));
    }

    private List<LatLng> createRectangle(LatLng center, double halfWidth, double halfHeight, boolean bottom) {
//        if(bottom) {
//            return Arrays.asList(new LatLng(center.latitude - halfHeight, center.longitude - halfWidth),
//                    new LatLng(center.latitude - halfHeight, center.longitude + halfWidth),
//                    new LatLng(center.latitude, center.longitude + halfWidth),
//                    new LatLng(center.latitude, center.longitude - halfWidth),
//                    new LatLng(center.latitude - halfHeight, center.longitude - halfWidth));
//        } else{
//            return Arrays.asList(new LatLng(center.latitude, center.longitude - halfWidth),
//                    new LatLng(center.latitude, center.longitude + halfWidth),
//                    new LatLng(center.latitude + halfHeight, center.longitude + halfWidth),
//                    new LatLng(center.latitude + halfHeight, center.longitude - halfWidth),
//                    new LatLng(center.latitude, center.longitude - halfWidth));
//        }
        if(bottom) {
            return Arrays.asList(new LatLng(center.latitude - halfHeight, center.longitude - halfWidth),
                    new LatLng(center.latitude - halfHeight, center.longitude),
                    new LatLng(center.latitude + halfHeight, center.longitude),
                    new LatLng(center.latitude + halfHeight, center.longitude - halfWidth),
                    new LatLng(center.latitude - halfHeight, center.longitude - halfWidth));
        } else{
            return Arrays.asList(new LatLng(center.latitude - halfHeight, center.longitude),
                    new LatLng(center.latitude - halfHeight, center.longitude + halfWidth),
                    new LatLng(center.latitude + halfHeight, center.longitude + halfWidth),
                    new LatLng(center.latitude + halfHeight, center.longitude),
                    new LatLng(center.latitude - halfHeight, center.longitude));
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mRedTeamArea.setPoints(createRectangle(latLng,AREA_WIDTH,AREA_HEIGHT, true));
        mBlueTeamArea.setPoints(createRectangle(latLng,AREA_WIDTH,AREA_HEIGHT, false));
    }

    public void pressConfirm(View view){
        EditText gameEditText = (EditText)findViewById(R.id.game_name);
        final String gameName = gameEditText.getText().toString();
        EditText playerEditText = (EditText)findViewById(R.id.player_name);
        final String playerName = playerEditText.getText().toString();
        if(gameName.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please choose a game name.", Toast.LENGTH_SHORT).show();
        } else if(playerName.length() == 0){
            Toast.makeText(getApplicationContext(), "Please choose a player name.", Toast.LENGTH_SHORT).show();
        } else {
            final Area area = new Area(mRedTeamArea.getPoints(),mBlueTeamArea.getPoints());
            final Player player = new Player(mCurrentLatLng,System.currentTimeMillis());

            mDatabase.child("games").child(gameName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        Toast.makeText(getApplicationContext(), "Game name already in use!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("/games/" + gameName, "waiting");
                    childUpdates.put("/areas/" + gameName, area.toMap());
                    childUpdates.put("/players/" + gameName + "/" + playerName, player.toMap());

                    mDatabase.updateChildren(childUpdates);
                    changeActivity(gameName,playerName);
                }

                @Override
                public void onCancelled(DatabaseError firebaseError) {}
            });

        }
    }
    public void changeActivity(String gameName,String playerName){
        Intent intent = new Intent(this, WaitingRoomActivity.class);
        intent.putExtra("game",gameName);
        intent.putExtra("player",playerName);
        intent.putExtra("creator",true);
        startActivity(intent);
    }
}

