package com.dk.ethereumwallet.core;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.TextView;

import com.dk.ethereumwallet.MainActivity;
import com.dk.ethereumwallet.R;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;

import static com.dk.ethereumwallet.core.EventReceiver.GEN_WALLET;
import static com.dk.ethereumwallet.core.EventReceiver.GET_ADDRESS;
import static com.dk.ethereumwallet.core.EventReceiver.GET_BALANCE;
import static com.dk.ethereumwallet.core.EventReceiver.PAY_TO_PEER;
import static com.dk.ethereumwallet.core.EventReceiver.REQ_ERROR;
import static com.dk.ethereumwallet.core.EventReceiver.UNLOCK_WALLET;

public class TestService extends IntentService {
    private static Credentials _cred = null;
    private static Web3j _web3 = null;

    public static Credentials credentials() {
        return _cred;
    }

    public TestService() {
        super("TestService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String action = bundle.getString(EventReceiver.ACTION);
            new Thread(() -> {
                try {
                    if (_cred == null) {
                        _cred = unlockWallet(bundle);
                        Log.d("cp", "[service] credencials loaded.");
                        Log.d("cp", "[service] Pay me: " + _cred.getAddress());
                    }

                    if (_web3 == null) {
                        _web3 = connectToEthNetwork();
                    }

                    switch (action) {
                        case GET_ADDRESS: // (password)
                            String address=getAddress(bundle);
                            publish(bundle, address);
                            break;
                        case GET_BALANCE: // (address)
                            String balance=getBalance(bundle);
                            publish(bundle, balance);
                            break;
                        case PAY_TO_PEER:
                            String txHash=payToPeer(bundle);
                            publish(bundle, txHash);
                            break;
                        case UNLOCK_WALLET: // (password)
                            unlockWallet(bundle);
                            publish(bundle, "UNLOCK_OK");
                            break;
                        case GEN_WALLET:
                        default:
                            throw new ValidationException("Unavailable action: " + action);
                    }
                } catch (Exception re) {
                    if (re instanceof org.web3j.crypto.CipherException) {
                        error("Password invalida");
                    } else {
                        error(re);
                    }
                }
            }).start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO: do something useful
        if (intent != null) {
            Log.d("cp",  "[service] started on sticky mode");
            Log.d("cp",  "[service] got action " + intent.getStringExtra(EventReceiver.ACTION));
            onHandleIntent(intent);
        }

        return Service.START_STICKY;
    }

    public String payToPeer(Bundle intent) throws InterruptedException, TransactionException, IOException, Exception {
        if (!intent.containsKey("address") || !intent.containsKey("amount") ||
                !intent.containsKey("password")) {
            throw new ValidationException("Address,amount and password are required in order send the transaction");
        }

        String address=intent.getString("address");
        BigDecimal amount=new BigDecimal(intent.getString("amount"));
        String password=intent.getString("password");

        _cred = _unlockWallet(password);

        TransactionReceipt receipt = Transfer.sendFunds(
                _web3, _cred,
                address, amount, Convert.Unit.ETHER)
                .send();

        String txHash = receipt.getTransactionHash();
        return txHash;
    }

    private String getAddress(Bundle intent) {
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        String mainAddress = settings.getString(MainActivity.MAIN_ADDRESS, "");

        if (mainAddress.isEmpty()) {
            Credentials cred = unlockWallet(intent);
            mainAddress = cred.getAddress();
            settings.edit()
                    .putString(MainActivity.MAIN_ADDRESS, mainAddress)
                    .commit();
        }

        return mainAddress;
    }

    private String getBalance(Bundle intent) throws IOException {
        if (!intent.containsKey(EventReceiver.DATA)) {
            throw new ValidationException("Address is required in order to calculate balance!");
        }

        String address=intent.getString(EventReceiver.DATA);
        Log.d("cp", "address for balance is " + address);
        try {
            return _web3.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .send()
                    .getBalance()
                    .toString();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private Credentials unlockWallet(Bundle intent) {
        if (!intent.containsKey(EventReceiver.DATA)) {
            throw new ValidationException("password is required to unlock wallet");
        }

        String password=intent.getString(EventReceiver.DATA);
        return _unlockWallet(password);
    }

    private Credentials _unlockWallet(String password) {
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

    private void publish(Bundle bundle, String resp) {
        Intent intent = new Intent(EventReceiver.NOTIFICATION);
        intent.putExtra(EventReceiver.ACTION, bundle.getString(EventReceiver.ACTION));
        intent.putExtra(EventReceiver.DATA, resp);
        sendBroadcast(intent);
    }

    private void error(Exception err) {
        Intent intent = new Intent(EventReceiver.NOTIFICATION);
        intent.putExtra(EventReceiver.ACTION, EventReceiver.REQ_ERROR);
        intent.putExtra(EventReceiver.DATA, err.getMessage());
        sendBroadcast(intent);
        err.printStackTrace();
    }

    private void error(String err) {
        Intent intent = new Intent(EventReceiver.NOTIFICATION);
        intent.putExtra(EventReceiver.ACTION, EventReceiver.REQ_ERROR);
        intent.putExtra(EventReceiver.DATA, err);
        sendBroadcast(intent);
    }

    private Web3j connectToEthNetwork() {
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        String miner = settings.getString("miner", getString(R.string.default_net));

        try {
            Log.d("cp", "[service] Attempt to connect to network at " + miner);
            return Web3jFactory.build(new HttpService(miner));

        } catch (Exception e) {
            String msg = "[service] Unable to connect to network at " + miner;
            Log.d("cp", msg);
            throw new RuntimeException(msg);
        }
    }
}

