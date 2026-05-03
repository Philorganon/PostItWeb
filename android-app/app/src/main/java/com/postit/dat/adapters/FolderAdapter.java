package com.postit.dat.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.postit.dat.R;

import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(String folderName);
    }

    private List<String> folders;
    private OnFolderClickListener listener;

    public FolderAdapter(List<String> folders, OnFolderClickListener listener) {
        this.folders = folders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String folder = folders.get(position);
        holder.tvName.setText(folder);
        holder.itemView.setOnClickListener(v -> listener.onFolderClick(folder));
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_folder_name);
        }
    }
}
