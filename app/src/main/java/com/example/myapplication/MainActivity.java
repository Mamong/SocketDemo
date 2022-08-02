package com.example.myapplication;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.socket.ClientPeer;
import com.example.socket.ServerPeer;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;


    private Button mEncryptButton;
    private Button mDecryptButton;
    private EditText mInputEdit;
    private TextView mShowText;

    private Button listenButton;
    private Button connectButton;
    private EditText portInputEdit;
    private EditText addressInputEdit;

    private TextView serverShowText;
    private TextView clientShowText;

    private byte[] mEncryptText;
    private byte[] mKey;

    private ClientPeer client;
    private ServerPeer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        mEncryptButton = (Button)findViewById(R.id.main_encrypt_button);
        mDecryptButton = (Button)findViewById(R.id.main_decrypt_button);
        mInputEdit = (EditText)findViewById(R.id.main_input_edittext);
        mShowText = (TextView)findViewById(R.id.main_show_encrypt_textview);

        mInputEdit.setText("192.168.2.79;17245;1892");

        mEncryptButton.setOnClickListener(this::onClick);
        mDecryptButton.setOnClickListener(this::onClick);

        listenButton = (Button)findViewById(R.id.listen_button);
        connectButton = (Button)findViewById(R.id.connect_button);
        portInputEdit = (EditText)findViewById(R.id.port_edittext);
        addressInputEdit = (EditText)findViewById(R.id.address_textedit);

        serverShowText = (TextView)findViewById(R.id.server_textview);
        clientShowText = (TextView)findViewById(R.id.client_status_textview);

        portInputEdit.setText("6000");
//        addressInputEdit.setText("192.168.5.132:63543");
        addressInputEdit.setText("192.168.5.159:63543");
//        addressInputEdit.setText("192.168.2.150:63543");

        listenButton.setOnClickListener(this::onClick);
        connectButton.setOnClickListener(this::onClick);

        server = new ServerPeer(this);
        client = new ClientPeer(this);
    }

    public void onClick(View v) {
        // TODO Auto-generated method stub

        String portString = portInputEdit.getText().toString();

        int key = v.getId();
        switch (key) {
            case R.id.listen_button: {
                int port = Integer.valueOf(portString);
                server.listenToPort(port);
            }
                break;
            case R.id.connect_button: {
                String addressString = addressInputEdit.getText().toString();
                String ip = addressString.split(":")[0];
                String port = addressString.split(":")[1];
                int port2 = Integer.valueOf(port);
                client.connectTo(ip, port2);
            }
                break;

            case R.id.main_encrypt_button:
                String source = mInputEdit.getText().toString();

//                AESEnc aesEncrypt = new AESEnc();
//
//                try {
//                    mEncryptText = aesEncrypt.encrypt(source.getBytes());
//                    mKey = aesEncrypt.getKey();
//
//                } catch (Exception e) {
//                    // TODO Auto-generated catch block
//
//                    e.printStackTrace();
//                }
//
//                java.math.BigInteger bi = new java.math.BigInteger(mEncryptText);
//                mShowText.setText("encrypt:"+bi.abs().toString(16)+"\n");
                break;

            case R.id.main_decrypt_button:
//                String text = null;
/*
                AESEnc aesDecrypt = new AESEnc(mKey);
                try {
                    text = new String(aesDecrypt.decrypt(mEncryptText));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }*/

//                try {
//                    text = ThreeDESUtil.decrypt("1DE6E5BAD0AF47B794B0D556446E380F39D74A35FED5F22597B5A6C18CC4C141","64DAD50715CE6D6BEC67685801DCF86E435723C44FF72AE9");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

//                mShowText.setText("decrypt:"+text+"\n");
                break;

            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}