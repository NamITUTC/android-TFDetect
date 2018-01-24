package org.tensorflow.demo;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by nam on 24/01/2018.
 */

public class DetectImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<Bitmap> imagesDetect;
    private ItemClick itemClick;



    public DetectImageAdapter(ArrayList<Bitmap> imagesDetect, ItemClick itemClick) {
        this.imagesDetect = imagesDetect;
        this.itemClick = itemClick;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DetectImageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_cut, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DetectImageViewHolder) {
            ((DetectImageViewHolder) holder).bind(imagesDetect.get(position),position);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return imagesDetect.size();
    }

    class DetectImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imgDetect;
        CheckBox chkSave;

        DetectImageViewHolder(final View itemView) {
            super(itemView);
            imgDetect = itemView.findViewById(R.id.img_cut);
            chkSave=itemView.findViewById(R.id.chk_save);


        }

        public void bind(Bitmap bitmap, final int position) {
            imgDetect.setImageBitmap(bitmap);

            chkSave.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(compoundButton.isChecked()){
                        itemClick.click(position);
                    }
                }
            });
        }
    }
}
