package rext.org.rextwallet.ui.settings.settings_backup_activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import rext.org.rextwallet.R;
import rext.org.rextwallet.module.RextContext;
import rext.org.rextwallet.module.wallet.WalletBackupHelper;
import rext.org.rextwallet.ui.backup_mnemonic_activity.MnemonicActivity;
import rext.org.rextwallet.ui.base.BaseActivity;
import rext.org.rextwallet.ui.base.dialogs.SimpleTextDialog;
import rext.org.rextwallet.ui.pincode_activity.PincodeActivity;
import rext.org.rextwallet.ui.transaction_send_activity.SendActivity;
import rext.org.rextwallet.utils.DialogsUtil;

/**
 * Created by Neoperol on 5/18/17.
 */

public class SettingsBackupActivity extends BaseActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL = 500;
    private static final int PIN_RESULT_MNEMONIC = 303;

    private View root;
    private EditText edit_password;
    private EditText edit_repeat_password;
    private Button btn_backup;
    private ProgressBar progress;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        root = getLayoutInflater().inflate(R.layout.fragment_settings_backup, container);
        setTitle(R.string.backup_wallet);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        edit_password = (EditText) root.findViewById(R.id.edit_password);
        edit_repeat_password = (EditText) root.findViewById(R.id.edit_repeat_password);
        progress = (ProgressBar) root.findViewById(R.id.progress);

        btn_backup = (Button) findViewById(R.id.btn_backup);
        btn_backup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissions();
                viewBackup();
            }
        });
    }

    private void viewBackup() {
        progress.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                org.rextj.core.Context.propagate(RextContext.CONTEXT);
                backup();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0,0,0, R.string.backup_words);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 0:
                if (rextModule.isWalletWatchOnly()){
                    Toast.makeText(this,R.string.error_watch_only_mode,Toast.LENGTH_SHORT).show();
                    return true;
                }
                Intent intent = new Intent(SettingsBackupActivity.this, PincodeActivity.class);
                intent.putExtra(PincodeActivity.CHECK_PIN,true);
                startActivityForResult(intent, PIN_RESULT_MNEMONIC);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void backup() {
        int backupRes = 0;
        try {

            String firstPassword = edit_password.getText().toString();
            String repeatPassword = edit_repeat_password.getText().toString();
            if (!firstPassword.equals(repeatPassword)) {
                backupRes = R.string.backup_passwords_doesnt_match;
                toastfromBackgroundMessage(backupRes);
                return;
            }
            File backupFile = new WalletBackupHelper().determineBackupFile(null);
            boolean result = rextModule.backupWallet(backupFile, firstPassword);

            if (result){
                rextApplication.getAppConf().setHasBackup(true);
                showSuccedBackupDialog(backupFile.getAbsolutePath());
            }else {
                backupRes = R.string.backup_fail;
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(SettingsBackupActivity.class).warn("#### backup fail", e);
            backupRes = R.string.backup_fail;
        } catch (Exception e){
            LoggerFactory.getLogger(SettingsBackupActivity.class).warn("#### backup fail", e);
            backupRes = R.string.backup_fail;
        }
        if (backupRes!=0){
            toastfromBackgroundMessage(backupRes);
        }
    }

    private void toastfromBackgroundMessage(final int msgRes){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SettingsBackupActivity.this,msgRes,Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showSuccedBackupDialog(final String backupAbsolutePath){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SimpleTextDialog succedDialog = DialogsUtil.buildSimpleTextDialog(
                        SettingsBackupActivity.this,
                        getString(R.string.backup_completed),
                        getString(R.string.backup_completed_text,backupAbsolutePath)
                );
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    succedDialog.setOkBtnBackgroundColor(getColor(R.color.colorPurple));
                }else {
                    succedDialog.setOkBtnBackgroundColor(ContextCompat.getColor(SettingsBackupActivity.this, R.color.lightGreen));
                }
                succedDialog.setOkBtnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });
                succedDialog.show(getFragmentManager(),getString(R.string.backup_succed_dialog));
            }
        });

    }

    private void checkPermissions() {
        // Assume thisActivity is the current activity
        if (Build.VERSION.SDK_INT > 22) {

            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //backup
                    viewBackup();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, R.string.write_external_denied, Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * PIN_RESULT_ZPIV
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PIN_RESULT_MNEMONIC) {
            if (resultCode == RESULT_OK) {
                Intent myIntent = new Intent(getApplicationContext(), MnemonicActivity.class);
                startActivity(myIntent);
            } else {
                // Spend cancelled
                Toast.makeText(SettingsBackupActivity.this, R.string.invalid_pincode, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
