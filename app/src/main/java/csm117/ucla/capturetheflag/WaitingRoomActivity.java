package csm117.ucla.capturetheflag;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

public class WaitingRoomActivity extends Activity {

    private String mGameName;
    private String mPlayerName;
    private boolean mTeamLeader;
    private String mTeam;
    private DatabaseReference mDatabase;

    private LinearLayout mBlueTeamView;
    private LinearLayout mRedTeamView;
    private LinearLayout mNoTeamView;
    private boolean mCreator;

    private boolean mNoTeamChange;


    @Override
    public void onBackPressed()
    {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit Game");
        builder.setMessage("Would you like to quit the game?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mDatabase.child("players").child(mGameName).child(mPlayerName).removeValue();

                if(mCreator){

                    mDatabase.child("areas").child(mGameName).removeValue();
                    mDatabase.child("games").child(mGameName).removeValue();
                    mDatabase.child("players").child(mGameName).removeValue();

                }
                goBack();
            }
        });
        builder.setNegativeButton("No", null);


        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        mGameName = getIntent().getStringExtra("game");
        mPlayerName = getIntent().getStringExtra("player");
        mCreator = getIntent().getExtras().getBoolean("creator");
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mTeamLeader = false;
        mTeam = "none";


        mDatabase.child("players").child(mGameName).child(mPlayerName).onDisconnect().removeValue();

        LinearLayout gameName = (LinearLayout)findViewById(R.id.gameName);
        TextView gameNameView = new TextView(this);
        gameNameView.setText("Game Name: "+mGameName);
        gameNameView.setGravity(Gravity.CENTER_HORIZONTAL);
        gameName.addView(gameNameView);
//
//        LinearLayout BlueTeamLayoutView = (LinearLayout)findViewById(R.id.blueTeamLayout);
//        BlueTeamLayoutView.setBackgroundColor(Color.BLUE);
//        BlueTeamLayoutView.setBackgroundResource(R.drawable.rounded_blue);

        mBlueTeamView = (LinearLayout) findViewById(R.id.blueTeam);
        mRedTeamView = (LinearLayout) findViewById(R.id.redTeam);
        mNoTeamView = (LinearLayout) findViewById(R.id.noTeam);

        if (mCreator) {
            Button button = new Button(this);
            button.setText("Start Game");
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    pressStart(view);
                }
            });

            LinearLayout startGameView = (LinearLayout) findViewById(R.id.startGame);
            startGameView.addView(button);
        }

        mDatabase.child("players").child(mGameName).orderByChild("time").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mBlueTeamView.removeAllViews();
                mRedTeamView.removeAllViews();
                mNoTeamView.removeAllViews();
                int bluePlayers = 0;
                int redPlayers = 0;
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Player temp = child.getValue(Player.class);
                    String team = temp.team;
                    String name = child.getKey();
                    String leader = "";
                    if(bluePlayers == 0 && team.equals("blue")){
                        bluePlayers++;
                        leader = " " + getResources().getString(R.string.leader);
                    } else if(redPlayers == 0 && team.equals("red")){
                        redPlayers++;
                        leader = " " + getResources().getString(R.string.leader);
                    }
                    if(name.equals(mPlayerName)){
                        mTeam = team;
                        mTeamLeader = !leader.equals("");

                    }
                    addPlayer(child.getKey()+leader, team);
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
            }
        });

        mDatabase.child("games").child(mGameName).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    if(!mCreator){
                        goBack();
                    }
                    return;
                }
                String str = (String)dataSnapshot.getValue();
                if(str.equals("started")){
                    mNoTeamChange = true;
                    Toast.makeText(getApplicationContext(), "Starting game", Toast.LENGTH_SHORT).show();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            startGame();
                        }
                    }, 2000);
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
            }
        });
    }

    public void addPlayer(String key, String team) {
        TextView view = new TextView(this);
        view.setText(key);
        switch (team) {
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

    public void pressBlueTeam(View view) {
        if(mNoTeamChange){
            return;
        }
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("team").setValue("blue");
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("time").setValue(System.currentTimeMillis());

    }

    public void pressRedTeam(View view) {
        if(mNoTeamChange){
            return;
        }
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("team").setValue("red");
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("time").setValue(System.currentTimeMillis());
    }

    public void pressNoTeam(View view) {
        if(mNoTeamChange){
            return;
        }
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("team").setValue("none");
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("time").setValue(System.currentTimeMillis());
    }

    public void pressStart(View view) {
        sortPlayers();
        mDatabase.child("games").child(mGameName).setValue("started");
    }

    public void startGame(){
        Intent intent = new Intent(this,PlaceFlagActivity.class);
        intent.putExtra("game",mGameName);
        intent.putExtra("player",mPlayerName);
        intent.putExtra("leader",mTeamLeader);
        intent.putExtra("team",mTeam);
        startActivity(intent);
        finish();

    }

    public void sortPlayers(){
        mDatabase.child("players").child(mGameName).orderByChild("time").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mNoTeamView.removeAllViews();

                int bluePlayers = 0;
                int redPlayers = 0;
                for (DataSnapshot child : dataSnapshot.getChildren()){
                    Player temp = child.getValue(Player.class);
                    if(temp.team.equals("red")){
                        redPlayers++;
                    } else if(temp.team.equals("blue")){
                        bluePlayers++;
                    }
                }

                Random rand = new Random();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Player temp = child.getValue(Player.class);
                    String team = temp.team;
                    if(team.equals("none")) {
                        if (bluePlayers < redPlayers) {
                            team = "blue";
                        } else if (redPlayers < bluePlayers) {
                            team = "red";
                        } else {
                            team = (rand.nextInt(2) == 0) ? "red" : "blue";
                        }
                        String name = child.getKey();
                        String leader = "";
                        if(bluePlayers == 0 && team.equals("blue")){
                            leader = " (LEADER)";
                        } else if(redPlayers == 0 && team.equals("red")){
                            leader = " (LEADER)";
                        }
                        if(team.equals("blue")) bluePlayers++; else redPlayers++;
                        if(name.equals(mPlayerName)){
                            mTeam = team;
                            mTeamLeader = !leader.equals("");
                        }
                        addPlayer(child.getKey()+leader, team);
                        mDatabase.child("players").child(mGameName).child(name).child("team").setValue(team);

                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
            }
        });
    }

    public void goBack(){
        startActivity(new Intent(WaitingRoomActivity.this, MainActivity.class));
        finish();
    }


}
