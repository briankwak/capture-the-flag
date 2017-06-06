package csm117.ucla.capturetheflag;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MyService extends Service {

    private static final String TAG = MyService.class.getSimpleName();
    private String mGameName;
    private String mPlayerName;
    private DatabaseReference mDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mGameName = intent.getStringExtra("game");
        mPlayerName = intent.getStringExtra("player");
        mDatabase = FirebaseDatabase.getInstance().getReference();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        mDatabase.child("players").child(mGameName).child(mPlayerName).removeValue();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}