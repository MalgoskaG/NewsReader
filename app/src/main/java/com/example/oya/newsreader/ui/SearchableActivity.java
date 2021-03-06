package com.example.oya.newsreader.ui;

import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.example.oya.newsreader.R;
import com.example.oya.newsreader.adapters.NewsAdapter;
import com.example.oya.newsreader.data.NewsDbHelper;
import com.example.oya.newsreader.model.NewsArticle;
import com.example.oya.newsreader.utils.Constants;
import com.example.oya.newsreader.utils.SearchLoader;
import com.example.oya.newsreader.data.NewsContract.BookmarkEntry;

import java.util.ArrayList;
import java.util.List;

// Search results are not saved in the database. It reads directly from internet using a loading (@link SearchLoader)

public class SearchableActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<NewsArticle>>, NewsAdapter.ListItemClickListener{

    private View loadingIndicator;
    private ArrayList<NewsArticle> searchResults;
    private NewsAdapter adapter;
    private RecyclerView recycler;
    private TextView empty_tv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        ViewFlipper vf = findViewById(R.id.main_flipper);
        vf.setDisplayedChild(1);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        String query = null;
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);

        }
        toolbar.setTitle("Search results for " + query);
        recycler = findViewById(R.id.recycler_search);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setItemAnimator(new DefaultItemAnimator());
        searchResults = new ArrayList<>();
        adapter = new NewsAdapter(this, searchResults, this);
        recycler.setAdapter(adapter);
        loadingIndicator = findViewById(R.id.loading_indicator_search);
        empty_tv = findViewById(R.id.empty_view_search);
        if (thereIsConnection()) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            loadingIndicator.setVisibility(View.VISIBLE);
            LoaderManager loaderManager = getLoaderManager();
            // Initialize the loader. Pass in the int ID constant defined above and pass in null for
            // the bundle. Pass in this activity for the LoaderCallbacks parameter (which is valid
            // because this activity implements the LoaderCallbacks interface).
            Bundle args = new Bundle();
            args.putString(Constants.SEARCH_QUERY, query);
            loaderManager.initLoader(Constants.SEARCH_LOADER_ID, args, this);
        } else {
            // Otherwise, display error
            // First, hide loading indicator so error message will be visible
            loadingIndicator.setVisibility(View.GONE);
            // Update empty state with no connection error message
            empty_tv.setText(R.string.no_results);
            recycler.setVisibility(View.GONE);
            empty_tv.setVisibility(View.VISIBLE);
        }
    }

    private boolean thereIsConnection(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // Get details on the currently active default data network
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        // If there is a network connection, fetch data
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    public Loader<List<NewsArticle>> onCreateLoader(int id, Bundle args) {
        return new SearchLoader(this, args.getString(Constants.SEARCH_QUERY));
    }

    @Override
    public void onLoadFinished(Loader<List<NewsArticle>> loader, List<NewsArticle> data) {
        loadingIndicator.setVisibility(View.GONE);
        if (data != null && !data.isEmpty()) {
            searchResults.clear();
            searchResults.addAll(data);
            adapter.notifyDataSetChanged();
        } else {
            recycler.setVisibility(View.GONE);
            empty_tv.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<NewsArticle>> loader) {

    }

    @Override
    public void onListItemClick(View view, int position) {
        switch (view.getId()) {
            case R.id.article_item_root: {
                openDetails(position);
                break;
            }
            case R.id.share: {
                shareTheLink(position);
                break;
            }
            case R.id.bookmark: {
                saveToBookmarks(position);
                break;
            }
        }
    }

    private void openDetails(int position) {
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra(Constants.CHOSEN_ARTICLE, searchResults.get(position));
        startActivity(intent);
    }

    private void shareTheLink(int position) {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, searchResults.get(position).getWebUrl());
        if (intent.resolveActivity(this.getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void saveToBookmarks(int position){
        ContentValues values = new ContentValues();
        values.put(BookmarkEntry.COLUMN_TITLE, searchResults.get(position).getTitle());
        values.put(BookmarkEntry.COLUMN_THUMBNAIL_URL, searchResults.get(position).getThumbnailUrl());
        values.put(BookmarkEntry.COLUMN_AUTHOR,searchResults.get(position).getAuthor());
        values.put(BookmarkEntry.COLUMN_DATE, searchResults.get(position).getDate());
        values.put(BookmarkEntry.COLUMN_WEB_URL, searchResults.get(position).getWebUrl());
        values.put(BookmarkEntry.COLUMN_SECTION, searchResults.get(position).getSection());
        values.put(BookmarkEntry.COLUMN_TRAIL, searchResults.get(position).getArticleTrail());
        values.put(BookmarkEntry.COLUMN_BODY, searchResults.get(position).getArticleBody());
        //Then save it to bookmarks table
        getContentResolver().insert(BookmarkEntry.CONTENT_URI, values);
        Toast.makeText(this, R.string.article_bookmarked, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(SearchableActivity.this, SettingsActivity.class);
            intent.putExtra(Constants.USER_CLICKED_SETTINGS_FROM, SettingsActivity.class.getSimpleName());
            startActivity(intent);
        } else if (id == R.id.action_bookmarks){
            Intent intent = new Intent(SearchableActivity.this, BookmarksActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

}
