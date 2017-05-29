package csm117.ucla.capturetheflag;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian on 5/28/2017.
 */

public class Player{
    public String name;
    public LatLng latLng;

    public Player(){}
    public Player(String name, LatLng latLng){
        this.name = name;
        this.latLng = latLng;
    }

    @Exclude
    public Map<String,Object> toMap(){
        HashMap<String,Object> location = new HashMap<>();
        location.put("location",latLng);
        return location;
    }
}