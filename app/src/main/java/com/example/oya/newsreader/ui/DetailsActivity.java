package com.example.oya.newsreader.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.oya.newsreader.R;
import com.example.oya.newsreader.model.NewsArticle;
import com.example.oya.newsreader.utils.Constants;

public class DetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        //Set the toolbar and enable up button
        Toolbar toolbar = findViewById(R.id.toolbar_det);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //Initialize views
        TextView title_tv = findViewById(R.id.details_title);
        TextView trail_tv = findViewById(R.id.details_trail);
        TextView body_tv = findViewById(R.id.details_body);
        ImageView details_iv = findViewById(R.id.details_image);
        TextView author_tv = findViewById(R.id.details_author);
        TextView section_tv = findViewById(R.id.details_section);
        //Get the chosen article info from the bundle
        Bundle bundle = getIntent().getExtras();
        NewsArticle chosenArticle = bundle.getParcelable(Constants.CHOSEN_ARTICLE);
        //Set the appropriate texts and image
        title_tv.setText(chosenArticle.getTitle());
        trail_tv.setText(Html.fromHtml(chosenArticle.getArticleTrail()));
        body_tv.setText(Html.fromHtml(chosenArticle.getArticleBody()));
        Glide.with(this)
                .load(chosenArticle.getThumbnailUrl())
                .into(details_iv);
        author_tv.setText("By " + chosenArticle.getAuthor());
        section_tv.setText(chosenArticle.getSection());
    }
}