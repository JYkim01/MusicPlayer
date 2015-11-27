package com.massivcode.androidmusicplayer.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.massivcode.androidmusicplayer.R;
import com.massivcode.androidmusicplayer.interfaces.Event;
import com.massivcode.androidmusicplayer.interfaces.MusicEvent;
import com.massivcode.androidmusicplayer.interfaces.Playback;
import com.massivcode.androidmusicplayer.interfaces.RequestEvent;
import com.massivcode.androidmusicplayer.models.MusicInfo;
import com.massivcode.androidmusicplayer.utils.MusicInfoUtil;
import com.suwonsmartapp.abl.AsyncBitmapLoader;

import java.util.ArrayList;
import java.util.HashMap;

import de.greenrobot.event.EventBus;

/**
 * Created by Ray Choe on 2015-11-25.
 */
public class CurrentPlaylistFragment extends DialogFragment implements View.OnClickListener {

    private static final String TAG = CurrentPlaylistFragment.class.getSimpleName();
    private ListView mCurrentPlaylistListView;
    private Button mCurrentPlaylistCloseButton;

    private ArrayList<Long> mPlaylist;
    private ArrayList<MusicInfo> mMusicDataList;

    public MusicEvent mCurrentEvent;
    private Playback mPlayback;

    private CurrentPlaylistAdapter mAdapter;

    private int mCurrentPlayingPosition;

    private Handler mHandler = new Handler();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mPlaylist = (ArrayList<Long>) getArguments().getSerializable("data");

        new Thread(new Runnable() {
            @Override
            public void run() {
                mMusicDataList = MusicInfoUtil.switchAllMusicInfoToSelectedMusicInfo((HashMap<Long, MusicInfo>) getArguments().getSerializable("map"), mPlaylist);
                mAdapter = new CurrentPlaylistAdapter(getActivity(), mMusicDataList);
            }
        }).start();

        EventBus.getDefault().post(new RequestEvent());
//        Log.d(TAG, "재생목록에서 이벤트가 요청되었습니다.");



    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_playlist, container, false);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.WHITE));

        setCancelable(false);

        initView(view);


        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        Log.d(TAG, "CurrentPlaylistFragment is attached");
        // EventBus 등록이 되어서 모든 이벤트를 수신 가능
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        Log.d(TAG, "CurrentPlaylistFragment is detached");
        // 해제 꼭 해주세요
        EventBus.getDefault().unregister(this);
    }

    // EventBus 용 이벤트 수신
    public void onEvent(Event event) {

        if (event instanceof MusicEvent) {
//            Log.d(TAG, "재생목록에서 뮤직이벤트를 받았습니다.");
            mCurrentEvent = (MusicEvent) event;
            mAdapter.notifyDataSetChanged();

        } else if(event instanceof Playback) {
//            Log.d(TAG, "재생목록에서 플레이백이벤트를 받았습니다.");
            mPlayback = (Playback) event;
            mAdapter.notifyDataSetChanged();
        }

        mCurrentPlaylistListView.setSelection(mAdapter.getCurrentPlayingPosition());


    }

    private void initView(View view) {
        mCurrentPlaylistListView = (ListView) view.findViewById(R.id.current_playlistView);
        mCurrentPlaylistCloseButton = (Button) view.findViewById(R.id.current_playlist_closeBtn);

        mCurrentPlaylistCloseButton.setOnClickListener(this);
        mCurrentPlaylistListView.setOnItemClickListener((AdapterView.OnItemClickListener) getActivity());

        mCurrentPlaylistListView.setAdapter(mAdapter);


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.current_playlist_closeBtn:
                dismiss();
                break;
        }
    }


    private class CurrentPlaylistAdapter extends BaseAdapter implements AsyncBitmapLoader.BitmapLoadListener {

        private Context mContext;
        private ArrayList<MusicInfo> mList;
        private AsyncBitmapLoader mAsyncBitmapLoader;

        public CurrentPlaylistAdapter(Context context, ArrayList<MusicInfo> list) {
            mContext = context;
            mList = list;

            mAsyncBitmapLoader = new AsyncBitmapLoader(context);
            mAsyncBitmapLoader.setBitmapLoadListener(this);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;



            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_current_playlist, parent, false);

                viewHolder = new ViewHolder();
                viewHolder.mCurrentPlaylistIsPlayingImageView = (ImageView) convertView.findViewById(R.id.item_songs_isPlay_iv);
                viewHolder.mCurrentPlaylistAlbumArtImageView = (ImageView) convertView.findViewById(R.id.item_artist_child_album_iv);
                viewHolder.mCurrentPlaylistArtistTextView = (TextView) convertView.findViewById(R.id.item_artist_child_artist_tv);
                viewHolder.mCurrentPlaylistTitleTextView = (TextView) convertView.findViewById(R.id.item_artist_child_title_tv);

                convertView.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            MusicInfo musicInfo = (MusicInfo) getItem(position);
            if(musicInfo.get_id() == mCurrentEvent.getMusicInfo().get_id()) {
                viewHolder.mCurrentPlaylistIsPlayingImageView.setVisibility(View.VISIBLE);
                mCurrentPlayingPosition = position;
                if(mPlayback.isPlaying()) {
                    viewHolder.mCurrentPlaylistIsPlayingImageView.setSelected(true);
                } else {
                    viewHolder.mCurrentPlaylistIsPlayingImageView.setSelected(false);
                }

            } else {
                viewHolder.mCurrentPlaylistIsPlayingImageView.setVisibility(View.GONE);
            }
            mAsyncBitmapLoader.loadBitmap(position, viewHolder.mCurrentPlaylistAlbumArtImageView);
            viewHolder.mCurrentPlaylistArtistTextView.setText(musicInfo.getArtist());
            viewHolder.mCurrentPlaylistTitleTextView.setText(musicInfo.getTitle());

            return convertView;
        }

        public int getCurrentPlayingPosition() {
            return mCurrentPlayingPosition;
        }


        @Override
        public Bitmap getBitmap(int position) {
            MusicInfo musicInfo = (MusicInfo) getItem(position);

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(mContext, musicInfo.getUri());

            byte[] albumArt = retriever.getEmbeddedPicture();


            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // 2의 배수

            Bitmap bitmap = null;
            if (null != albumArt) {
                bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length, options);
            } else {
                bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);
            }

            // id 로부터 bitmap 생성
            return bitmap;
        }

        private class ViewHolder {
            ImageView mCurrentPlaylistAlbumArtImageView;
            ImageView mCurrentPlaylistIsPlayingImageView;
            TextView mCurrentPlaylistTitleTextView;
            TextView mCurrentPlaylistArtistTextView;
        }
    }


}
