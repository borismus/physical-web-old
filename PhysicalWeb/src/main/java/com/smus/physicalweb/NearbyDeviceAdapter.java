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

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.*;

/**
 * Created by smus on 1/24/14.
 */

public class NearbyDeviceAdapter extends BaseAdapter {
  String TAG = "NearbyDeviceAdapter";

  private ArrayList<NearbyDevice> mNearbyDevices;
  private Activity mActivity;

  private long mLastChangeRequestTime = 0;
  private Timer mNotificationTimer;

  private long NOTIFY_DELAY = 300;

  NearbyDeviceAdapter(Activity activity) {
    mNearbyDevices = new ArrayList<NearbyDevice>();
    mActivity = activity;
  }

  @Override
  public int getCount() {
    return mNearbyDevices.size();
  }

  @Override
  public Object getItem(int position) {
    return mNearbyDevices.get(position);
  }

  @Override
  public long getItemId(int position) {
    NearbyDevice device = mNearbyDevices.get(position);
    return System.identityHashCode(device);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View view = mActivity.getLayoutInflater().inflate(
        R.layout.listitem_device, null);
    NearbyDevice device = mNearbyDevices.get(position);

    DeviceMetadata deviceMetadata = device.getInfo();
    if (deviceMetadata != null) {
      TextView infoView = (TextView) view.findViewById(R.id.title);
      infoView.setText(deviceMetadata.title);

      infoView = (TextView) view.findViewById(R.id.url);
      infoView.setText(deviceMetadata.siteUrl);

      infoView = (TextView) view.findViewById(R.id.description);
      infoView.setText(deviceMetadata.description);

      ImageView iconView = (ImageView) view.findViewById(R.id.icon);
      iconView.setImageBitmap(deviceMetadata.icon);
    } else {
      Log.i(TAG, String.format("Device with URL %s has no metadata.", device.getUrl()));
    }
    return view;
  }

  public void addDevice(final NearbyDevice device) {
    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mNearbyDevices.add(device);
        device.setAdapter(NearbyDeviceAdapter.this);
        queueChangedNotification();
      }
    });
  }

  public NearbyDevice getExistingDevice(NearbyDevice candidateDevice) {
    for (NearbyDevice device : mNearbyDevices) {
      if (device.getUrl().equals(candidateDevice.getUrl())) {
        return device;
      }
    }
    return null;
  }

  public ArrayList<NearbyDevice> removeExpiredDevices() {
    // Get a list of devices that we need to remove.
    ArrayList<NearbyDevice> toRemove = new ArrayList<NearbyDevice>();
    for (NearbyDevice device : mNearbyDevices) {
      if (device.isLastSeenAfter(NearbyDeviceManager.MAX_INACTIVE_TIME)) {
        toRemove.add(device);
      }
    }

    // Remove those devices from the list and notify the listener.
    for (final NearbyDevice device : toRemove) {
      mActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          mNearbyDevices.remove(device);
          queueChangedNotification();
        }
      });
    }
    return toRemove;
  }

  public void updateListUI() {
    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        queueChangedNotification();
      }
    });
  }

  public void queueChangedNotification() {
    long now = System.currentTimeMillis();
    // If a notification was recently issued, create a pending notification.
    if (now - mLastChangeRequestTime < NOTIFY_DELAY) {
      // Ignore if there's a pending timer already.
      if (mNotificationTimer != null) {
        Log.i(TAG, "queueChangedNotification: Timer already exists.");
        return;
      }
      Log.i(TAG, "queueChangedNotification: Scheduling timer.");
      mNotificationTimer = new Timer();
      mNotificationTimer.schedule(new TimerTask() {
        @Override
        public void run() {
          mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              notifyDataSetChanged();
            }
          });
        }
      }, new Date(now + NOTIFY_DELAY));
    } else {
      // Otherwise, if there's no active timer, notify immediately.
      Log.i(TAG, "queueChangedNotification: Immediately notifying.");
      notifyDataSetChanged();
    }
  }

  @Override
  public void notifyDataSetChanged() {
    Log.i(TAG, "queueChangedNotification: notifyDataSetChanged");
    Collections.sort(mNearbyDevices, mRssiComparator);

    super.notifyDataSetChanged();

    // Cancel the pending notification timer if there is one.
    if (mNotificationTimer != null) {
      mNotificationTimer.cancel();
      mNotificationTimer = null;
    }
    mLastChangeRequestTime = System.currentTimeMillis();
  }

  private Comparator<NearbyDevice> mRssiComparator = new Comparator<NearbyDevice>() {
    @Override
    public int compare(NearbyDevice lhs, NearbyDevice rhs) {
      return rhs.getAverageRSSI() - lhs.getAverageRSSI();
    }
  };

}