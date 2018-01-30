package com.dk.ethereumwallet;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import org.web3j.crypto.Wallet;
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
    public static final String PREFS_NAME = "AppSettings";

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
                if (_cred == null || web3j == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(view, R.string.not_available, Snackbar.LENGTH_LONG).show();
                        }
                    });
                    return;
                }

                startActivity(new Intent(MainActivity.this, PaymentActivity.class));
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
        if (web3j == null) {
            connectToEthNetwork();
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            updateBalance(web3j, _cred.getAddress());
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                    }
                }
            }).start();
        }
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

    private void connectToEthNetwork() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                String miner = settings.getString("miner", getString(R.string.default_net));

                try {
                    Log.d("cp", "Attempt to connect to network at " + miner);
                    web3j = Web3jFactory.build(new HttpService(miner));
                } catch (Exception e) {
                    String msg = "Unable to connect to network at " + miner;
                    runOnUiThread(() -> {
                        Snackbar.make(getCurrentFocus(), msg, Snackbar.LENGTH_LONG).show();
                    });
                    Log.d("cp", msg);
                    return;
                }

                try {
                    Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
                    String clientVersion = web3ClientVersion.getWeb3ClientVersion();

                    _cred = getWallet("123");

                    updateBalance(web3j, _cred.getAddress());

                    Log.d("cp", "clientVersion " + clientVersion);
                    Log.d("cp", "[INFO] credencials loaded.");
                    Log.d("cp", "Pay me: " + _cred.getAddress());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.address))
                                    .setText(_cred.getAddress().substring(0, 10));
                        }
                    });
                } catch (Exception e) {
                    // throw new RuntimeException(e);
                    e.printStackTrace();
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

    private void updateBalance(Web3j web3, String address) throws Exception {
        web3.ethGetBalance(address, DefaultBlockParameterName.LATEST)
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
}
