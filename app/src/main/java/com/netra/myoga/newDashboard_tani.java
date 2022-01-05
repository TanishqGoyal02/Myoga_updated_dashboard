package com.netra.myoga;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;


public class newDashboard_tani extends AppCompatActivity  {


        CardView cardView1, cardView2, cardView3;
        ImageView imageView3;
        TextView textView11,textView12,textView13,textView14,textView15,textView16;
        Animation ani_from_bottom, ani_from_top,ani_from_left;
        Button button5,button6,button7;
        boolean isPressed = false;

        @Override
        protected void onCreate(Bundle savedInstance) {
            super.onCreate(savedInstance);
            setContentView(R.layout.improved_das_tani);

        cardView1 = findViewById(R.id.cardview1);
        cardView2 = findViewById(R.id.cardview2);
        cardView3 = findViewById(R.id.cardview3);
        imageView3=findViewById(R.id.imageView3);
        textView11 = findViewById(R.id.textView11);
        textView12 = findViewById(R.id.textView12);
        textView13 = findViewById(R.id.textView13);
        textView14 = findViewById(R.id.textView14);
        textView15 = findViewById(R.id.textView15);
        textView16 = findViewById(R.id.textView16);
        button5 = findViewById(R.id.button5);
        button6 = findViewById(R.id.button6);
        button7 = findViewById(R.id.button7);



//Load Animations
        ani_from_bottom = AnimationUtils.loadAnimation(this, R.anim.ani_from_bottom);
        ani_from_top = AnimationUtils.loadAnimation(this, R.anim.ani_from_top);
        ani_from_left = AnimationUtils.loadAnimation(this, R.anim.ani_from_left);
        //set Animation

        cardView1.setAnimation(ani_from_bottom);
        cardView2.setAnimation(ani_from_bottom);
        cardView3.setAnimation(ani_from_bottom);
        imageView3.setAnimation(ani_from_top);
        textView11.setAnimation(ani_from_left);
        textView12.setAnimation(ani_from_left);
        textView13.setAnimation(ani_from_left);
        textView14.setAnimation(ani_from_left);
        textView15.setAnimation(ani_from_left);
        textView16.setAnimation(ani_from_left);


        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i("TEST", "OLD DASBOARDGOT CLICKED");
                Intent intent = new Intent(newDashboard_tani.this, DashboardActivity.class);
                startActivity(intent);
            }
        });


        button6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i("TEST", "DOJO button clicked");
                Intent intent = new Intent(newDashboard_tani.this, MainActivity.class);
                startActivity(intent);
            }
        });



        button7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i("TEST", "SOS button clicked");
                Intent intent = new Intent(newDashboard_tani.this, SOSActivity.class);
                startActivity(intent);
            }
        });


        //Hide status bar and navigation bar at the bottom
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        this.getWindow().getDecorView().setSystemUiVisibility(

                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }


    @Override
    public void onBackPressed() {

        if (isPressed){

            finishAffinity();
            System.exit(0);
        }
        else{
            Toast.makeText(getApplicationContext(),"Please click back again to exit",Toast.LENGTH_SHORT).show();
            isPressed = true;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                isPressed = false;
            }
        };

        new Handler().postDelayed(runnable,2000);
    }


}
