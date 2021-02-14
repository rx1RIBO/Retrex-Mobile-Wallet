package rext.org.rextwallet.ui.donate;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.rextj.core.Coin;
import org.rextj.core.InsufficientMoneyException;
import org.rextj.core.Transaction;

import java.math.BigDecimal;

import global.RextRate;
import rext.org.rextwallet.R;
import rext.org.rextwallet.module.RextContext;
import rext.org.rextwallet.service.RextWalletService;
import rext.org.rextwallet.ui.base.BaseDrawerActivity;
import rext.org.rextwallet.ui.base.dialogs.SimpleTextDialog;
import rext.org.rextwallet.utils.DialogsUtil;
import rext.org.rextwallet.utils.NavigationUtils;

import static rext.org.rextwallet.service.IntentsConstants.ACTION_BROADCAST_TRANSACTION;
import static rext.org.rextwallet.service.IntentsConstants.DATA_TRANSACTION_HASH;

/**
 * Created by furszy on 7/24/17.
 */

public class DonateActivity extends BaseDrawerActivity {

    private View root;
    private EditText edit_amount;
    private Button btn_donate;
    private SimpleTextDialog errorDialog;
    private TextView txt_local_currency;

    private RextRate rextRate;

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {
        root = getLayoutInflater().inflate(R.layout.donations_fragment,container);
        edit_amount = (EditText) root.findViewById(R.id.edit_amount);
        txt_local_currency= (TextView) root.findViewById(R.id.txt_local_currency);


        rextRate = rextModule.getRate(rextApplication.getAppConf().getSelectedRateCoin());

        if (rextRate!=null)
            txt_local_currency.setText("0 "+rextRate.getCode());

        edit_amount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    if (rextRate != null) {
                        String valueStr = s.toString();
                        if (valueStr.charAt(0) == '.') {
                            valueStr = "0" + valueStr;
                        }
                        if (valueStr.charAt(valueStr.length()-1) == '.'){
                            valueStr = valueStr.replace(".","");
                        }
                        Coin coin = Coin.parseCoin(valueStr);
                        txt_local_currency.setText(
                                rextApplication.getCentralFormats().format(
                                        new BigDecimal(coin.getValue() * rextRate.getRate().doubleValue()).movePointLeft(8)
                                )
                                        + " " + rextRate.getCode()
                        );
                    }else {
                        // rate null -> no connection.
                        txt_local_currency.setText(R.string.no_rate);
                    }
                }else {
                    if (rextRate!=null)
                        txt_local_currency.setText("0 "+rextRate.getCode());
                    else
                        txt_local_currency.setText(R.string.no_rate);
                }

            }
        });
        btn_donate = (Button) root.findViewById(R.id.btn_donate);
        btn_donate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    send();
                }catch (Exception e){
                    e.printStackTrace();
                    showErrorDialog(e.getMessage());
                }
            }
        });
    }


    private void send() {
        try {
            // create the tx
            String addressStr = RextContext.DONATE_ADDRESS;
            if (!rextModule.chechAddress(addressStr))
                throw new IllegalArgumentException("Address not valid");
            String amountStr = edit_amount.getText().toString();
            if (amountStr.length() < 1) throw new IllegalArgumentException(String.valueOf(R.string.amount_error));
            if (amountStr.length()==1 && amountStr.equals(".")) throw new IllegalArgumentException(String.valueOf(R.string.amount_error));
            if (amountStr.charAt(0)=='.'){
                amountStr = "0"+amountStr;
            }
            Coin amount = Coin.parseCoin(amountStr);
            if (amount.isZero()) throw new IllegalArgumentException("Amount zero, please correct it");
            if (amount.isLessThan(Transaction.MIN_NONDUST_OUTPUT)) throw new IllegalArgumentException("Amount must be greater than the minimum amount accepted from miners, "+Transaction.MIN_NONDUST_OUTPUT.toFriendlyString());
            if (amount.isGreaterThan(Coin.valueOf(rextModule.getAvailableBalance())))
                throw new IllegalArgumentException("Insufficient balance");
            String memo = "Donation!";
            // build a tx with the default fee
            Transaction transaction = rextModule.buildSendTx(addressStr, amount, memo,rextModule.getReceiveAddress());
            // send it
            rextModule.commitTx(transaction);
            Intent intent = new Intent(DonateActivity.this, RextWalletService.class);
            intent.setAction(ACTION_BROADCAST_TRANSACTION);
            intent.putExtra(DATA_TRANSACTION_HASH,transaction.getHash().getBytes());
            startService(intent);

            Toast.makeText(this,R.string.donation_thanks,Toast.LENGTH_LONG).show();
            onBackPressed();

        } catch (InsufficientMoneyException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Insufficient balance");
        }
    }

    private void showErrorDialog(String message) {
        if (errorDialog==null){
            errorDialog = DialogsUtil.buildSimpleErrorTextDialog(this,getResources().getString(R.string.invalid_inputs),message);
        }else {
            errorDialog.setBody(message);
        }
        errorDialog.show(getFragmentManager(),getResources().getString(R.string.send_error_dialog_tag));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        NavigationUtils.goBackToHome(this);
    }
}
