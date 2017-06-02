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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class GameActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<LocationSettingsResult>,
        GoogleMap.OnMarkerClickListener {

    protected static final String TAG = "GameActivity";
    protected static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 0x49;
    private static final float MAP_ZOOM = 15;
    private static final double OUTER_CIRCLE_RADIUS = 100;
    private static final double INNER_CIRCLE_RADIUS = 25;

    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected LocationSettingsRequest mLocationSettingsRequest;
    protected Location mCurrentLocation;
    private String mLastUpdateTime;

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private Marker mMarker;
    private GoogleMap mMap;
    private LatLngInterpolator.Linear mInterpolator;
    private String mGameName;
    private String mPlayerName;
    private String mTeam;
    private DatabaseReference mDatabase;
    private boolean mDead;

    private LatLng mRedMin;
    private LatLng mRedMax;
    private LatLng mBlueMin;
    private LatLng mBlueMax;
    private LatLng mRedFlag;
    private LatLng mBlueFlag;

    private Marker mRedFlagMarker;
    private Marker mBlueFlagMarker;

    private HashMap<String,Marker> mPlayerMarkers;

    private Circle mCircle;
    private Circle mInnerCircle;

    @Override
    public void onBackPressed()
    {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Return from Map");
        builder.setMessage("Would you like to exit from the game and return to the home menu?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                startActivity(new Intent(GameActivity.this, MainActivity.class));
                finish();
            }
        });
        builder.setNegativeButton("No", null);


        AlertDialog dialog = builder.create();
        dialog.show();

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mGameName = getIntent().getStringExtra("game");
        mPlayerName = getIntent().getStringExtra("player");
        mTeam = getIntent().getStringExtra("team");

        mPlayerMarkers = new HashMap<>();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buildGoogleApiClient();
        createLocationRequest();
        buildLocationSettingsRequest();

        mInterpolator = new LatLngInterpolator.Linear();


        mDatabase = FirebaseDatabase.getInstance().getReference();

        final LinearLayout bottomTextView = (LinearLayout)findViewById(R.id.bottomText);

        mDatabase.child("areas").child(mGameName).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Area area = dataSnapshot.getValue(Area.class);
                boolean redFlag = area.redFlag;
                boolean blueFlag = area.blueFlag;
                mRedFlag = area.redFlag();
                mBlueFlag = area.blueFlag();

                if(blueFlag && mBlueFlagMarker == null){
                    mBlueFlagMarker = mMap.addMarker(new MarkerOptions().position(mBlueFlag).title("Blue Flag"));
                    mBlueFlagMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                }
                if(redFlag && mRedFlagMarker == null){
                    mRedFlagMarker = mMap.addMarker(new MarkerOptions().position(mRedFlag).title("Red Flag"));
                    mRedFlagMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                }

                if(mCircle != null){
                    if(mTeam.equals("blue")){
                        mBlueFlagMarker.setVisible(true);
                        mRedFlagMarker.setVisible(Area.withinCircle(mRedFlag,mCircle));
                    } else{
                        mRedFlagMarker.setVisible(true);
                        mBlueFlagMarker.setVisible(Area.withinCircle(mRedFlag,mCircle));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
            }
        });

        mDatabase.child("players").child(mGameName).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot child : dataSnapshot.getChildren()){
                    String name = child.getKey();
                    //if(!name.equals(mPlayerName)){
                    Player player = child.getValue(Player.class);
                    Marker m = mPlayerMarkers.get(name);
                    LatLng playerLoc = player.getLatLng();
                    if(m != null) {

                        mDead = player.dead;
                        m.setPosition(playerLoc);
                        //MarkerAnimation.animateMarkerToICS(m,playerLoc,mInterpolator);
                        if(name.equals(mPlayerName)){
                            mCircle.setCenter(playerLoc);
                            mInnerCircle.setCenter(playerLoc);

                        }
                        if(!player.team.equals(mTeam)) {
                            m.setVisible(Area.withinCircle(playerLoc, mCircle));
                        }
                    } else{
                        if(name.equals(mPlayerName)){
                            mCircle = mMap.addCircle(new CircleOptions()
                                    .center(playerLoc)
                                    .radius(OUTER_CIRCLE_RADIUS)
                                    .fillColor(Color.argb(64, 255, 255, 255))
                                    .strokeColor(Color.argb(192, 255, 255, 255)));
                            mCircle.setZIndex(1);

                            mInnerCircle = mMap.addCircle(new CircleOptions()
                                    .center(playerLoc)
                                    .radius(INNER_CIRCLE_RADIUS)
                                    .fillColor(Color.argb(32, 255, 255, 0))
                                    .strokeColor(Color.argb(192, 255, 255, 0)));
                            mInnerCircle.setZIndex(1);

                            mDead = player.dead;
                        }


                        m = mMap.addMarker(new MarkerOptions().position(playerLoc).title(name));

                        if(player.dead){
                            m.setVisible(false);
                        } else if(!player.team.equals(mTeam) && mCircle != null) {
                            m.setVisible(Area.withinCircle(playerLoc, mCircle));
                        } else{
                            m.setVisible(true);
                        }
                        if(player.team.equals("blue")) {
                            m.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                        } else{
                            m.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        }
                        mPlayerMarkers.put(name,m);
                    }
                    //}
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
            }
        });


        mMap.setOnMarkerClickListener(this);
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
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
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
     * {@link com.google.android.gms.location.LocationSettingsResult} object and determines if
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
                    status.startResolutionForResult(GameActivity.this, REQUEST_CHECK_SETTINGS);
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
//        if(mMarker == null && mCurrentLocation != null) {
//            mMarker = map.addMarker(new MarkerOptions().position(new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude())).title("Marker"));
//        }
        buildRectangles();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
        if (mCurrentLocation == null) {

            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mDatabase.child("players").child(mGameName).child(mPlayerName).child("lat").setValue(mCurrentLocation.getLatitude());
            mDatabase.child("players").child(mGameName).child(mPlayerName).child("lng").setValue(mCurrentLocation.getLongitude());


            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude()),
                    MAP_ZOOM));
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        }
        checkLocationSettings();
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
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("lat").setValue(mCurrentLocation.getLatitude());
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("lng").setValue(mCurrentLocation.getLongitude());



