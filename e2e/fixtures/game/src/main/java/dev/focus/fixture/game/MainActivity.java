package dev.focus.fixture.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/** One line of text, so a test can see that this app, and not something else, is in front. */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView text = new TextView(this);
        text.setText("Dummy Game is open");
        text.setTextSize(24);
        text.setGravity(Gravity.CENTER);
        setContentView(text);
    }
}
