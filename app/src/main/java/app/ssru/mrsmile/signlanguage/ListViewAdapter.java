package app.ssru.mrsmile.signlanguage;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;


/**
 * Created by Admin on 29/7/2558.
 */
public class ListViewAdapter extends BaseAdapter {

    private Context mContext = null;
    private LayoutInflater inflater;
    private List<String> list = null;
    private static FrontBuild frontBuild;

    public ListViewAdapter(Context mContext, List<String> list) {
        this.mContext = mContext;
        this.list = list;
        this.inflater = LayoutInflater.from(mContext);
        frontBuild = new FrontBuild(this.mContext);
    }

    public class ViewHolder{
        TextView txtItem;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public String getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        final ViewHolder holder;

        if (view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.listview_row, null);

            holder.txtItem = (TextView) view.findViewById(R.id.txtItem);
                        view.setTag(holder);

        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.txtItem.setText(list.get(position));
        holder.txtItem.setTypeface(frontBuild.FEFCIT2);

        return view;
    }
}
