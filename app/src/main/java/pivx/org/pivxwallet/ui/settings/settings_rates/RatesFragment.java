package rext.org.rextwallet.ui.settings.settings_rates;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import rext.org.rextwallet.R;
import global.RextRate;
import rext.org.rextwallet.ui.base.BaseRecyclerFragment;
import rext.org.rextwallet.ui.base.tools.adapter.BaseRecyclerAdapter;
import rext.org.rextwallet.ui.base.tools.adapter.BaseRecyclerViewHolder;
import rext.org.rextwallet.ui.base.tools.adapter.ListItemListeners;

/**
 * Created by furszy on 7/2/17.
 */

public class RatesFragment extends BaseRecyclerFragment<RextRate> implements ListItemListeners<RextRate> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        setEmptyText("No rate available");
        setEmptyTextColor(Color.parseColor("#cccccc"));
        return view;
    }

    @Override
    protected List<RextRate> onLoading() {
        return rextModule.listRates();
    }

    @Override
    protected BaseRecyclerAdapter<RextRate, ? extends RextRateHolder> initAdapter() {
        BaseRecyclerAdapter<RextRate, RextRateHolder> adapter = new BaseRecyclerAdapter<RextRate, RextRateHolder>(getActivity()) {
            @Override
            protected RextRateHolder createHolder(View itemView, int type) {
                return new RextRateHolder(itemView,type);
            }

            @Override
            protected int getCardViewResource(int type) {
                return R.layout.rate_row;
            }

            @Override
            protected void bindHolder(RextRateHolder holder, RextRate data, int position) {
                holder.txt_name.setText(data.getCode());
                if (list.get(0).getCode().equals(data.getCode()))
                    holder.view_line.setVisibility(View.GONE);
            }
        };
        adapter.setListEventListener(this);
        return adapter;
    }

    @Override
    public void onItemClickListener(RextRate data, int position) {
        rextApplication.getAppConf().setSelectedRateCoin(data.getCode());
        Toast.makeText(getActivity(),R.string.rate_selected,Toast.LENGTH_SHORT).show();
        getActivity().onBackPressed();
    }

    @Override
    public void onLongItemClickListener(RextRate data, int position) {

    }

    private  class RextRateHolder extends BaseRecyclerViewHolder{

        private TextView txt_name;
        private View view_line;

        protected RextRateHolder(View itemView, int holderType) {
            super(itemView, holderType);
            txt_name = (TextView) itemView.findViewById(R.id.txt_name);
            view_line = itemView.findViewById(R.id.view_line);
        }
    }
}
