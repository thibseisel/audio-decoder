package fr.ts.audiodecoder;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

class TrackAdapter extends CursorAdapter {

    TrackAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.track_item, parent, false);
        ViewHolder holder = new ViewHolder(itemView, cursor);
        itemView.setTag(holder);
        return itemView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.title.setText(cursor.getString(holder.colTitle));
        holder.subtitle.setText(cursor.getString(holder.colArtist));
    }

    private static class ViewHolder {
        final TextView title;
        final TextView subtitle;

        final int colTitle;
        final int colArtist;

        ViewHolder(View itemView, Cursor cursor) {
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);

            colTitle = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            colArtist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        }
    }
}
