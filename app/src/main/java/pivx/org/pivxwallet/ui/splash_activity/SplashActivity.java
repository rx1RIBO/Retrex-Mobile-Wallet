package rext.org.rextwallet.ui.splash_activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.VideoView;

import org.slf4j.LoggerFactory;

import rext.org.rextwallet.RextApplication;
import rext.org.rextwallet.R;
import rext.org.rextwallet.ui.loading.LoadingActivity;
import rext.org.rextwallet.ui.start_activity.StartActivity;
import rext.org.rextwallet.ui.wallet_activity.WalletActivity;

import static rext.org.rextwallet.service.IntentsConstants.ACTION_APP_CORE_CRASH;

/**
 * Created by Neoperol on 6/13/17.
 */

public class SplashActivity extends AppCompatActivity {
    /** Duration of wait **/
    private final int SPLASH_DISPLAY_LENGTH = 2500;
    private boolean ispaused = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler().postDelayed(this::jump, SPLASH_DISPLAY_LENGTH);
    }


    private void jump() {

        RextApplication app = RextApplication.getInstance();
        if (!app.hasCoreCrashed()) {
            if (app.isCoreStarting()){
                Intent intent = new Intent(this, LoadingActivity.class);
                startActivity(intent);
            }else {
                if (app.getAppConf().isAppInit()) {
                    Intent intent = new Intent(this, WalletActivity.class);
                    startActivity(intent);
                } else {
                    // Jump to your Next Activity or MainActivity
                    Intent intent = new Intent(this, StartActivity.class);
                    startActivity(intent);
                }
            }
        }
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ispaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ispaused) {
            jump();
        }

    }
}
