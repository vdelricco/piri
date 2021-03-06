package com.raqun.piri.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.raqun.PiriParam;
import com.raqun.piri.sample.model.Book;

public class MainActivity extends AppCompatActivity {
    @PiriParam
    protected String mainString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Piri.bind(this);

        final Button navButton = (Button) findViewById(R.id.nav_button);

        final Long id = 1234567890L;
        final String name = "StringTest";
        final Book book = new Book();
        book.setBookId(8006);
        book.setBookName("BookTest");

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new SecondActivityIntentCreator(MainActivity.this, id, 38)
                        .book(book)
                        .name(name)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK, Intent.FLAG_ACTIVITY_NEW_TASK)
                        .create();
                startActivity(intent);
            }
        });
    }
}
