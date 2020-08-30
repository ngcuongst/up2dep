package saarland.cispa.up2dep.javaexemplary;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestBuilder;

import org.robolectric.shadows.util.SQLiteLibraryLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void testSQLiteLoader(SQLiteLibraryLoader sqLiteLibraryLoader){
        try {
            sqLiteLibraryLoader.doLoad();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private RequestBuilder testPicasso(){
        Picasso piscasso = Picasso.with(this);
        RequestBuilder requestBuilder = piscasso.load("https://example.com");
        return requestBuilder;
    }


}
