package com.oklab.githubjourney.asynctasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.oklab.githubjourney.adapters.FeedListAdapter;
import com.oklab.githubjourney.data.FeedDataEntry;
import com.oklab.githubjourney.data.UserSessionData;
import com.oklab.githubjourney.fragments.FeedListFragment;
import com.oklab.githubjourney.githubjourney.R;
import com.oklab.githubjourney.services.AtomParser;
import com.oklab.githubjourney.utils.Utils;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by olgakuklina on 2017-01-14.
 */

public class FeedsAsyncTask extends AsyncTask<Integer, Void, List<FeedDataEntry>>{

    public interface OnFeedLoadedListener{
        void onFeedLoaded(List<FeedDataEntry> feedDataEntry);
    }
    private static final String TAG = FeedsAsyncTask.class.getSimpleName();
    private final Context context;
    private UserSessionData currentSessionData;
    private OnFeedLoadedListener listener;

    public FeedsAsyncTask(Context context, OnFeedLoadedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        SharedPreferences prefs = context.getSharedPreferences(Utils.SHARED_PREF_NAME, 0);
        String sessionDataStr = prefs.getString("userSessionData", null);
        currentSessionData = UserSessionData.createUserSessionDataFromString(sessionDataStr);
    }

    @Override
    protected List<FeedDataEntry> doInBackground(Integer... args ) {
        int page = args[0];
        try {
            HttpURLConnection connect = (HttpURLConnection) new URL(context.getString(R.string.url_feeds)).openConnection();
            connect.setRequestMethod("GET");

            String authentication  = "basic " + currentSessionData.getCredentials();
            connect.setRequestProperty("Authorization", authentication);

            connect.connect();
            int responseCode = connect.getResponseCode();

            Log.v(TAG, "responseCode = " + responseCode);
            if(responseCode!= HttpStatus.SC_OK) {
                return null;
            }
            InputStream inputStream = connect.getInputStream();
            String response = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Log.v(TAG, "response = " + response);
            JSONObject jObj = new  JSONObject(response);

            String currentUserURL = jObj.getString("current_user_url") + "&page=" + page;
            Log.v(TAG, "currentUserURL = " + currentUserURL);
            return new AtomParser().parse(currentUserURL);

        } catch (Exception e) {
            Log.e(TAG, "Get user feeds failed", e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<FeedDataEntry> entryList) {
        super.onPostExecute(entryList);
        listener.onFeedLoaded(entryList);
    }
}