package csm117.ucla.capturetheflag;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.graphics.Color;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class WaitingRoomActivity extends Activity {

    private String mGameName;
    private String mPlayerName;
    private DatabaseReference mDatabase;

    private LinearLayout mBlueTeamView;
    private LinearLayout mRedTeamView;
    private LinearLayout mNoTeamView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        mGameName = getIntent().getStringExtra("game");
        mPlayerName = getIntent().getStringExtra("player");
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mBlueTeamView = (LinearLayout)findViewById(R.id.blueTeam);
        mRedTeamView = (LinearLayout)findViewById(R.id.redTeam);
        mNoTeamView = (LinearLayout)findViewById(R.id.noTeam);

        mDatabase.child("players").child(mGameName).orderByChild("time").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mBlueTeamView.removeAllViews();
                mRedTeamView.removeAllViews();
                mNoTeamView.removeAllViews();
                for(DataSnapshot child : dataSnapshot.getChildren()){
                    Player temp = (Player)child.getValue(Player.class);
                    addPlayer(child.getKey(),temp);
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {}
        });
    }

    public void addPlayer(String key, Player value){
        TextView view = new TextView(this);
        view.setText(key);
        switch(value.team){
            case "none":
                mNoTeamView.addView(view);
                break;
            case "red":
                mRedTeamView.addView(view);
                break;
            case "blue":
                mBlueTeamView.addView(view);
                break;
        }
    }

    public void pressBlueTeam(View view){
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/players/" + mGameName + "/" + mPlayerName + "/team","blue");
        childUpdates.put("/players/" + mGameName + "/" + mPlayerName + "/time",System.currentTimeMillis());
        mDatabase.updateChildren(childUpdates);
    }
    public void pressRedTeam(View view){
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/players/" + mGameName + "/" + mPlayerName + "/team","red");
        childUpdates.put("/players/" + mGameName + "/" + mPlayerName + "/time",System.currentTimeMillis());
        mDatabase.updateChildren(childUpdates);
    }
    public void pressNoTeam(View view){
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/players/" + mGameName + "/" + mPlayerName + "/team","none");
        childUpdates.put("/players/" + mGameName + "/" + mPlayerName + "/time",System.currentTimeMillis());
        mDatabase.updateChildren(childUpdates);
    }
}
