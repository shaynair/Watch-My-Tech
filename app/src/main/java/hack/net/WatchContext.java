package hack.net;

import android.app.Application;

public class WatchContext extends Application {

  private StreamService service;

  @Override
  public void onCreate() {
    super.onCreate();
    service = new StreamService();
  }

  public StreamService getStream() {
    return service;
  }
}
