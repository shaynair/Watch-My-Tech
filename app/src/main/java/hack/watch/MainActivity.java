package hack.watch;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONObject;

import hack.net.StreamService;
import hack.net.WatchContext;

public class MainActivity extends AppCompatActivity {

  private ImageView eye, iconMsg;
  private TextView number;
  private ProgressBar progress;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(hack.watch.R.layout.activity_main);

    eye = (ImageView) findViewById(R.id.eye);
    iconMsg = (ImageView) findViewById(R.id.icon_msg);
    number = (TextView) findViewById(R.id.number);
    progress = (ProgressBar) findViewById(R.id.progress);

    gen(getCurrentFocus());
  }

  public void gen(View view) {
    progress.setVisibility(View.VISIBLE);
    iconMsg.setVisibility(View.GONE);

    StreamService ss = ((WatchContext)getApplication()).getStream();
    ss.on("startSessionSuccess", new Emitter.Listener() {
      @Override
      public void call(final Object... args) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            progress.setVisibility(View.GONE);
            number.setText(String.valueOf((Integer) args[0]));
          }
        });
      }
    });
    ss.emit("startSession");
  }

  public void join(View view) {
    progress.setVisibility(View.VISIBLE);
    iconMsg.setVisibility(View.GONE);

    StreamService ss = ((WatchContext)getApplication()).getStream();
    ss.on("joinSessionSuccess", new Emitter.Listener() {
      @Override
      public void call(final Object... args) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            progress.setVisibility(View.GONE);
            startActivity(new Intent(MainActivity.this, VideoActivity.class));
          }
        });
      }
    });
    ss.on("watch_error", new Emitter.Listener() {
      @Override
      public void call(final Object... args) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            iconMsg.setVisibility(View.VISIBLE);
          }
        });
      }
    });
    ss.emit("joinSession");
  }
}
