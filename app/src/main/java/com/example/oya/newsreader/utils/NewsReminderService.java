package com.example.oya.newsreader.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.oya.newsreader.model.NewsArticle;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import java.util.List;

public class NewsReminderService extends JobService{

    private static AsyncTask mBackgroundTask;

    @Override
    public boolean onStartJob(final JobParameters job) {
        mBackgroundTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                Context context = NewsReminderService.this;
                List<NewsArticle> newArticle = NetworkUtils.checkForNewArticle(context);
                if(!newArticle.isEmpty()) NotificationUtils.informUserOfTheNewArticle(context, newArticle);
                Log.v("NewsReminderService", "doInBackGround is called");
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                jobFinished(job, false);
                super.onPostExecute(o);

            }
        };
        mBackgroundTask.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        if(mBackgroundTask != null) mBackgroundTask.cancel(true);
        return true;
    }
}