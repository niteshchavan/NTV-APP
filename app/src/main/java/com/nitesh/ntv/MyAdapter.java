package com.nitesh.ntv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private final List<Channel> data;
    private final LayoutInflater inflater;
    private final OnItemClickListener listener;

    public MyAdapter(List<Channel> data, Context context, OnItemClickListener listener) {
        this.data = data;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener; // Initialize listener correctly
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_layout, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Channel channel = data.get(position);
        holder.channelName.setText(channel.getName());
        Glide.with(holder.itemView.getContext()).load(channel.getImage()).into(holder.channelImage);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public interface OnItemClickListener {
        void onItemClick(Channel channel);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final ImageView channelImage;
        private final TextView channelName;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            channelImage = itemView.findViewById(R.id.channel_image);
            channelName = itemView.findViewById(R.id.channel_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position =  getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                Channel channel = data.get(position);
                if (listener != null) {
                    listener.onItemClick(channel); // Ensure listener is not null before invoking
                }
            }
        }
    }
}
