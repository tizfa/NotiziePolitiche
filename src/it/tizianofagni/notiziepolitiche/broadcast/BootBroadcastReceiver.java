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

import it.tizianofagni.notiziepolitiche.service.FeedRetrieverService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, Intent intent) {

		new Thread(new Runnable() {

			@Override
			public void run() {

				Context ctx = context.getApplicationContext();
				
				// Acquire wake lock.
				Log.i(BootBroadcastReceiver.class.getName(), "acquire lock");
				FeedRetrieverService.acquireStaticLock(ctx);
				
				Log.i(BootBroadcastReceiver.class.getName(),
						"Start feeds retriever service");
				Intent serviceIntent = new Intent(ctx,
						FeedRetrieverService.class);
				serviceIntent.putExtra(FeedRetrieverService.INTENT_START_SERVICE_ON_BOOT,1);
				ctx.startService(serviceIntent);
			}
		}).start();
	}
}