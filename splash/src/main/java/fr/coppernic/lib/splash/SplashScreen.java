package fr.coppernic.lib.splash;

public class SplashScreen extends SplashScreenBase {

    @Override
    protected void onResume() {
        super.onResume();
        startTargetActivity();
    }

}
