package com.dk.ethereumwallet;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import rx.Subscription;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final BigInteger GAS_PRICE = new BigInteger("210000");
    public static final BigInteger GAS_LIMIT = new BigInteger("600000");

    private Web3j web3j;
    private Subscription sub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        connectToEthNetwork();
    }

    private void connectToEthNetwork() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    web3j = Web3jFactory.build(new HttpService("http://192.168.43.201:8545"));
                    Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
                    String clientVersion = web3ClientVersion.getWeb3ClientVersion();

                    Log.d("cp", "clientVersion " + clientVersion);

                    Credentials credentials = getWallet("123");

                    sub = watchBalance(web3j, credentials.getAddress());
                    final String address = credentials.getAddress();

                    Log.d("cp", "[INFO] credencials loaded.");
                    Log.d("cp", "Pay me: " + credentials.getAddress());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.address))
                                    .setText(address.substring(0, 16));
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private Credentials getWallet(String password) {
        File dir = getFilesDir();
        String FILENAME = "wallet_file";
        File walletFile;

        try {
            InputStream stream = openFileInput(FILENAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String name = reader.readLine();
            stream.close();
            walletFile = new File(dir, name);
            Log.d("cp", "wallet file  is '" + name + "'");
        } catch (IOException e) {
            Log.d("cp", "could not open wallet file " + e.getMessage());
            try {
                Log.d("cp", "generating wallet file");

                String walletName = WalletUtils.generateNewWalletFile(password, dir, false);
                walletFile = new File(dir, walletName);

                Log.d("cp", "generated wallet file at " + walletFile);

                FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
                fos.write(walletName.getBytes());
                fos.close();
            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }
        }

        try {
            Credentials c = WalletUtils.loadCredentials(password, walletFile);
            Log.d("cp", "loaded wallet file ");
            return c;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Subscription watchBalance(Web3j web3, String address) throws Exception {
        return web3.ethGetBalance(address, DefaultBlockParameterName.LATEST)
            .observable()
            .subscribe(val -> {
                BigDecimal balance = Convert.fromWei(
                    val.getBalance().toString(),
                    Convert.Unit.ETHER
                );

                Log.d("cp", "New balance for " + address + ": " + balance);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.balance))
                                .setText(balance.toString() + " ETH");
                    }
                });
            });
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

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
