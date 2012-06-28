package fr.prados.android.hidden.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class RealPermissionsActivity extends Activity
{
	private static final String[] sFilter=
		new String[]{
		"system",
		"android.process.media",
		"android.process.acore",
		"com.android.phone",
		"com.google.process.gapps",
		};
	private static final String TAG="TAG";
	private TextView mConsole;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mConsole=(TextView)findViewById(R.id.console);
	}

	@Override
    protected void onResume()
    {
    	super.onResume();
    	mConsole.setText(detectRealPrivileged());
    }

	private StringBuilder detectRealPrivileged()
	{
		StringBuilder builder=new StringBuilder();
		builder.append("Some applications may share a process and can have some hidden permissions by side effect.\n\n");
		builder.append("List all hidden permissions:\n----\n");
		HashSet<String> filter=new HashSet<String>();
		for (int i=0;i<sFilter.length;++i)
			filter.add(sFilter[i]);
		
		PackageManager pm=getPackageManager();
    	List<ApplicationInfo> apps=pm.getInstalledApplications(PackageManager.GET_META_DATA);

    	// Get all packages info
    	List<PackageInfo> packagesInfos=new ArrayList<PackageInfo>();
    	for (ApplicationInfo appInfo:apps)
    	{
    		try
    		{
    			if (filter.contains(appInfo.processName)) continue;
    			packagesInfos.add(pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS|PackageManager.GET_SIGNATURES));
    		}
    		catch (NameNotFoundException e)
    		{
    			// Ignore
    		}
    		
    	}
    	
    	
    	// Select applications who share process
    	HashMap<String,List<PackageInfo>> union=new HashMap<String,List<PackageInfo>>();
    	for (PackageInfo packageInfo:packagesInfos)
    	{
    		List<PackageInfo> shareProcess=union.get(packageInfo.applicationInfo.processName);
    		if (shareProcess==null)
    		{
    			shareProcess=new ArrayList<PackageInfo>();
    			union.put(packageInfo.applicationInfo.processName, shareProcess);
    		}
    		shareProcess.add(packageInfo);
    	}
    	
    	// Merge all real permissions
    	boolean find=false;
		HashSet<String> allRealPermissions=new HashSet<String>();
		HashSet<String> addedPermissions=new HashSet<String>();
    	for (List<PackageInfo> infos:union.values())
    	{
    		if (infos.size()>1)
    		{
    			// Get all permissions
    			allRealPermissions.clear();
    			for (PackageInfo packageinfo:infos)
    			{
					if (packageinfo.requestedPermissions!=null)
					{
						for (int i=0;i<packageinfo.requestedPermissions.length;++i)
						{
							allRealPermissions.add(packageinfo.requestedPermissions[i]);
						}
					}
    			}
    			if (allRealPermissions.size()!=0)
    			{
    				// Detect added permissions with the shared process
	    			for (PackageInfo packageinfo:infos)
	    			{
	    				addedPermissions.clear();
	    				addedPermissions.addAll(allRealPermissions);
	    				if (packageinfo.requestedPermissions!=null)
	    				{
		    				for (int i=0;i<packageinfo.requestedPermissions.length;++i)
		    				{
		    					addedPermissions.remove(packageinfo.requestedPermissions[i]);
		    				}
	    				}
	    				if (addedPermissions.size()!=0)
	    				{
	    					find=true;
		    				builder.append(pm.getApplicationLabel(packageinfo.applicationInfo))
//		    					.append(" (process ").append(packageinfo.applicationInfo.processName).append(")")
		    					.append("\n");
		    				for (String perm:addedPermissions)
		    				{
		    					if (perm.startsWith("android.permission"))
		    						perm="..."+perm.substring(19);
		    					builder.append("  ").append(perm).append('\n');
		    					Log.d(TAG,"+ "+perm);
		    				}
		    				builder.append("----\n\n");
	    				}
	    			}
    			}
    		}
    	}
    	if (!find)
    		builder.append("No hidden permission.");
    	return builder;
	}
}