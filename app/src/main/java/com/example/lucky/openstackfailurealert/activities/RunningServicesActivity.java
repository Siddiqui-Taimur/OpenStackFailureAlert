package com.example.lucky.openstackfailurealert.activities;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.lucky.openstackfailurealert.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Taimur Siddiqui (siddiq.taimur@gmail.com)
 */

public class RunningServicesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running_services);

        final List<String> listData = new ArrayList<String>();
        boolean check = false;
        Map<String, ?> allEntries = getSharedPreferences("SIDD_FAILURE_ALERT_PREF", MODE_PRIVATE).getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String[] url_and_interval = entry.getValue().toString().split("~");
            if(url_and_interval.length==3) {
                check  = true;
                listData.add("Address: " + url_and_interval[0] + "\n" +
                        "Interval: " + url_and_interval[1] + " seconds");
            }
        }

        if(!check)
            Toast.makeText(this, "No failure check service is running right now", Toast.LENGTH_LONG).show();

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(RunningServicesActivity.this, R.layout.result_list_item, listData);

        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(arrayAdapter);


        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           final int pos, long id) {

                final String key  = listData.get(pos).split("\\n")[0].substring(9);
                Toast.makeText(RunningServicesActivity.this, key, Toast.LENGTH_LONG).show();

                new AlertDialog.Builder(RunningServicesActivity.this)
                        .setMessage("Delete this Failure Check Service?")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                listData.remove(pos);
                                arrayAdapter.notifyDataSetChanged();
                                SharedPreferences myPrefs = getSharedPreferences("SIDD_FAILURE_ALERT_PREF", MODE_PRIVATE);
                                SharedPreferences.Editor editor = myPrefs.edit();
                                editor.remove(key);
                                editor.commit();
                            }})
                        .setNegativeButton("NO", null).show();


                return true;
            }
        });

    }
}
