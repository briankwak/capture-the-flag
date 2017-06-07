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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.database.ChildEventListener;
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

public class PlaceFlagActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<LocationSettingsResult> {

    protected static final String TAG = "PlaceFlagActivity";
    protected static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 0x49;
    private static final float MAP_ZOOM = 17;
    private static final double OUTER_CIRCLE_RADIUS = 100;
    private static final double INNER_CIRCLE_RADIUS = 25;
    private static final float ANCHOR_VALUE = 0.5f;

    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected LocationSettingsRequest mLocationSettingsRequest;
    protected Location mCurrentLocation;
    private String mLastUpdateTime;

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private Marker mMarker;
    private GoogleMap mMap;
    private LatLngInterpolator.Linear mInterpolator;
    private String mGameName;
    private String mPlayerName;
    private String mTeam;
    private DatabaseReference mDatabase;
    private ValueEventListener activityListener;
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
        builder.setTitle("Quit Game");
        builder.setMessage("Would you like to quit the game?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mDatabase.child("players").child(mGameName).child(mPlayerName).removeValue();
                startActivity(new Intent(PlaceFlagActivity.this, MainActivity.class));
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
        setContentView(R.layout.activity_place_flag);

        mGameName = getIntent().getStringExtra("game");
        mPlayerName = getIntent().getStringExtra("player");
        mTeam = getIntent().getStringExtra("team");
        final boolean leader = getIntent().getExtras().getBoolean("leader");

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
                if(!dataSnapshot.exists()){
                    return;
                }
                Area area = dataSnapshot.getValue(Area.class);
                boolean redFlag = area.redFlag;
                boolean blueFlag = area.blueFlag;
                mRedFlag = area.redFlag();
                mBlueFlag = area.blueFlag();

                if(blueFlag && mBlueFlagMarker == null){
                    mBlueFlagMarker = mMap.addMarker(new MarkerOptions().position(mBlueFlag).title("Blue Flag").anchor(3.0f/42.0f, 1.0f));
                    mBlueFlagMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.blueflag));
                    mBlueFlagMarker.setVisible(false);
                }
                if(redFlag && mRedFlagMarker == null){
                    mRedFlagMarker = mMap.addMarker(new MarkerOptions().position(mRedFlag).title("Red Flag").anchor(3.0f/42.0f, 1.0f));
                    mRedFlagMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.redflag));
                    mRedFlagMarker.setVisible(false);
                }

                if(mCircle != null && mBlueFlagMarker != null){
                    if(mTeam.equals("blue")){
                        mBlueFlagMarker.setVisible(true);
                    } else{
                        mBlueFlagMarker.setVisible(Area.withinCircle(mBlueFlag,mCircle));
                    }
                }
                if(mCircle != null && mRedFlagMarker != null){
                    if(mTeam.equals("red")){
                        mRedFlagMarker.setVisible(true);
                    } else{
                        mRedFlagMarker.setVisible(Area.withinCircle(mRedFlag,mCircle));
                    }
                }

                if(leader){
                    if(mTeam.equals("blue")){
                        if(!blueFlag){
                            showButton();
                        } else if(!redFlag){
                            writeWaitingText(true);
                        } else{
                            writeWaitingText(true);
                            changeActivity();
                        }
                    } else{
                        if(!redFlag){
                            showButton();
                        } else if(!blueFlag){
                            writeWaitingText(true);
                        } else{
                            writeWaitingText(true);
                            changeActivity();
                        }
                    }

                } else{
                    if(!blueFlag || !redFlag){
                        writeWaitingText(mTeam.equals("blue") ? blueFlag : redFlag);
                    }
                    else {
                        changeActivity();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
            }
        });



        mDatabase.child("players").child(mGameName).addChildEventListener(new ChildEventListener(){
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot){
                String name = dataSnapshot.getKey();
                mPlayerMarkers.get(name).remove();
                mPlayerMarkers.remove(name);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError){}

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
                            m.setPosition(playerLoc);
                            //MarkerAnimation.animateMarkerToICS(m,playerLoc,mInterpolator);
                            if(name.equals(mPlayerName)){
                                mCircle.setCenter(playerLoc);
                                mInnerCircle.setCenter(playerLoc);
                                
                            }
                            if(!player.team.equals(mTeam)) {
                                m.setVisible(Area.withinCircle(playerLoc, mCircle));
                            } else{
                                m.setVisible(true);
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
                            }


                            m = mMap.addMarker(new MarkerOptions()
                                                    .position(playerLoc)
                                                    .title(name)
                                                    .anchor(ANCHOR_VALUE,ANCHOR_VALUE));


                            if(!player.team.equals(mTeam) && mCircle != null) {
                                m.setVisible(Area.withinCircle(playerLoc, mCircle));
                            } else{
                                m.setVisible(true);
                            }
                            if(mCircle != null && mBlueFlagMarker != null){
                                if(mTeam.equals("blue")){
                                    mBlueFlagMarker.setVisible(true);
                                } else{
                                    mBlueFlagMarker.setVisible(Area.withinCircle(mBlueFlag,mCircle));
                                }
                            }
                            if(mCircle != null && mRedFlagMarker != null){
                                if(mTeam.equals("red")){
                                    mRedFlagMarker.setVisible(true);
                                } else{
                                    mRedFlagMarker.setVisible(Area.withinCircle(mRedFlag,mCircle));
                                }
                            }

                            if(player.team.equals("blue")) {
                                m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.blueplayermarker));
                            } else{
                                m.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.redplayermarker));
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


    }

    private void changeActivity() {
        Toast.makeText(getApplicationContext(), "Starting game", Toast.LENGTH_SHORT).show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                Intent intent = new Intent(PlaceFlagActivity.this, GameActivity.class);
                intent.putExtra("game",mGameName);
                intent.putExtra("player",mPlayerName);
                intent.putExtra("team",mTeam);
                startActivity(intent);
                finish();
            }
        }, 2000);
        if (mDatabase != null && activityListener!=null) {
            mDatabase.removeEventListener(activityListener);
        }

    }

    private void showButton() {
        Button button = new Button(this);
        button.setText("Place Flag at Current Location");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                pressPlaceFlag(view);
            }
        });
        LinearLayout bottomTextView = (LinearLayout)findViewById(R.id.bottomText);
        bottomTextView.removeAllViews();
        bottomTextView.addView(button);
    }

    private void writeWaitingText(boolean flag) {
        TextView view = new TextView(this);
        String waitingText = "";
        if(flag){
            waitingText = "Waiting for other team...";
        } else{
            waitingText = "Waiting for team leader to place flag...";
        }
        view.setText(waitingText);

        LinearLayout bottomTextView = (LinearLayout) findViewById(R.id.bottomText);
        bottomTextView.removeAllViews();
        bottomTextView.addView(view);
    }

    private void pressPlaceFlag(View view) {
        LatLng min;
        LatLng max;
        if(mTeam.equals("blue")){
            min = mBlueMin;
            max = mBlueMax;
        } else{
            min = mRedMin;
            max = mRedMax;
        }
        double lat = mCurrentLocation.getLatitude();
        double lng = mCurrentLocation.getLongitude();
        if(Area.withinArea(new LatLng(lat,lng),min,max)){
            if(mTeam.equals("blue")){
                mDatabase.child("areas").child(mGameName).child("blueFlagLat").setValue(lat);
                mDatabase.child("areas").child(mGameName).child("blueFlagLong").setValue(lng);
                mDatabase.child("areas").child(mGameName).child("blueFlag").setValue(true);
            } else{
                mDatabase.child("areas").child(mGameName).child("redFlagLat").setValue(lat);
                mDatabase.child("areas").child(mGameName).child("redFlagLong").setValue(lng);
                mDatabase.child("areas").child(mGameName).child("redFlag").setValue(true);
            }

        } else{
            Toast.makeText(getApplicationContext(), "You are not in your team's territory!", Toast.LENGTH_SHORT).show();
        }

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
                    status.startResolutionForResult(PlaceFlagActivity.this, REQUEST_CHECK_SETTINGS);
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


        boolean success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.style_json));

        if (!success) {
            Log.e(TAG, "Style parsing failed.");
        }
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


            /*TODO: FOR TESTING PURPOSES ONLY; REMOVE WHEN DONE

            mDatabase.child("areas").child(mGameName).child("blueFlagLat").setValue(mCurrentLocation.getLatitude());
            mDatabase.child("areas").child(mGameName).child("blueFlagLong").setValue(mCurrentLocation.getLongitude());
            mDatabase.child("areas").child(mGameName).child("blueFlag").setValue(true);
            mDatabase.child("areas").child(mGameName).child("redFlagLat").setValue(mCurrentLocation.getLatitude()-.0002);
            mDatabase.child("areas").child(mGameName).child("redFlagLong").setValue(mCurrentLocation.getLongitude());
            mDatabase.child("areas").child(mGameName).child("redFlag").setValue(true);

             */
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

}
