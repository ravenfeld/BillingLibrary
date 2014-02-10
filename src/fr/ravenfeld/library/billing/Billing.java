package fr.ravenfeld.library.billing;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.robotmedia.billing.BillingController;
import net.robotmedia.billing.BillingRequest.ResponseCode;
import net.robotmedia.billing.helper.AbstractBillingActivity;
import net.robotmedia.billing.model.Transaction;
import net.robotmedia.billing.model.Transaction.PurchaseState;

import java.util.List;


public class Billing extends AbstractBillingActivity {

    public static final String EXTRA_CHECK_TRANSACTION =
            "fr.ravenfeld.library.billing.EXTRA_CHECK_TRANSACTION";
    private View mBuyButton;
    private View mExitButton;
    private TextView mTextView;
    private SharedPreferences mSharedPreferences;
    private String mIdProduct;
    private String mKeyPublicBilling;
    private String mKeyPrefBilling;
    private double mSizePopup;
    private int mIdImage;
    private boolean mCheckTransaction = false;
    private int mIdLayout;
    private boolean mLayoutUser = false;

    public void onCreate(Bundle savedInstanceState, String sharedPrefName, String keyPrefBilling, String idProduct, String keyPublicBilling,
                         int idImage, double sizePopup) {

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (intent != null) {
            mCheckTransaction = intent.getBooleanExtra(EXTRA_CHECK_TRANSACTION, false);
        }

        mSharedPreferences = getBaseContext().getSharedPreferences(sharedPrefName, 0);
        mKeyPrefBilling = keyPrefBilling;
        mIdProduct = idProduct;
        mKeyPublicBilling = keyPublicBilling;
        mSizePopup = sizePopup;
        mIdImage = idImage;
        BillingController.registerObserver(mBillingObserver);
        BillingController.checkBillingSupported(this);
    }

    public void onCreate(Bundle savedInstanceState, String sharedPrefName, String keyPrefBilling, String idProduct, String keyPublicBilling,
                         int idLayout) {

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (intent != null) {
            mCheckTransaction = intent.getBooleanExtra(EXTRA_CHECK_TRANSACTION, false);
        }

        mSharedPreferences = getBaseContext().getSharedPreferences(sharedPrefName, 0);
        mKeyPrefBilling = keyPrefBilling;
        mIdProduct = idProduct;
        mKeyPublicBilling = keyPublicBilling;
        mLayoutUser = true;
        mIdLayout = idLayout;
        BillingController.registerObserver(mBillingObserver);
        BillingController.checkBillingSupported(this);
    }

    protected void onResume() {
        super.onResume();
        restoreTransactions();
    }

    private void showPopup() {
        boolean appBuy = mSharedPreferences.getBoolean(mKeyPrefBilling, false);
        if (!mCheckTransaction && !appBuy) {
            initView();
            initButtons();

        } else {
            finish();
        }

    }

    private void initView() {
        if (mLayoutUser) {
            initViewUser();
        } else {
            initViewDefault();
        }
    }

