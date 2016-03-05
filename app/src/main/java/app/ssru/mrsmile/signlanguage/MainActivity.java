package app.ssru.mrsmile.signlanguage;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static FrontBuild frontBuild;
    private static TextView txtHand;
    private static ListView listView;

    private static List<String> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtHand = (TextView) findViewById(R.id.txtHead);
        listView = (ListView) findViewById(R.id.listView);

        frontBuild = new FrontBuild(getApplicationContext());

        txtHand.setTypeface(frontBuild.CANTERBURY);

        list = new ArrayList<>();
        list.add("The Translation");
        list.add("Video Sign language");
        list.add("How to use");
        list.add("Exit");

        ListViewAdapter adapter = new ListViewAdapter(getApplicationContext(), list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = null;

                switch (position) {
                    case 0 :
                        intent = new Intent(getApplicationContext(), TheTranslation.class);
                        break;
                    case 1 :
                        intent = new Intent(getApplicationContext(), VideoSign.class);
                        break;
                    case 2 :
                        intent = new Intent(getApplicationContext(), HowTo.class);
                        break;
                    case 3:
                        finish();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(), position + "", Toast.LENGTH_SHORT).show();
                }

                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
       // super.onBackPressed();
    }
}
