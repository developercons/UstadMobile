package com.ustadmobile.port.android.netwokmanager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.ustadmobile.lib.db.entities.NetworkNode;
import com.ustadmobile.port.sharedse.networkmanager.BleEntryStatusTask;
import com.ustadmobile.port.sharedse.networkmanager.BleMessage;
import com.ustadmobile.port.sharedse.networkmanager.BleMessageUtil;
import com.ustadmobile.port.sharedse.networkmanager.NetworkManagerBle;

import java.util.List;

import static com.ustadmobile.port.sharedse.networkmanager.NetworkManagerBle.DEFAULT_MTU;
import static com.ustadmobile.port.sharedse.networkmanager.NetworkManagerBle.ENTRY_STATUS_REQUEST;

/**
 * This class handles all android specific entry status check from a peer device also,
 * it is responsible for creating BLE GATT client callback.
 *
 * <p>
 * <b>Note: Operation Flow</b>
 * - Once {@link BleEntryStatusTaskAndroid#run()} is called, it creates
 * {@link BleMessageGattClientCallback} and pass the list of entries to be checked
 * and peer device to be checked from. After entry status check
 * {@link BleEntryStatusTask#onResponseReceived} will be called to report back the results.
 *
 *
 * Use {@link BleEntryStatusTaskAndroid#run()} to start executing the task itself,
 * this method will be called in {@link NetworkManagerBle#startMonitoringAvailability}
 * when pending task to be executed is found.
 * </p>
 *
 * @see BleMessageGattClientCallback
 * @see BleEntryStatusTask
 * @see NetworkManagerBle
 *
 *  @author kileha3
 */
public class BleEntryStatusTaskAndroid extends BleEntryStatusTask {

    private BleMessageGattClientCallback mCallback;

    private BluetoothManager bluetoothManager;

    private NetworkNode peerToCheck;

    private Context context;

    /**
     * Constructor to be used when creating platform specific instance of BleEntryStatusTask
     * @param context Platform specific application context.
     * @param entryUidsToCheck List of Id's to be checked for availability from a peer device.
     * @param peerToCheck Peer device for those entries to be checked from.
     */
    public BleEntryStatusTaskAndroid(Context context, List<Long> entryUidsToCheck,
                                     NetworkNode peerToCheck) {
        super(context,entryUidsToCheck,peerToCheck);
        this.context = context;
        this.peerToCheck = peerToCheck;
        byte [] messagePayload = BleMessageUtil.bleMessageLongToBytes(entryUidsToCheck);
        this.message = new BleMessage(ENTRY_STATUS_REQUEST,messagePayload,DEFAULT_MTU);
    }

    /**
     * Set bluetooth manager for BLE GATT communication
     * @param bluetoothManager BluetoothManager instance
     */
    public void setBluetoothManager(BluetoothManager bluetoothManager){
        this.bluetoothManager = bluetoothManager;
    }

    /**
     * Start entry status check task
     */
    @Override
    public void run() {
        mCallback = new BleMessageGattClientCallback(message,peerToCheck.getBluetoothMacAddress());
        mCallback.setOnResponseReceived(this);
        BluetoothDevice destinationPeer =
                bluetoothManager.getAdapter().getRemoteDevice(peerToCheck.getBluetoothMacAddress());
        destinationPeer.connectGatt(context,false,mCallback);
    }


    /**
     * Get BleMessageGattClientCallback instance
     * @return Instance of a BleMessageGattClientCallback
     */
    public BleMessageGattClientCallback getGattClientCallback() {
        return mCallback;
    }


}
