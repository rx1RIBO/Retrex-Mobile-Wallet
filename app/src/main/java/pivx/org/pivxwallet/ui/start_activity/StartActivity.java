package rext.org.rextwallet.ui.start_activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import rext.org.rextwallet.R;
import rext.org.rextwallet.ui.base.BaseActivity;
import rext.org.rextwallet.ui.restore_activity.RestoreActivity;
import rext.org.rextwallet.ui.tutorial_activity.TutorialActivity;

import static rext.org.rextwallet.ui.restore_activity.RestoreActivity.ACTION_RESTORE_AND_JUMP_TO_WIZARD;

/**
 * Created by mati on 18/04/17.
 */

public class StartActivity extends BaseActivity {

    private Button buttonCreate;
    private Button buttonRestore;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        getLayoutInflater().inflate(R.layout.fragment_start, container);

        buttonCreate = (Button) findViewById(R.id.btnCreate);
        buttonCreate.setOnClickListener(v -> {
            // Open Create Wallet
            rextModule.createWallet();
            Intent myIntent = new Intent(v.getContext(), TutorialActivity.class);
            startActivity(myIntent);
            finish();
        });

        // Open Restore Wallet
        buttonRestore = (Button) findViewById(R.id.btnRestore);
        buttonRestore.setOnClickListener(v -> {
            Intent myIntent = new Intent(v.getContext(), RestoreActivity.class);
            myIntent.setAction(ACTION_RESTORE_AND_JUMP_TO_WIZARD);
            startActivity(myIntent);
        });

    }

    public boolean hasToolbar() {
        return false;
    }

    public boolean isFullScreen(){
        return true;
    }

    @Override
    public boolean isCoreNeeded(){
        return false;
    }
}