//        if(mMarker == null) {
//            mMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),location.getLongitude())).title("Marker"));
//        } else{
//            //mMarker.setPosition(new LatLng(location.getLatitude(),location.getLongitude()));
//            MarkerAnimation.animateMarkerToICS(mMarker,new LatLng(location.getLatitude(),location.getLongitude()),mInterpolator);
//        }
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

    /* buncha rectangle nonsense again */
    private void buildRectangles(){

        mDatabase.child("areas").child(mGameName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Area area = dataSnapshot.getValue(Area.class);
                mRedMin = area.redMin();
                mRedMax = area.redMax();
                mBlueMin = area.blueMin();
                mBlueMax = area.blueMax();
                mMap.addPolygon(new PolygonOptions()
                        .addAll(createRectangle(mRedMin,mRedMax))
                        .fillColor(Color.argb(32, 255, 0, 0))
                        .strokeColor(Color.argb(128, 255, 0, 0))
                        .strokeWidth(10));

                mMap.addPolygon(new PolygonOptions()
                        .addAll(createRectangle(mBlueMin,mBlueMax))
                        .fillColor(Color.argb(32, 0, 0, 255))
                        .strokeColor(Color.argb(128, 0, 0, 255))
                        .strokeWidth(10));
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
            }
        });
    }

    private List<LatLng> createRectangle(LatLng min, LatLng max) {
        return Arrays.asList(min,
                new LatLng(min.latitude, max.longitude),
                max,
                new LatLng(max.latitude,min.longitude),
                min);
    }

    @Override
    public boolean onMarkerClick(Marker marker){
        Toast.makeText(getApplicationContext(),"blah blah blah",Toast.LENGTH_SHORT);
        mDatabase.child("players").child(mGameName).child(marker.getTitle()).child("dead").setValue(true);
        return true;
    }




}
