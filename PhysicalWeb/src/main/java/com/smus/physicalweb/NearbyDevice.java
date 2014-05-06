/*
 * Copyright 2014 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smus.physicalweb;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.ArrayList;

/**
 * Represents a nearby device.
 *
 * Created by smus on 1/24/14.
 */
public class NearbyDevice implements MetadataResolver.OnMetadataListener {

  String TAG = "NearbyDevice";

  private BluetoothDevice mBluetoothDevice;

  private DeviceMetadata mDeviceMetadata;
  private String mUrl;
  private NearbyDeviceAdapter mAdapter;

  private int HISTORY_LENGTH = 3;
  private ArrayList<Integer> mRSSIHistory;
  private long mLastSeen;


  public NearbyDevice(BluetoothDevice bluetoothDevice, int RSSI) {
    mBluetoothDevice = bluetoothDevice;
    String url = MetadataResolver.getURLForDevice(this);
    initialize(url, RSSI);
  }

  // Constructor for testing purposes only.
  public NearbyDevice(String url, int RSSI) {
    initialize(url, RSSI);
  }

  private void initialize(String url, int RSSI) {
    mUrl = url;
    mLastSeen = System.nanoTime();

    mRSSIHistory = new ArrayList<Integer>();
    mRSSIHistory.add(RSSI);
  }

  public void setAdapter(NearbyDeviceAdapter adapter) {
    mAdapter = adapter;
  }

  public int getLastRSSI() { return mRSSIHistory.get(mRSSIHistory.size() - 1); }

  public int getAverageRSSI() {
    Log.i(TAG, "getAverageRSSI. Elements: " + mRSSIHistory.size());
    int sum = 0;
    for (int rssi : mRSSIHistory) {
      sum += rssi;
    }
    return sum/mRSSIHistory.size();
  }

  public DeviceMetadata getInfo() { return mDeviceMetadata; }

  public String getUrl() { return mUrl; }

  public String getName() {
    if (mBluetoothDevice != null) {
      String name = mBluetoothDevice.getName();
      if (name != null) {
        return name;
      } else {
        return "No device name";
      }
    } else {
      return mUrl;
    }
  }

  public void updateLastSeen(int RSSI) {
    mLastSeen = System.nanoTime();

    if (mRSSIHistory.size() >= HISTORY_LENGTH) {
      mRSSIHistory.remove(0);
    }
    mRSSIHistory.add(RSSI);
  }

  public boolean isLastSeenAfter(long threshold) {
    long notSeenMs = (System.nanoTime() - mLastSeen) / 1000000;
    return notSeenMs > threshold;
  }

  @Override
  public void onDeviceInfo(DeviceMetadata deviceMetadata) {
    mDeviceMetadata = deviceMetadata;
    if (mAdapter != null) {
      mAdapter.updateListUI();
    }
  }

  public boolean isBroadcastingUrl() {
    return mUrl != null;
  }
}
