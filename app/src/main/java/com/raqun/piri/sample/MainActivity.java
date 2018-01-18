package com.raqun.piri.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.raqun.PiriActivity;
import com.raqun.piri.sample.model.Book;

@PiriActivity
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button navButton = (Button) findViewById(R.id.nav_button);

        final Long id = 1234567890L;
        final String name = "StringTest";
        final Book book = new Book();
        book.setBookId(8006);
        book.setBookName("BookTest");
        IntentCreator creator;
        AbstractIntentCreator yes;

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(PiriIntentFactory.newIntentForSecondActivity(MainActivity.this, id, name, book));
            }
        });
    }
}
