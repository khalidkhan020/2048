package com.appzone.game.activitys;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.AppCompatImageView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.appzone.game.R;
import com.appzone.game.board.MainView;
import com.appzone.game.board.model.MainGame;
import com.appzone.game.board.model.Tile;
import com.appzone.game.util.IabResult;
import com.appzone.game.util.Inventory;
import com.appzone.game.util.Purchase;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

public class MainActivity extends BillingBaseActivity implements View.OnClickListener, MainView.OnScoreUpdate, MainGame.OnSpetialTileValueFind {

    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String SCORE = "score";
    private static final String HIGH_SCORE = "high score temp";
    private static final String UNDO_SCORE = "undo score";
    private static final String CAN_UNDO = "can undo";
    private static final String UNDO_GRID = "undo";
    private static final String GAME_STATE = "game state";
    private static final String UNDO_GAME_STATE = "undo game state";
    private MainView view;
    private AdView adView;
    private InterstitialAd mInterstitialAd;
    TextView tvMode, tvHighScore, tvScore;
    AppCompatImageView makeAddFree, tvShare;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        view = findViewById(R.id.gameBoard);
        adView = findViewById(R.id.adView);
        tvShare = findViewById(R.id.ivShare);
        tvHighScore = findViewById(R.id.tvHighScore);
        tvScore = findViewById(R.id.tvScore);
        findViewById(R.id.tvUndu).setOnClickListener(this);
        tvShare.setOnClickListener(this);
        findViewById(R.id.reset).setOnClickListener(this);
        makeAddFree = findViewById(R.id.makeAddFree);
        makeAddFree.setOnClickListener(this);
        view.game.setSpetialTileValueFind(this);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        view.hasSaveState = settings.getBoolean("save_state", false);
        view.setOnScoreUpdate(this);
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasState")) {
                load();
            }
        }
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-7563654685686304/5322942009");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        });

    }

    @Override
    protected void onInventoryQueryFinished(IabResult result, Inventory inventory) {
        Purchase gasYearly = inventory.getPurchase(SKU_ADDFREE_YEARLY);
        if (gasYearly != null && gasYearly.isAutoRenewing()) {
            mInfiniteGasSku = SKU_ADDFREE_YEARLY;
            mAutoRenewEnabled = true;
        } else {
            mInfiniteGasSku = "";
            mAutoRenewEnabled = false;
        }
        mSubscribedToInfiniteGas = (gasYearly != null && verifyDeveloperPayload(gasYearly));
        hideAds(mSubscribedToInfiniteGas);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            //Do nothing
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            view.game.move(2);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            view.game.move(0);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            view.game.move(3);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            view.game.move(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("hasState", true);
        save();
    }

    protected void onPause() {
        super.onPause();
        save();
    }

    private void save() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = view.game.grid.field;
        Tile[][] undoField = view.game.grid.undoField;
        editor.putInt(WIDTH, field.length);
        editor.putInt(HEIGHT, field.length);
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] != null) {
                    editor.putInt(xx + " " + yy, field[xx][yy].getValue());
                } else {
                    editor.putInt(xx + " " + yy, 0);
                }

                if (undoField[xx][yy] != null) {
                    editor.putInt(UNDO_GRID + xx + " " + yy, undoField[xx][yy].getValue());
                } else {
                    editor.putInt(UNDO_GRID + xx + " " + yy, 0);
                }
            }
        }
        editor.putLong(SCORE, view.game.score);
        editor.putLong(HIGH_SCORE, view.game.highScore);
        editor.putLong(UNDO_SCORE, view.game.lastScore);
        editor.putBoolean(CAN_UNDO, view.game.canUndo);
        editor.putInt(GAME_STATE, view.game.gameState);
        editor.putInt(UNDO_GAME_STATE, view.game.lastGameState);
        editor.commit();
    }

    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        //Stopping all animations
        view.game.aGrid.cancelAnimations();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        for (int x = 0; x < view.game.grid.field.length; x++) {
            for (int y = 0; y < view.game.grid.field[0].length; y++) {
                int value = settings.getInt(x + " " + y, -1);
                if (value > 0) {
                    view.game.grid.field[x][y] = new Tile(x, y, value);
                } else if (value == 0) {
                    view.game.grid.field[x][y] = null;
                }

                int undoValue = settings.getInt(UNDO_GRID + x + " " + y, -1);
                if (undoValue > 0) {
                    view.game.grid.undoField[x][y] = new Tile(x, y, undoValue);
                } else if (value == 0) {
                    view.game.grid.undoField[x][y] = null;
                }
            }
        }

        view.game.score = settings.getLong(SCORE, view.game.score);
        view.game.highScore = settings.getLong(HIGH_SCORE, view.game.highScore);
        view.game.lastScore = settings.getLong(UNDO_SCORE, view.game.lastScore);
        view.game.canUndo = settings.getBoolean(CAN_UNDO, view.game.canUndo);
        view.game.gameState = settings.getInt(GAME_STATE, view.game.gameState);
        view.game.lastGameState = settings.getInt(UNDO_GAME_STATE, view.game.lastGameState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvUndu:
                view.game.revertUndoState();
                break;
            case R.id.reset:
                if (!view.game.isGameLost()) {
                    new AlertDialog.Builder(this)
                            .setPositiveButton(com.appzone.game.R.string.reset, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    view.game.newGame();
                                }
                            })
                            .setNegativeButton(com.appzone.game.R.string.continue_game, null)
                            .setTitle(com.appzone.game.R.string.reset_dialog_title)
                            .setMessage(com.appzone.game.R.string.reset_dialog_message)
                            .show();
                } else {
                    view.game.newGame();
                }
                break;
            case R.id.makeAddFree:
                onAddfree(v);
                break;
            case R.id.ivShare:
            if(!isSharing) {
                isSharing = true;
            Thread thread=new Thread(new Runnable() {
                @Override
                public void run() {
                    Intent sendIntent = new Intent();

                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
                    sendIntent.setType("text/plain");
                    sendIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(Intent.createChooser(sendIntent, "Share with..."));
                    isSharing = false;
                }
            });
            thread.start();

            }
                break;
        }
    }
    boolean isSharing=false;
    @Override
    public void onScoreUpdated() {
        tvHighScore.setText(view.game.highScore + "");
        tvScore.setText(view.game.score + "");
    }

    @Override
    protected void hideAds(boolean hide) {
        if (hide) {
            makeAddFree.setVisibility(View.GONE);
            adView.setVisibility(View.GONE);
            adView.destroy();
        } else {
            makeAddFree.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    @Override
    public void onEndLessMode() {
        tvMode.setText("Endless Mode Enabled.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }
    }

    @Override
    public void spetialFound(int value) {
        if (!mSubscribedToInfiniteGas)
            if (value > 512 && mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            }
    }
}
