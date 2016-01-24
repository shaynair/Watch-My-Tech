package hack.watch;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.VideoView;

import com.github.nkzawa.emitter.Emitter;

import hack.net.StreamService;
import hack.net.WatchContext;

public class VideoActivity extends Activity {

  private ImageView video;
  private EditText edit;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_video);

    video = (ImageView)findViewById(R.id.image);
    edit = (EditText)findViewById(R.id.editText);

    AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
    anim.setDuration(1000);
    anim.setRepeatCount(1);
    ((ImageView)findViewById(R.id.eye)).startAnimation(anim);

    StreamService ss = ((WatchContext)getApplication()).getStream();

    ss.on("image", new Emitter.Listener() {
      @Override
      public void call(final Object... args) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            byte[] decodedString = Base64.decode((String) args[0], Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory
                .decodeByteArray(decodedString, 0, decodedString.length);
            video.setImageBitmap(decodedByte);
          }
        });
      }
    });
  }

  public void send(View view) {
    if (edit.getAlpha() < 1.0f) {
      AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
      anim.setDuration(1000);
      anim.setRepeatCount(1);
      edit.startAnimation(anim);
      return;
    }
    StreamService ss = ((WatchContext)getApplication()).getStream();

    ss.emit("customBroadCast", edit.getText());
  }
}
