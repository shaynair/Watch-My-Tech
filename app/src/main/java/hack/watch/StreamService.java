package hack.watch;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class StreamService {

  private static final String URL = "watchmy.tech:3000";
  private Socket socket;

  public void make() {
    try {
      IO.Options opts = new IO.Options();
      opts.transports = new String[]{"websocket"};

      socket = IO.socket("http://" + URL, opts);
      socket.connect();
    } catch (URISyntaxException ignore) {}
  }

  public void refresh() {
    destroy();
    make();
  }

  public boolean connected() {
    return socket.connected();
  }

  public void emit(String name, Object... args) {
    socket.emit(name, args);
  }

  public void on(String name, Emitter.Listener callback) {
    socket.on(name, callback);
  }

  public void off() {
    socket.off();
  }

  public void off(String name) {
    socket.off(name);
  }

  public void destroy() {
    if (socket != null) {
      socket.disconnect();
      off();
    }
  }
}
