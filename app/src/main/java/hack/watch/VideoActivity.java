package hack.watch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Base64;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import hack.net.StreamService;
import io.socket.emitter.Emitter;

public class VideoActivity extends Activity {

  private ImageView video;
  private TextView loading;
  private EditText edit;
  private int sessionID = 0;
  private Queue<FaceBitmap> bitmap = new ConcurrentLinkedQueue<>();

  private boolean vibrating = false;
  private Timer timer = new Timer();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_video);

    Intent intent = getIntent();
    if (intent != null) {
      sessionID = intent.getIntExtra("SESSION", sessionID);
    }

    video = (ImageView)findViewById(R.id.image);
    edit = (EditText)findViewById(R.id.editText);
    loading = (TextView)findViewById(R.id.textView3);
    final ImageView eye = (ImageView)findViewById(R.id.eye);
    AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
    anim.setDuration(3000);
    anim.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationEnd(Animation animation) {
        eye.setVisibility(View.INVISIBLE);
      }

      @Override
      public void onAnimationStart(Animation animation) {
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }
    });
    eye.startAnimation(anim);

    StreamService ss = ((WatchContext)getApplication()).getStream();

    ss.off();
    ss.on("watch_error", new Emitter.Listener() {
      @Override
      public void call(final Object... args) {
        System.out.println("================Error; " + args[0]);
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            edit.setError((String) args[0]);
            edit.requestFocus();
          }
        });
      }
    });
    ss.on("image", new Emitter.Listener() {
      @Override
      public void call(final Object... args) {
        System.out.println(
            "================Got " + args.length + ", " + ((JSONArray) args[0]).length() +
            " images.");
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            try {
              JSONArray arr = ((JSONArray) args[0]);
              for (int i = 0; i < arr.length(); i++) {
                JSONObject ob = arr.getJSONObject(i);
                byte[] decodedString =
                    Base64.decode(ob.getString("stringData")
                                    .replace("data:image/png;base64,", ""),
                                  Base64.DEFAULT);
                bitmap.add(new FaceBitmap(BitmapFactory
                                              .decodeByteArray(decodedString, 0,
                                                               decodedString.length),
                                          ob.getBoolean("foundFace")));
              }
            } catch (JSONException ignore) {
            }
          }
        });
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();

    StreamService ss = ((WatchContext)getApplication()).getStream();
    if (!ss.connected()) {
      Intent intent = new Intent(this, MainActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
    }

    schedule();
  }

  public void schedule() {
    final int scheduleVar = bitmap.isEmpty() ? 500 : 3000 / bitmap.size();

    System.out.println("Still got " + bitmap.size() + " images in " + scheduleVar + " time.");

    timer.schedule(new TimerTask() {
      public void run() {
        if (!bitmap.isEmpty()) {
          final FaceBitmap dat = bitmap.poll();
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              loading.setVisibility(View.INVISIBLE);
              video.setImageBitmap(dat.getBitmap());

              if (dat.isFoundFace() && !vibrating) {
                // Vibrate for 500 milliseconds
                ((Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE))
                    .vibrate(
                        500);
                vibrating = true;

                timer.schedule(new TimerTask() {
                  public void run() {
                    vibrating = false;
                  }
                }, 2000);
              }
            }
          });
        }
        schedule();
      }
    }, scheduleVar);
  }

  public void send(View view) {
    edit.setError(null);
    if (edit.getAlpha() < 1.0f) {
      AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
      anim.setDuration(1000);
      anim.setAnimationListener(new Animation.AnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
          edit.setAlpha(1);
        }
        @Override
        public void onAnimationStart(Animation animation) {
        }
        @Override
        public void onAnimationRepeat(Animation animation) {
        }
      });
      edit.startAnimation(anim);
      return;
    }
    StreamService ss = ((WatchContext)getApplication()).getStream();

    try {
      JSONObject send = new JSONObject();
      send.put("sessionID", sessionID);
      send.put("message", edit.getText().toString());

      ss.emit("customBroadCast", send);
    } catch (JSONException ignore) {}
  }

  @Override
  public void onStop() {
    super.onStop();

    timer.purge();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    timer.cancel();
  }

  private static class FaceBitmap {
    private Bitmap bitmap;
    private boolean foundFace = false;

    public FaceBitmap(Bitmap bitmap, boolean foundFace) {
      this.bitmap = bitmap;
      this.foundFace = foundFace;
    }

    public boolean isFoundFace() {
      return foundFace;
    }

    public Bitmap getBitmap() {
      return bitmap;
    }
  }
}
