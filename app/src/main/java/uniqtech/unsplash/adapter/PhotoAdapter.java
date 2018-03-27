package uniqtech.unsplash.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

import uniqtech.unsplash.R;
import uniqtech.unsplash.models.Photo;

/**
 * Created by eddiemorgan on 3/25/18.
 */

public class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Photo> photos;
    private Context context;
    private boolean isMultiple;

    public PhotoAdapter(Context c, ArrayList<Photo> photos, boolean isMultiple) {
        this.context = c;
        this.photos = photos;
        this.isMultiple = isMultiple;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh = null;
        View view = LayoutInflater.from(context).inflate(R.layout.cell_photo, parent, false);
        int width = parent.getWidth();
        if (width > 0) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = (int) ((width / 3) * 2f);
        }
        vh = new PhotoViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder hold, int position) {
        if (hold instanceof PhotoViewHolder) {
            PhotoViewHolder holder = (PhotoViewHolder)hold;
            Photo photo = photos.get(position);

            holder.cardView.setCardBackgroundColor(Color.parseColor(photo.getColor()));

            Glide.with(holder.imageView.getContext())
                    .load(photo.getThumb())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return photos == null ? 0 : photos.size();
    }

    public class PhotoViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        ImageView imageView;

        PhotoViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card);
            imageView = itemView.findViewById(R.id.image);
        }
    }
}
