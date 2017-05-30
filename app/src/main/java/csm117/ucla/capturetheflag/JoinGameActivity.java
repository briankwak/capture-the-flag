package csm117.ucla.capturetheflag;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class JoinGameActivity extends AppCompatActivity {
    protected static final String TAG = "JoinGameActivity";

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_game);
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void pressConfirm(View view){
        EditText gameEditText = (EditText)findViewById(R.id.game_name);
        final String gameName = gameEditText.getText().toString();
        EditText playerEditText = (EditText)findViewById(R.id.player_name);
        String playerName = playerEditText.getText().toString();
        if(gameName.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please choose a game name.", Toast.LENGTH_SHORT).show();
        } else if(playerName.length() == 0){
            Toast.makeText(getApplicationContext(), "Please choose a player name.", Toast.LENGTH_SHORT).show();
        } else {
            mDatabase.child("games").child(gameName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        if((boolean)dataSnapshot.getValue()){
                            Toast.makeText(getApplicationContext(), "Game already in progress", Toast.LENGTH_SHORT).show();
                        }
                        handleKey(gameName);
                        return;
                    }

                    Toast.makeText(getApplicationContext(), "No such game exists!", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onCancelled(DatabaseError firebaseError) {}
            });

        }
    }

    public void handleKey(String s){
        final String gameName = s;
        EditText playerEditText = (EditText)findViewById(R.id.player_name);
        final String playerName = playerEditText.getText().toString();

        mDatabase.child("players").child(gameName).child(playerName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
               if(dataSnapshot.exists()){
                   Toast.makeText(getApplicationContext(), "Player name already in use!", Toast.LENGTH_SHORT).show();
                   return;
                }
                Player player = new Player(new LatLng(0.0,0.0),System.currentTimeMillis());

                mDatabase.child("players").child(gameName).child(playerName).setValue(player.toMap());
                changeActivity(gameName,playerName);
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {}
        });
    }

    public void changeActivity(String gameName,String playerName){
        Intent intent = new Intent(this, WaitingRoomActivity.class);
        intent.putExtra("game",gameName);
        intent.putExtra("player",playerName);
        intent.putExtra("creator",false);
        startActivity(intent);
    }
}
