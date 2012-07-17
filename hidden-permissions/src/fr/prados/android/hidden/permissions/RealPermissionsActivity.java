package fr.prados.android.hidden.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RealPermissionsActivity extends Activity
{
	private static final String[] sAndroidFilter = new String[]
	{ 
		"system", 
		"android.process.media", 
		"android.process.acore",
		};

	private static final String[] sGoogleFilter = new String[]
	{ 
		//"com.google.process.gapps", 
	};

	private static final String TAG = "TAG";

	private MenuItem mAndroid;
	private MenuItem mGoogle;

	private LinearLayout mInfo;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mInfo = (LinearLayout) findViewById(R.id.info);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		updateScreen();
	}

	private void updateScreen()
	{
		mInfo.removeAllViews();
		for (LinearLayout layout : detectRealPrivileged())
		{
			mInfo.addView(layout);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(
			R.menu.menu, menu);
		mAndroid = menu.findItem(R.id.android);
		mGoogle = menu.findItem(R.id.google);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		updateAndroidMenu();
		updateGoogleMenu();
		return true;
	}

	private void updateAndroidMenu()
	{
		mAndroid.setTitle(mAndroid.isChecked() ? R.string.android_checked : R.string.android);
	}

	private void updateGoogleMenu()
	{
		mGoogle.setTitle(mGoogle.isChecked() ? R.string.google_checked : R.string.google);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		final int id = item.getItemId();
		switch (id)
		{
			case R.id.android:
				item.setChecked(!item.isChecked());
				updateAndroidMenu();
				updateScreen();
				break;
			case R.id.google:
				item.setChecked(!item.isChecked());
				updateGoogleMenu();
				updateScreen();
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	private ArrayList<LinearLayout> detectRealPrivileged()
	{
		ArrayList<LinearLayout> views = new ArrayList<LinearLayout>();
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		StringBuilder builder = new StringBuilder();
		builder.append(getString(R.string.title)).append("\n");
		builder.append(getString(R.string.subtitle)).append("\n----\n");
		HashSet<String> filter = new HashSet<String>();
		boolean removeAndroid=(mAndroid==null || mAndroid.isChecked());
		boolean removeGoogle=(mGoogle==null || mGoogle.isChecked());
		Log.d(TAG,"removeAndroid="+removeAndroid);
		Log.d(TAG,"removeGoogle="+removeGoogle);
		if (removeAndroid)
		{
			for (int i = 0; i < sAndroidFilter.length; ++i)
				filter.add(sAndroidFilter[i]);
		}
		if (removeGoogle)
		{
			for (int i = 0; i < sGoogleFilter.length; ++i)
				filter.add(sGoogleFilter[i]);
		}

		PackageManager pm = getPackageManager();
		List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

		// Get all packages info
		List<PackageInfo> packagesInfos = new ArrayList<PackageInfo>();
		for (ApplicationInfo appInfo : apps)
		{
			try
			{
				if (removeAndroid 
						&& appInfo.processName.startsWith("com.android") 
						|| appInfo.processName.startsWith("android.process"))
					continue;
				if (removeGoogle && appInfo.processName.startsWith("com.google"))
					continue;
				if (filter.contains(appInfo.processName))
					continue;
				packagesInfos.add(pm.getPackageInfo(
					appInfo.packageName, PackageManager.GET_PERMISSIONS | PackageManager.GET_SIGNATURES));
			}
			catch (NameNotFoundException e)
			{
				// Ignore
			}

		}

		// Select applications who share process
		HashMap<String, List<PackageInfo>> union = new HashMap<String, List<PackageInfo>>();
		for (PackageInfo packageInfo : packagesInfos)
		{
			List<PackageInfo> shareProcess = union.get(packageInfo.applicationInfo.processName);
			if (shareProcess == null)
			{
				shareProcess = new ArrayList<PackageInfo>();
				union.put(
					packageInfo.applicationInfo.processName, shareProcess);
			}
			shareProcess.add(packageInfo);
		}

		// Merge all real permissions
		boolean find = false;
		HashSet<String> allRealPermissions = new HashSet<String>();
		HashSet<String> addedPermissions = new HashSet<String>();
		for (List<PackageInfo> infos : union.values())
		{
			if (infos.size() > 1)
			{
				// Get all permissions
				allRealPermissions.clear();
				for (PackageInfo packageinfo : infos)
				{
					if (packageinfo.requestedPermissions != null)
					{
						for (int i = 0; i < packageinfo.requestedPermissions.length; ++i)
						{
							allRealPermissions.add(packageinfo.requestedPermissions[i]);
						}
					}
				}
				if (allRealPermissions.size() != 0)
				{
					// Detect added permissions with the shared process
					for (PackageInfo packageinfo : infos)
					{
						LinearLayout layout = new LinearLayout(getApplicationContext());
						addedPermissions.clear();
						addedPermissions.addAll(allRealPermissions);
						if (packageinfo.requestedPermissions != null)
						{
							for (int i = 0; i < packageinfo.requestedPermissions.length; ++i)
							{
								addedPermissions.remove(packageinfo.requestedPermissions[i]);
							}
						}
						if (addedPermissions.size() != 0)
						{
							layout = (LinearLayout) inflater.inflate(
								R.layout.package_description, null);
							TextView packageName, processName;
							LinearLayout permissions;
							packageName = (TextView) layout.findViewById(R.id.package_name);
							processName = (TextView) layout.findViewById(R.id.process_name);
							permissions = (LinearLayout) layout.findViewById(R.id.permissions);
							packageName.setText(packageinfo.packageName);
							processName.setText(packageinfo.applicationInfo.processName);

							find = true;
							builder.append(
								packageinfo.packageName).append(
								" (").append(
								packageinfo.applicationInfo.processName).append(
								")\n");
							for (String perm : addedPermissions)
							{
								if (perm.startsWith("android.permission"))
									perm = "..." + perm.substring(19);
								builder.append(
									"  ").append(
									perm).append(
									'\n');
								//Log.d(TAG, "+ " + perm);

								TextView permission = new TextView(getApplicationContext());
								permission.setTextAppearance(
									getApplicationContext(), R.style.value);
								permission.setText(perm);
								permissions.addView(permission);
							}
							builder.append("----\n\n");
							views.add(layout);

						}

					}
				}
			}
		}
		if (!find)
			builder.append("No side effet permissions.");
		return views;
	}
}