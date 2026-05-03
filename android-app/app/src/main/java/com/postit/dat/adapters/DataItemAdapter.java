package com.postit.dat.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.postit.dat.R;
import com.postit.dat.models.DataItem;

import java.util.List;

public class DataItemAdapter extends RecyclerView.Adapter<DataItemAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(DataItem item);
    }

    private List<DataItem> items;
    private OnItemClickListener clickListener;
    private OnItemClickListener acceptListener;

    public DataItemAdapter(List<DataItem> items, OnItemClickListener clickListener,
                           OnItemClickListener acceptListener) {
        this.items = items;
        this.clickListener = clickListener;
        this.acceptListener = acceptListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataItem item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvSize.setText(item.getFormattedSize());

        // Icon sesuai tipe
        int iconRes;
        switch (item.getType()) {
            case "image": iconRes = R.drawable.ic_image; break;
            case "video": iconRes = R.drawable.ic_video; break;
            case "text":  iconRes = R.drawable.ic_text; break;
            default:      iconRes = R.drawable.ic_file; break;
        }
        holder.ivType.setImageResource(iconRes);

        holder.itemView.setOnClickListener(v -> clickListener.onClick(item));
        holder.btnAccept.setOnClickListener(v -> acceptListener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSize;
        ImageView ivType;
        Button btnAccept;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvSize = itemView.findViewById(R.id.tv_item_size);
            ivType = itemView.findViewById(R.id.iv_item_type);
            btnAccept = itemView.findViewById(R.id.btn_accept);
        }
    }
}
