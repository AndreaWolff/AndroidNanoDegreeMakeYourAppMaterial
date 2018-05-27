package com.example.xyzreader.ui.detail;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.list.ArticleListActivity;
import com.example.xyzreader.ui.util.GlideUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import static android.support.v4.app.ShareCompat.IntentBuilder.from;
import static android.text.format.DateUtils.FORMAT_ABBREV_ALL;
import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.view.View.GONE;
import static com.example.xyzreader.data.ArticleLoader.Query.AUTHOR;
import static com.example.xyzreader.data.ArticleLoader.Query.BODY;
import static com.example.xyzreader.data.ArticleLoader.Query.PHOTO_URL;
import static com.example.xyzreader.data.ArticleLoader.Query.PUBLISHED_DATE;
import static com.example.xyzreader.data.ArticleLoader.Query.TITLE;
import static java.lang.System.currentTimeMillis;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private ImageView mPhotoView;
    private TextView bylineView;
    private TextView bodyView;
    private CollapsingToolbarLayout collapsingToolbarLayout;

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");

    // Use default locale format
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat outputFormat = new SimpleDateFormat();

    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mPhotoView = mRootView.findViewById(R.id.photo);
        bylineView = mRootView.findViewById(R.id.article_byline);
        bodyView =  mRootView.findViewById(R.id.article_body);

        Toolbar toolbar = mRootView.findViewById(R.id.toolbar);
        collapsingToolbarLayout = mRootView.findViewById(R.id.collapsing_toolbar_layout);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        // Setting up back navigation taken from
        // https://stackoverflow.com/questions/27230827/back-button-using-getsupportactionbar-and-appcompat-v7-toolbar
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                getActivity().onBackPressed();
                getActivity().finish();
            }
        });

        FloatingActionButton fab = mRootView.findViewById(R.id.share_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();
        return mRootView;
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

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);

            collapsingToolbarLayout.setTitle(mCursor.getString(TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                String dateAndAuthor = DateUtils.getRelativeTimeSpanString(publishedDate.getTime(), currentTimeMillis(), HOUR_IN_MILLIS, FORMAT_ABBREV_ALL).toString() + " by " + mCursor.getString(AUTHOR);
                bylineView.setText(dateAndAuthor);

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(outputFormat.format(publishedDate) + " by " + mCursor.getString(AUTHOR)));
            }

//            bodyView.setText(mCursor.getString(BODY));
            bodyView.setText(Html.fromHtml(mCursor.getString(BODY)));
//            bodyView.setText(Html.fromHtml(mCursor.getString(BODY).replaceAll("/(?:\r\n|\r|\n)/g", "<br/>")));
//            bodyView.setText(Html.fromHtml(mCursor.getString(BODY).replaceAll("\\. ", ".<br/>")));
//            bodyView.setText(Html.fromHtml(mCursor.getString(BODY).replaceAll("(\r)", " ").replaceAll("(\n)", "<br />")));
//            bodyView.setText(Html.fromHtml(mCursor.getString(BODY).replaceAll("(\r\n|\n)", "<br />")));

            GlideUtil.displayImage(mCursor.getString(PHOTO_URL), mPhotoView);
        } else {
            mRootView.setVisibility(GONE);
            collapsingToolbarLayout.setTitle("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }
}
