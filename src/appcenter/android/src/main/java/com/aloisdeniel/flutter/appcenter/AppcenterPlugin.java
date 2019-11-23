package com.aloisdeniel.flutter.appcenter;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import android.app.Application;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.distribute.Distribute;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.Log;

import com.microsoft.appcenter.distribute.Distribute;
import com.microsoft.appcenter.distribute.DistributeListener;
import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.UpdateAction;

/**
 * AppcenterPlugin
 */
public class AppcenterPlugin implements MethodCallHandler {

  private Registrar registrar;

  private AppcenterPlugin(Registrar registrar) {
    this.registrar = registrar;
  }

  /**
   * Plugin registration.
   */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(),
        "aloisdeniel.github.com/flutter_plugin_appcenter/appcenter");

    final AppcenterPlugin plugin = new AppcenterPlugin(registrar);
    channel.setMethodCallHandler(plugin);
  }

  @Override
  public void onMethodCall(MethodCall call, final Result result) {

    Application app = this.registrar.activity().getApplication();

    switch (call.method) {
    case "installId":
      AppCenter.getInstallId().thenAccept(new AppCenterConsumer<UUID>() {
        @Override
        public void accept(UUID uuid) {
          result.success(uuid.toString());
        }
      });

      break;
    case "isEnabled":
      AppCenter.isEnabled().thenAccept(new AppCenterConsumer<Boolean>() {
        @Override
        public void accept(Boolean enabled) {
          result.success(enabled.booleanValue());
        }
      });

      break;
    case "setEnabled":
      Boolean isEnabled = call.argument("isEnabled");
      AppCenter.setEnabled(isEnabled).thenAccept(new AppCenterConsumer<Void>() {
        @Override
        public void accept(Void v) {
          result.success(null);
        }
      });
      break;
    case "configure":
      String secret = call.argument("app_secret");
      AppCenter.configure(app, secret);
      result.success(null);
      break;
    case "start":
      Log.d("xtremeload", "APPCENTER START");
      String start_secret = call.argument("app_secret");
      List<String> services = call.argument("services");
      List<Class> servicesClasses = new ArrayList<Class>();
      for (String name : services) {
        try {
          Class c = Class.forName(name);
          servicesClasses.add(c);
        } catch (ClassNotFoundException notFound) {
          System.out.print(notFound.getException().toString());
        }
      }

      servicesClasses.add(Distribute.class);
      Class[] servicesClassesArray = new Class[servicesClasses.size()];
      servicesClassesArray = servicesClasses.toArray(servicesClassesArray);
      Distribute.setListener(new MyDistributeListener());
      AppCenter.start(app, start_secret, servicesClassesArray);
      Log.d("xtremeload", "APPCENTER STARTED");

      result.success(null);
      break;

    default:
      result.notImplemented();
      break;
    }
  }

  class MyDistributeListener implements DistributeListener {
    @Override
    public boolean onReleaseAvailable(Activity activity, ReleaseDetails releaseDetails) {

      // Look at releaseDetails public methods to get version information, release
      // notes text or release notes URL
      Log.d("xtremeload", "Update available");
      String versionName = releaseDetails.getShortVersion();
      int versionCode = releaseDetails.getVersion();
      String releaseNotes = releaseDetails.getReleaseNotes();
      Uri releaseNotesUrl = releaseDetails.getReleaseNotesUrl();

      // Build our own dialog title and message
      AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
      dialogBuilder.setTitle("Version " + versionName + " available!"); // you should use a string resource instead of
                                                                        // course, this is just to simplify example
      dialogBuilder.setMessage(releaseNotes);

      // Mimic default SDK buttons
      dialogBuilder.setPositiveButton(
          com.microsoft.appcenter.distribute.R.string.appcenter_distribute_update_dialog_download,
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

              // This method is used to tell the SDK what button was clicked
              Distribute.notifyUpdateAction(UpdateAction.UPDATE);
            }
          });

      // We can postpone the release only if the update is not mandatory
      if (!releaseDetails.isMandatoryUpdate()) {
        dialogBuilder.setNegativeButton(
            com.microsoft.appcenter.distribute.R.string.appcenter_distribute_update_dialog_postpone,
            new DialogInterface.OnClickListener() {

              @Override
              public void onClick(DialogInterface dialog, int which) {

                // This method is used to tell the SDK what button was clicked
                Distribute.notifyUpdateAction(UpdateAction.POSTPONE);
              }
            });
      }
      dialogBuilder.setCancelable(false); // if it's cancelable you should map cancel to postpone, but only for optional
                                          // updates
      dialogBuilder.create().show();

      // Return true if you are using your own dialog, false otherwise
      return true;
    }
  }
}
