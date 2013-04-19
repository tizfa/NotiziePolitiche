/*
 * Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package it.tizianofagni.notiziepolitiche.broadcast;

import it.tizianofagni.notiziepolitiche.activity.CategoriesListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaBroadcastReceiver extends BroadcastReceiver {
	public void onReceive(Context context, Intent intent) {
		// Toast.makeText(context,
		// context.toString(),Toast.LENGTH_LONG).show();
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
			
			Intent appIntent = new Intent(context,
					CategoriesListActivity.class);
			appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(appIntent);
			context.unregisterReceiver(this);
		} else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
			//Toast.makeText(context, "SD Card unmounted",Toast.LENGTH_LONG).show();
		} else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
			//Toast.makeText(context, "SD Card scanner started",Toast.LENGTH_LONG).show();
		} else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
			//Toast.makeText(context, "SD Card scanner finished",Toast.LENGTH_LONG).show();
		} else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
			//Toast.makeText(context, "SD Card eject", Toast.LENGTH_LONG).show();
		} else if (action.equals(Intent.ACTION_UMS_CONNECTED)) {
			//Toast.makeText(context, "connected", Toast.LENGTH_LONG).show();
		} else if (action.equals(Intent.ACTION_UMS_DISCONNECTED)) {
			//Toast.makeText(context, "disconnected", Toast.LENGTH_LONG).show();
		}
	}
}
