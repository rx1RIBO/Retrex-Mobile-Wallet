package rext.org.rextwallet.ui.settings.faq;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ChildViewHolder;

import rext.org.rextwallet.R;

class ChildFaqHolder extends ChildViewHolder<String>{

    public TextView textView;

    /**
     * Default constructor.
     *
     * @param itemView The {@link View} being hosted in this ViewHolder
     */
    public ChildFaqHolder(@NonNull View itemView) {
        super(itemView);
        textView = (TextView) itemView.findViewById(R.id.textView);
    }
}
