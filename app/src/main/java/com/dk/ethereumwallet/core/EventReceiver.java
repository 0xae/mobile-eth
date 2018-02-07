package com.dk.ethereumwallet.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.dk.ethereumwallet.MainActivity;

import junit.framework.Test;

import org.jetbrains.annotations.NotNull;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

/**
 * Created by ayrton on 2/6/18.
 */
public class EventReceiver extends BroadcastReceiver {
    public static final String NOTIFICATION = "event_machine_notfs";
    public static final String DATA = "DATA";
    public static final String ACTION = "ACTION";
    public static final String GET_BALANCE = "GET_BALANCE";
    public static final String PAY_TO_PEER = "PAY_TO_PEER";
    public static final String GET_ADDRESS = "GET_ADDRESS";
    public static final String UNLOCK_WALLET = "UNLOCK_WALLET";
    public static final String GEN_WALLET = "GEN_WALLET";
    public static final String REQ_ERROR = "REQ_ERROR";
    private final EventHandler handler;

    public EventReceiver(EventHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String data = bundle.getString(EventReceiver.DATA);
            String action = bundle.getString(EventReceiver.ACTION);
            Log.d("cp", "[receiver] receive response("+action+") from service, data: " + data);

            switch (action) {
                case GET_ADDRESS:
                    handler.onPublicAddress(data);
                    break;
                case GET_BALANCE:
                    handler.onBalance(new BigDecimal(data));
                    break;
                case UNLOCK_WALLET:
                    handler.onUnlockWallet(TestService.credentials());
                    break;
                case PAY_TO_PEER:
                    handler.onPaymentSent(data); //txHash
                    break;
                case REQ_ERROR:
                    handler.onException(data);
                    break;
            }
        }
    }

    public static class Message {
        private String action;
        private String data;
        private String password;
        private String amount;
        private String address;

        private void setAction(String action) {
            this.action = action;
        }

        private String getAction() {
            return action;
        }

        private void setData(String data) {
            this.data = data;
        }

        private String getData() {
            return data;
        }

        private Message() {
        }

        public static Message getAddress(@NotNull  String password) {
            Message m = new Message();
            m.action = EventReceiver.GET_ADDRESS;
            m.data = password;
            return m;
        }

        public static Message getBalance(@NotNull String address) {
            Message m = new Message();
            m.setAction(EventReceiver.GET_BALANCE);
            m.setData(address);
            return m;
        }

        public static Message payToPeer(@NotNull String password, String address, String amount) {
            Message m = new Message();
            m.setAction(EventReceiver.PAY_TO_PEER);
            m.address = address;
            m.amount = amount;
            m.password = password;
            return m;
        }

        public Intent send(Context context){
            Intent i = new Intent(context, TestService.class);
            i.putExtra(EventReceiver.ACTION, getAction());
            if (PAY_TO_PEER.equals(EventReceiver.ACTION)) {
                i.putExtra("address", address);
                i.putExtra("password", password);
                i.putExtra("amount", amount);
            } else {
                i.putExtra(EventReceiver.DATA, getData());
            }
            context.startService(i);
            return i;
        }
    }

    public interface EventHandler {
        public void onBalance(BigDecimal value);
        public void onPaymentSent(String transactionHash);
        public void onPaymentReceived();
        public void onUnlockWallet(Credentials cred);
        public void onPublicAddress(String address);
        public void onNetworkDown();
        public void onNetworkUp();
        public void onException(String msg);
    }
}
