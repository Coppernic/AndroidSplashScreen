package fr.coppernic.lib.splash;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

import timber.log.Timber;

public class SplashScreenBase extends Activity {
    private static final String TAG = "SplashScreenBase";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final int REQUEST = 777;
    private final Handler handler = new Handler();
    private Runnable runTargetActivity;
    private MetaConfig metaConfig;
    private Intent launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        metaConfig = new MetaConfig(this);
        launcher = metaConfig.getTargetIntent();
        if (launcher == null) {
            Timber.e("No intent to launch target activity");
            finish();
        } else {
            Timber.d("Launcher intent : %s", launcher);
            runTargetActivity = new Runnable() {
                @Override
                public void run() {
                    if (shouldStartActivityForResult(launcher)) {
                        if (DEBUG) {
                            Timber.v("Start activity for result : %s", launcher);
                        }
                        startActivityForResult(launcher, REQUEST);
                    } else {
                        launcher.setFlags(0);
                        if (DEBUG) {
                            Timber.v("Start activity : %s", launcher);
                        }
                        startActivity(launcher);
                        finish();
                    }
                }
            };
        }
    }

    void startTargetActivity() {
        if (runTargetActivity != null) {
            handler.postDelayed(runTargetActivity, metaConfig.getTiming());
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (runTargetActivity != null) {
            handler.removeCallbacks(runTargetActivity);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) {
            Timber.d("onActivityResult, code : %d, result : %d, intent : %s ",
                     requestCode, resultCode, (data == null ? "null" : data));
        }
        setResult(resultCode, data);
        finish();
    }

    //TODO To be completed...
    private boolean shouldStartActivityForResult(Intent intent) {
        boolean ret = true;
        String action = intent.getAction();
        if (action != null
            && action.equals(Intent.ACTION_MAIN)
            && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            ret = false;
        } else if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            ret = false;
        }
        return ret;
    }
}
