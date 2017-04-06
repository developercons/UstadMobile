package com.ustadmobile.port.android.p2p;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.util.Log;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.port.sharedse.impl.UstadMobileSystemImplSE;

import edu.rit.se.wifibuddy.FailureReason;
import edu.rit.se.wifibuddy.WifiDirectHandler;


public class P2PServiceAndroid extends Service{


    private final static int NOTIFICATION_CODE =12;
    private WifiDirectHandler wifiDirectHandler;
    private final IBinder mBinder = new LocalServiceBinder();

    /**
     * Fixed length of time to wait if a no prompt network fails
     */
    public static final int NOPROMPT_RETRY_WAIT_INTERVAL = 10000;

    public static final int NOPROMPT_RETRY_MAX_RETRIES = 6;

    private P2PManagerAndroid p2pManager;



    /**
     * Action Listener that will watch for any failure of the no prompt network and recreate it
     * as required - sometimes group creation will failed with the "BUSY" status if the wifi is
     * doing other stuff.
     */
    protected WifiP2pManager.ActionListener mNoPromptActionListener = new WifiP2pManager.ActionListener() {

        private int retryCount;

        @Override
        public void onSuccess() {
            Log.i(WifiDirectHandler.TAG, "P2PServiceAndroid:noPromptActionListener:onSuccess");
            retryCount = 0;
        }

        @Override
        public void onFailure(int i) {
            Log.e(WifiDirectHandler.TAG, "P2PServiceAndroid:noPromptActionListener:onFailure: " +
                    FailureReason.fromInteger(i));
            retryCount++;
            if(retryCount < NOPROMPT_RETRY_MAX_RETRIES) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(WifiDirectHandler.TAG, "P2PServiceAndroid:noPromptActionListener: Retry #"
                                + retryCount);
                        getWifiDirectHandlerAPI().removeGroup(
                                new WifiP2pManager.ActionListener() {
                              @Override
                              public void onSuccess() {
                                  getWifiDirectHandlerAPI().startAddingNoPromptService(P2PManagerAndroid.makeServiceData(),
                                          mNoPromptActionListener);
                              }

                              @Override
                              public void onFailure(int i) {
                                  getWifiDirectHandlerAPI().startAddingNoPromptService(P2PManagerAndroid.makeServiceData(),
                                          mNoPromptActionListener);
                              }
                          });

                    }
                }, NOPROMPT_RETRY_WAIT_INTERVAL);
            }else {
                Log.e(WifiDirectHandler.TAG, "P2PServiceAndroid:noPromptActionListener: Exceeded retry counts!");

                retryCount = 0;
            }
        }
    };


    public P2PServiceAndroid(){

    }



    @Override
    public void onCreate() {
        super.onCreate();
        Intent wifiServiceIntent = new Intent(this, WifiDirectHandler.class);
        bindService(wifiServiceIntent, wifiP2PServiceConnection, BIND_AUTO_CREATE);
    }




    @Override
    public void onDestroy() {
        if(p2pManager != null) {
            p2pManager.onDestroy();
        }

        if(wifiDirectHandler!=null){
            wifiDirectHandler.removeGroup();
            wifiDirectHandler.stopServiceDiscovery();
            wifiDirectHandler.removeService();
            UstadMobileSystemImpl.getInstance().setAppPref("devices","",getApplicationContext());
        }
        unbindService(wifiP2PServiceConnection);


        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;
    }



    ServiceConnection wifiP2PServiceConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            wifiDirectHandler = ((WifiDirectHandler.WifiTesterBinder) iBinder).getService();
            wifiDirectHandler.setStopDiscoveryAfterGroupFormed(false);
            p2pManager = (P2PManagerAndroid) UstadMobileSystemImplSE.getInstanceSE().getP2PManager();
            p2pManager.init(P2PServiceAndroid.this);
        }


        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            wifiDirectHandler = null;
        }
    };



    public class LocalServiceBinder extends Binder {
        public P2PServiceAndroid getService(){
            return P2PServiceAndroid.this;
        }

    }


    public void dismissNotification(){
        stopForeground(true);

    }


    /**
     * show fore ground notification when super node mode is activated
     */
    public void showNotification() {
        Bitmap bitmap=BitmapFactory.decodeResource(getResources(),R.drawable.launcher_icon);
        final Notification notification = new NotificationCompat.Builder(this)
                .setCategory(Notification.CATEGORY_PROMO)
                .setContentTitle("Ustad Mobile")
                .setSmallIcon(R.drawable.launcher_icon)
                .setLargeIcon(bitmap)
                .setContentText(Html.fromHtml("Super node mode is running..."))
                .setColor(getResources().getColor(R.color.primary_dark))
                .setAutoCancel(true)
                .setVisibility(1)
                .setPriority(Notification.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml("Server node running...")))
                .build();

        startForeground(NOTIFICATION_CODE,notification);

    }

    /**
     *
     * @return instance of the WifiDirectHandler API
     */
    public WifiDirectHandler getWifiDirectHandlerAPI(){
        return wifiDirectHandler;
    }

}