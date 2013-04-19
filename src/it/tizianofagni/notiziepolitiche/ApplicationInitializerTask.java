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


package it.tizianofagni.notiziepolitiche;



import it.tizianofagni.notiziepolitiche.R;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

public class ApplicationInitializerTask<ACTIVITY extends Activity> extends
		AsyncTask<Integer, Integer, Integer> {

	private WeakReference<ACTIVITY> act;
	private WeakReference<ProgressDialog> progressDlg;
	private ApplicationInitializerListener listener;
	
	public interface ApplicationInitializerListener {
		public void applicationInitialized();
	}
	
	
	public ApplicationInitializerTask(ACTIVITY act, ApplicationInitializerListener l) {
		this.act = new WeakReference<ACTIVITY>(act);
		this.progressDlg = null;
		this.listener = l;
	}

	@Override
	protected Integer doInBackground(Integer... params) {

		final ACTIVITY activity = act.get();
		if (activity == null) {
			cancel(true);
			return 0;
		}

		boolean firstTime = true;
		
		NotiziePoliticheApplication app = (NotiziePoliticheApplication) activity
				.getApplication();
		try {
			while (!app.initApplication()) {
				if (firstTime) {
					activity.runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							progressDlg = new WeakReference<ProgressDialog>(ProgressDialog.show(activity, activity.getResources().getString(R.string.waiting_messagge), 
									activity.getResources().getString(R.string.waiting_messagge_application), true));
							progressDlg.get().setOwnerActivity(activity);
						}
					});
					
					firstTime = false;
				}
				
				try {
					Thread.sleep(300);
				} catch (Exception e) {
					Log.w(getClass().getName(),
							"Sleeping for application initialization", e);
				}
			}
		} catch (Exception e) {
			Log.e(getClass().getName(), "During application initialization", e);
		}
		return 0;
	}

	@Override
	protected void onPostExecute(Integer result) {
		// TODO Auto-generated method stub
		super.onPostExecute(result);

		if (result == null)
			return;
		
		ACTIVITY activity = act.get();
		if (activity == null) {
			cancel(true);
			return;
		}
		
		listener.applicationInitialized();

		if (progressDlg != null) {
			ProgressDialog dlg = progressDlg.get();
			if (dlg != null)
				dlg.dismiss();
		}
	}
}
