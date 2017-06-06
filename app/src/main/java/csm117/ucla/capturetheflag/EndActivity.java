package csm117.ucla.capturetheflag;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EndActivity extends AppCompatActivity {
    private Boolean mWin;
    private DatabaseReference mDatabase;
    private String mGameName;
    private String mPlayerName;
    private String winningTeam;


    private LinearLayout mBlueTeamView;
    private LinearLayout mRedTeamView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end);
        mBlueTeamView = (LinearLayout) findViewById(R.id.blueTeam);
        mRedTeamView = (LinearLayout) findViewById(R.id.redTeam);


        mWin = getIntent().getExtras().getBoolean("win");
        winningTeam = getIntent().getStringExtra("winningTeam");
        winningTeam = winningTeam.substring(0,1).toUpperCase() + winningTeam.substring(1).toLowerCase();
        mGameName = getIntent().getStringExtra("game");
        mPlayerName = getIntent().getStringExtra("player");
        mDatabase = FirebaseDatabase.getInstance().getReference();

        TextView winCondition = (TextView)findViewById(R.id.winCondition);
       /* if (mWin)
            winCondition.setText("Winner "); //Fix
        else
            winCondition.setText("You Lose D:");*/
        winCondition.setText(winningTeam + " Team Wins!");


        mDatabase.child("players").child(mGameName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot child : dataSnapshot.getChildren()) {
                    String name = child.getKey();
                    Player player = child.getValue(Player.class);
                    addPlayer(name, player.team);
                    }
                }


            @Override
            public void onCancelled(DatabaseError firebaseError) {
            }
        });

    }
    public void pressBackToMenu(View view) {
        mDatabase.child("players").child(mGameName).child(mPlayerName).removeValue();
        Toast.makeText(getApplicationContext(), "Going back to main menu", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();

    }

    public void addPlayer(String key, String team) {
        TextView view = new TextView(this);
        view.setText(key);
        switch (team) {
            case "red":
                mRedTeamView.addView(view);
                break;
            case "blue":
                mBlueTeamView.addView(view);
                break;
        }
    }

    @Override
    public void onBackPressed()
    {
        pressBackToMenu(null);

    }
}