    private void initViewDefault() {
        setContentView(R.layout.popup_full);
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int layoutWidth = (int) (display.getWidth() * mSizePopup);
        if (layoutWidth % 2 == 0) {
            layoutWidth += 1;
        }
        LinearLayout popup = (LinearLayout) getLayoutInflater().inflate(R.layout.popup, null);
        LinearLayout.LayoutParams layoutParam = new LinearLayout.LayoutParams(layoutWidth, 0);
        layoutParam.height = LayoutParams.FILL_PARENT;

        LinearLayout main = (LinearLayout) findViewById(R.id.popup_billing);

        main.removeAllViewsInLayout();

        main.addView(popup, layoutParam);

        ImageView image = (ImageView) findViewById(R.id.image);
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), mIdImage);
        image.setImageBitmap(scaleBitmap(imageBitmap, layoutWidth));
        mBuyButton = (View) findViewById(R.id.unblock_button);
        mExitButton = (View) findViewById(R.id.exit_button);
        mTextView = (TextView) findViewById(R.id.text);
    }

    private void initViewUser() {
        setContentView(mIdLayout);
        mBuyButton = (View) findViewById(R.id.unblock_button);
        mExitButton = (View) findViewById(R.id.exit_button);
        mTextView = (TextView) findViewById(R.id.text);
    }

    private Bitmap scaleBitmap(Bitmap b, int width) {
        int bw = b.getWidth();
        int bh = b.getHeight();
        double s = (double) width / (double) bw;
        int newHeight = (int) (bh * s);

        Bitmap result = Bitmap.createScaledBitmap(b, width, newHeight, false);
        return (result);
    }

    private void initButtons() {
        if (mBuyButton != null) {
            mBuyButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    BillingController.requestPurchase(Billing.this, mIdProduct);
                }
            });
        }
        if (mExitButton != null) {
            mExitButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        boolean app_buy = mSharedPreferences.getBoolean(mKeyPrefBilling, false);
        if (app_buy) {
            if (mExitButton != null) {
                mExitButton.setVisibility(Button.VISIBLE);
            }
            if (mBuyButton != null) {
                mBuyButton.setVisibility(Button.GONE);
            }
        } else {
            if (mExitButton != null) {
                mExitButton.setVisibility(Button.VISIBLE);
            }
            if (mBuyButton != null) {
                mBuyButton.setVisibility(Button.VISIBLE);
            }
        }

    }

    @Override
    public byte[] getObfuscationSalt() {
        return new byte[]{41, -90, -116, -41, 66, -53, 122, -110, -127, -96, -88, 77, 127, 115, 1, 73, 57, 110, 48, -116};
    }

    @Override
    public String getPublicKey() {
        return mKeyPublicBilling;
    }

    @Override
    public void onBillingChecked(boolean supported) {
        if (supported) {
            restoreTransactions();
        } else {
            showDialog(R.string.billing_not_supported_message);
        }
    }

    @Override
    public void onSubscriptionChecked(boolean supported) {

    }

    @Override
    public void restoreTransactions() {
        super.restoreTransactions();

        BillingController.restoreTransactions(this);
        checkPurchase();

    }

    private void checkPurchase() {
        if (mIdProduct != null) {
            List<Transaction> transactions = BillingController.getTransactions(this.getBaseContext(), mIdProduct);
            if (transactions.isEmpty()) {
                showPopup();
            }
            for (Transaction transaction : transactions) {
                if (transaction.purchaseState == PurchaseState.PURCHASED) {
                    finish();
                } else {
                    canceledApp();
                    showPopup();
                }
            }
        }
    }

    @Override
    public void onPurchaseStateChanged(String id_product, PurchaseState status) {
        if (id_product.equalsIgnoreCase(mIdProduct) && status == PurchaseState.PURCHASED) {
            purchasedApp();
            if (mBuyButton != null) {
                mBuyButton.setVisibility(Button.GONE);
            }
            if (mTextView != null) {
                mTextView.setText(R.string.popup_text_unlock);
            }
            if (mExitButton != null) {
                mExitButton.setVisibility(Button.VISIBLE);
            }else{
                finish();
            }
        } else {
            canceledApp();
        }
    }

    @Override
    public void onRequestPurchaseResponse(String id_product, ResponseCode code) {
        if (id_product.equalsIgnoreCase(mIdProduct) && code == ResponseCode.RESULT_OK) {
            purchasedApp();
            if (mBuyButton != null) {
                mBuyButton.setVisibility(Button.GONE);
            }
            if (mTextView != null) {
                mTextView.setText(R.string.popup_text_unlock);
            }
            if (mExitButton != null) {
                mExitButton.setVisibility(Button.VISIBLE);
            }else{
                finish();
            }
        } else {
            canceledApp();
        }
    }


    private void purchasedApp() {
        SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
        prefEditor.putBoolean(mKeyPrefBilling, true);
        prefEditor.commit();
    }

    private void canceledApp() {
        SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
        prefEditor.putBoolean(mKeyPrefBilling, false);
        prefEditor.commit();
    }
}
