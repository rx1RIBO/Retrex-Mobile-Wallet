package rext.org.rextwallet.ui.initial;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import rext.org.rextwallet.RextApplication;
import rext.org.rextwallet.ui.splash_activity.SplashActivity;
import rext.org.rextwallet.ui.wallet_activity.WalletActivity;
import rext.org.rextwallet.utils.AppConf;

/**
 * Created by furszy on 8/19/17.
 */

public class InitialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RextApplication rextApplication = RextApplication.getInstance();
        AppConf appConf = rextApplication.getAppConf();
        // show report dialog if something happen with the previous process
        Intent intent;
        if (!appConf.isAppInit() || appConf.isSplashSoundEnabled()){
            intent = new Intent(this, SplashActivity.class);
        }else {
            intent = new Intent(this, WalletActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
