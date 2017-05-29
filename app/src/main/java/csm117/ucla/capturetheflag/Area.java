package csm117.ucla.capturetheflag;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Area{
    public LatLng redMin;
    public LatLng redMax;
    public LatLng blueMin;
    public LatLng blueMax;

    public Area(){
    }
    public Area(List<LatLng> redTeamArea, List<LatLng> blueTeamArea){
        redMin = redTeamArea.get(0);
        redMax = redTeamArea.get(2);
        blueMin = blueTeamArea.get(0);
        blueMax = blueTeamArea.get(2);
    }
    @Exclude
    public Map<String,Object> toMap() {

        HashMap<String, Object> result = new HashMap<>();
        result.put("redMin",redMin);
        result.put("redMax",redMax);
        result.put("blueMin",blueMin);
        result.put("blueMax",blueMax);
        return result;
    }
}
