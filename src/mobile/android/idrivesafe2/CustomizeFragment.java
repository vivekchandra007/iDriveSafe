package mobile.android.idrivesafe2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class CustomizeFragment extends Fragment {

	private View view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.customize_view, container, false);

		initialize();
		changeFonts();
		setupListeners();
		return view;
	}

	private void initialize() {
		Spinner spinProfiles = (Spinner) view.findViewById(R.id.spinProfiles);
		String[] items = new String[] { getActivity().getString(R.string.no), getActivity().getString(R.string.yes) };
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				R.layout.profiles_dropdown_view, items);
		spinProfiles.setAdapter(adapter);
		setPreferencesOnScreen();
	}

	private void setupListeners() {
		final RelativeLayout rLaySafeModeSendMsg = (RelativeLayout) view
				.findViewById(R.id.rLaySafeModeSendMsg);
		rLaySafeModeSendMsg.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CheckBox chkBSafeModeSendMsg = (CheckBox) view
						.findViewById(R.id.chkBSafeModeSendMsg);
				RelativeLayout rLaySafeModeMsg = (RelativeLayout) view
						.findViewById(R.id.rLaySafeModeMsg);
				if (chkBSafeModeSendMsg.isChecked()) {
					chkBSafeModeSendMsg.setChecked(false);
					rLaySafeModeMsg.setVisibility(View.GONE);
					MainActivity.safeModeSendMsg = false;
					PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_SEND_MSG, "false").commit();
				} else {
					chkBSafeModeSendMsg.setChecked(true);
					rLaySafeModeMsg.setVisibility(View.VISIBLE);
					MainActivity.safeModeSendMsg = true;
					PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_SEND_MSG, "true").commit();
				}
			}
		});

		final CheckBox chkBSafeModeSendMsg = (CheckBox) view
				.findViewById(R.id.chkBSafeModeSendMsg);
		chkBSafeModeSendMsg
				.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						CheckBox chkBSafeModeSendMsg = (CheckBox) view
								.findViewById(R.id.chkBSafeModeSendMsg);
						RelativeLayout rLaySafeModeMsg = (RelativeLayout) view
								.findViewById(R.id.rLaySafeModeMsg);
						if (chkBSafeModeSendMsg.isChecked()) {
							rLaySafeModeMsg.setVisibility(View.VISIBLE);
							MainActivity.safeModeSendMsg = true;
							PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_SEND_MSG, "true").commit();
						} else {
							rLaySafeModeMsg.setVisibility(View.GONE);
							MainActivity.safeModeSendMsg = false;
							PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_SEND_MSG, "false").commit();
						}
					}
				});

		final RelativeLayout rLayResetToDefaultsOuterRing = (RelativeLayout) view
				.findViewById(R.id.rLayResetToDefaultsOuterRing);
		rLayResetToDefaultsOuterRing
				.setOnTouchListener(new View.OnTouchListener() {

					@Override
					public boolean onTouch(View v, MotionEvent event) {
						TextView tvResetToDefaults = (TextView) view
								.findViewById(R.id.tvResetToDefaults);
						switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							tvResetToDefaults.setTextColor(getActivity()
									.getResources()
									.getColor(R.color.theme_blue));
							rLayResetToDefaultsOuterRing
									.setBackground(getActivity()
											.getResources()
											.getDrawable(
													R.drawable.rounded_shape_red));
							return true;
						case MotionEvent.ACTION_UP:
							tvResetToDefaults.setTextColor(getActivity()
									.getResources().getColor(R.color.black));
							rLayResetToDefaultsOuterRing
									.setBackground(getActivity()
											.getResources()
											.getDrawable(
													R.drawable.rounded_shape_blue));
							AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				            builder.setMessage(getActivity().getString(R.string.reset_warning))
				                   .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
				                       public void onClick(DialogInterface dialog, int id) {
				                    	   restoreDefaultSettings();
				                       }
				                   })
				                   .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				                       public void onClick(DialogInterface dialog, int id) {
				                           // User cancelled the dialog
				                    	   return;
				                       }
				                   })
				                   .setCancelable(false)
				            	   .create().show();
							return true;
						}
						return false;
					}
				});

		final ImageView imgVEditSafeSpeedLimit = (ImageView) view
				.findViewById(R.id.imgVEditSafeSpeedLimit);
		imgVEditSafeSpeedLimit.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {				
				EditText txtSafeSpeedLimit = (EditText) view
						.findViewById(R.id.txtSafeSpeedLimit);
				if (!txtSafeSpeedLimit.isEnabled()) {
					// Make Safe Speed editable
					imgVEditSafeSpeedLimit.setImageDrawable(getActivity()
							.getResources()
							.getDrawable(R.drawable.save_content));
					txtSafeSpeedLimit.setBackground(getActivity()
							.getResources()
							.getDrawable(R.drawable.rounded_shape_blue_red));
					txtSafeSpeedLimit.setEnabled(true);
					txtSafeSpeedLimit.setText(String.valueOf(MainActivity.safeSpeedLimit));
					InputFilter[] fArray = new InputFilter[1];
					fArray[0] = new InputFilter.LengthFilter(3);
					txtSafeSpeedLimit.setFilters(fArray);
					txtSafeSpeedLimit.requestFocus();
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(txtSafeSpeedLimit, InputMethodManager.SHOW_IMPLICIT);
				} else {
					// Save edited Safe Speed
					imgVEditSafeSpeedLimit.setImageDrawable(getActivity()
							.getResources().getDrawable(
									R.drawable.edit_content));
					txtSafeSpeedLimit.setBackground(getActivity()
							.getResources()
							.getDrawable(R.drawable.rounded_shape_blue));
					InputFilter[] fArray = new InputFilter[1];
					fArray[0] = new InputFilter.LengthFilter(9);
					txtSafeSpeedLimit.setFilters(fArray);
					try {
						PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_SPEED_LIMIT, txtSafeSpeedLimit.getText().toString()).commit();
						MainActivity.safeSpeedLimit = Integer.parseInt(txtSafeSpeedLimit.getText().toString());
						txtSafeSpeedLimit.setText(MainActivity.safeSpeedLimit + " km/h");
					} catch (Exception e) {
						//do nothing
						txtSafeSpeedLimit.setText(txtSafeSpeedLimit.getText().toString() + " km/h");
					}									
					txtSafeSpeedLimit.setEnabled(false);
				}
			}
		});
		
		final ImageView imgVEditSafeModeMsg = (ImageView) view
				.findViewById(R.id.imgVEditSafeModeMsg);
		imgVEditSafeModeMsg.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				EditText txtSafeModeMsg = (EditText) view
						.findViewById(R.id.txtSafeModeMsg);
				if (!txtSafeModeMsg.isEnabled()) {
					// Make Safe Mode message editable
					imgVEditSafeModeMsg.setImageDrawable(getActivity()
							.getResources()
							.getDrawable(R.drawable.save_content));
					txtSafeModeMsg.setEnabled(true);
					txtSafeModeMsg.requestFocus();
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(txtSafeModeMsg, InputMethodManager.SHOW_IMPLICIT);
				} else {
					// Save Safe Mode message
					imgVEditSafeModeMsg.setImageDrawable(getActivity()
							.getResources().getDrawable(
									R.drawable.edit_content));
					MainActivity.safeModeMsg = txtSafeModeMsg.getText().toString();
					PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_MSG, MainActivity.safeModeMsg).commit();
					txtSafeModeMsg.setEnabled(false);
				}
			}
		});
		
		Spinner spinProfiles = (Spinner) view.findViewById(R.id.spinProfiles);
		spinProfiles.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				
				if (position == 0) {
					chkBSafeModeSendMsg.setChecked(false);
					chkBSafeModeSendMsg.setEnabled(false);
					rLaySafeModeSendMsg.setVisibility(View.GONE);
					
					MainActivity.profileToBeSelected = MainActivity.PROFILE_GENERAL;
					PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_PROFILE_SELECTED, MainActivity.profileToBeSelected).commit();
				} else if (position == 1) {
					rLaySafeModeSendMsg.setVisibility(View.VISIBLE);
					chkBSafeModeSendMsg.setEnabled(true);
					chkBSafeModeSendMsg.setChecked(true);					
					
					MainActivity.profileToBeSelected = MainActivity.PROFILE_SILENT;
					PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_PROFILE_SELECTED, MainActivity.profileToBeSelected).commit();
				}				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing when nothing is selected
				
			}
			
		});
	}
		

	private void changeFonts() {
		Typeface myTypeface = Typeface.createFromAsset(getActivity()
				.getAssets(), "fonts/KoshgarianRegular.ttf");

		TextView tvSafeSpeedLimit = (TextView) view
				.findViewById(R.id.tvSafeSpeedLimit);
		TextView tvProfileToBeSelected = (TextView) view
				.findViewById(R.id.tvProfileToBeSelected);
		TextView tvSafeModeSendMsg = (TextView) view
				.findViewById(R.id.tvSafeModeSendMsg);
		TextView tvResetToDefaults = (TextView) view
				.findViewById(R.id.tvResetToDefaults);
		tvSafeSpeedLimit.setTypeface(myTypeface);
		tvProfileToBeSelected.setTypeface(myTypeface);
		tvSafeModeSendMsg.setTypeface(myTypeface);
		tvResetToDefaults.setTypeface(myTypeface);
	}

	private void restoreDefaultSettings() {
		// Set defaults
		MainActivity.safeSpeedLimit =MainActivity. DEFAULT_SAFE_SPEED_LIMIT;
		PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_SPEED_LIMIT, String.valueOf(MainActivity.safeSpeedLimit)).commit();
		
		MainActivity.profileToBeSelected = MainActivity.PROFILE_SILENT;
		PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_PROFILE_SELECTED, MainActivity.profileToBeSelected).commit();
		
		MainActivity.safeModeSendMsg = true;
		PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_SEND_MSG, "true").commit();
		
		MainActivity.safeModeMsg = getActivity().getString(R.string.pref_auto_message_text);
		PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(MainActivity.PREF_MSG, MainActivity.safeModeMsg).commit();
		
		setPreferencesOnScreen();
		
		Toast.makeText(getActivity().getApplicationContext(), getString(R.string.reset_success), Toast.LENGTH_SHORT).show();
	}

	private void setPreferencesOnScreen() {
		EditText txtSafeSpeedLimit = (EditText) view
				.findViewById(R.id.txtSafeSpeedLimit);
		Spinner spinProfiles = (Spinner) view.findViewById(R.id.spinProfiles);
		CheckBox chkBSafeModeSendMsg = (CheckBox) view
				.findViewById(R.id.chkBSafeModeSendMsg);
		RelativeLayout rLaySafeModeMsg = (RelativeLayout) view
				.findViewById(R.id.rLaySafeModeMsg);
		EditText txtSafeModeMsg = (EditText) view
				.findViewById(R.id.txtSafeModeMsg);

		txtSafeSpeedLimit.setText(MainActivity.safeSpeedLimit + " km/h");
		if (MainActivity.profileToBeSelected.equalsIgnoreCase(MainActivity.PROFILE_GENERAL))
			spinProfiles.setSelection(0);
		else
			spinProfiles.setSelection(1);
		chkBSafeModeSendMsg.setChecked(MainActivity.safeModeSendMsg);
		if (MainActivity.safeModeSendMsg) {
			rLaySafeModeMsg.setVisibility(View.VISIBLE);
			txtSafeModeMsg.setText(MainActivity.safeModeMsg);
		} else
			rLaySafeModeMsg.setVisibility(View.GONE);
	}
}
