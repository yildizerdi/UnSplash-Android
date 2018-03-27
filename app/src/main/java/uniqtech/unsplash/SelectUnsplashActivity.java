package uniqtech.unsplash;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import uniqtech.unsplash.adapter.PhotoAdapter;
import uniqtech.unsplash.interfaces.onSearchListener;
import uniqtech.unsplash.interfaces.onSimpleSearchActionsListener;
import uniqtech.unsplash.models.Parameters;
import uniqtech.unsplash.models.Photo;
import uniqtech.unsplash.tools.EndlessRecyclerOnScrollListener;
import uniqtech.unsplash.tools.GridSpacingItemDecoration;
import uniqtech.unsplash.tools.WebServices;
import uniqtech.unsplash.utils.Util;
import uniqtech.unsplash.widgets.MaterialSearchView;

public class SelectUnsplashActivity extends AppCompatActivity implements onSimpleSearchActionsListener, onSearchListener {

    public static final String PAGE_LIMIT = "page_limit";
    public static final String IS_MULTIPLE = "isMultiple";
    public static final String CLIENT_ID = "clientID";

    private PhotoAdapter adapter;
    private RecyclerView photosListView;
    private ArrayList<Photo> photos;

    private boolean mSearchViewAdded = false;
    private MaterialSearchView mSearchView;
    private WindowManager mWindowManager;
    private Toolbar mToolbar;
    private MenuItem searchItem;
    private boolean searchActive = false;
    private GridLayoutManager mLayoutManager;

    private String clientID = "", query = "";
    private int page = 1, pageLimit = 10;
    private AsyncTask task;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        photosListView = findViewById(R.id.list);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        dialog = new ProgressDialog(this);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mSearchView = new MaterialSearchView(this);
        mSearchView.setOnSearchListener(this);
        mSearchView.setSearchResultsListener(this);
        mSearchView.setHintText("Search");

        if (mToolbar != null) {
            // Delay adding SearchView until Toolbar has finished loading
            mToolbar.post(new Runnable() {
                @Override
                public void run() {
                    if (!mSearchViewAdded && mWindowManager != null) {
                        mWindowManager.addView(mSearchView,
                                MaterialSearchView.getSearchViewLayoutParams(SelectUnsplashActivity.this));
                        mSearchViewAdded = true;
                    }
                }
            });
        }

        Intent intent = getIntent();
        clientID = intent.getStringExtra(CLIENT_ID);
        pageLimit = intent.getIntExtra(PAGE_LIMIT,10);

        photos = new ArrayList<>();

        adapter = new PhotoAdapter(this, photos, intent.getBooleanExtra(IS_MULTIPLE,false));
        mLayoutManager = new GridLayoutManager(this, 2);
        photosListView.setLayoutManager(mLayoutManager);
        photosListView.setItemAnimator(new DefaultItemAnimator());
        photosListView.addItemDecoration(new GridSpacingItemDecoration(2, Util.dpToPx(this,4), true));
        photosListView.setHasFixedSize(true);
        photosListView.setAdapter(adapter);

