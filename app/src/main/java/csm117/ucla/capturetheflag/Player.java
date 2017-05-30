package csm117.ucla.capturetheflag;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian on 5/28/2017.
 */

public class Player{
    public LatLng latLng;
    public String team;
    public long time;

    public Player(){}
    public Player(LatLng latLng,long time){
        this.latLng = latLng;
        this.team = "none";
        this.time = time;
    }

    @Exclude
    public Map<String,Object> toMap(){
        HashMap<String,Object> location = new HashMap<>();
        location.put("location",latLng);
        location.put("team",team);
        location.put("time",time);
        return location;
    }
}