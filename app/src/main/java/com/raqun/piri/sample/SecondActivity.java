package com.raqun.piri.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.raqun.PiriParam;
import com.raqun.piri.sample.model.Book;

/**
 * Created by tyln on 09/05/2017.
 */

public class SecondActivity extends AppCompatActivity {
    @PiriParam(required = true)
    protected Long id;

    @PiriParam
    protected String name;

    @PiriParam
    protected Book book;

    @PiriParam(required = true)
    protected Integer integer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        Piri.bind(this);

        final TextView textViewId = (TextView) findViewById(R.id.textview_id);
        textViewId.setText("The id passed with Piri: " + id);

        final TextView textViewName = (TextView) findViewById(R.id.textview_name);
        textViewName.setText("The name passed with Piri: " + name);

        final TextView textViewBookId = (TextView) findViewById(R.id.textview_book_id);
        textViewBookId.setText("The book id passed with Piri: " + book.getBookId());

        final TextView textViewBookName = (TextView) findViewById(R.id.textview_book_name);
        textViewBookName.setText("The book name passed with Piri: " + book.getBookName());

        final TextView textViewInteger = (TextView) findViewById(R.id.textview_integer);
        textViewInteger.setText("The integer name passed with Piri: " + integer);
    }
}
