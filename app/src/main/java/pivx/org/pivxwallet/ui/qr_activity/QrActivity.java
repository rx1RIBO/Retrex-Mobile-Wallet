package rext.org.rextwallet.ui.qr_activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import rext.org.rextwallet.R;
import rext.org.rextwallet.ui.base.BaseActivity;
import rext.org.rextwallet.utils.NavigationUtils;

/**
 * Created by furszy on 6/8/17.
 */

public class QrActivity extends BaseActivity {

    private View root;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        super.onCreateView(savedInstanceState, container);;
        root = getLayoutInflater().inflate(R.layout.qr_activity,container,true);
        setTitle(R.string.my_address);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = getIntent();
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

}
