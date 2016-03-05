package app.ssru.mrsmile.signlanguage;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;


public class SplashScreen extends AppCompatActivity {

    private static FrontBuild frontBuild;
    private static TextView txtShow;
    private static long Delay = 3500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);


        frontBuild = new FrontBuild(getApplicationContext());
        txtShow = (TextView) findViewById(R.id.txtShow);
        txtShow.setTypeface(frontBuild.CANTERBURY);

        // Create a Timer
        Timer RunSplash = new Timer();

        // Task to do when the timer ends
        TimerTask ShowSplash = new TimerTask() {
            @Override
            public void run() {
                finish();

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        };

        // Start the timer
        RunSplash.schedule(ShowSplash, Delay);
    }

}
