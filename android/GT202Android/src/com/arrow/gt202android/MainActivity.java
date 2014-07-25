package com.arrow.gt202android;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.arrow.gt202android_v370.R;

public class MainActivity extends Activity{

	//private DatagramSocket udpSocket;
	//private byte[] udpData = new byte[16];
	//private DatagramPacket udpPacket = new DatagramPacket(udpData, 16);
	//Thread serverThread = null;

	Thread multicastThread = null;
	MulticastSocket multicastSocket = null;
	DatagramPacket multicastPacket = null;
	DatagramPacket multicastSendPacket = null;
	
	public static final int PACKET_LENGTH = 16;
	private byte[] multicastData = new byte[PACKET_LENGTH];
	MulticastLock multicastLock = null;
	final int MULTICAST_PORT = 5111;
	final String GROUP_IP="224.2.2.2";
	InetAddress group = null;

	private boolean isTestMode = false;
	private GestureDetector gd = null;
	public static final int SERVERPORT = 2222;

	// App State
	private static final int SERVER_SOCKET_ESTABLISHED = 0;
	private static final int SERVER_SOCKET_ESTABLISH_FAILED = 1;

	private static final int UPDATE_TEMPERATURE = 4;
	private static final int UPDATE_HUMIDITY = 5;
	private static final int ACK_TURN_LED_ON = 6;
	private static final int ACK_TURN_LED_OFF = 7;
	private static final int ADD_DEBUG_TEXT = 8;
	private static final int UPDATE_T_H_PERIODICALLY = 9; // AL

	private static final int MULTICAST_SOCKET_ESTABLISHED = 10;
	private static final int MULTICAST_SOCKET_ESTABLISH_FAILED = 11;
	private static final int MULTICAST_SOCKET_CLOSED = 12;
	private static final int CMD_GET_ALL = 13;
	
	Boolean isConnectedToDevice = false;
	String strTargetTemperature;
	String strTargetHumidity;

	//old protocol
	/*
	byte[] cmdGetTemperature = { (byte)0x55, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};
	byte[] cmdGetHumidity = { (byte)0x55, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdTurnOnLED = { (byte)0x55, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdTurnOffLED = { (byte)0x55, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdGetTemperatureAndHumidity = { (byte)0x55, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdSetTemperature = { (byte)0x55, (byte)0x05, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};
	byte[] cmdSetHumidity = { (byte)0x55, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };

	byte[] cmdDVDNumber0 = { (byte)0x55, (byte)0x10, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDNumber1 = { (byte)0x55, (byte)0x11, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDNumber2 = { (byte)0x55, (byte)0x12, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDNumber3 = { (byte)0x55, (byte)0x13, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDNumber4 = { (byte)0x55, (byte)0x14, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDNumber5 = { (byte)0x55, (byte)0x15, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDNumber6 = { (byte)0x55, (byte)0x16, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDNumber7 = { (byte)0x55, (byte)0x17, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDNumber8 = { (byte)0x55, (byte)0x18, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDNumber9 = { (byte)0x55, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDUp = { (byte)0x55, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDDown = { (byte)0x55, (byte)0x21, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDLeft = { (byte)0x55, (byte)0x22, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDRight = { (byte)0x55, (byte)0x23, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDOk = { (byte)0x55, (byte)0x24, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDSet = { (byte)0x55, (byte)0x25, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDBack = { (byte)0x55, (byte)0x26, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDPower = { (byte)0x55, (byte)0x27, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDMute = { (byte)0x55, (byte)0x28 ,(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDPlay = { (byte)0x55, (byte)0x29, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDPause = { (byte)0x55, (byte)0x30, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDStop = { (byte)0x55, (byte)0x31, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDNext = { (byte)0x55, (byte)0x32, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDPrev = { (byte)0x55, (byte)0x33, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDFB = { (byte)0x55, (byte)0x34, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	byte[] cmdDVDFF = { (byte)0x55, (byte)0x35, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	*/

