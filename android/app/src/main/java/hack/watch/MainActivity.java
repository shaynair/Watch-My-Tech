package hack.watch;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import io.socket.emitter.Emitter;

public class MainActivity extends Activity {

  private ImageView eye, iconMsg;
  private TextView number;
  private ProgressBar progress;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(hack.watch.R.layout.activity_main);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    eye = (ImageView) findViewById(R.id.eye);
    iconMsg = (ImageView) findViewById(R.id.icon_msg);
    number = (TextView) findViewById(R.id.number);
    progress = (ProgressBar) findViewById(R.id.progress);

  }

  @Override
  public void onResume() {
    super.onResume();

    progress.setVisibility(View.VISIBLE);
    number.setText("");
    number.setFocusable(false);
    number.setClickable(false);
    number.setEnabled(false);
    number.setFocusableInTouchMode(false);
    number.setCursorVisible(false);
    iconMsg.setVisibility(View.GONE);

    StreamService ss = ((WatchContext)getApplication()).getStream();
    ss.refresh();
    ss.off();
    ss.on("startSessionSuccess", new Emitter.Listener() {
      @Override
      public void call(final Object... args) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            number.setFocusable(true);
            number.setFocusableInTouchMode(true);
            number.setClickable(true);
            number.setEnabled(true);
            number.setCursorVisible(true);

            progress.setVisibility(View.GONE);
            number.setText(String.valueOf((Integer) args[0]));
          }
        });
      }
    });
    final Emitter.Listener transitionListener = new Emitter.Listener() {
      @Override
      public void call(final Object... args) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            progress.setVisibility(View.GONE);
            Intent intent = new Intent(MainActivity.this, VideoActivity.class);
            intent.putExtra("SESSION", Integer.parseInt(number.getText().toString()));

            startActivity(intent);
          }
        });
      }
    };
    ss.on("joinSessionSuccess", transitionListener);
    ss.on("image", transitionListener);
    ss.on("watch_error", new Emitter.Listener() {
      @Override
      public void call(final Object... args) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            progress.setVisibility(View.GONE);
            iconMsg.setVisibility(View.VISIBLE);
            number.setError((String)args[0]);
            number.requestFocus();
          }
        });
      }
    });

    ss.emit("startSession");
  }

  public void gen(View view) {
    progress.setVisibility(View.VISIBLE);
    iconMsg.setVisibility(View.GONE);
    number.setError(null);

    StreamService ss = ((WatchContext)getApplication()).getStream();
    if (ss.connected()) {
      ss.emit("startSession");
    }
  }

  public void join(View view) {
    progress.setVisibility(View.VISIBLE);
    iconMsg.setVisibility(View.GONE);
    number.setError(null);

    StreamService ss = ((WatchContext)getApplication()).getStream();
    ss.emit("joinSession", number.getText().toString());
  }

  @Override
  public void onDestroy(){
    super.onDestroy();

    StreamService ss = ((WatchContext)getApplication()).getStream();
    ss.destroy();
  }
}
