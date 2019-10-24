package fr.coppernic.lib.splash;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import fr.coppernic.lib.splash.base.SplashScreenBase;


/**
 * Class of splash screen that is asking for security permission before launching target activity.
 * <p>This class should be the entry point of the application. Thus it has the intent filter
 * <pre>
 * <code>
 * {@code
 * <intent-filter>
 *     <action android:name="android.intent.action.MAIN"/>
 *     <category android:name="android.intent.category.LAUNCHER"/>
 * </intent-filter>
 * }
 * </code>
 * </pre>
 * Target activity is put in manifest in meta data node of splash screen activity node.
 * Meta data node is as follow :
 * <pre>
 * <code>
 * {@code
 * <meta-data
 *     android:name="activity"
 *     android:value="fr.coppernic.master.setup.SetupActivity"/>
 * }
 * </code>
 * </pre>
 * <ul>
 * <li><b>name</b> Name of meta data key
 * <li><b>value</b> Full name of activity class to launch after splash
 * </ul>
 * <p>Example :
 * <pre>
 * <code>
 * {@code
 * <activity android:name="fr.coppernic.master.setup.SetupActivity"
 *     android:label="@string/app_name">
 * </activity>
 * <activity android:name="fr.coppernic.master.setup.CpcMasterSetup"
 *     android:icon="@drawable/android_blue"
 *     android:label="@string/app_name">
 * <intent-filter>
 *     <action android:name="android.intent.action.MAIN"/>
 *     <category android:name="android.intent.category.LAUNCHER"/>
 * </intent-filter>
 * <meta-data
 *     android:name="activity"
 *     android:value="fr.coppernic.master.setup.SetupActivity"/>
 * </activity>
 * }
 * </code>
 * </pre>
 */
public class PermissionSplashScreen extends SplashScreenBase
    implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final int UID_SYSTEM = 1000;

    private static final String KEY_REQUEST = "PermissionSplashScreen_request";
    private static final Logger LOG = LoggerFactory.getLogger(TAG);

    private final Set<String> pendingPermissions = new TreeSet<>();
    private String[] permissionsArray;


    /**
     * Concat strings
     *
     * @param in  list of strings to concatenate
     * @param sep String separator
     * @return Concatenated string
     */
    public static String concatString(Collection<String> in, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String tmp : in) {
            sb.append(tmp).append(sep);
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - sep.length());
        }
        return sb.toString();
    }

    /**
     * Return true if the app is a system one
     *
     * @param context Context
     * @return true if system, false otherwise
     */
    public static boolean isSharingSystemUid(Context context) {
        return (UID_SYSTEM == getPackageUid(context));
    }

    /**
     * Return the unique id of the context's package
     *
     * @param context Context
     * @return UID
     */
    public static int getPackageUid(Context context) {
        int ret = 0;
        String packageName = context.getPackageName();
        final PackageManager pm = context.getPackageManager();
        try {
            final PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_GIDS);
            ret = pi.applicationInfo.uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            PackageInfo info = getPackageManager()
                .getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                permissionsArray = info.requestedPermissions;
            } else {
                permissionsArray = new String[]{};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LOG.trace("onStart, current request : {}", getRequestNumber());
        requestPermissions(getDeniedPermissions(Arrays.asList(permissionsArray)));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LOG.trace("onStop, current request : {}", getRequestNumber());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // We are checking isSharingSystemUid() because we can rely on checkSelfPermission on regular app
        // Issue on ID Platform where app is killed and restarted in the middle of permission request
        if (requestCode != getRequestNumber() && isSharingSystemUid(this)) {
            LOG.error("Request does not correspond, {} instead of {}", requestCode, getRequestNumber());
            finish();
            return;
        } else {
            LOG.debug("onRequestPermissionsResult : {}", requestCode);
            //Reset the protection against multiple requests to be able to ask again.
            clearRequestNumber();
        }

        List<String> granted = new ArrayList<>();
        List<String> denied = new ArrayList<>();

        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                denied.add(permissions[i]);
            } else {
                granted.add(permissions[i]);
            }
        }

        LOG.debug("Granted : {}", concatString(granted, ", "));
        LOG.debug("Denied : {}", concatString(denied, ", "));

        // Call child class specific permission handlers
        onPermissionsGranted(granted);
        onPermissionsDenied(denied);

        //Clear pending permissions
        pendingPermissions.removeAll(granted);
        pendingPermissions.removeAll(denied);

        // Will trigger target activity if pending permissions is empty
        if (!shouldRequestPermissions(pendingPermissions)) {
            startTargetActivity();
        } else {
            LOG.debug("Waiting for next onRequestPermissionsResult");
        }
    }

    /**
     * Overrive this method to handle case where permissions are granted
     *
     * @param granted List of granted permissions
     */
    protected void onPermissionsGranted(List<String> granted) {

    }

    /**
     * Override this method to handle case where permissions are denied
     *
     * @param granted List of denied permissions
     */
    protected void onPermissionsDenied(List<String> granted) {

    }

    private List<String> getDeniedPermissions(List<String> permissions) {
        List<String> ret = new ArrayList<>();
        if (isSharingSystemUid(this)) {
            //Force asking permissions when sharing system uid
            ret.addAll(permissions);
        } else {
            for (String p : permissions) {
                if (ActivityCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_DENIED) {
                    ret.add(p);
                }
            }
        }
        return ret;
    }

    private void requestPermissions(Collection<String> permissions) {
        if (shouldRequestPermissions(permissions)) {
            int request = getRequestNumber() + 1;
            LOG.info("[" + request + "] Requesting permissions : \n" + concatString(permissions, ",\n"));
            pendingPermissions.addAll(permissions);
            //registering the current request to have only one request at a time.
            setRequestNumber(request);
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[]{}), request);
        } else if (isRequestOngoing()) {
            LOG.debug("Request is onGoing, waiting for onRequestPermissionsResult");
        } else {
            // No request ongoing and no need to request permission, starting target activity here
            startTargetActivity();
        }
    }

    /**
     * Called to determine if permission are needed to be asked
     *
     * @param permissions Permission list
     * @return true if request needs to be sent
     */
    protected boolean shouldRequestPermissions(Collection<String> permissions) {
        boolean ret = true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            LOG.info("Android OS version is below Marshmallow, do not ask anything");
            ret = false;
        } else if (permissions.isEmpty()) {
            LOG.info("Permissions list is empty, starting target activity...");
            ret = false;
        } else if (isRequestOngoing()) {
            // Protection against multiple requests
            LOG.info("Request is ongoing");
            ret = false;
        }
        return ret;
    }

    private boolean isRequestOngoing() {
        return getRequestNumber() > 0;
    }

    private int getRequestNumber() {
        return model.bundle.getInt(KEY_REQUEST, 0);
    }

    private void setRequestNumber(int i) {
        LOG.trace("Set request number " + i);
        model.bundle.putInt(KEY_REQUEST, i);
    }

    private void clearRequestNumber() {
        setRequestNumber(0);
    }
}
