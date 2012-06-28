package fr.prados.add.permission;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class OptionalPrivilegedActivity extends Activity
{
	private EditText mPhoneNumber;
	private Button mSendSms;
	private TextView mStatus;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mPhoneNumber=(EditText)findViewById(R.id.phoneNumber);
		mSendSms=(Button)findViewById(R.id.sendSms);
		mStatus=(TextView)findViewById(R.id.status);
		
	}
	@Override
	protected void onResume()
	{
		super.onResume();
		// Check if I have the privilege to send SMS
		if (checkPermission(Manifest.permission.SEND_SMS, Process.myPid(), Process.myUid())
				==PackageManager.PERMISSION_DENIED)
		{
			mSendSms.setEnabled(false);
			mStatus.setText(R.string.no_privilege);
		}
		else
			mStatus.setText("");
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
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}
}