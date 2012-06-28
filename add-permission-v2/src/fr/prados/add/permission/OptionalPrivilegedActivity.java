package fr.prados.add.permission;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class OptionalPrivilegedActivity extends Activity
{
	private static final String TAG="PRIV";
	
	private EditText mPhoneNumber;

	private Button mSendSms;

	private Button mAddPriv;

	private TextView mStatus;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mPhoneNumber = (EditText) findViewById(R.id.phoneNumber);
		mSendSms = (Button) findViewById(R.id.sendSms);
		mAddPriv = (Button) findViewById(R.id.addPriv);
		mStatus = (TextView) findViewById(R.id.status);

	}

	@Override
	protected void onResume()
	{
		super.onResume();
		// Check if I have the privilege to send SMS
		if (checkPermission(
			Manifest.permission.SEND_SMS, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_DENIED)
		{
			mPhoneNumber.setEnabled(false);
			mSendSms.setEnabled(false);
			mAddPriv.setVisibility(View.VISIBLE);
			mStatus.setText(R.string.no_permission);
		}
		else
		{
			mPhoneNumber.setEnabled(true);
			mSendSms.setEnabled(true);
			mAddPriv.setVisibility(View.GONE);
			mStatus.setText(R.string.with_permission);
		}
	}

	public void sendSms(View view)
	{
		try
		{
			SmsManager.getDefault().sendTextMessage(
				mPhoneNumber.getText().toString(), null, "Hello", null, null);
			Toast.makeText(
				this, "Sms sent", Toast.LENGTH_LONG).show();
		}
		catch (IllegalArgumentException e)
		{
			Toast.makeText(
				this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}

	public void addPrivilege(View view)
	{
		startActivity(getInstallIntent("sms"));
	}
	private Intent getInstallIntent(String module)
	{
		
		if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0)==0)
		{
			Log.d(TAG,"Use market");
			String app=getPackageName()+"."+module;
	    	return new Intent(
	    		Intent.ACTION_VIEW,
	    		Uri.parse("market://details?id="+app))
	    		.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		}
		else
		{
			Log.d(TAG,"Use inner apk");
			final Uri uri=copyAssets(module);
			assert (uri!=null);
			
			final Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(uri, "application/vnd.android.package-archive");
			return intent;
		}
	}

	private Uri copyAssets(String module)
	{
		AssetManager assetManager = getAssets();
		InputStream in = null;
		OutputStream out = null;
		try
		{
			final String name=module+".apk";
			in = assetManager.open(name);
			out = openFileOutput(name, Context.MODE_WORLD_READABLE);;
			final byte[] buffer = new byte[4096];
			int read;
			while ((read = in.read(buffer)) != -1)
			{
				out.write(buffer, 0, read);
			}
			return Uri.fromFile(getFileStreamPath(name));
		}
		catch (IOException e)
		{
			Log.e(TAG, e.getMessage());
			return null;
		}
		finally
		{
			try
			{
				if (out!=null) out.close();
				if (in!=null) in.close();
			}
			catch (IOException e)
			{
				// Ignore
			}
		}
	}
}