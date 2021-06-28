package com.example.starchat.activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.starchat.R;
import com.example.starchat.adapter.DeviceAdapter;
import com.example.starchat.bean.DeviceBean;
import com.example.starchat.broadcastReceiver.WifiBroadcastReceiver;
import com.example.starchat.jiekou.WifiActionListener;
import com.example.starchat.service.ServerService;
import com.example.starchat.util.ToastUtil;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * APP首页，设备发现与连接模块
 * @author 陈
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private boolean p2pEnabled = false;
    private boolean isConnected = false;

    private ListView mDeviceListView;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiBroadcastReceiver mReceiver;
    private final WifiActionListener mActionListener = new WifiActionListener() {
        @Override
        public void wifiP2pEnabled(boolean enabled) {
            p2pEnabled = enabled;
        }

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

            mEditor = mSharedPreferences.edit();

            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                isConnected = true;
                mDeviceRole.setText(getString(R.string.server));
                mEditor.putString(getString(R.string.role), getString(R.string.server));
                mEditor.apply();
                //绑定Service启动服务器
                bindServerService();

            } else if (wifiP2pInfo.groupFormed) {
                isConnected = true;
                mDeviceRole.setText(getString(R.string.client));
                mEditor.putString(getString(R.string.role), getString(R.string.client));
                mEditor.commit();
            }

            //获得groupOwner的地址，即服务器的地址，并存储在SharedPreference中
            InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            String address = groupOwnerAddress.getHostAddress();
            mEditor.putString(getString(R.string.serverIP), address);
            mEditor.commit();

            ToastUtil.showToast(MainActivity.this, "连接成功");
        }

        @Override
        public void onDisconnection() {

            //如果连接断开则与Service解绑
            String role;
            if ((role = mSharedPreferences.getString("role", null)) != null) {
                if (mServiceConnection != null && role.equals("服务器")) {
                    unbindService(mServiceConnection);
                    mServiceConnection = null;
                }
            }

            mDeviceRole.setText(getString(R.string.device_role));
            isConnected = false;
            ToastUtil.showToast(MainActivity.this, "已断开连接");
            discoverPeers();
        }

        @Override
        public void onSelfDeviceAvailable(WifiP2pDevice wifiP2pDevice) {

        }

        @Override
        public void onPeersAvailable(Collection<WifiP2pDevice> wifiP2pDeviceList) {
            // 显示在ListView上
            mDeviceArray.clear();
            mDeviceBeans.clear();

            Iterator iterator = wifiP2pDeviceList.iterator();
            while (iterator.hasNext()) {
                WifiP2pDevice device = (WifiP2pDevice) iterator.next();
                mDeviceArray.add(device);
                DeviceBean deviceBean = new DeviceBean();
                deviceBean.deviceName = device.deviceName;
                //获取设备类型代码
                int deviceType = Integer.parseInt(device.primaryDeviceType.split("-")[0]);
                switch (deviceType) {
                    case 1:
                    case 7:
                        deviceBean.deviceType = R.drawable.monitor;
                        break;
                    case 3:
                        deviceBean.deviceType = R.drawable.printer;
                        break;
                    case 10:
                        deviceBean.deviceType = R.drawable.phone;
                        break;
                    default:
                        deviceBean.deviceType = R.drawable.close;
                        break;
                }

                if (device.status == WifiP2pDevice.AVAILABLE) {
                    deviceBean.deviceStatus = getString(R.string.usable);
                } else if (device.status == WifiP2pDevice.INVITED) {
                    deviceBean.deviceStatus = getString(R.string.connecting);
                } else if (device.status == WifiP2pDevice.CONNECTED) {
                    deviceBean.deviceStatus = getString(R.string.connected);
                    String role = mSharedPreferences.getString(getString(R.string.role), "设备角色");
                    mDeviceRole.setText(role);
                    if (role != null) {
                        if (role.equals("服务器")) {
                            bindServerService();
                        }
                    }
                    isConnected = true;
                } else {
                    deviceBean.deviceStatus = getString(R.string.usable);
                }

                mDeviceBeans.add(deviceBean);
            }
            //设置列表适配器
            mAdapter = new DeviceAdapter(mDeviceBeans, MainActivity.this);
            mDeviceListView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChannelDisconnected() {
            //如果连接断开则与服务解绑
            String role;
            if ((role = mSharedPreferences.getString("role", null)) != null) {
                if (mServiceConnection != null && role.equals("服务器")) {
                    unbindService(mServiceConnection);
                    mServiceConnection = null;
                }
            }

            p2pEnabled = false;
            isConnected = false;
            mDeviceRole.setText(R.string.device_role);

        }
    };

    private Button mBnFind;
    private TextView mDeviceRole;
    private Button mBnInRoom;
    private Button mBnRemoveGroup;

    private DeviceAdapter mAdapter;
    private List<WifiP2pDevice> mDeviceArray = new ArrayList<>();
    private List<DeviceBean> mDeviceBeans = new ArrayList<>();

    //存储设备角色，在应用重新进入时提取内容到界面
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private ServiceConnection mServiceConnection;


    /**
     * onCreate()阶段，主要用于初始化参数
     * @param savedInstanceState 实例状态
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences("starChatData", MODE_PRIVATE);

        //初始化界面
        initView();

        //初始化WifiP2p
        initWifiP2p();

        //配置点击事件
        initEvent();
    }

    /**
     * 初始化首页界面
     */
    private void initView() {
        getWindow().setStatusBarColor(getResources().getColor(R.color.Tiffany));
        mDeviceListView = findViewById(R.id.deviceList);
        mBnFind = findViewById(R.id.bn_find);
        mDeviceRole = findViewById(R.id.text_role);
        mBnInRoom = findViewById(R.id.bn_in_room);
        mBnRemoveGroup = findViewById(R.id.bn_remove_group);

    }

    /**
     * 初始化WifiP2p
     */
    private void initWifiP2p() {
        //设置WiFi P2P
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WifiBroadcastReceiver(mManager, mChannel, mActionListener);
    }

    /**
     * 初始化组件事件
     */
    private void initEvent() {

        //满足条件进入聊天室
        mBnInRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mConnectedState.getText().toString().equals("已连接")
                if (isConnected) {
                    Intent intent = new Intent(MainActivity.this, ChatRoomActivity.class);
                    startActivity(intent);
                } else {
                    ToastUtil.showToast(MainActivity.this, "未连接到设备，无法进入");
                }
            }
        });

        //退出群组
        mBnRemoveGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        //点击设备列表项，1.若可用则连接；2.若连接中则可取消连接；3.若已连接则断开连接
        mDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                final WifiP2pDevice device = mDeviceArray.get(position);
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                if (device.status == WifiP2pDevice.AVAILABLE) {
                    if (isConnected && mSharedPreferences.getString(getString(R.string.role), null).equals(getString(R.string.client))) {
                        ToastUtil.showToast(MainActivity.this, "客户端只能连接到一个设备，请断开现在的连接");
                        return;
                    }
                    //可连接则连接
                    connect(config);
                } else if (device.status == WifiP2pDevice.INVITED) {
                    //连接中可取消连接
                    cancelConnect();

                } else if (device.status == WifiP2pDevice.CONNECTED) {
                    //已连接可断开连接
                    disconnect();
                }
            }
        });

        //点击按钮发现设备
        mBnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverPeers();
            }
        });
    }

    /**
     * 服务器绑定ServerService
     */
    public void bindServerService() {
        Intent intent = new Intent(MainActivity.this, ServerService.class);
        if (mServiceConnection == null) {
            Log.d(TAG, "onPeersAvailable: ok");
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {

                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
            };
        }
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 使用WifiP2pManager的discoverPeers()方法发现设备
     */
    private void discoverPeers() {
        checkPermission();
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                ToastUtil.showToast(MainActivity.this, "找到了这些设备");
            }

            @Override
            public void onFailure(int reason) {
                ToastUtil.showToast(MainActivity.this, "没有找到设备");
            }
        });
    }

    /**
     * 使用WifiP2pManager的connect()方法连接设备
     *
     * @param config 提供WifiP2p的配置信息，作为connect()的一个参数。
     */
    private void connect(WifiP2pConfig config) {
        checkPermission();
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                isConnected = true;
            }

            @Override
            public void onFailure(int reason) {
                ToastUtil.showToast(MainActivity.this, "连接失败，请重试");
                cancelConnect();
            }
        });
    }

    /**
     * 使用WifiP2pManager的removeGroup()方法退出群组
     */
    private void disconnect() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                isConnected = false;
                ToastUtil.showToast(MainActivity.this, "断开连接");
            }

            @Override
            public void onFailure(int reason) {
                //没法退出群组，则取消连接
                cancelConnect();
            }
        });
    }

    /**
     * 使用WifiP2pManager的cancelConnect()方法取消连接，
     * 在连接阶段断开连接。
     */
    private void cancelConnect() {
        mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                isConnected = false;
                ToastUtil.showToast(MainActivity.this, "已取消连接");
            }

            @Override
            public void onFailure(int reason) {
                ToastUtil.showToast(MainActivity.this, "无法取消连接");
                discoverPeers();
            }
        });
    }

    /**
     * 需要询问ACCESS_FINE_LOCATION权限，即位置权限是否开启。
     */
    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    /**
     * 在onResume阶段注册广播接收器。
     */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, WifiBroadcastReceiver.getIntentFilter());
        discoverPeers();
        Log.d(TAG, "onResume: " + "ok");
    }

    /**
     * 在onPause阶段注销广播接收器。
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        Log.d(TAG, "onPause: " + "ok");
    }

    /**
     * 在onDestroy阶段解绑服务，服务器断开。
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServiceConnection != null && mDeviceRole.getText().toString().equals("服务器")) {
            Log.d(TAG, "onUnbind: yes");
            unbindService(mServiceConnection);
            mServiceConnection = null;
        }
    }
}