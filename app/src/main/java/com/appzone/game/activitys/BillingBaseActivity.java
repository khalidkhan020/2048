package com.appzone.game.activitys;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;

import com.appzone.game.util.IabBroadcastReceiver;
import com.appzone.game.util.IabHelper;
import com.appzone.game.util.IabResult;
import com.appzone.game.util.Inventory;
import com.appzone.game.util.Purchase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by khalid on 3/10/18.
 */

public abstract class BillingBaseActivity extends AppCompatActivity implements IabBroadcastReceiver.IabBroadcastListener {

    static final String SKU_ADDFREE_YEARLY = "com.appzone.game.6month_subscriptionforaddfreeapp";
    static final int RC_REQUEST = 10001;
    IabHelper mHelper;
    IabBroadcastReceiver mBroadcastReceiver;
    boolean mAutoRenewEnabled = false;
    boolean mSubscribedToInfiniteGas = false;
    String mInfiniteGasSku = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgwiZi2enjjw09JFE9DRV/IHLIMdhet/2loboIxzajuMq4sUZ5iFGxpLsC93ThCs+OnEcNfixX1piJjg98uFiV8KGhdRqICFcRtxeUbarwaX63lo1QRezG42FG2OebkL94C8hMplf31gCHGLwyj9pUQVnhnrJWEKDwwwHcqNnh4OBTNaCqebC+Ftd67LT7X+gATFrmw8eTvoSqCY1GJB0JFDih4lVn/VaknGcsDCcBT4ObHe4LqfvcroHhitdln3NyND01xC6zPExFXPb281M9E4aaJpX/lRROe2SspUtEAHI7nxm+VMvv6vsL/JVtwRSGA+h00OG4n5HkepBCBqSkwIDAQAB";
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.enableDebugLogging(false);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    complain("Problem setting up in-app billing: " + result);
                    return;
                }
                if (mHelper == null) return;
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });
        mBroadcastReceiver = new IabBroadcastReceiver(BillingBaseActivity.this);
        IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
        registerReceiver(mBroadcastReceiver, broadcastFilter);

    }

    protected abstract void onInventoryQueryFinished(IabResult result, Inventory inventory);

    protected abstract void hideAds(boolean hide);

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mHelper == null) return;
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }
            onInventoryQueryFinished(result, inventory);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mHelper == null) return;
        if (!mHelper.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    public void onAddfree(View arg0) {
        if (!mHelper.subscriptionsSupported()) {
            complain("Subscriptions not supported on your device yet. Sorry!");
            return;
        }
        if (!mSubscribedToInfiniteGas || !mAutoRenewEnabled) {
            String payload = "jbjbgiugiunugungygygygb8899870987887889";
            List<String> oldSkus = null;
            if (!TextUtils.isEmpty(mInfiniteGasSku)) {
                oldSkus = new ArrayList<>();
                oldSkus.add(mInfiniteGasSku);
            }
            try {
                mHelper.launchPurchaseFlow(this, SKU_ADDFREE_YEARLY, IabHelper.ITEM_TYPE_SUBS,
                        oldSkus, RC_REQUEST, mPurchaseFinishedListener, payload);
            } catch (IabHelper.IabAsyncInProgressException e) {
                complain("Error launching purchase flow. Another async operation in progress.");
            }
        }
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                return;
            }

            if (purchase.getSku().equals(SKU_ADDFREE_YEARLY)) {
                mSubscribedToInfiniteGas = true;
                mAutoRenewEnabled = purchase.isAutoRenewing();
                mInfiniteGasSku = purchase.getSku();
                hideAds(mSubscribedToInfiniteGas);
            }
        }
    };

    @Override
    public void receivedBroadcast() {
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error querying inventory. Another async operation in progress.");
        }
    }

    void complain(String message) {
        alert("Error: " + message);
    }

    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        bld.create().show();
    }
}
