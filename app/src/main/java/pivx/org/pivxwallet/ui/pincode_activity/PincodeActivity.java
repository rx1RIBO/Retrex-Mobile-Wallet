package rext.org.rextwallet.ui.pincode_activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;
import java.util.Random;

import global.PivtrumGlobalData;
import pivtrum.PivtrumPeerData;
import rext.org.rextwallet.R;
import rext.org.rextwallet.module.RextContext;
import rext.org.rextwallet.ui.backup_mnemonic_activity.MnemonicActivity;
import rext.org.rextwallet.ui.base.BaseActivity;
import rext.org.rextwallet.ui.loading.LoadingActivity;
import rext.org.rextwallet.ui.settings.settings_pincode_activity.KeyboardFragment;
import rext.org.rextwallet.ui.start_activity.StartActivity;

import static rext.org.rextwallet.ui.backup_mnemonic_activity.MnemonicActivity.INTENT_EXTRA_INIT_VIEW;

/**
 * Created by Neoperol on 4/20/17.
 */

public class PincodeActivity extends BaseActivity implements KeyboardFragment.onKeyListener {

    public static final String CHECK_PIN = "check_pin";
    private static final int REQUEST_LOADING = 201;

    private boolean checkPin = false;

    private ImageView i1, i2, i3, i4;
    private int[] pin = new int[4];
    private int lastPos = 0;

    private KeyboardFragment keyboardFragment;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {

        if (getIntent()!=null && getIntent().hasExtra(CHECK_PIN)){
            checkPin = true;
        }

        if (rextApplication.getAppConf().getPincode()!=null && !checkPin){
            goNext();
            finish();
        }

        getLayoutInflater().inflate(R.layout.fragment_pincode, container);
        setTitle(R.string.title_pincode);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        i1 = (ImageView) findViewById(R.id.imageview_circle1);
        i2 = (ImageView) findViewById(R.id.imageview_circle2);
        i3 = (ImageView) findViewById(R.id.imageview_circle3);
        i4 = (ImageView) findViewById(R.id.imageview_circle4);
        keyboardFragment = (KeyboardFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_keyboard);
        keyboardFragment.setOnKeyListener(this);
        keyboardFragment.setTextButtonsColor(Color.WHITE);
        int clearID = getResources().getIdentifier(String.valueOf(R.drawable.ic_keyboard_clear_white), "drawable", getPackageName());
        int nextID = getResources().getIdentifier(String.valueOf(R.drawable.ic_keyboard_next_white), "drawable", getPackageName());
        keyboardFragment.setNextButton(nextID);
        keyboardFragment.setBackButton(clearID);
    }

    private void goNext() {
        if (rextApplication.getAppConf().getTrustedNode() == null){
            // select random trusted node
            //List<PivtrumPeerData> nodes = PivtrumGlobalData.listTrustedHosts(RextContext.NETWORK_PARAMETERS ,RextContext.NETWORK_PARAMETERS.getPort());
            //Random random = new Random();
            //rextApplication.setTrustedServer(nodes.get(random.nextInt(nodes.size())));
            if (rextModule.isStarted())
                rextApplication.stopBlockchain();
        }

        rextApplication.getAppConf().setAppInit(true);

        Intent myIntent = new Intent(PincodeActivity.this, LoadingActivity.class);
        myIntent.putExtra(INTENT_EXTRA_INIT_VIEW,true);
        startActivityForResult(myIntent, REQUEST_LOADING);
    }


    @Override
    public void onKeyClicked(KeyboardFragment.KEYS key) {
        if (lastPos<4) {
            if (key.getValue() < 10) {
                pin[lastPos] = key.getValue();
                activeCheck(lastPos);
                lastPos++;
                if (lastPos == 4) {
                    String pincode = String.valueOf(pin[0]) + String.valueOf(pin[1]) + String.valueOf(pin[2]) + String.valueOf(pin[3]);

                    if (!checkPin) {
                        rextApplication.getAppConf().savePincode(pincode);
                        Toast.makeText(this, R.string.pincode_saved, Toast.LENGTH_SHORT).show();
                        goNext();
                    }else {
                        // check pin and return result
                        if(rextApplication.getAppConf().getPincode().equals(pincode)){
                            Intent intent = new Intent();
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        }else {
                            Toast.makeText(this,R.string.bad_pin_code,Toast.LENGTH_LONG).show();
                            clear();
                        }
                    }
                }
            } else if (key == KeyboardFragment.KEYS.DELETE) {
                if (lastPos!=0) {
                    lastPos--;
                    unactiveCheck(lastPos);
                }
            } else if (key == KeyboardFragment.KEYS.CLEAR) {
                clear();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // todo: controlar esto
        if (rextApplication.getAppConf().getPincode() == null){
            startActivity(new Intent(this, StartActivity.class));
            finish();
        }
    }

    private void clear(){
        unactiveCheck(0);
        unactiveCheck(1);
        unactiveCheck(2);
        unactiveCheck(3);
        lastPos = 0;
    }

    private void activeCheck(int pos){
        switch (pos){
            case 0:
                i1.setImageResource(R.drawable.pin_circle_active);
                break;
            case 1:
                i2.setImageResource(R.drawable.pin_circle_active);
                break;
            case 2:
                i3.setImageResource(R.drawable.pin_circle_active);
                break;
            case 3:
                i4.setImageResource(R.drawable.pin_circle_active);
                break;
        }
    }

    private void unactiveCheck(int pos){
        switch (pos){
            case 0:
                i1.setImageResource(R.drawable.pin_circle);
                break;
            case 1:
                i2.setImageResource(R.drawable.pin_circle);
                break;
            case 2:
                i3.setImageResource(R.drawable.pin_circle);
                break;
            case 3:
                i4.setImageResource(R.drawable.pin_circle);
                break;
        }
    }

    @Override
    public boolean isCoreNeeded() {
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOADING){
            Intent myIntent = new Intent(PincodeActivity.this, MnemonicActivity.class);
            myIntent.putExtra(INTENT_EXTRA_INIT_VIEW,true);
            startActivity(myIntent);
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
