package com.dk.ethereumwallet.core;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * helper methods.
 */
public class BackendService extends IntentService {
    public static String UNCLOCK_WALLET = "unlock_wallet";
    public static String GET_ADDRESS = "get_address";
    public static String GET_BALANCE = "get_balance";

    public BackendService() {
        super("BackendService");
    }

    /**
     * @see IntentService
     */
    public static Intent start(Context context, String action) {
        Intent intent = new Intent(context, BackendService.class);
        intent.setAction(action);
        context.startService(intent);
        return intent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent,flags,startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d("cp", "action: " + action);
        }
    }
}
