package com.example.lucky.openstackfailurealert.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.lucky.openstackfailurealert.R;

/**
 * @author Taimur Siddiqui (siddiq.taimur@gmail.com)
 */

public class HomeActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.openstack_white_50);
//        getSupportActionBar().setDisplayUseLogoEnabled(true);

        (Toast.makeText(HomeActivity.this, "Select Service, for which you want to get Alert upon failure", Toast.LENGTH_LONG)).show();

        Button btnArr[] = new Button[]{
                (Button) findViewById(R.id.horizon),
                (Button) findViewById(R.id.keystone),
                (Button) findViewById(R.id.compute),
                (Button) findViewById(R.id.cinder),
                (Button) findViewById(R.id.neutron),
                (Button) findViewById(R.id.draas),
                (Button) findViewById(R.id.glance),
                (Button) findViewById(R.id.swift)
        };

        for(int i=0; i<8; i++)
            btnArr[i].setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {

        String serverName  = null;

        switch(view.getId()) {
            case R.id.horizon:
                serverName = "Dashboard";
                break;
            case R.id.keystone:
                serverName = "Keystone";
                break;
            case R.id.compute:
                serverName = "Compute";
                break;
            case R.id.draas:
                serverName = "DRaaS";
                break;
            case R.id.cinder:
                serverName = "Cinder";
                break;
            case R.id.glance:
                serverName = "Glance";
                break;
            case R.id.swift:
                serverName = "Swift";
                break;
            case R.id.neutron:
                serverName = "Neutron";
                break;
        }

        Intent intent = new Intent(HomeActivity.this, StartingServiceActivity.class);
        intent.putExtra("SERVER_NAME", serverName);
        startActivity(intent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.running_services:
                Intent intent = new Intent(HomeActivity.this, RunningServicesActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
