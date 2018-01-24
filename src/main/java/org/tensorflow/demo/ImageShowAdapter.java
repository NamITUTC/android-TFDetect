package org.tensorflow.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Created by nam on 16/01/2018.
 */

public class ImageShowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<Uri> imageSources;    private Context context;
    private IClick iClick;

    public interface IClick{
        void itemClick(int position);
    }

    public ImageShowAdapter(ArrayList<Uri> imageSources, Context context, IClick iClick) {
        this.imageSources = imageSources;
        this.context = context;
        this.iClick = iClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ImageShowViewHolder(LayoutInflater.from(context).inflate(R.layout.item_image, parent,false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        ImageShowViewHolder holder1 = (ImageShowViewHolder) holder;
        holder1.binData(imageSources.get(position), context);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iClick.itemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageSources.size();
    }

    class ImageShowViewHolder extends RecyclerView.ViewHolder {

        ImageView imgImage;

        ImageShowViewHolder(View itemView) {
            super(itemView);
            imgImage = itemView.findViewById(R.id.img_image);

        }

        void binData(Uri imageSource, Context context) {
            //Glide.with(context).load(imageSource).into(imgImage);
            Bitmap bitmap = null;
            /*bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageSource);
            imgImage.setImageBitmap(bitmap);*/
            Glide.with(context).load(imageSource).into(imgImage);

        }
    }
}
