package com.dk.ethereumwallet.services;

/**
 * Created by ayrton on 2/6/18.
 */

public class WalletService {
    private static final WalletService instance = new WalletService();
    private WalletService() {
    }

    public static WalletService getInstance() {
        return instance;
    }
}
