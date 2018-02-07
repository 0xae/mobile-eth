package com.dk.ethereumwallet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dk.ethereumwallet.core.EventReceiver;
import com.dk.ethereumwallet.core.TestService;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        EventReceiver.EventHandler {
    public static final String PREFS_NAME = "AppSettings";
    public static final String MAIN_ADDRESS = "main_address";

    public static final BigInteger GAS_PRICE = new BigInteger("210000");
    public static final BigInteger GAS_LIMIT = new BigInteger("600000");

    private static Web3j web3j = null;
    private static Credentials _cred = null;

    public static Web3j web3j() {
        return web3j;
    }

    private static void credentials(Credentials cred) {
        _cred = cred;
    }

    public static Credentials credentials() {
        return _cred;
    }

    private BroadcastReceiver receiver;
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        receiver = new EventReceiver(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(EventReceiver.NOTIFICATION));
        EventReceiver.Message
                .getAddress("123")
                .send(getApplicationContext());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public void onBalance(BigDecimal value) {
        Log.d("cp", "[activity] new balance: " + value);
        if (value.equals(currentBalance) || BigDecimal.ZERO.equals(value))
            return;

        currentBalance = value;
        BigDecimal balanceFmt = Convert.fromWei(
            value.toString(), Convert.Unit.ETHER
        );

        ((TextView)findViewById(R.id.balance))
            .setText(balanceFmt + " ETH");
    }

    @Override
    public void onPaymentSent(String transactionHash) {
    }

    @Override
    public void onPaymentReceived() {
    }

    @Override
    public void onUnlockWallet(Credentials cred) {
    }

    @Override
    public void onPublicAddress(String address) {
        Log.d("cp", "[activity] new address: " + address);
        ((TextView)findViewById(R.id.address))
            .setText(address);

        EventReceiver.Message
                .getBalance(address)
                .send(getApplicationContext());
    }

    @Override
    public void onNetworkDown() {
    }

    @Override
    public void onNetworkUp() {
    }

    @Override
    public void onException(String msg) {
        Log.d("cp", "An exception ocurred: " + msg);
        Toast.makeText(MainActivity.this,
                        msg, Toast.LENGTH_SHORT)
            .show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.menu_qrcode) {
            startActivity(new Intent(MainActivity.this, PaymentScannerActivity.class));
        } else if (id == R.id.receive_payment) {
            Intent intent = new Intent(MainActivity.this, ReceivePaymentActivity.class);
            if (_cred != null) {
                String address = _cred.getAddress();
                intent.putExtra("address", address);
            }
            startActivity(intent);
        } else if (id == R.id.peer_map) {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        } else if (id == R.id.app_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
