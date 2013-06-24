package es.skastro.arduino.robotics;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileanarchy.android.widgets.joystick.JoystickMovedListener;
import com.mobileanarchy.android.widgets.joystick.JoystickView;

import es.skastro.android.util.bluetooth.BluetoothService;
import es.skastro.android.util.bluetooth.DeviceListActivity;

public class ControlActivity extends Activity {

    MenuItem mnuBluetooth;
    MenuItem mnuBluetoothStatus;
    MenuItem mnuBluetoothConnect;
    final static int CONNECT_BLUETOOTH_SECURE = 100;
    final static int CONNECT_BLUETOOTH_INSECURE = 101;
    BluetoothAdapter mBluetoothAdapter;
    // Member object for the chat services
    private BluetoothService mChatService = null;

    private ImageButton btnHorn, btnLigths;
    public static final String TOAST = "toast";

    TextView txtX, txtY, txtSent, txtReceived;
    JoystickView joystick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        // If the adapter is null, then Bluetooth is not supported
        if ((mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }

        btnHorn = (ImageButton) findViewById(R.id.btnHorn);
        btnHorn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendHorn();
            }
        });

        btnLigths = (ImageButton) findViewById(R.id.btnLigths);
        btnLigths.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSwitchLights();
            }
        });

        txtX = (TextView) findViewById(R.id.TextViewX);
        txtY = (TextView) findViewById(R.id.TextViewY);
        joystick = (JoystickView) findViewById(R.id.joystickView);

        txtSent = (TextView) findViewById(R.id.txtSent);
        txtSent.setText(Html.fromHtml("<b>Last sent:</b> "));

        txtReceived = (TextView) findViewById(R.id.txtReceived);
        txtReceived.setText(Html.fromHtml("<b>Last received:</b> "));

        joystick.setYAxisInverted(false);
        joystick.setMovementRange(127);
        joystick.setOnJostickMovedListener(_listener);

        setStatus(R.string.title_not_connected);
    }

    private JoystickMovedListener _listener = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            txtX.setText(Integer.toString(pan));
            txtY.setText(Integer.toString(tilt));
            sendCoordinates(pan, tilt);
        }

        @Override
        public void OnReleased() {
            txtX.setText("released");
            txtY.setText("released");
            sendCoordinates(0, 0);
        }

        public void OnReturnedToCenter() {
            txtX.setText("stopped");
            txtY.setText("stopped");
            sendCoordinates(0, 0);
        };

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.control, menu);
        mnuBluetooth = menu.findItem(R.id.bluetooth_menu);
        mnuBluetoothStatus = menu.findItem(R.id.bluetooth_status);
        mnuBluetoothConnect = menu.findItem(R.id.bluetooth_connect);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.bluetooth_connect:
            Intent intent0 = new Intent(ControlActivity.this, DeviceListActivity.class);
            startActivityForResult(intent0, CONNECT_BLUETOOTH_SECURE);
            break;
        case R.id.bluetooth_connect_insecure:
            Intent intent1 = new Intent(ControlActivity.this, DeviceListActivity.class);
            startActivityForResult(intent1, CONNECT_BLUETOOTH_INSECURE);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String address;
            switch (requestCode) {
            case CONNECT_BLUETOOTH_SECURE:
                address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                mnuBluetoothStatus.setTitle("Connection status: " + address);
                mnuBluetooth.setIcon(R.drawable.bluetooth_on);
                startConnection(address, true);
                break;
            case CONNECT_BLUETOOTH_INSECURE:
                address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                mnuBluetoothStatus.setTitle("Connection status: " + address);
                mnuBluetooth.setIcon(R.drawable.bluetooth_on);
                startConnection(address, false);
                break;
            }
        }
    }

    private void startConnection(String address, boolean secure) {
        if (mChatService == null)
            mChatService = new BluetoothService(this, mHandler);

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mChatService.connect(device, secure);
    }

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    /* ACTIONS */
    /**
     * Send the speed to the device. Sent values are contained on range [1,255]
     * 
     * @param pan
     *            a value between -127 and 127
     * @param tilt
     *            a value between -127 and 127
     */
    final char commandStart = '^';
    final char commandTerminator = '$';

    public void sendCoordinates(int pan, int tilt) {
        byte[] message = { commandStart, 'S', (byte) (pan + 128), ',', (byte) (tilt + 128), commandTerminator };
        sendMessage(message);
        txtSent.setText(Html.fromHtml("<b>Last sent:</b> CONTROL " + pan + ", " + tilt));
    }

    public void sendHorn() {
        sendMessage(commandStart + "H" + commandTerminator);
        txtSent.setText(Html.fromHtml("<b>Last sent:</b> HORN"));
    }

    public void sendSwitchLights() {
        sendMessage(commandStart + "L" + commandTerminator);
        txtSent.setText(Html.fromHtml("<b>Last sent:</b> SWITCH LIGTHS"));
    }

    /**
     * Sends a message.
     * 
     * @param message
     *            A string of text to send.
     */
    private synchronized void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService == null || mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    private synchronized boolean sendMessage(byte[] array) {
        if (mChatService == null || mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return false;
        }
        mChatService.write(array);
        return true;
    }

    // private synchronized void sendAndWait(byte[] array) {
    // if(sendMessage(array))
    // }

    // The Handler that gets information back from the BluetoothService
    private final Handler mHandler = new Handler() {
        String mConnectedDeviceName = "UNKNOWN";

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothService.MESSAGE_STATE_CHANGE:
                // if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    // mConversationArrayAdapter.clear();
                    break;
                case BluetoothService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case BluetoothService.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                txtSent.setText(Html.fromHtml("<b>Last sent:</b> " + writeMessage));

                // mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case BluetoothService.MESSAGE_READ:
                try {
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    // Toast.makeText(getApplicationContext(), "Received: " + readMessage, Toast.LENGTH_SHORT).show();
                    txtReceived.setText(Html.fromHtml("<b>Last received:</b> " + readMessage));
                    Log.d("ControlActivity", "Bluetooth received: " + readMessage);
                    // mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                } catch (Exception e) {
                }
                break;
            case BluetoothService.MESSAGE_DEVICE_NAME:
                // // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                // Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT)
                // .show();
                break;
            case BluetoothService.MESSAGE_TOAST:
                txtReceived.setText(Html.fromHtml("<b>Last TOAST:</b> " + msg.getData().getString(TOAST)));

                // Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
}