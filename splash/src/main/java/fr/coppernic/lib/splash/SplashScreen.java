package fr.coppernic.lib.splash;

import fr.coppernic.lib.splash.base.SplashScreenBase;

public class SplashScreen extends SplashScreenBase {

    @Override
    protected void onResume() {
        super.onResume();
        startTargetActivity();
    }

}
