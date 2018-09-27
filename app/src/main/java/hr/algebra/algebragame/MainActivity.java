package hr.algebra.algebragame;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.RelativeLayout;

import hr.algebra.algebragame.views.SpotOnView;

public class MainActivity extends AppCompatActivity {

    private SpotOnView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);
        view = new SpotOnView(this, getPreferences(Context.MODE_PRIVATE), relativeLayout);
        relativeLayout.addView(view, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.resume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.pause();
    }
}
