package com.example.oya.newsreader.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.example.oya.newsreader.R;
import com.example.oya.newsreader.data.NewsContract.NewsEntry;
import com.example.oya.newsreader.model.NewsArticle;
import com.example.oya.newsreader.ui.SortSectionsActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public final class NetworkUtils {

    private static final String LOG_TAG = NetworkUtils.class.getSimpleName();

    private NetworkUtils() {
        //Make it impossible to instantiate
        throw new AssertionError();
    }

    public static ContentValues[] fetchArticles(String section, Context context) {
        // Create URL object
        URL url = null;
        url = buildUrl(section, context);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e);
        }

        return createContentValuesFromJson(jsonResponse);
    }

    public static List<NewsArticle> searchOnline(String query) {
        // Create URL object
        URL url = buildSearchUrl(query);
        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e);
        }
        return createListFromJson(jsonResponse);
    }

    public static List<NewsArticle> checkForNewArticle(Context context) {
        // Create URL object
        URL url = buildUrlForNotification(context);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e);
        }

        return createListFromJson(jsonResponse);
    }

    private static URL buildSearchUrl(String query) {
        Uri uri = Uri.parse(Constants.BASE_URL).buildUpon()
                .appendQueryParameter("q", query)
                .appendQueryParameter(Constants.SHOW_FIELDS_KEY, Constants.SHOW_FIELDS_VALUE)
                .appendQueryParameter(Constants.ORDER_BY_PARAM, "relevance")
                .appendQueryParameter(Constants.PAGE_SIZE_PARAM, "25")
                .appendQueryParameter(Constants.GUARDIAN_API_KEY, Constants.GUARDIAN_API_VALUE)
                .build();

        URL url = null;
        try {
            url = new URL(uri.toString());
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    private static URL buildUrlForNotification(Context context) {

        Uri builtUri = Uri.parse(Constants.BASE_URL).buildUpon()
                .encodedQuery(Constants.FROM_DATE + "=" + getFormattedDateTime())
                .appendQueryParameter(Constants.SECTION_PARAM, buildSectionsParam(context))
                .appendQueryParameter(Constants.SHOW_FIELDS_KEY, Constants.SHOW_FIELDS_VALUE)
                .appendQueryParameter(Constants.ORDER_BY_PARAM, context.getString(R.string.pref_orderby_default))
                .appendQueryParameter(Constants.PAGE_SIZE_PARAM, "1")
                .appendQueryParameter(Constants.GUARDIAN_API_KEY, Constants.GUARDIAN_API_VALUE)
                .build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    private static String getFormattedDateTime() {
        return getFormattedSystemDate() + "T" + getFormattedSystemTime() + "Z";
    }

    private static String getFormattedSystemDate() {
        Date currentDate = Calendar.getInstance().getTime();
        return new SimpleDateFormat("yyyy-MM-dd").format(currentDate);
    }

    private static String getFormattedSystemTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        TimeZone timeZone = TimeZone.getTimeZone("GMT");
        dateFormat.setTimeZone(timeZone);
        long now = Calendar.getInstance().getTimeInMillis();
        return dateFormat.format(now - 1800000);
    }

    private static String buildSectionsParam(Context context) {
        StringBuilder sectionsParam = new StringBuilder();
        ArrayList<String> sections = SortSectionsActivity.getSections(context);
        int size = sections.size();
        for (int i = 0; i < size - 1; i++) {
            sectionsParam.append(sections.get(i));
            sectionsParam.append("|");
        }
        sectionsParam.append(sections.get(size - 1));
        return sectionsParam.toString();
    }

    private static URL buildUrl(String section, Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        Uri builtUri = Uri.parse(Constants.BASE_URL).buildUpon()
                .appendQueryParameter(Constants.SECTION_PARAM, section)
                .appendQueryParameter(Constants.SHOW_FIELDS_KEY, Constants.SHOW_FIELDS_VALUE)
                .appendQueryParameter(Constants.ORDER_BY_PARAM, sharedPreferences.getString(context.getString(R.string.pref_key_orderBy), context.getString(R.string.pref_orderby_default)))
                .appendQueryParameter(Constants.PAGE_SIZE_PARAM, sharedPreferences.getString(context.getString(R.string.pref_key_itemsPerPage), context.getString(R.string.pref_itemPerPage_default)))
                .appendQueryParameter(Constants.GUARDIAN_API_KEY, Constants.GUARDIAN_API_VALUE)
                .build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // Closing the input stream could throw an IOException, which is why
                // the makeHttpRequest(URL url) method signature specifies than an IOException
                // could be thrown.
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    private static ContentValues[] createContentValuesFromJson(String newsJSON) {
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(newsJSON)) {
            return null;
        }

        ContentValues[] contentValues = null;
        // Try to parse the JSON response string. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {

            // Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(newsJSON);

            JSONObject response = baseJsonResponse.getJSONObject(Constants.RESPONSE);

            JSONArray resultsArray = response.getJSONArray(Constants.RESULTS);

            contentValues = new ContentValues[resultsArray.length()];

            // For each earthquake in the earthquakeArray, create an {@link Earthquake} object
            for (int i = 0; i < resultsArray.length(); i++) {

                // Get a single earthquake at position i within the list of earthquakes
                JSONObject currentArticle = resultsArray.getJSONObject(i);

                String title = currentArticle.getString(Constants.WEB_TITLE);
                String webUrl = currentArticle.getString(Constants.WEB_URL);
                String date = currentArticle.optString(Constants.DATE_AND_TIME, "");
                String section = currentArticle.getString(Constants.SECTION);
                JSONObject fields = currentArticle.getJSONObject(Constants.FIELDS);
                String author = fields.optString(Constants.AUTHOR_NAME, " ");
                String trail = fields.optString(Constants.TRAIL, "");
                String body = fields.optString(Constants.BODY, " ");
                String thumbnail = fields.optString(Constants.THUMBNAIL, "");

                ContentValues values = new ContentValues();
                values.put(NewsEntry.COLUMN_TITLE, title);
                values.put(NewsEntry.COLUMN_THUMBNAIL_URL, thumbnail);
                values.put(NewsEntry.COLUMN_AUTHOR, author);
                values.put(NewsEntry.COLUMN_DATE, date);
                values.put(NewsEntry.COLUMN_WEB_URL, webUrl);
                values.put(NewsEntry.COLUMN_SECTION, section);
                values.put(NewsEntry.COLUMN_TRAIL, trail);
                values.put(NewsEntry.COLUMN_BODY, body);

                contentValues[i] = values;
            }

        } catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e("QueryUtils", "Problem parsing the earthquake JSON results", e);
        }
        return contentValues;
    }

    private static List<NewsArticle> createListFromJson(String newsJSON) {
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(newsJSON)) {
            return null;
        }

        ArrayList<NewsArticle> articles = new ArrayList<>();

        // Try to parse the JSON response string. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {

            // Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(newsJSON);

            JSONObject response = baseJsonResponse.getJSONObject(Constants.RESPONSE);

            JSONArray resultsArray = response.getJSONArray(Constants.RESULTS);

            // For each earthquake in the earthquakeArray, create an {@link Earthquake} object
            for (int i = 0; i < resultsArray.length(); i++) {

                // Get a single earthquake at position i within the list of earthquakes
                JSONObject currentArticle = resultsArray.getJSONObject(i);

                String title = currentArticle.getString(Constants.WEB_TITLE);
                String webUrl = currentArticle.getString(Constants.WEB_URL);
                String date = currentArticle.optString(Constants.DATE_AND_TIME, "");
                String section = currentArticle.getString(Constants.SECTION);
                JSONObject fields = currentArticle.getJSONObject(Constants.FIELDS);
                String author = fields.optString(Constants.AUTHOR_NAME, " ");
                String trail = fields.optString(Constants.TRAIL, "");
                String body = fields.optString(Constants.BODY, " ");
                String thumbnail = fields.optString(Constants.THUMBNAIL, "");

               articles.add(new NewsArticle(0, title, thumbnail, author, date, webUrl, section, trail, body));
            }

        } catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e("QueryUtils", "Problem parsing the earthquake JSON results", e);
        }
        return articles;
    }

}