        task = new GetPhotos().execute(clientID, String.valueOf(page), String.valueOf(pageLimit));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        searchItem = menu.findItem(R.id.search);
        searchItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                searchActive = true;
                mSearchView.display();
                openKeyboard();
                return true;
            }
        });
        if(searchActive)
            mSearchView.display();
        return true;

    }

    private void openKeyboard(){
        new Handler().postDelayed(new Runnable() {
            public void run() {
                mSearchView.getSearchView().dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                mSearchView.getSearchView().dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
            }
        }, 200);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSearch(String query) {
        this.query = query;
        page = 1;
        photos.clear();
        adapter.notifyDataSetChanged();
        if(task != null)
            task.cancel(true);
        task = new SearchPhotos().execute(clientID, String.valueOf(page), String.valueOf(pageLimit), query);
    }

    @Override
    public void searchViewOpened() {
    }

    @Override
    public void searchViewClosed() {
    }

    @Override
    public void onItemClicked(String item) {
    }

    @Override
    public void onScroll() {

    }

    @Override
    public void error(String localizedMessage) {

    }

    @Override
    public void onCancelSearch() {
        searchActive = false;
        mSearchView.hide();
        page = 1;
        photos.clear();
        adapter.notifyDataSetChanged();
        if(task != null)
            task.cancel(true);
        task = new GetPhotos().execute(clientID, String.valueOf(page), String.valueOf(pageLimit));
    }

    @Override
    protected void onDestroy() {
        if(task != null)
            task.cancel(true);
        super.onDestroy();
    }

    @SuppressLint("StaticFieldLeak")
    private class GetPhotos extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            WebServices serviceForUser = new WebServices();
            Map<String, String> params = new HashMap<>();
            params.put(Parameters.CLIENT_ID, strings[0]);
            params.put(Parameters.PAGE, strings[1]);
            params.put(Parameters.PER_PAGE, strings[2]);
            return serviceForUser.WebServicePost(Parameters.PHOTOS, params);
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                dialog.dismiss();
                Log.d("result", s);
                JSONArray array = new JSONArray(s);
                for(int i=0; i<array.length(); i++){
                    JSONObject object = array.getJSONObject(i);
                    JSONObject urls = object.getJSONObject("urls");

                    Photo photo = new Photo();
                    photo.setId(object.getString("id"));
                    photo.setColor(object.getString("color"));
                    photo.setRaw(urls.getString("raw"));
                    photo.setFull(urls.getString("full"));
                    photo.setThumb(urls.getString("thumb"));

                    photos.add(photo);
                }
                adapter.notifyDataSetChanged();
                photosListView.setOnScrollListener(new EndlessRecyclerOnScrollListener(mLayoutManager, pageLimit) {
                    @Override
                    public void onLoadMore(int current_page) {
                        page++;
                        if(searchActive)
                            task = new SearchPhotos().execute(clientID, String.valueOf(page), String.valueOf(pageLimit), query);
                        else
                            task = new GetPhotos().execute(clientID, String.valueOf(page), String.valueOf(pageLimit));
                    }
                });
            } catch (NullPointerException | JSONException e) {
                e.printStackTrace();
                Toast.makeText(SelectUnsplashActivity.this, getString(R.string.error_network), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = ProgressDialog.show(SelectUnsplashActivity.this, getString(R.string.loading), getString(R.string.please_wait));
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class SearchPhotos extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            WebServices serviceForUser = new WebServices();
            Map<String, String> params = new HashMap<>();
            params.put(Parameters.CLIENT_ID, strings[0]);
            params.put(Parameters.PAGE, strings[1]);
            params.put(Parameters.PER_PAGE, strings[2]);
            params.put(Parameters.QUERY, strings[3]);
            return serviceForUser.WebServicePost(Parameters.SEARCH_PHOTOS, params);
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                dialog.dismiss();
                Log.d("result", s);
                JSONObject result = new JSONObject(s);
                JSONArray array = result.getJSONArray("results");
                for(int i=0; i<array.length(); i++){
                    JSONObject object = array.getJSONObject(i);
                    JSONObject urls = object.getJSONObject("urls");

                    Photo photo = new Photo();
                    photo.setId(object.getString("id"));
                    photo.setColor(object.getString("color"));
                    photo.setRaw(urls.getString("raw"));
                    photo.setFull(urls.getString("full"));
                    photo.setThumb(urls.getString("thumb"));

                    photos.add(photo);
                }
                adapter.notifyDataSetChanged();
                photosListView.setOnScrollListener(new EndlessRecyclerOnScrollListener(mLayoutManager, pageLimit) {
                    @Override
                    public void onLoadMore(int current_page) {
                        page++;
                        if(searchActive)
                            task = new SearchPhotos().execute(clientID, String.valueOf(page), String.valueOf(pageLimit), query);
                        else
                            task = new GetPhotos().execute(clientID, String.valueOf(page), String.valueOf(pageLimit));
                    }
                });
            } catch (NullPointerException | JSONException e) {
                e.printStackTrace();
                Toast.makeText(SelectUnsplashActivity.this, getString(R.string.error_network), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = ProgressDialog.show(SelectUnsplashActivity.this, getString(R.string.loading), getString(R.string.please_wait));
        }
    }

}
