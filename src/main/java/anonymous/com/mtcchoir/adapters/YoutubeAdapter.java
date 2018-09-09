package anonymous.com.mtcchoir.adapters;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import anonymous.com.mtcchoir.R;
import anonymous.com.mtcchoir.Utils.AppController;
import anonymous.com.mtcchoir.models.YoutubeItem;

public class YoutubeAdapter extends  RecyclerView.Adapter<YoutubeAdapter.MyViewHolder>{
    private List<YoutubeItem> itemList;
    private YoutubeAdapterInteractionListener youtubeAdapterInteractionListener;

    /**
     * View holder class
     * */
    class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView mLink;
        private TextView mSongName;
        private TextView mAddedUser;
        private ImageView mDeleteImageView;


        MyViewHolder(View view) {
            super(view);
            mLink = view.findViewById(R.id.text_link);
            mSongName = view.findViewById(R.id.text_song_name);
            mAddedUser = view.findViewById(R.id.text_added_user);
            mDeleteImageView = view.findViewById(R.id.image_delete);
            youtubeAdapterInteractionListener = (YoutubeAdapterInteractionListener) view.getContext();
        }
    }

    public YoutubeAdapter(List<YoutubeItem> itemList) {
        this.itemList = itemList;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") final int position) {

        holder.mLink.setText(itemList.get(position).getmLink());
        holder.mSongName.setText(String.valueOf(position+1)+". "+itemList.get(position).getmSongName());
        holder.mAddedUser.setText("Added By : "+itemList.get(position).getmAddedUser());
        if(itemList.get(position).getmAddedUser().equals(AppController.mUserName))  holder.mDeleteImageView.setVisibility(View.VISIBLE);
        holder.mLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                youtubeAdapterInteractionListener.onLinkClick(itemList.get(position).getmLink());
            }
        });
        holder.mDeleteImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                youtubeAdapterInteractionListener.onDelete(itemList.get(position).getmSongName());
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_youtube_fragment,parent, false);
        return new MyViewHolder(v);
    }

    public interface YoutubeAdapterInteractionListener{

        void onLinkClick(String link);
        void onDelete(String songName);
    }
}
