package hack.watch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
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

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

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

                JSONArray faces = ob.getJSONArray("faces");
                Rect[] rects = new Rect[faces.length()];
                for (int j = 0; j < faces.length(); j++) {
                  JSONObject oneRect = faces.getJSONObject(j);
                  int x = oneRect.getInt("positionX") - 5, y = oneRect.getInt("positionY") - 5;
                  rects[j] = new Rect(x, y, x + oneRect.getInt("width") + 10, y + oneRect.getInt("height") + 10);
                }

                bitmap.add(new FaceBitmap(BitmapFactory.decodeByteArray(decodedString, 0,
                                                                        decodedString.length), rects));
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
      finish();

      Intent intent = new Intent(this, MainActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
    }

    schedule();
  }

  public void schedule() {
    final int scheduleVar = bitmap.isEmpty() ? 500 : 3000 / bitmap.size();

    final Paint p = new Paint();
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(2.0f);
    p.setAntiAlias(true);
    p.setFilterBitmap(true);
    p.setDither(true);
    p.setColor(Color.RED);

    timer.schedule(new TimerTask() {
      public void run() {
        if (!bitmap.isEmpty()) {
          final FaceBitmap dat = bitmap.poll();
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              loading.setVisibility(View.INVISIBLE);

              Bitmap bt = dat.getBitmap();


              if (dat.getFaces().length > 0) {
                // draw faces
                Bitmap tempBitmap = Bitmap
                    .createBitmap(bt.getWidth(), bt.getHeight(), Bitmap.Config.RGB_565);
                Canvas c = new Canvas(tempBitmap);
                c.drawBitmap(bt, 0, 0, null);
                for (Rect r : dat.getFaces()) {
                  c.drawRect(r, p);
                }

                video.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
              } else {
                video.setImageBitmap(bt);
              }

                if (dat.getFaces().length > 0 && !vibrating) {
                  // Vibrate for 500 milliseconds
                  ((Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE))
                      .vibrate(
                          500);
                  vibrating = true;

                  timer.schedule(new TimerTask() {
                    public void run() {
                      vibrating = false;
                    }
                  }, 2500);
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
    private Rect[] faces;

    public FaceBitmap(Bitmap bitmap, Rect[] faces) {
      this.bitmap = bitmap;
      this.faces = faces;
    }

    public Rect[] getFaces() {
      return faces;
    }

    public Bitmap getBitmap() {
      return bitmap;
    }
  }
}
