package hack.watch;

import android.app.Application;
import android.content.Intent;

import hack.net.StreamService;

public class WatchContext extends Application {

  private StreamService service;

  @Override
  public void onCreate() {
    super.onCreate();
    service = new StreamService();
  }

  public void refresh() {
    service.refresh();
  }

  public StreamService getStream() {
    return service;
  }
}
