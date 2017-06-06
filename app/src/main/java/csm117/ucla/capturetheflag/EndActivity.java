package csm117.ucla.capturetheflag;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class EndActivity extends AppCompatActivity {
    private Boolean mWin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end);
        mWin = getIntent().getExtras().getBoolean("win");

        TextView winCondition = (TextView)findViewById(R.id.winCondition);
        if (mWin)
            winCondition.setText("Congrats You Win!");
        else
            winCondition.setText("You Lose D:");

    }
    public void pressBackToMenu(View view) {
        Toast.makeText(getApplicationContext(), "Going back to main menu", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);

    }
}