package biz.bokhorst.xprivacy;

import biz.bokhorst.xprivacy.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class XBatchEdit extends Activity {

	public static final String cRestrictionName = "Restriction";

	private AppListAdapter mAppAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set layout
		setContentView(R.layout.xbatchlist);

		// Get restriction name
		Bundle extras = getIntent().getExtras();
		final String restrictionName = extras.getString(cRestrictionName);

		// Display restriction name
		TextView tvRestriction = (TextView) findViewById(R.id.tvRestriction);
		tvRestriction.setText(XRestriction.getLocalizedName(this, restrictionName));

		// Handle help
		tvRestriction.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Dialog dialog = new Dialog(XBatchEdit.this);
				dialog.setTitle(getString(R.string.help_application));
				dialog.setContentView(R.layout.xhelp);
				dialog.setCancelable(true);
				dialog.show();
			}
		});

		// Get app list
		SparseArray<XApplicationInfo> mapApp = new SparseArray<XApplicationInfo>();
		List<XApplicationInfo> listApp = new ArrayList<XApplicationInfo>();
		for (ApplicationInfo appInfo : getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA))
			if (appInfo.uid != XRestriction.cUidAndroid
					&& !appInfo.packageName.equals(XBatchEdit.class.getPackage().getName())) {
				XApplicationInfo xAppInfo = mapApp.get(appInfo.uid);
				if (xAppInfo == null) {
					xAppInfo = new XApplicationInfo(appInfo, restrictionName, getPackageManager());
					mapApp.put(appInfo.uid, xAppInfo);
					listApp.add(xAppInfo);
				} else
					xAppInfo.AddApplicationName((String) getPackageManager().getApplicationLabel(appInfo));
			}
		Collections.sort(listApp);

		// Fill app list view adapter
		final ListView lvApp = (ListView) findViewById(R.id.lvApp);
		mAppAdapter = new AppListAdapter(this, R.layout.xbatchentry, listApp, restrictionName);
		lvApp.setAdapter(mAppAdapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mAppAdapter.notifyDataSetChanged();
	}

	private class AppListAdapter extends ArrayAdapter<XApplicationInfo> {

		private String mRestrictionName;

		public AppListAdapter(Context context, int resource, List<XApplicationInfo> objects, String restrictionName) {
			super(context, resource, objects);
			mRestrictionName = restrictionName;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View row = inflater.inflate(R.layout.xbatchentry, parent, false);
			ImageView imgIcon = (ImageView) row.findViewById(R.id.imgBatchEntryIcon);
			ImageView imgInternet = (ImageView) row.findViewById(R.id.imgBatchEntryInternet);
			ImageView imgUsed = (ImageView) row.findViewById(R.id.imgBatchEntryUsed);
			final CheckedTextView ctvApp = (CheckedTextView) row.findViewById(R.id.tvBatchEntryName);

			// Get entry
			final XApplicationInfo appEntry = getItem(position);

			// Set icon
			imgIcon.setImageDrawable(appEntry.getDrawable());
			imgIcon.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent intentSettings = new Intent(view.getContext(), XAppEdit.class);
					intentSettings.putExtra(XAppEdit.cPackageName, appEntry.getPackageName());
					intentSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					view.getContext().startActivity(intentSettings);
				}
			});

			// Set icon/title
			ctvApp.setText(appEntry.toString());

			// Check if internet access
			if (!appEntry.hasInternet())
				imgInternet.setVisibility(View.INVISIBLE);

			// Check if used
			if (appEntry.isUsed())
				ctvApp.setTypeface(null, Typeface.BOLD_ITALIC);
			else
				imgUsed.setVisibility(View.INVISIBLE);

			// Handle used click
			imgUsed.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					// Get audit
					ContentResolver contentResolver = view.getContext().getContentResolver();
					Cursor cursor = contentResolver.query(XPrivacyProvider.URI_AUDIT, null, mRestrictionName,
							new String[] { Integer.toString(appEntry.getUid()) }, null);
					List<String> listAudit = new ArrayList<String>();
					while (cursor.moveToNext())
						listAudit.add(cursor.getString(cursor.getColumnIndex(XPrivacyProvider.COL_METHOD)));
					cursor.close();
					Collections.sort(listAudit);

					// Display audit
					String localRestrictionName = XRestriction.getLocalizedName(view.getContext(), mRestrictionName);
					AlertDialog alertDialog = new AlertDialog.Builder(XBatchEdit.this).create();
					alertDialog.setTitle(localRestrictionName);
					alertDialog.setIcon(R.drawable.ic_launcher);
					alertDialog.setMessage(TextUtils.join("\n", listAudit));
					alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
					alertDialog.show();
				}
			});

			// Display restriction
			boolean restricted = XRestriction.getRestricted(null, row.getContext(), appEntry.getUid(),
					mRestrictionName, false);
			ctvApp.setChecked(restricted);

			// Listen for restriction changes
			ctvApp.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					boolean restricted = XRestriction.getRestricted(null, view.getContext(), appEntry.getUid(),
							mRestrictionName, false);
					restricted = !restricted;
					ctvApp.setChecked(restricted);
					XRestriction.setRestricted(null, view.getContext(), appEntry.getUid(), mRestrictionName, restricted);
				}
			});

			return row;
		}
	}

	private class XApplicationInfo implements Comparable<XApplicationInfo> {
		private Drawable mDrawable;
		private List<String> mListApplicationName;
		private String mPackageName;
		private boolean mHasInternet;
		private boolean mIsUsed;
		private int mUid;

		public XApplicationInfo(ApplicationInfo appInfo, String restrictionName, PackageManager packageManager) {
			mDrawable = appInfo.loadIcon(packageManager);
			mListApplicationName = new ArrayList<String>();
			mListApplicationName.add((String) packageManager.getApplicationLabel(appInfo));
			mPackageName = appInfo.packageName;
			mHasInternet = XRestriction.hasInternet(getBaseContext(), appInfo.packageName);
			mIsUsed = XRestriction.isUsed(getBaseContext(), appInfo.uid, restrictionName);
			mUid = appInfo.uid;
		}

		public void AddApplicationName(String Name) {
			mListApplicationName.add(Name);
		}

		public String getPackageName() {
			return mPackageName;
		}

		public Drawable getDrawable() {
			return mDrawable;
		}

		public boolean hasInternet() {
			return mHasInternet;
		}

		public boolean isUsed() {
			return mIsUsed;
		}

		public int getUid() {
			return mUid;
		}

		@Override
		@SuppressLint("DefaultLocale")
		public String toString() {
			return String.format("%s (%d)", TextUtils.join(", ", mListApplicationName), mUid);
		}

		@Override
		public int compareTo(XApplicationInfo other) {
			return toString().compareToIgnoreCase(other.toString());
		}
	}
}
