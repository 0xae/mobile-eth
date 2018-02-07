package com.dk.ethereumwallet;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

public class PaymentActivity extends AppCompatActivity {
    public static final BigInteger GAS_PRICE = new BigInteger("210000");
    public static final BigInteger GAS_LIMIT = new BigInteger("600000");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment2);
        String exampleAddress = "0xcB14a11F75CDFb1613E30ebF3512103dB0f27A0C";

        Bundle extras = getIntent().getExtras();
        String addr;
        if (extras != null && (addr = extras.getString("address")) != null) {
            ((TextView)findViewById(R.id.payment_address)).setText(addr);
        }

        ((Button) findViewById(R.id.send_payment))
            .setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    String address;
                    String value;

                    address = ((TextView)findViewById(R.id.payment_address)).getText().toString();
                    value = ((TextView)findViewById(R.id.payment_value)).getText().toString();

                    new AlertDialog.Builder(PaymentActivity.this)
                        .setTitle("Confirmar pagamento")
                        .setMessage("Confirmar pagamento de " + value + " para " + address + " ?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                new Thread(() -> {
                                    try {
                                        BigDecimal amount = new BigDecimal(value);
                                        payToPerson(MainActivity.web3j(), MainActivity.credentials(), address, amount);
                                        Toast.makeText(PaymentActivity.this,
                                                "Pagamento enviado!", Toast.LENGTH_SHORT)
                                                .show();
                                        runOnUiThread(() -> {
                                            Intent intent = new Intent(PaymentActivity.this, MainActivity.class);
                                            finish();
                                            startActivity(intent);
                                        });
                                    } catch (RuntimeException e) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(PaymentActivity.this,
                                                "O seu pagamento nao pode ser enviado."+
                                                        "verique se tem fundos disponiveis.", Toast.LENGTH_SHORT)
                                                .show();
                                        });
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) { }
                        }).show();
                }
            });

        ((Button) findViewById(R.id.cancel_payment))
        .setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PaymentActivity.this, MainActivity.class));
            }
        });
    }

    private void payToPerson(Web3j web3, Credentials cred, String address, BigDecimal amount) {
        try {
            TransactionReceipt receipt = Transfer.sendFunds(
                    web3, cred,
                    address, amount, Convert.Unit.ETHER)
                    .send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
