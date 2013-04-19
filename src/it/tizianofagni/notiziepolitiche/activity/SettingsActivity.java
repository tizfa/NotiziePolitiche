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

package it.tizianofagni.notiziepolitiche.activity;

import it.tizianofagni.notiziepolitiche.R;
import it.tizianofagni.notiziepolitiche.AppConstants;
import it.tizianofagni.notiziepolitiche.NotiziePoliticheApplication;
import it.tizianofagni.notiziepolitiche.utils.GenericUtils;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.layout.preferences);
		setTitle(GenericUtils.getAppName(this) + " - Preferenze");

		Preference pr = findPreference(getResources().getString(
				R.string.keyBuyFullVersion));
		pr.setTitle(getString(R.string.buyFullVersion_title, getString(R.string.app_name_full)));
		pr.setShouldDisableView(true);
		if (GenericUtils.isLiteVersion(this)) {
			pr.setEnabled(true);
		} else {
			pr.setEnabled(false);
			PreferenceCategory pc = (PreferenceCategory) findPreference(getString(R.string.keyInfoCategory));
			pc.removePreference(pr);
		}
		pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(
						Intent.ACTION_VIEW,
						Uri.parse("http://market.android.com/search?q=pname:it.tizianofagni.notiziepolitiche.full"));
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				SettingsActivity.this.startActivity(intent);
				return true;
			}
		});

		pr = findPreference(getResources()
				.getString(R.string.keyInfoOnSoftware));
		pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				showCreditsDialog();
				return true;
			}
		});

		pr = findPreference(getResources().getString(
				R.string.keyAutoRefreshData));
		pr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
				if (app.isLiteVersion()) {
					GenericUtils.showDlgForProVersion(SettingsActivity.this);
					return false;
				}

				return true;
			}
		});

		pr = findPreference(getResources().getString(
				R.string.keyRetrieveFeedsInterval));
		pr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				int minValue = 3;
				int maxValue = 2000;

				EditTextPreference pref = (EditTextPreference) preference;
				Editable ed = pref.getEditText().getText();
				if (ed == null || ed.toString().equals("")) {
					makeNumberErrorValidationMessage(minValue, maxValue);
					return false;
				}

				try {
					int val = Integer.parseInt(ed.toString());
					if (val < minValue || val > maxValue) {
						makeNumberErrorValidationMessage(minValue, maxValue);
						return false;
					}

				} catch (Exception e) {
					makeNumberErrorValidationMessage(minValue, maxValue);
					return false;
				}

				return true;
			}
		});

		pr = findPreference(getResources().getString(
				R.string.keyNewsArticleTitleFontSize));
		pr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				int minValue = 8;
				int maxValue = 30;

				EditTextPreference pref = (EditTextPreference) preference;
				Editable ed = pref.getEditText().getText();
				if (ed == null || ed.toString().equals("")) {
					makeNumberErrorValidationMessage(minValue, maxValue);
					return false;
				}

				try {
					int val = Integer.parseInt(ed.toString());
					if (val < minValue || val > maxValue) {
						makeNumberErrorValidationMessage(minValue, maxValue);
						return false;
					}

				} catch (Exception e) {
					makeNumberErrorValidationMessage(minValue, maxValue);
					return false;
				}

				return true;
			}
		});

		pr = findPreference(getResources().getString(
				R.string.keyNewsArticleDescriptionFontSize));
		pr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				int minValue = 5;
				int maxValue = 25;

				EditTextPreference pref = (EditTextPreference) preference;
				Editable ed = pref.getEditText().getText();
				if (ed == null || ed.toString().equals("")) {
					makeNumberErrorValidationMessage(minValue, maxValue);
					return false;
				}

				try {
					int val = Integer.parseInt(ed.toString());
					if (val < minValue || val > maxValue) {
						makeNumberErrorValidationMessage(minValue, maxValue);
						return false;
					}

				} catch (Exception e) {
					makeNumberErrorValidationMessage(minValue, maxValue);
					return false;
				}

				return true;
			}
		});
	}

	private void makeNumberErrorValidationMessage(int minNumber, int maxNumber) {
		CharSequence text = getResources().getString(
				R.string.number_range_error_validation, minNumber, maxNumber);
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(SettingsActivity.this, text, duration);
		toast.show();
	}

	private void showCreditsDialog() {

		final String credits = "Autore: Tiziano Fagni<br>"
				+ "Contatti: <a href=\"http://www.tizianofagni.it\">http://www.tizianofagni.it</a><br>"
				+ "<br>Crediti<br>Le icone utilizzate all'interno del software fanno parte del set di "
				+ "icone \"AwOken\", accessibile al link&nbsp;<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">"
				+ "<a href=\"http://alecive.deviantart.com/art/AwOken-Awesome-Token-1-5-163570862\">http://alecive.deviantart.com/art/AwOken-Awesome-Token-1-5-163570862</a>.<br>"
				+ "<br>Note<br>Tutti i contenuti accessibili tramite il software appartengono ai rispettivi proprietari (agenzie di stampa, quotidiani online, "
				+ "ecc.)&nbsp; e il software si limita soltanto a segnalare le notizie.<br><br>";
		Spanned spannedText = Html.fromHtml(credits);

		Dialog dialog = new Dialog(this);

		dialog.setContentView(R.layout.about_dlg);
		dialog.setTitle(getResources().getString(R.string.software_info_title));
		TextView textView = (TextView) dialog
				.findViewById(R.id.creditsApplication);
		textView.setText(spannedText);
		Linkify.addLinks(textView, Linkify.ALL);

		View v = dialog.findViewById(R.id.layoutApplicationAbout);
		TextView titleApp = (TextView) v.findViewById(R.id.titleApplication);
		titleApp.setText(getResources().getString(R.string.software_info_about,
				getAppName(), AppConstants.applicationVersion.toString()));

		dialog.show();
	}

	private String getAppName() {
		NotiziePoliticheApplication app = (NotiziePoliticheApplication) getApplication();
		if (app.isLiteVersion()) {
			return getResources().getString(R.string.app_name_lite);
		} else {
			return getResources().getString(R.string.app_name_full);
		}
	}
}
