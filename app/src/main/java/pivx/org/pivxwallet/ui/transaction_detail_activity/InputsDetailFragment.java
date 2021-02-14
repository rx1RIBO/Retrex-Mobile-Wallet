package rext.org.rextwallet.ui.transaction_detail_activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rext.org.rextwallet.R;
import rext.org.rextwallet.module.RextContext;
import rext.org.rextwallet.ui.base.BaseRecyclerFragment;
import rext.org.rextwallet.ui.base.tools.adapter.BaseRecyclerAdapter;
import rext.org.rextwallet.ui.base.tools.adapter.BaseRecyclerViewHolder;
import global.wrappers.InputWrapper;
import wallet.exceptions.TxNotFoundException;

/**
 * Created by furszy on 8/14/17.
 */

public class InputsDetailFragment extends BaseRecyclerFragment<InputWrapper> {

    public static final String INTENT_EXTRA_UNSPENT_WRAPPERS = "unspent_wrappers";

    private Set<InputWrapper> list;
    private BaseRecyclerAdapter adapter;
    private int myPosition;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            list = new HashSet<>();
            setHasOptionsMenu(true);
            Intent intent = getActivity().getIntent();
            if (intent != null) {
                if (intent.hasExtra(INTENT_EXTRA_UNSPENT_WRAPPERS)) {
                    list = (Set<InputWrapper>) intent.getSerializableExtra(INTENT_EXTRA_UNSPENT_WRAPPERS);
                    for (InputWrapper inputWrapper : list) {
                        inputWrapper.setUnspent(rextModule.getUnspent(inputWrapper.getParentTxHash(), inputWrapper.getIndex()));
                    }
                }
            }
            setSwipeRefresh(false);
        } catch (TxNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.invalid_inputs,Toast.LENGTH_SHORT).show();
            getActivity().onBackPressed();
        }
    }

    @Override
    protected List<InputWrapper> onLoading() {
        return new ArrayList<>(list);
    }

    @Override
    protected BaseRecyclerAdapter<InputWrapper, ? extends BaseRecyclerViewHolder> initAdapter() {
        String myInputs = getResources().getString(R.string.input);
        adapter = new BaseRecyclerAdapter<InputWrapper, FragmentTxDetail.DetailOutputHolder>(getActivity()) {

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

            @Override
            protected FragmentTxDetail.DetailOutputHolder createHolder(View itemView, int type) {
                return new FragmentTxDetail.DetailOutputHolder(itemView,type);
            }

            @Override
            protected int getCardViewResource(int type) {
                return R.layout.detail_output_row;
            }

            @Override
            protected void bindHolder(final FragmentTxDetail.DetailOutputHolder holder, final InputWrapper data, int position) {
                myPosition = position +1;
                holder.txt_num.setText(myInputs + " " +myPosition);
                holder.txt_address.setText(data.getLabel(RextContext.NETWORK_PARAMETERS));
                holder.txt_value.setText(data.getUnspent().getValue().toFriendlyString());
            }
        };
        return adapter;
    }
}
