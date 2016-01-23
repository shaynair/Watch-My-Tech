package hack.watch;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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

  }

  public void join(View view) {

  }
}
