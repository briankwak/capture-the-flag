package csm117.ucla.capturetheflag;

import android.location.Location;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Area{
    public double redMinLat;
    public double redMinLong;
    public double redMaxLat;
    public double redMaxLong;
    public double blueMinLat;
    public double blueMinLong;
    public double blueMaxLat;
    public double blueMaxLong;
    public double blueFlagLat;
    public double redFlagLat;
    public double blueFlagLong;
    public double redFlagLong;
    public boolean redFlag;  //placed
    public boolean blueFlag; // placed

    public Area(){
    }
    public Area(List<LatLng> redTeamArea, List<LatLng> blueTeamArea){
        redMinLat = redTeamArea.get(0).latitude;
        redMaxLat = redTeamArea.get(2).latitude;
        blueMinLat = blueTeamArea.get(0).latitude;
        blueMaxLat = blueTeamArea.get(2).latitude;
        redMinLong = redTeamArea.get(0).longitude;
        redMaxLong = redTeamArea.get(2).longitude;
        blueMinLong = blueTeamArea.get(0).longitude;
        blueMaxLong = blueTeamArea.get(2).longitude;
    }
    @Exclude
    public Map<String,Object> toMap() {

        HashMap<String, Object> result = new HashMap<>();
        result.put("redMinLat",redMinLat);
        result.put("redMaxLat",redMaxLat);
        result.put("blueMinLat",blueMinLat);
        result.put("blueMaxLat",blueMaxLat);
        result.put("redMinLong",redMinLong);
        result.put("redMaxLong",redMaxLong);
        result.put("blueMinLong",blueMinLong);
        result.put("blueMaxLong",blueMaxLong);
        return result;
    }

    public LatLng redMin(){
        return new LatLng(redMinLat,redMinLong);
    }
    public LatLng redMax(){
        return new LatLng(redMaxLat,redMaxLong);
    }
    public LatLng blueMin(){
        return new LatLng(blueMinLat,blueMinLong);
    }
    public LatLng blueMax(){
        return new LatLng(blueMaxLat,blueMaxLong);
    }
    public LatLng redFlag(){
        return new LatLng(redFlagLat,redFlagLong);
    }
    public LatLng blueFlag(){
        return new LatLng(blueFlagLat,blueFlagLong);
    }

    public static boolean withinArea(LatLng x, LatLng min, LatLng max){
        return min.latitude <= x.latitude && x.latitude <= max.latitude && min.longitude <= x.longitude && x.longitude <= max.longitude;
    }

    public static boolean withinCircle(LatLng x, Circle circle){
        float[] distance = new float[2];

        LatLng center = circle.getCenter();
        Location.distanceBetween( x.latitude, x.longitude,
                center.latitude, center.longitude, distance);

        return distance[0] <= circle.getRadius();
    }

}
