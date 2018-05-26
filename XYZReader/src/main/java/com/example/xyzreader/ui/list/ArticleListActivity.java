package com.example.xyzreader.ui.list;

import android.annotation.SuppressLint;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.ui.detail.ArticleDetailActivity;
import com.example.xyzreader.ui.util.GlideUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import static android.content.Intent.ACTION_VIEW;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static android.text.Html.fromHtml;
import static android.text.format.DateUtils.FORMAT_ABBREV_ALL;
import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static com.example.xyzreader.data.ArticleLoader.Query.AUTHOR;
import static com.example.xyzreader.data.ArticleLoader.Query.PUBLISHED_DATE;
import static com.example.xyzreader.data.ArticleLoader.Query.THUMB_URL;
import static com.example.xyzreader.data.ArticleLoader.Query._ID;
import static com.example.xyzreader.data.UpdaterService.BROADCAST_ACTION_STATE_CHANGE;
import static com.example.xyzreader.data.UpdaterService.EXTRA_REFRESHING;
import static java.lang.System.currentTimeMillis;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 *
 * TODO: MAKE TABLET HAVE A GRID
 */
public class ArticleListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.toString();

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView articleRecyclerView;
    private boolean isRefreshing;

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");

    // Use default locale format
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat outputFormat = new SimpleDateFormat();

    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_layout);
        collapsingToolbarLayout.setTitle(this.getString(R.string.app_name));

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                unregisterReceiver(mRefreshingReceiver);
                registerReceiver(mRefreshingReceiver, new IntentFilter(BROADCAST_ACTION_STATE_CHANGE));
            }
        });

        articleRecyclerView = findViewById(R.id.recycler_view);

        if (savedInstanceState == null) {
            getLoaderManager().initLoader(0, null, this);
            refresh();
        } else {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver, new IntentFilter(BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                isRefreshing = intent.getBooleanExtra(EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        swipeRefreshLayout.setRefreshing(isRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        articleRecyclerView.setAdapter(adapter);
        articleRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        articleRecyclerView.addItemDecoration(new DividerItemDecoration(this, VERTICAL));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        articleRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private Cursor mCursor;

        Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(_ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);

            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(ACTION_VIEW, ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                }
            });

            return vh;
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();

            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                holder.subtitleView.setText(
                        fromHtml(DateUtils.getRelativeTimeSpanString(publishedDate.getTime(), currentTimeMillis(),
                                        HOUR_IN_MILLIS, FORMAT_ABBREV_ALL).toString() + "<br/>" + " by " + mCursor.getString(AUTHOR)));
            } else {
                holder.subtitleView.setText(fromHtml(outputFormat.format(publishedDate) + "<br/>" + " by " + mCursor.getString(AUTHOR)));
            }

            GlideUtil.displayImage(mCursor.getString(THUMB_URL), holder.thumbnailView);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        ViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }
}
