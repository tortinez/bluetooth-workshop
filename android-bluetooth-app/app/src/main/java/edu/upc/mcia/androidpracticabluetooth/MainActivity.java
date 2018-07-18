package edu.upc.mcia.androidpracticabluetooth;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Arrays;

import edu.upc.mcia.androidpracticabluetooth.bluetooth.BluetoothEventHandler;
import edu.upc.mcia.androidpracticabluetooth.bluetooth.ConnectionManager;
import edu.upc.mcia.androidpracticabluetooth.command.BitsCommand;
import edu.upc.mcia.androidpracticabluetooth.command.BytesCommand;
import edu.upc.mcia.androidpracticabluetooth.fragment.BitsFragment;
import edu.upc.mcia.androidpracticabluetooth.fragment.BytesFragment;
import edu.upc.mcia.androidpracticabluetooth.fragment.SectionsPagerAdapter;

public class MainActivity extends AppCompatActivity
        implements ActionBar.TabListener, BluetoothEventHandler.BluetoothEventListener,
        BitsFragment.OnBitsFragmentListener, BytesFragment.OnBytesFragmentListener {

    // Constants
    private final static String TAG = "UI";
    private final static int ENABLE_BLUETOOTH_REQUEST = 30862;

    // Navigation support
    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;
    private int activeTab;

    // Dialogs
    private ProgressDialog progressDialog;
    private AlertDialog alertDialog;

    // Drawables
    private Drawable tickDrawable;
    private Drawable errorDrawable;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private ConnectionManager connectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "-- onCreate --");

        // Create bluetooth connection manager
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectionManager = new ConnectionManager(bluetoothAdapter, this);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(sectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < sectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(actionBar.newTab().setText(sectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
        }

        // Read and scale drawables
        Bitmap tickBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.ic_tick_verd)).getBitmap();
        tickDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(tickBitmap, 75, 75, true));
        Bitmap crossBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.ic_creu_vermella)).getBitmap();
        errorDrawable = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(crossBitmap, 75, 75, true));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings)
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager
        viewPager.setCurrentItem(tab.getPosition());
        activeTab = tab.getPosition();

        // Change reception mode
        switch (activeTab) {
            case SectionsPagerAdapter.TAB_BITS:
                connectionManager.setReceptionMode(ConnectionManager.MODE_BITS);
                break;
            case SectionsPagerAdapter.TAB_BYTES:
                connectionManager.setReceptionMode(ConnectionManager.MODE_BYTES);
                break;
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, android.support.v4.app.FragmentTransaction ft) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "-- onStart --");
        intentaConnectarAmbLaPlaca();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Si resultat de request bluetooth i result_ok
        if (requestCode == ENABLE_BLUETOOTH_REQUEST) {
            if (resultCode == RESULT_OK) {
                intentaConnectarAmbLaPlaca();
            } else {
                Toast.makeText(this, R.string.main_enableBluetooth, Toast.LENGTH_LONG).show();
                finish(); // Engega Bluetooth o tanca app
            }
        }
    }

    private void intentaConnectarAmbLaPlaca() {
        // Comprova si aquest terminal te Bluetooth
        if (bluetoothAdapter == null) {
            String missatgeError = getString(R.string.main_noBluetooth);
            Log.e(TAG, missatgeError);
            Toast.makeText(this, missatgeError, Toast.LENGTH_LONG).show();
            finish(); // tanca app
        } else {
            // Comprova si el Bluetooth esta engegat, sino demana-ho
            if (bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "El Bluetooth esta habilitat!");
                progressDialog = new ProgressDialog(this);
                progressDialog.setTitle("Bluetooth");
                progressDialog.setMessage(getString(R.string.main_bluetoothSearch));
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();
                connectionManager.turnOn();
            } else {
                Log.d(TAG, "Es necessari habilitar el Bluetooth!");
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, ENABLE_BLUETOOTH_REQUEST);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "-- onStop --");
        connectionManager.turnOff();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "-- onDestroy --");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "-- onPause --");
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "-- onResume --");
    }

    @Override
    public void handleBluetoothEvent(Message msg) {
        switch (msg.what) {
            case ConnectionManager.ACTION_SEARCHING_DEVICE:
                progressDialog.setMessage(getString(R.string.main_bluetoothSearch));
                break;
            case ConnectionManager.ACTION_SEARCHING_FAILED:
                Log.e(TAG, getString(R.string.main_bluetoothPair));
                progressDialog.dismiss();
                showErrorDialog();
                break;
            case ConnectionManager.ACTION_CONNECTING:
                String str = getString(R.string.main_connecting);
                progressDialog.setMessage((msg.arg1 > 1) ? str + " (" + getString(R.string.main_intent) + String.format(" %d)", msg.arg1) : str);
                break;
            case ConnectionManager.ACTION_CONNECTED:
                progressDialog.dismiss();
                showSuccessDialog();
                break;
            case ConnectionManager.ACTION_DISCONNECTED:
                intentaConnectarAmbLaPlaca();
                break;
            case ConnectionManager.ACTION_BITS_RECEPTION:
                processBitsCommandFromModule((BitsCommand) msg.obj);
                break;
            case ConnectionManager.ACTION_BYTES_RECEPTION:
                processBytesCommandFromModule((BytesCommand) msg.obj);
                break;
        }
    }

    private void showSuccessDialog() {
        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.main_connected));
        alertDialog.setCancelable(false);
        alertDialog.setIcon(tickDrawable);
        alertDialog.show();
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(2500);
                    alertDialog.dismiss();
                } catch (InterruptedException ie) {
                }
            }
        }.start();
    }

    private void showErrorDialog() {
        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Error Bluetooth");
        alertDialog.setCancelable(false);
        alertDialog.setMessage(getString(R.string.main_bluetoothPair));
        alertDialog.setIcon(errorDrawable);
        alertDialog.show();
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(5000);
                    alertDialog.dismiss();
                    //finish();
                } catch (InterruptedException ie) {
                }
            }
        }.start();
    }

    // ////////////////////////////////////////////////////////////////////////
    //
    // Events of Bits tab
    //
    // ////////////////////////////////////////////////////////////////////////
    private void processBitsCommandFromModule(BitsCommand command) {
        if (activeTab == SectionsPagerAdapter.TAB_BITS) {
            BitsFragment fragment = (BitsFragment) sectionsPagerAdapter.getItem(SectionsPagerAdapter.TAB_BITS);
            fragment.displayReceivedCommand(command);
        }
    }

    @Override
    public void onSendBitsCommand(BitsCommand command) {
        Log.i(TAG, "Comanda: " + command);
        try {
            connectionManager.sendCommand(command);
        } catch (Exception e) {
            Log.e(TAG, "Error enviant comanda: " + e);
            Toast.makeText(this, getString(R.string.main_sendError) + command, Toast.LENGTH_SHORT).show();
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    //
    // Events of Bytes tab
    //
    // ////////////////////////////////////////////////////////////////////////

    private void processBytesCommandFromModule(BytesCommand command) {
        if (activeTab == SectionsPagerAdapter.TAB_BYTES) {
            BytesFragment fragment = (BytesFragment) sectionsPagerAdapter.getItem(SectionsPagerAdapter.TAB_BYTES);
            fragment.displayReceivedCommand(command);
        }
    }

    @Override
    public void onSendBytesCommand(BytesCommand command) {
        Log.i(TAG, "Bytes: " + Arrays.toString(command.array));
        try {
            connectionManager.sendCommand(command);
        } catch (Exception e) {
            Log.e(TAG, "Error enviant comanda: " + e);
            Toast.makeText(this, getString(R.string.main_sendError) + command, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onChangedReceptionLength(int receptionLength) {
        connectionManager.setReceptionLength(receptionLength);
    }

}
