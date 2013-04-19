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

package it.tizianofagni.notiziepolitiche.utils;

import it.tizianofagni.notiziepolitiche.R;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Debug;
import android.text.format.DateUtils;
import android.util.Log;

public class GenericUtils {
	public static String getStackTrace(Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}

	public static void makeMessageDlgForExit(Context ctx, String ctxName,
			String msgError, Throwable t) {
		Log.e(ctxName, GenericUtils.getStackTrace(t));
		AlertDialog dlg = new AlertDialog.Builder(ctx).create();
		dlg.setMessage(msgError);
		dlg.setButton(Dialog.BUTTON_POSITIVE, "Ok",
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						System.exit(-1);
					}
				});

		dlg.setCancelable(false);
		dlg.show();
	}

	public static String md5(String input) {
		String res = "";
		try {
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(input.getBytes());
			byte[] md5 = algorithm.digest();
			String tmp = "";
			for (int i = 0; i < md5.length; i++) {
				tmp = (Integer.toHexString(0xFF & md5[i]));
				if (tmp.length() == 1) {
					res += "0" + tmp;
				} else {
					res += tmp;
				}
			}
		} catch (NoSuchAlgorithmException ex) {
		}
		return res;
	}

	// helper method for clearCache() , recursive
	// returns number of deleted files
	static int clearCacheFolder(final File dir, final int numDays) {

		int deletedFiles = 0;
		if (dir != null && dir.isDirectory()) {
			try {
				for (File child : dir.listFiles()) {

					// first delete subdirectories recursively
					if (child.isDirectory()) {
						deletedFiles += clearCacheFolder(child, numDays);
					}

					// then delete the files and subdirectories in this dir
					// only empty directories can be deleted, so subdirs have
					// been done first
					if (child.lastModified() < (new Date().getTime() - numDays
							* DateUtils.DAY_IN_MILLIS)) {
						if (child.delete()) {
							deletedFiles++;
						}
					}
				}
			} catch (Exception e) {
				Log.e(GenericUtils.class.getName(),
						String.format("Failed to clean the cache, error %s",
								e.getMessage()));
			}
		}
		return deletedFiles;
	}

	/*
	 * Delete the files older than numDays days from the application cache 0
	 * means all files.
	 */
	public static void clearCache(final Context context, final int numDays) {
		Log.i(GenericUtils.class.getName(), String.format(
				"Deleting cached files older than %d days", numDays));
		int numDeletedFiles = clearCacheFolder(context.getCacheDir(), numDays);
		Log.i(GenericUtils.class.getName(),
				String.format("%d old files deleted", numDeletedFiles));
	}

	public static void logHeap(Class clazz) {
		Double allocated = new Double(Debug.getNativeHeapAllocatedSize())
				/ new Double((1048576));
		Double available = new Double(Debug.getNativeHeapSize() / 1048576.0);
		Double free = new Double(Debug.getNativeHeapFreeSize() / 1048576.0);
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);

		Log.d(GenericUtils.class.getName(),
				"debug. =================================");
		Log.d(GenericUtils.class.getName(), "debug.heap native: allocated "
				+ df.format(allocated) + "MB of " + df.format(available)
				+ "MB (" + df.format(free) + "MB free) in ["
				+ clazz.getName().replaceAll("com.myapp.android.", "") + "]");
		Log.d(GenericUtils.class.getName(),
				"debug.memory: allocated: "
						+ df.format(new Double(Runtime.getRuntime()
								.totalMemory() / 1048576))
						+ "MB of "
						+ df.format(new Double(
								Runtime.getRuntime().maxMemory() / 1048576))
						+ "MB ("
						+ df.format(new Double(Runtime.getRuntime()
								.freeMemory() / 1048576)) + "MB free)");
		System.gc();
		System.gc();

		// don't need to add the following lines, it's just an app specific
		// handling in my app
		/*
		 * if (allocated>=(new Double(Runtime.getRuntime().maxMemory())/new
		 * Double((1048576))-MEMORY_BUFFER_LIMIT_FOR_RESTART)) {
		 * android.os.Process.killProcess(android.os.Process.myPid()); }
		 */
	}

	public static void showDlgForProVersion(final Context ctx) {
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setMessage(
				ctx.getResources().getString(
						R.string.functionality_not_available,
						ctx.getResources().getString(R.string.app_name_full)))
				.setCancelable(false)
				.setPositiveButton(
						ctx.getResources().getString(R.string.go_to_market),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent intent = new Intent(
										Intent.ACTION_VIEW,
										Uri.parse("http://market.android.com/search?q=pname:it.tizianofagni.notiziepolitiche.full"));
								intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
								ctx.startActivity(intent);
							}
						})
				.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Indicates if the software is running as lite or full version.
	 * 
	 * @return True if the software is running as lite, false if is running as
	 *         full version.
	 */
	public static boolean isLiteVersion(Context ctx) {
		//return ctx.getPackageName().toLowerCase().endsWith("lite");
		return false;
	}

	public static String getAppName(Context ctx) {
		if (isLiteVersion(ctx)) {
			return ctx.getString(R.string.app_name_lite);
		} else {
			return ctx.getString(R.string.app_name_full);
		}
	}
	
	
	
	/**
	  Remove a directory and all of its contents.

	  The results of executing File.delete() on a File object
	  that represents a directory seems to be platform
	  dependent. This method removes the directory
	  and all of its contents.

	  @return true if the complete directory was removed, false if it could not be.
	  If false is returned then some of the files in the directory may have been removed.

	*/
	public static boolean removeDirectory(File directory) {

	  // System.out.println("removeDirectory " + directory);

	  if (directory == null)
	    return false;
	  if (!directory.exists())
	    return true;
	  if (!directory.isDirectory())
	    return false;

	  String[] list = directory.list();

	  // Some JVMs return null for File.list() when the
	  // directory is empty.
	  if (list != null) {
	    for (int i = 0; i < list.length; i++) {
	      File entry = new File(directory, list[i]);

	      //        System.out.println("\tremoving entry " + entry);

	      if (entry.isDirectory())
	      {
	        if (!removeDirectory(entry))
	          return false;
	      }
	      else
	      {
	        if (!entry.delete())
	          return false;
	      }
	    }
	  }

	  return directory.delete();
	}
}