	private static final byte cmdGetTemperature = (byte)0x00;
	private static final byte cmdGetHumidity = (byte)0x01;
	private static final byte cmdTurnOnLED = (byte)0x02;
	private static final byte cmdTurnOffLED = (byte)0x03;
	private static final byte cmdGetAll = (byte)0xFE;
	private static final byte cmdDVDNumber0 = (byte)0x10;
	private static final byte cmdDVDNumber1 = (byte)0x11;
	private static final byte cmdDVDNumber2 = (byte)0x12;
	private static final byte cmdDVDNumber3 = (byte)0x13;
	private static final byte cmdDVDNumber4 = (byte)0x14;
	private static final byte cmdDVDNumber5 = (byte)0x15;
	private static final byte cmdDVDNumber6 = (byte)0x16;
	private static final byte cmdDVDNumber7 = (byte)0x17;
	private static final byte cmdDVDNumber8 = (byte)0x18;
	private static final byte cmdDVDNumber9 = (byte)0x19;
	private static final byte cmdDVDUp = (byte)0x20;
	private static final byte cmdDVDDown = (byte)0x21;
	private static final byte cmdDVDLeft = (byte)0x22;
	private static final byte cmdDVDRight = (byte)0x23;
	private static final byte cmdDVDOk = (byte)0x24;
	private static final byte cmdDVDSet = (byte)0x25;
	private static final byte cmdDVDBack = (byte)0x26;
	private static final byte cmdDVDPower = (byte)0x27;
	private static final byte cmdDVDMute = (byte)0x28;
	private static final byte cmdDVDPlay = (byte)0x29;
	private static final byte cmdDVDPause = (byte)0x30;
	private static final byte cmdDVDStop = (byte)0x31;
	private static final byte cmdDVDNext = (byte)0x32;
	private static final byte cmdDVDPrev = (byte)0x33;
	private static final byte cmdDVDFF = (byte)0x34;
	private static final byte cmdDVDFB = (byte)0x35;
	byte[] MAC_ANY = { (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	
	ProgressDialog pDialog;
	
	TextView debugText;
	
	ImageButton switchBulb;
	TextView txtTemperature;
	TextView txtHumidity;
	ScrollView scrollViewDebug;
	RelativeLayout layout;
	
	AlertDialog dlgTemperature;
	AlertDialog dlgHumidity;
	AlertDialog dlgDVDRemote;

	boolean isLightOn = false;
	
	Spinner macSpinner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		layout = (RelativeLayout) findViewById(R.id.layoutHome);

		debugText = (TextView) findViewById(R.id.textViewDebug);
		switchBulb = (ImageButton) findViewById(R.id.btnLightBulb);
		txtTemperature = (TextView) findViewById(R.id.textViewTemperature);
		txtHumidity = (TextView) findViewById(R.id.textViewHumidity);
		scrollViewDebug = (ScrollView) findViewById(R.id.scrollViewDebug);

		scrollViewDebug.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					scrollViewDebug.fullScroll(View.FOCUS_DOWN);
				}
			}
		});

		debugText.setText("");
		debugText.setClickable(true);
		debugText.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				scrollViewDebug.setVisibility(View.INVISIBLE);
			}
		});
		
		updateMacSpinner(MAC_ANY);
		macSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				itemMacSpinner itemMac = (itemMacSpinner) parent.getItemAtPosition(position);
				try {
					sendCommand(cmdGetAll, itemMac.getMac());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		macSpinner.setOnTouchListener(new Spinner.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(MotionEvent.ACTION_DOWN == event.getAction()) {
                	itemMacSpinner itemMac = (itemMacSpinner) macSpinner.getSelectedItem();
            		try {
            			sendCommand(cmdGetAll, itemMac.getMac());
            		} catch (IOException e) {
            			e.printStackTrace();
            		}
                }
                return false;
            }
        });
	    
		// Init gesture detector
		gd = new GestureDetector(this, new OnDoubleClick());

		strTargetTemperature = txtTemperature.getText().toString();
		strTargetHumidity = txtHumidity.getText().toString();
		
		//this.serverThread = new Thread(new ServerThread());
		//this.serverThread.start();

		this.multicastThread = new Thread(new MulticastThread());
		this.multicastThread.start();

		// Periodically update T & H, ack as heart beat packages
		Timer mTimer = new Timer();        
	 	mTimer.schedule(new TimerTask() {            
			@Override
			public void run() {
				Context context = getApplicationContext();
				ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);  
				android.net.NetworkInfo wifi =connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				if(wifi.isAvailable() == false)
				{
					if (multicastSocket != null) {
						multicastSocket.close();
						multicastSocket = null;
					}
					if (multicastLock != null) {
						multicastLock.release();
						multicastLock = null;
					}
					if (multicastThread != null) {
						multicastThread.interrupt();
						multicastThread = null;
						
						Message message;
						message = mainHandler.obtainMessage(MULTICAST_SOCKET_CLOSED, GROUP_IP + ":" + MULTICAST_PORT);
						mainHandler.sendMessage(message);
					}
				}
				
				if ((wifi.isAvailable() == true) && (wifi.isConnected() == true)) {
					
					if(multicastThread == null) {
						multicastThread = new Thread(new MulticastThread());
						multicastThread.start();
					}
				}
				
				//Message message;
				//message = mainHandler.obtainMessage(CMD_GET_ALL, GROUP_IP + ":" + MULTICAST_PORT);
				//mainHandler.sendMessage(message);

			}
	 	}, 10*1000, 10*1000); 	
	}

	@Override
	public boolean onTouchEvent(MotionEvent event){
		return gd.onTouchEvent(event);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			/*if (serverThread != null) {
				serverThread.interrupt();
			}
			udpSocket.close();*/
			
			if (multicastThread != null) {
				multicastThread.interrupt();
			}
			multicastSocket.close();
			multicastLock.release();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.action_debug_text) {
			if (item.isChecked()) {
				item.setChecked(false);
				scrollViewDebug.setVisibility(View.INVISIBLE);
			} else {
				item.setChecked(true);
				scrollViewDebug.setVisibility(View.VISIBLE);
			}
	    }
		else if (item.getItemId() == R.id.action_get_temp_humid) {
			onClickBtnTemperatureAndHumidity();
		}
     
		return super.onMenuItemSelected(featureId, item);
    }

	public void onClickBtnTemperatureAndHumidity() {
		itemMacSpinner itemMac = (itemMacSpinner) macSpinner.getSelectedItem();
		try {
			sendCommand(cmdGetTemperature, itemMac.getMac());
			sendCommand(cmdGetHumidity, itemMac.getMac());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void onClickBtnGetTemperature(View view) {
		itemMacSpinner itemMac = (itemMacSpinner) macSpinner.getSelectedItem();
		try {
			sendCommand(cmdGetTemperature, itemMac.getMac());
		} catch (IOException e) {
			e.printStackTrace();
		}

		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.dialog_temperature, 
				(ViewGroup) findViewById(R.id.dialog_temperature));
		
		dlgTemperature = new AlertDialog.Builder(this)
   		.setTitle("Set Temperature")
//     	.setPositiveButton("Submit", null)
//     	.setNegativeButton("Cancel", null)
     	.setView(layout)
     	.show();
		
		TextView currentTemp = (TextView) dlgTemperature.findViewById(R.id.txtCurrentTemp);
		currentTemp.setText(txtTemperature.getText());

		TextView targetTemp = (TextView) dlgTemperature.findViewById(R.id.txtTargetTemp);
		targetTemp.setText(strTargetTemperature);
	}

	public void onClickBtnIncTemperature(View view) {
		TextView targetTemp = (TextView) dlgTemperature.findViewById(R.id.txtTargetTemp);
		
		int temperature = Integer.parseInt(targetTemp.getText().toString().replace("ºC", ""));
		if (temperature < 30) {
			temperature++;
			
			strTargetTemperature = Integer.toString(temperature) + "ºC";
			targetTemp.setText(strTargetTemperature);
			setTemperature(temperature);
		}
	}
	
	public void onClickBtnDecTemperature(View view) {
		TextView targetTemp = (TextView) dlgTemperature.findViewById(R.id.txtTargetTemp);

		int temperature = Integer.parseInt(targetTemp.getText().toString().replace("ºC", ""));
		if (temperature > 4) {
			temperature--;

			strTargetTemperature = Integer.toString(temperature) + "ºC";
			targetTemp.setText(strTargetTemperature);
			setTemperature(temperature);
		}
	}

	public void setTemperature(int temp) {
		/*
		try {
			byte[] cmd = cmdSetTemperature;
			cmd[4] = (byte)(temp & 0xFF);
			cmd[5] = (byte)((temp >> 8) & 0xFF);
					
			sendCommand(cmd);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}
		
	public void onClickBtnGetHumidity(View view) {
		itemMacSpinner itemMac = (itemMacSpinner) macSpinner.getSelectedItem();
		try {
			sendCommand(cmdGetHumidity, itemMac.getMac());
		} catch (IOException e) {
			e.printStackTrace();
		}

		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.dialog_humidity, 
				(ViewGroup) findViewById(R.id.dialog_humidity));
		
		dlgHumidity = new AlertDialog.Builder(this)
   		.setTitle("Set Humidity")
//     	.setPositiveButton("Submit", null)
//     	.setNegativeButton("Cancel", null)
     	.setView(layout)
     	.show();
		
		TextView currentHumidity = (TextView) dlgHumidity.findViewById(R.id.txtCurrentHumidity);
		currentHumidity.setText(txtHumidity.getText());

		TextView targetHumidity = (TextView) dlgHumidity.findViewById(R.id.txtTargetHumidity);
		targetHumidity.setText(strTargetHumidity);
	}

	public void onClickBtnIncHumidity(View view) {
		TextView targetHumidity = (TextView) dlgHumidity.findViewById(R.id.txtTargetHumidity);
		
		int humidity = Integer.parseInt(targetHumidity.getText().toString().replace("%", ""));
		if (humidity < 100) {
			humidity++;
			
			strTargetHumidity = Integer.toString(humidity) + "%";
			targetHumidity.setText(strTargetHumidity);
			setHumidity(humidity);
		}
	}
	
	public void onClickBtnDecHumidity(View view) {
		TextView targetHumidity = (TextView) dlgHumidity.findViewById(R.id.txtTargetHumidity);

		int humidity = Integer.parseInt(targetHumidity.getText().toString().replace("%", ""));
		if (humidity > 0) {
			humidity--;

			strTargetHumidity = Integer.toString(humidity) + "%";
			targetHumidity.setText(strTargetHumidity);
			setHumidity(humidity);
		}
	}

	public void setHumidity(int humidity) {
		/*
		try {
			byte[] cmd = cmdSetHumidity;
			cmd[4] = (byte)(humidity & 0xFF);
			cmd[5] = (byte)((humidity >> 8) & 0xFF);
					
			sendCommand(cmd);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}
		
	public void onClickSwitchLightBulb(View view) {
		if (isTestMode) {
			byte[] debug = { (byte) 0xAA, (byte) 0x04, (byte) 0x53,
					(byte) 0x00, (byte) 0x1B, (byte) 0x00, (byte) 0x1C };
//			byte[] debug = { (byte) 0xAA, (byte) 0x01, (byte) 0x53,
//					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFE };
			try {
				sendBytes(debug);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			return;
		}
		
		itemMacSpinner itemMac = (itemMacSpinner) macSpinner.getSelectedItem();
		
		if (!isLightOn) {
			try {
				sendCommand(cmdTurnOnLED, itemMac.getMac());
				// switchBulb.setImageResource(R.drawable.lightbulb_on);
				// layout.setBackgroundResource(R.drawable.background);
				// isLightOn = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				sendCommand(cmdTurnOffLED, itemMac.getMac());
				// switchBulb.setImageResource(R.drawable.lightbulb_off);
				// layout.setBackgroundResource(R.drawable.background_dim);
				// isLightOn = false;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		
	}

	public void onClickBtnDVDRemote(View view) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.dialog_dvd, 
				(ViewGroup) findViewById(R.id.dialog_dvd));
		
		dlgDVDRemote = new AlertDialog.Builder(this)
   		.setTitle("DVD")
//     	.setPositiveButton("Submit", null)
//     	.setNegativeButton("Cancel", null)
     	.setView(layout)
     	.show();
	}
	
	public void onClickBtnDVDCommand(View view) {
		int id = view.getId();
		
		itemMacSpinner itemMac = (itemMacSpinner) macSpinner.getSelectedItem();
		try {
			switch (id) {
			case R.id.dvd_power:
				sendCommand(cmdDVDPower, itemMac.getMac());
				break;
			case R.id.dvd_mute:
				sendCommand(cmdDVDMute, itemMac.getMac());
				break;
			case R.id.dvd_one:
				sendCommand(cmdDVDNumber1, itemMac.getMac());
				break;
			case R.id.dvd_two:
				sendCommand(cmdDVDNumber2, itemMac.getMac());
				break;
			case R.id.dvd_three:
				sendCommand(cmdDVDNumber3, itemMac.getMac());
				break;
			case R.id.dvd_four:
				sendCommand(cmdDVDNumber4, itemMac.getMac());
				break;
			case R.id.dvd_five:
				sendCommand(cmdDVDNumber5, itemMac.getMac());
				break;
			case R.id.dvd_six:
				sendCommand(cmdDVDNumber6, itemMac.getMac());
				break;
			case R.id.dvd_seven:
				sendCommand(cmdDVDNumber7, itemMac.getMac());
				break;
			case R.id.dvd_eight:
				sendCommand(cmdDVDNumber8, itemMac.getMac());
				break;
			case R.id.dvd_nine:
				sendCommand(cmdDVDNumber9, itemMac.getMac());
				break;
			case R.id.dvd_zero:
				sendCommand(cmdDVDNumber0, itemMac.getMac());
				break;
			case R.id.dvd_ok:
				sendCommand(cmdDVDOk, itemMac.getMac());
				break;				
			case R.id.dvd_set:
				sendCommand(cmdDVDSet, itemMac.getMac());
				break;
			case R.id.dvd_back:
				sendCommand(cmdDVDBack, itemMac.getMac());
				break;
			case R.id.dvd_up:
				sendCommand(cmdDVDUp, itemMac.getMac());
				break;
			case R.id.dvd_down:
				sendCommand(cmdDVDDown, itemMac.getMac());
				break;
			case R.id.dvd_left:
				sendCommand(cmdDVDLeft, itemMac.getMac());
				break;
			case R.id.dvd_right:
				sendCommand(cmdDVDRight, itemMac.getMac());
				break;
			case R.id.dvd_play:
				sendCommand(cmdDVDPlay, itemMac.getMac());
				break;
			case R.id.dvd_pause:
				sendCommand(cmdDVDPause, itemMac.getMac());
				break;
			case R.id.dvd_stop:
				sendCommand(cmdDVDStop, itemMac.getMac());
				break;
			case R.id.dvd_ff:
				sendCommand(cmdDVDFF, itemMac.getMac());
				break;
			case R.id.dvd_fb:
				sendCommand(cmdDVDFB, itemMac.getMac());
				break;
			case R.id.dvd_prev:
				sendCommand(cmdDVDPrev, itemMac.getMac());
				break;
			case R.id.dvd_next:
				sendCommand(cmdDVDNext, itemMac.getMac());
				break;
			default:
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onClicktvDebug(View view) {
		scrollViewDebug.setVisibility(View.INVISIBLE);
	}

	public void sendCommand(byte cmdType, byte[] macAddress) throws IOException {

		byte[] cmd = new byte[PACKET_LENGTH];
		
		// Build the cmd packet according to protocol KL26+GT202_APP_Protocol
		cmd[0] = (byte)0x55;
		System.arraycopy(macAddress, 0, cmd, 1, 6);
		cmd[7] = cmdType;

		// Add CRC byte
		int crc = 0;
		for (int i = 0; i < (PACKET_LENGTH-1); i++) {
			crc += (cmd[i] & 0xFF);
		}
		cmd[PACKET_LENGTH-1] = (byte)((crc % 256) & 0xFF); 
		
		sendBytes(cmd);
	}
	
	public void sendBytes(byte[] myByteArray) throws IOException {
		sendBytes(myByteArray, 0, myByteArray.length);
	}

	public void sendBytes(byte[] myByteArray, int start, int len)
			throws IOException {
		if (len < 0)
			throw new IllegalArgumentException("Negative length not allowed");
		if (start < 0 || start >= myByteArray.length)
			throw new IndexOutOfBoundsException("Out of bounds: " + start);

		/*if(isConnectedToDevice == true)
		{
			int device_port = 2222;
			InetAddress device_address = null;
			DatagramSocket s = null;  
			try {  
				device_address = InetAddress.getByName(udpPacket.getAddress().getHostAddress());
				s = new DatagramSocket();  
			} catch (SocketException e) {  
				e.printStackTrace();  
			}  
			DatagramPacket p = new DatagramPacket(myByteArray, len, device_address, device_port);  
			try {  
				s.send(p);  
			} catch (IOException e) {  
				e.printStackTrace();  
			} 
		}*/
		
			
		if(multicastSocket != null) {
			multicastSendPacket = new DatagramPacket(myByteArray, len, group, MULTICAST_PORT);
			new SendThread().start();
		}
	}
	
	public class SendThread extends Thread {

		public void run(){
			try {
				multicastSocket.send(multicastSendPacket);
				
				Message message;
				message = mainHandler.obtainMessage(ADD_DEBUG_TEXT,
						"Data Sent: " + byteToHexString(multicastSendPacket.getData()));
				mainHandler.sendMessage(message);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	class OnDoubleClick extends GestureDetector.SimpleOnGestureListener{
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			scrollViewDebug.setVisibility(View.VISIBLE);
			return true;
		}
	}

	class MulticastThread implements Runnable {
		/*public void run() {
			MulticastSocket multicastSocket = null;
			DatagramPacket dataPacket = null;
			final int MULTICAST_PORT = 5111;
			final String GROUP_IP="224.2.2.2";
			final int MAX_DATA_PACKET_LENGTH = 68;
			byte[] cmd = new byte[MAX_DATA_PACKET_LENGTH];
			byte[] receiveData = new byte[256];
			DatagramPacket receiveDataPacket = null;
			
			MulticastLock multicastLock = null;

			try 
			{
				WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);

				//获取组播锁
				multicastLock = wifiMgr.createMulticastLock("multicast.test");
				multicastLock.acquire();
				
				multicastSocket = new MulticastSocket(MULTICAST_PORT);
				multicastSocket.setLoopbackMode(true);
				InetAddress group = InetAddress.getByName(GROUP_IP);
				multicastSocket.joinGroup(group);

				//start build packet
				cmd[0] = (byte)0x55;

				String ssid = "ioe_wifi";
				cmd[1] = (byte) ssid.getBytes().length;
				System.arraycopy(ssid.getBytes(), 0, cmd, 2, ssid.getBytes().length);
				
				String key = "654321";
				cmd[2 + ssid.getBytes().length] = (byte) key.getBytes().length;
				System.arraycopy(key.getBytes(), 0, cmd, 2 + ssid.getBytes().length + 1, key.getBytes().length);
				
				int crc = 0;
				for (int i = 0; i < cmd.length; i++) {
					crc += (cmd[i] & 0xFF);
				}
				
				crc = crc % 256;
				
				byte[] cmd_with_crc = new byte[cmd.length + 1];
				System.arraycopy(cmd, 0, cmd_with_crc, 0, cmd.length);
				cmd_with_crc[cmd.length] = (byte)(crc & 0xFF); 
				
				dataPacket = new DatagramPacket(cmd_with_crc, cmd_with_crc.length, group, MULTICAST_PORT);
				//end build packet
				
				receiveDataPacket = new DatagramPacket(receiveData, receiveData.length, group, MULTICAST_PORT);

				Message message;
				message = mainHandler.obtainMessage(MULTICAST_SOCKET_ESTABLISHED, GROUP_IP + ":" + MULTICAST_PORT);
				mainHandler.sendMessage(message);
			}
			catch (IOException e) {
				e.printStackTrace();
				
				Message message;
				message = mainHandler.obtainMessage(MULTICAST_SOCKET_ESTABLISH_FAILED, GROUP_IP + ":" + MULTICAST_PORT);
				mainHandler.sendMessage(message);
			}

			while ((!Thread.currentThread().isInterrupted()) && (multicastSocket != null)) {
			
				try {     
					multicastSocket.send(dataPacket);
					Thread.sleep(500);
					
					multicastSocket.receive(receiveDataPacket);
					// TODO
					Message message;
					message = mainHandler.obtainMessage(ADD_DEBUG_TEXT, "Get multicast packet from " + receiveDataPacket.getAddress().getHostAddress());
					mainHandler.sendMessage(message);
					
				} catch(Exception e) {
					e.printStackTrace();
				}    
			}
			multicastSocket.close();
			multicastLock.release();
		}*/
		
		public void run() {

			prepareMulticast();
			
			while ((!Thread.currentThread().isInterrupted()) && (multicastSocket != null)) {
			
				try {     
					multicastSocket.receive(multicastPacket);

					/*
					Message message;
					message = mainHandler.obtainMessage(ADD_DEBUG_TEXT, "Get multicast packet from " + multicastPacket.getAddress().getHostAddress());
					mainHandler.sendMessage(message);
					*/
					multicastPacketProcess(multicastPacket);
					
					Thread.sleep(100);

				} catch(Exception e) {
					e.printStackTrace();
				}    
			}
		}
	}
		
	public void prepareMulticast() {

		try 
		{
			WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);

			//获取组播锁
			multicastLock = wifiMgr.createMulticastLock("multicast.test");
			multicastLock.acquire();
			
			multicastSocket = new MulticastSocket(MULTICAST_PORT);
			multicastSocket.setLoopbackMode(true);
			group = InetAddress.getByName(GROUP_IP);
			multicastSocket.joinGroup(group);
			multicastPacket = new DatagramPacket(multicastData, multicastData.length, group, MULTICAST_PORT);

			Message message;
			message = mainHandler.obtainMessage(MULTICAST_SOCKET_ESTABLISHED, GROUP_IP + ":" + MULTICAST_PORT);
			mainHandler.sendMessage(message);
		}
		catch (IOException e) {
			e.printStackTrace();
			
			Message message;
			message = mainHandler.obtainMessage(MULTICAST_SOCKET_ESTABLISH_FAILED, GROUP_IP + ":" + MULTICAST_PORT);
			mainHandler.sendMessage(message);
		}
	}
	
	/*class ServerThread implements Runnable {
		public void run() {
			String ipAddress = "";

			try {
				WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
				WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
				ipAddress = intToIp(wifiInfo.getIpAddress());

				udpSocket = new DatagramSocket(SERVERPORT);

				String connectedAddr = ipAddress + ":" + SERVERPORT;
				Message message;
				message = mainHandler.obtainMessage(SERVER_SOCKET_ESTABLISHED,
						connectedAddr);
				mainHandler.sendMessage(message);
			} catch (IOException e) {
				e.printStackTrace();

				String connectedAddr = ipAddress + ":" + SERVERPORT;
				Message message;
				message = mainHandler.obtainMessage(
						SERVER_SOCKET_ESTABLISH_FAILED, connectedAddr);
				mainHandler.sendMessage(message);
			}

			while (!Thread.currentThread().isInterrupted()) {
				try {
					udpSocket.receive(udpPacket);
					isConnectedToDevice = true;

					Message message;
					message = mainHandler.obtainMessage(ADD_DEBUG_TEXT, "Client connected!");
					mainHandler.sendMessage(message);						
					udpPacketProcess(udpPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}*/

	public void multicastPacketProcess(DatagramPacket multicastPacket) {

		try 
		{
			if(multicastPacket.getLength() != PACKET_LENGTH)
			{
				Message message;
				
				message = mainHandler.obtainMessage(ADD_DEBUG_TEXT,
						"multicastPacket length is not 16 but " + multicastPacket.getLength() + ", packet: " + byteToHexString(multicastData));
				mainHandler.sendMessage(message);
				return;
			}
				
			
			if ((multicastData[0] & 0xFF) != 0xAA) {
				
				String preamble = "0x"
						+ Integer.toHexString(multicastData[0] & 0xFF);

				Message message;
				message = mainHandler.obtainMessage(ADD_DEBUG_TEXT,
						"Invalid Packet received, started with: "
								+ preamble);
				mainHandler.sendMessage(message);
				
				throw new IOException("Invalid packet");
			}

			// validate CRC byte
			int crc = 0;
			for (int i = 0; i < (PACKET_LENGTH-1); i++) {
				crc += (multicastData[i] & 0xFF);
			}
			if (crc % 256 != (multicastData[PACKET_LENGTH-1] & 0xFF)) {
				Message message;
				message = mainHandler.obtainMessage(ADD_DEBUG_TEXT,
						"Invalid Packet received, CRC error: "
								+ byteToHexString(multicastData));
				mainHandler.sendMessage(message);

				throw new IOException("CRC error");
			}

			// CRC validated, complete 16-byte command sequence
			{
				Message message;
				message = mainHandler.obtainMessage(ADD_DEBUG_TEXT,
						"Data received: " + byteToHexString(multicastData));
				mainHandler.sendMessage(message);
			}

			ByteBuffer cmd = ByteBuffer.wrap(multicastData);

			// Drop the first 0xAA
			cmd.get();
			
			// Get Mac address
			byte[] macAddress = new byte[6];
			for(int j = 0; j < 6; j++) {
				macAddress[j] = cmd.get();
			}
			appendMacSpinner(macAddress);

			// Get command type
			byte cmdType = cmd.get();
			switch (cmdType) {
			case cmdGetTemperature: {
				// Drop the LED status(one byte)
				cmd.get();
				// Get Temperature (two bytes)
				cmd.order(ByteOrder.LITTLE_ENDIAN);
				int temperature = cmd.getShort();
				cmd.order(ByteOrder.BIG_ENDIAN);

				Message message;
				message = mainHandler.obtainMessage(UPDATE_TEMPERATURE,
						Integer.toString(temperature));
				mainHandler.sendMessage(message);

				break;
			}
			case cmdGetHumidity: {
				// Drop the LED status(one byte)
				cmd.get();
				// Drop the Temperature value(two bytes)
				cmd.getShort();
				// Get Humidity (two bytes)
				cmd.order(ByteOrder.LITTLE_ENDIAN);
				int humidity = cmd.getShort();
				cmd.order(ByteOrder.BIG_ENDIAN);
				
				Message message;
				message = mainHandler.obtainMessage(UPDATE_HUMIDITY,
						Integer.toString(humidity));
				mainHandler.sendMessage(message);

				break;
			}
			case cmdTurnOnLED: {
				Message message;
				message = mainHandler.obtainMessage(
						ACK_TURN_LED_ON, null);
				mainHandler.sendMessage(message);
				break;
			}
			case cmdTurnOffLED: {
				Message message;
				message = mainHandler.obtainMessage(
						ACK_TURN_LED_OFF, null);
				mainHandler.sendMessage(message);
				break;
			}
			case cmdGetAll: {
				byte ledStatus = cmd.get();

				cmd.order(ByteOrder.LITTLE_ENDIAN);
				int temperature = cmd.getShort();
				cmd.order(ByteOrder.BIG_ENDIAN);

				cmd.order(ByteOrder.LITTLE_ENDIAN);
				int humidity = cmd.getShort();
				cmd.order(ByteOrder.BIG_ENDIAN);

				Message message;
				message = mainHandler.obtainMessage(UPDATE_TEMPERATURE,
						Integer.toString(temperature));
				mainHandler.sendMessage(message);

				message = mainHandler.obtainMessage(UPDATE_HUMIDITY,
						Integer.toString(humidity));
				mainHandler.sendMessage(message);
				
				if(ledStatus == 0x01){
					message = mainHandler.obtainMessage(
							ACK_TURN_LED_OFF, null);
					mainHandler.sendMessage(message);
				}
				else{
					message = mainHandler.obtainMessage(
							ACK_TURN_LED_ON, null);
					mainHandler.sendMessage(message);
				}
				break;
			}
			default:
				break;
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	/*public void udpPacketProcess(DatagramPacket udpPacket) {

		try 
		{
			if(udpPacket.getLength() == 0)
			{
				Message message;
				
				message = mainHandler.obtainMessage(ADD_DEBUG_TEXT,
						"Client disconnected!"+udpPacket.getAddress().getHostAddress());
				mainHandler.sendMessage(message);
			}
				
			
			if ((udpData[0] & 0xFF) != 0xAA) {
				String preamble = "0x"
						+ Integer.toHexString(udpData[0] & 0xFF);

				Message message;
				message = mainHandler.obtainMessage(ADD_DEBUG_TEXT,
						"Invalid Packet received, started with: "
								+ preamble);
				mainHandler.sendMessage(message);

				throw new IOException("Invalid packet");
			}

			// validate CRC byte
			int crc = 0;
			for (int i = 0; i < 6; i++) {
				crc += (udpData[i] & 0xFF);
			}
			if (crc % 256 != (udpData[6] & 0xFF)) {
				Message message;
				message = mainHandler.obtainMessage(ADD_DEBUG_TEXT,
						"Invalid Packet received, CRC error: "
								+ byteToHexString(udpData));
				mainHandler.sendMessage(message);

				throw new IOException("CRC error");
			}

			// CRC validated, complete 7-byte command sequence
			{
				Message message;
				message = mainHandler.obtainMessage(ADD_DEBUG_TEXT,
						"Data received: " + byteToHexString(udpData));
				mainHandler.sendMessage(message);
			}

			ByteBuffer cmd = ByteBuffer.wrap(udpData);

			// Drop the first 0xAA
			cmd.get();

			byte cmdType = cmd.get();
			switch (cmdType) {
			case 0x00: {
				// Get Temperature
				cmd.order(ByteOrder.LITTLE_ENDIAN);
				int temperature = cmd.getInt();
				cmd.order(ByteOrder.BIG_ENDIAN);

				Message message;
				message = mainHandler.obtainMessage(UPDATE_TEMPERATURE,
						Integer.toString(temperature));
				mainHandler.sendMessage(message);

				break;
			}
			case 0x01: {
				// Get Humidity
				cmd.order(ByteOrder.LITTLE_ENDIAN);
				int humidity = cmd.getInt();
				cmd.order(ByteOrder.BIG_ENDIAN);
				
				Message message;
				message = mainHandler.obtainMessage(UPDATE_HUMIDITY,
						Integer.toString(humidity));
				mainHandler.sendMessage(message);

				break;
			}
			case 0x02: {
				// ACK - Turn On LED
				int ack = cmd.getInt();

				if (ack == 0x00) {
					Message message;
					message = mainHandler.obtainMessage(
							ACK_TURN_LED_ON, null);
					mainHandler.sendMessage(message);
				}

				break;
			}
			case 0x03: {
				// ACK - Turn Off LED
				int ack = cmd.getInt();

				if (ack == 0x00) {
					Message message;
					message = mainHandler.obtainMessage(
							ACK_TURN_LED_OFF, null);
					mainHandler.sendMessage(message);
				}

				break;
			}
			case 0x04: {
				// Get Humidity & Temperature
				cmd.order(ByteOrder.LITTLE_ENDIAN);
				int humidity = cmd.getShort();
				int temperature = cmd.getShort();
				cmd.order(ByteOrder.BIG_ENDIAN);

				Message message;
				message = mainHandler.obtainMessage(UPDATE_TEMPERATURE,
						Integer.toString(temperature));
				mainHandler.sendMessage(message);

				message = mainHandler.obtainMessage(UPDATE_HUMIDITY,
						Integer.toString(humidity));
				mainHandler.sendMessage(message);

				break;
			}
			case 0x05: {
				// Set Temperature
				cmd.order(ByteOrder.LITTLE_ENDIAN);
				int dummy = cmd.getShort();
				int temperature = cmd.getShort();
				cmd.order(ByteOrder.BIG_ENDIAN);

				break;
			}
			case 0x06: {
				// Set Humidity
				cmd.order(ByteOrder.LITTLE_ENDIAN);
				int dummy = cmd.getShort();
				int humidity = cmd.getShort();
				cmd.order(ByteOrder.BIG_ENDIAN);

				break;
			}
			default:
				break;
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}*/

	private String byteToHexString(byte[] data) {
		String hex = "";
		for (int i = 0; i < data.length; i++) {
			hex += "0x";
			if (data[i] >= 0 && data[i] <= 9)
				hex += "0";
			hex += Integer.toHexString(data[i] & 0xFF) + " ";

		}

		return hex;
	}
	
	private String byteToHexStringWithout0x(byte[] data) {
		String hex = "";
		for (int i = 0; i < data.length; i++) {
			if (data[i] >= 0 && data[i] <= 9)
				hex += "0";
			hex += Integer.toHexString(data[i] & 0xFF) + " ";
		}
		return hex;
	}

	private Handler mainHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			
			String oldText = debugText.getText().toString();
			if (oldText.length() > 1000) {
				oldText = "";
			}

			int what = msg.what;
			switch (what) {
			case SERVER_SOCKET_ESTABLISHED: {
				String addr = (String) msg.obj;
				debugText.setText(oldText
						+ "Socket server started at " + addr + "\n" + "Waiting devices connect.\r\n");
				scrollViewDebug.fullScroll(View.FOCUS_DOWN);
			}
				break;
			case SERVER_SOCKET_ESTABLISH_FAILED: {
				String addr = (String) msg.obj;
				debugText.setText(oldText
						+ "Failed to established server socket started at"
						+ addr + "\n");
				scrollViewDebug.fullScroll(View.FOCUS_DOWN);
			}
				break;
			case UPDATE_T_H_PERIODICALLY:
				onClickBtnTemperatureAndHumidity();
				break;
			case UPDATE_TEMPERATURE:
				String temperature = (String) msg.obj + "ºC";
				debugText.setText(oldText
						+ "Updated Temperature to " + temperature + "\n");
				scrollViewDebug.fullScroll(View.FOCUS_DOWN);

				txtTemperature.setText(temperature);
				Toast.makeText(getApplicationContext(),
						"Updated Temperature to " + temperature,
						Toast.LENGTH_SHORT).show();
				break;
			case UPDATE_HUMIDITY:
				String humidity = (String) msg.obj + "%";
				debugText.setText(oldText
						+ "Updated Humidity to " + humidity + "\n");
				scrollViewDebug.fullScroll(View.FOCUS_DOWN);

				txtHumidity.setText(humidity);
				Toast.makeText(getApplicationContext(),
						"Updated Humidity to " + humidity, Toast.LENGTH_SHORT)
						.show();
				break;
			case ACK_TURN_LED_ON:
				debugText.setText(oldText
						+ "ACK: LED Turned on!\n");
				scrollViewDebug.fullScroll(View.FOCUS_DOWN);

				switchBulb.setImageResource(R.drawable.lightbulb_on);
				layout.setBackgroundResource(R.drawable.background);

				isLightOn = true;

				break;
			case ACK_TURN_LED_OFF:
				debugText.setText(oldText
						+ "ACK: LED Turned off!\n");
				scrollViewDebug.fullScroll(View.FOCUS_DOWN);

				switchBulb.setImageResource(R.drawable.lightbulb_off);
				layout.setBackgroundResource(R.drawable.background_dim);

				isLightOn = false;

				break;
			case ADD_DEBUG_TEXT:
				String text = (String) msg.obj + "\n";
				debugText.setText(oldText + text);
				scrollViewDebug.fullScroll(View.FOCUS_DOWN);

				break;
			case MULTICAST_SOCKET_ESTABLISHED:
				{
					String addr = (String) msg.obj;
					debugText.setText(oldText
							+ "Socket multicast started at " + addr + "\r\n");
					scrollViewDebug.fullScroll(View.FOCUS_DOWN);
				}
				break;
			case MULTICAST_SOCKET_ESTABLISH_FAILED:
				{
					String addr = (String) msg.obj;
					debugText.setText(oldText
							+ "Failed to established multicast socket at "
							+ addr + "\n");
					scrollViewDebug.fullScroll(View.FOCUS_DOWN);
				}
				break;
			case MULTICAST_SOCKET_CLOSED:
			{
				String addr = (String) msg.obj;
				debugText.setText(oldText
						+ "Closed multicast socket at "
						+ addr + "\n");
				scrollViewDebug.fullScroll(View.FOCUS_DOWN);
			}			
			break;
			case CMD_GET_ALL:
			{
				itemMacSpinner itemMac = (itemMacSpinner) macSpinner.getSelectedItem();
				try {
					sendCommand(cmdGetAll, itemMac.getMac());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			break;
			default:
				break;
			}
		}
	};

	public static String getLocalIpAddressString() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("IPADDRESS", ex.toString());
		}

		return null;
	}
	
	public static String intToIp(int i) {      
		return (i & 0xFF ) + "." + ((i >> 8 ) & 0xFF) + "." + ((i >> 16 ) & 0xFF) + "." + ( i >> 24 & 0xFF);
	} 
	
	public class itemMacSpinner {
		private byte[] mac = new byte[6];
		private String strMac;
		public itemMacSpinner(byte[] macAddress) {
			super();
			for(int i = 0; i < 6; i++) {
				mac[i] = macAddress[i];
			}
			
			strMac = byteToHexStringWithout0x(mac);
		}
		public byte[] getMac() {
			return mac;
		}
		public void setMac(byte[] macAddress) {
			for(int i = 0; i < 6; i++) {
				mac[i] = macAddress[i];
			}
		}
		public String getStrMac() {
			return strMac;
		}
	}
	
	public class adapterMacSpinner extends BaseAdapter {
		private List<itemMacSpinner> mList;
		private Context mContext;

		public adapterMacSpinner(Context pContext, List<itemMacSpinner> pList) {
			this.mContext = pContext;
			this.mList = pList;
		}

		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public Object getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater _LayoutInflater = LayoutInflater.from(mContext);
			convertView = _LayoutInflater.inflate(R.layout.item_mac_spinner, null);
			if(convertView != null)
			{
				TextView itemMac=(TextView)convertView.findViewById(R.id.itemMac);
				itemMac.setText(mList.get(position).getStrMac());
			}
			return convertView;
		}
	}
	
	public void updateMacSpinner(byte[] macAddress) {
		macSpinner = (Spinner) findViewById(R.id.spinnerMac);
		List<itemMacSpinner> listMacSpinner = new ArrayList<itemMacSpinner>();
		listMacSpinner.add(new itemMacSpinner(macAddress));
		adapterMacSpinner adapter = new adapterMacSpinner(this, listMacSpinner);
		macSpinner.setAdapter(adapter);
	}
	
	public void appendMacSpinner(byte[] macAddress) {
		macSpinner = (Spinner) findViewById(R.id.spinnerMac);
		adapterMacSpinner adapter = (adapterMacSpinner) macSpinner.getAdapter();
		int count = adapter.getCount();
		boolean macStored = false;
		for (int i = 0; i < count; i++) {
			itemMacSpinner item = (itemMacSpinner) adapter.getItem(i);
			byte[] mac = item.getMac();
			if(IsSameMacAddress(mac, macAddress) == true) {
				macStored = true;
				break;
			}
		}
		
		if(macStored == false) {
			adapter.mList.add(new itemMacSpinner(macAddress));
		}
	}
	
	public boolean IsSameMacAddress(byte[] mac1, byte[] mac2) {
		for (int j = 0; j < 6; j++) {
			if (mac1[j] != mac2[j]) {
				return false;
			}
		}
		return true;
	}
}
