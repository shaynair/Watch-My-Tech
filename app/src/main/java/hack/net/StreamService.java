package hack.net;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class StreamService extends Service {

  private static final String URL = "watchmy.tech:3000";
  private Socket socket;

  public StreamService() {
    try {
      socket = IO.socket("http://" + URL);
    } catch (URISyntaxException ignore) {}
  }

  @Override
  public void onCreate() {
    super.onCreate();

    socket.connect();
  }

  public void emit(String name, Object... args) {
    socket.emit(name, args);
  }

  public void on(String name, Emitter.Listener callback) {
    socket.on(name, callback);
  }

  @Override
  public IBinder onBind(Intent intent) {
    // TODO: Return the communication channel to the service.
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    socket.disconnect();
    socket.off();
  }
}
