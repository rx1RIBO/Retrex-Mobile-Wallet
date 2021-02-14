package rext.org.rextwallet.ui.wallet_activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.rextj.core.Coin;
import org.rextj.core.CoinDefinition;
import org.rextj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rext.org.rextwallet.R;
import global.RextRate;
import rext.org.rextwallet.ui.base.BaseRecyclerFragment;
import rext.org.rextwallet.ui.base.tools.adapter.BaseRecyclerAdapter;
import rext.org.rextwallet.ui.base.tools.adapter.BaseRecyclerViewHolder;
import rext.org.rextwallet.ui.base.tools.adapter.ListItemListeners;
import rext.org.rextwallet.ui.transaction_detail_activity.TransactionDetailActivity;
import global.wrappers.TransactionWrapper;

import static rext.org.rextwallet.ui.transaction_detail_activity.FragmentTxDetail.IS_DETAIL;
import static rext.org.rextwallet.ui.transaction_detail_activity.FragmentTxDetail.IS_ZPIV_WALLET;
import static rext.org.rextwallet.ui.transaction_detail_activity.FragmentTxDetail.TX_WRAPPER;
import static rext.org.rextwallet.utils.TxUtils.getAddressOrContact;

/**
 * Created by furszy on 6/29/17.
 */

public class TransactionsFragmentBase extends BaseRecyclerFragment<TransactionWrapper> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsFragmentBase.class);

    private RextRate rextRate;
    private MonetaryFormat coinFormat = MonetaryFormat.BTC;
    private int scale = 3;
    private Boolean isPrivate = false;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra("Private")) {
            isPrivate = intent.getBooleanExtra("Private",false);
        }
        setEmptyTextColor(Color.parseColor("#cccccc"));

        init();
        return view;
    }

    private void init(){
        if (isPrivate) {
            setEmptyView(R.drawable.img_zpiv_transaction_empty);
            setEmptyText(getString(R.string.empty_zpiv_transactions));

        } else {
            setEmptyView(R.drawable.img_transaction_empty);
            setEmptyText(getString(R.string.no_transactions));
        }
    }

    public void change(boolean isPrivate){
        this.isPrivate = isPrivate;
        init();
        refresh();
    }

    @Override
    protected List<TransactionWrapper> onLoading() {
        List<TransactionWrapper> list = null;
        if (isPrivate){
            list = rextModule.listPrivateTxes();
        }else
            list = rextModule.listTx();
        Collections.sort(list, (o1, o2) -> {
            if(o1.getTransaction().getUpdateTime().getTime() == o2.getTransaction().getUpdateTime().getTime())
                return 0;
            return o1.getTransaction().getUpdateTime().getTime() > o2.getTransaction().getUpdateTime().getTime() ? -1 : 1;
        });
        return list;
    }

    @Override
    protected BaseRecyclerAdapter<TransactionWrapper, ? extends BaseRecyclerViewHolder> initAdapter() {
        BaseRecyclerAdapter<TransactionWrapper, TransactionViewHolderBase> adapter = new BaseRecyclerAdapter<TransactionWrapper, TransactionViewHolderBase>(getActivity()) {
            @Override
            protected TransactionViewHolderBase createHolder(View itemView, int type) {
                return new TransactionViewHolderBase(itemView);
            }

            @Override
            protected int getCardViewResource(int type) {
                return R.layout.transaction_row;
            }

            @Override
            protected void bindHolder(TransactionViewHolderBase holder, TransactionWrapper data, int position) {
                String amount = data.getAmount().toFriendlyString();
                if (amount.length() <= 10){
                    holder.txt_scale.setVisibility(View.GONE);
                    holder.amount.setText(amount);
                }else {
                    // format amount
                    holder.txt_scale.setVisibility(View.VISIBLE);
                    holder.amount.setText(parseToCoinWith4Decimals(data.getAmount().toPlainString()).toFriendlyString());
                }

                String localCurrency = null;
                if (rextRate != null) {
                    localCurrency = rextApplication.getCentralFormats().format(
                                    new BigDecimal(data.getAmount().getValue() * rextRate.getRate().doubleValue()).movePointLeft(8)
                                    )
                                    + " " + rextRate.getCode();
                    holder.amountLocal.setText(localCurrency);
                    holder.amountLocal.setVisibility(View.VISIBLE);
                }else {
                    holder.amountLocal.setVisibility(View.INVISIBLE);
                }


                if (data.isSent()){
                    holder.imageView.setImageResource(R.drawable.ic_transaction_send);
                    holder.amount.setTextColor(ContextCompat.getColor(context, R.color.red));
                }else if (data.isZcSpend()) {
                    if (data.isPrivate()){
                        holder.imageView.setImageResource(R.drawable.ic_transaction_send_zpiv);
                    }else {
                        holder.imageView.setImageResource(R.drawable.ic_transaction_receive_zpiv);
                    }
                    holder.amount.setTextColor(ContextCompat.getColor(context,data.isPrivate() ? R.color.red : R.color.green));
                }else if (data.isZcMint()){
                    holder.imageView.setImageResource(R.drawable.ic_transaction_convert_zpiv);
                    holder.amount.setTextColor(ContextCompat.getColor(context, data.isPrivate() ? R.color.green : R.color.red));
                }else if (!data.isStake()){
                    holder.imageView.setImageResource(R.mipmap.ic_transaction_receive);
                    holder.amount.setTextColor(ContextCompat.getColor(context, R.color.green));
                } else {
                    holder.imageView.setImageResource(R.drawable.ic_transaction_mining);
                    holder.amount.setTextColor(ContextCompat.getColor(context, R.color.green));
                }

                if (data.isZcMint()){
                    holder.title.setText(R.string.zerocoin_mint);
                }else
                    holder.title.setText(getAddressOrContact(rextModule,data));

                /*if (data.getOutputLabels()!=null && !data.getOutputLabels().isEmpty()){
                    AddressLabel contact = data.getOutputLabels().get(0);
                    if (contact!=null) {
                        if (contact.getName() != null)
                            holder.title.setText(contact.getName());
                        else
                            holder.title.setText(contact.getAddresses().get(0));
                    }else {
                        holder.title.setText(data.getTransaction().getOutput(0).getScriptPubKey().getToAddress(rextModule.getConf().getNetworkParams()).toBase58());
                    }
                }else {
                    holder.title.setText(data.getTransaction().getOutput(0).getScriptPubKey().getToAddress(rextModule.getConf().getNetworkParams()).toBase58());
                }*/
                String memo = data.getTransaction().getMemo();
                holder.description.setText( (memo != null && !memo.equals("") ) ? memo : "No description");

                int spendableDepth;
                int drawableRes;
                if (!data.isZcMint()){
                    spendableDepth = 2; // todo: Get this data from rextj..
                    drawableRes = R.drawable.ic_transaction_zpiv_pending;
                }else {
                    spendableDepth = CoinDefinition.MINT_REQUIRED_CONFIRMATIONS;
                    drawableRes = R.drawable.ic_transaction_piv_pending;
                }

                if (data.getTransaction().getConfidence().getDepthInBlocks() < spendableDepth){
                    holder.img_pending.setVisibility(View.VISIBLE);
                    holder.img_pending.setImageResource(drawableRes);
                }else {
                    holder.img_pending.setVisibility(View.GONE);
                }


            }
        };
        adapter.setListEventListener(new ListItemListeners<TransactionWrapper>() {
            @Override
            public void onItemClickListener(TransactionWrapper data, int position) {
                Intent intent = new Intent(getActivity(), TransactionDetailActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(TX_WRAPPER,data);
                bundle.putBoolean(IS_DETAIL,true);
                bundle.putBoolean(IS_ZPIV_WALLET, isPrivate);
                bundle.putBoolean("Private", isPrivate);
                intent.putExtras(bundle);
                startActivity(intent);
            }

            @Override
            public void onLongItemClickListener(TransactionWrapper data, int position) {

            }
        });
        return adapter;
    }

    @Override
    public void onResume() {
        super.onResume();
        rextRate = rextModule.getRate(rextApplication.getAppConf().getSelectedRateCoin());
    }

    /**
     * Converts to a coin with max. 4 decimal places. Last place gets rounded.
     * 0.01234 -> 0.0123
     * 0.01235 -> 0.0124
     *
     * @param input
     * @return
     */
    public Coin parseToCoinWith4Decimals(String input) {
        try {
            return Coin.valueOf(new BigDecimal(parseToCoin(cleanInput(input)).value).setScale(-scale - 1,
                    BigDecimal.ROUND_HALF_UP).setScale(scale + 1).toBigInteger().longValue());
        } catch (Throwable t) {
            if (input != null && input.length() > 0)
                logger.warn("Exception at parseToCoinWith4Decimals: " + t.toString());
            return Coin.ZERO;
        }
    }

    public  Coin parseToCoin(String input) {
        if (input != null && input.length() > 0) {
            try {
                return coinFormat.parse(cleanInput(input));
            } catch (Throwable t) {
                logger.warn("Exception at parseToBtc: " + t.toString());
                return Coin.ZERO;
            }
        } else {
            return Coin.ZERO;
        }
    }

    private  String cleanInput(String input) {
        input = input.replace(",", ".");
        // don't use String.valueOf(Double.parseDouble(input)) as return value as it gives scientific
        // notation (1.0E-6) which screw up coinFormat.parse
        //noinspection ResultOfMethodCallIgnored
        Double.parseDouble(input);
        return input;
    }
}
