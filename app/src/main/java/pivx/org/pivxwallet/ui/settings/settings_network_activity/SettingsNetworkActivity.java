package rext.org.rextwallet.ui.settings.settings_network_activity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import rext.org.rextwallet.R;
import rext.org.rextwallet.ui.base.BaseActivity;

/**
 * Created by Neoperol on 6/8/17.
 */

public class SettingsNetworkActivity extends BaseActivity {

    View root;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        root = getLayoutInflater().inflate(R.layout.fragment_network, container);
        setTitle(R.string.settings_title_network);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }
}
