package fr.coppernic.lib.splash.base;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SplashScreenBase extends AppCompatActivity {
    public static final String TAG = "SplashScreen";
    private static final int REQUEST = 777;

    private static final Logger LOG = LoggerFactory.getLogger(TAG);
    private final Handler handler = new Handler();

    private Runnable runTargetActivity;
    private MetaConfig metaConfig;
    private Intent launcher;

    protected SplashViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        model = ViewModelProviders.of(this).get(SplashViewModel.class);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        metaConfig = new MetaConfig(this);
        launcher = metaConfig.getTargetIntent();
        if (launcher == null) {
            LOG.error("No intent to launch target activity");
            finish();
        } else {
            LOG.debug("Launcher intent : {}", launcher);
            runTargetActivity = new Runnable() {
                @Override
                public void run() {
                    if (shouldStartActivityForResult(launcher)) {
                        LOG.trace("Start activity for result : {}", launcher);
                        startActivityForResult(launcher, REQUEST);
                    } else {
                        launcher.setFlags(0);
                        LOG.trace("Start activity : {}", launcher);
                        startActivity(launcher);
                        finish();
                    }
                }
            };
        }
    }

    protected void startTargetActivity() {
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
        LOG.debug("onActivityResult, code : {}, result : {}, intent : {} ",
                  requestCode, resultCode, (data == null ? "null" : data));

        setResult(resultCode, data);
        finish();
    }

    //To be completed...
    private boolean shouldStartActivityForResult(Intent intent) {
        boolean ret;
        String action = intent.getAction();
        if (action != null
            && action.equals(Intent.ACTION_MAIN)
            && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            ret = false;
        } else {
            ret = (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) == 0
                  || (intent.getFlags() & Intent.FLAG_ACTIVITY_FORWARD_RESULT) == 0;
        }
        return ret;
    }
}
