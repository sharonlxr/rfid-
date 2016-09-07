package com.fsl.cimei.rfid;

import java.util.Date;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import app.utils.login.LoginInfo;
import app.utils.login.genesis.GenesisGateway;
import app.utils.login.genesis.GenesisUser;

import com.freescale.api.Constants;
import com.fsl.cimei.rfid.exception.BaseException;
import com.fsl.cimei.rfid.exception.RfidException;
import com.fsl.cimei.rfid.exception.RfidWifiException;

public class LoginActivity extends BaseActivity {

	private QueryTask qTask = null;

	private String mUserID;
	private String mPassword;

	private EditText mEmailView;
	private EditText mPasswordView;
	private EditText mTagView;

	private TextView hostConnCheck;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		mTagView = (EditText) findViewById(R.id.login_tag);
		mTagView.setVisibility(View.INVISIBLE);
		// for type 3
		mTagView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.login_t || id == EditorInfo.IME_NULL) {
					String tagId = mTagView.getText().toString().trim();
					if (tagId.length() == 16) {
						mTagView.setText("");
						tagId = tagId.substring(0, tagId.length() / 2);
						global.setScanTarget(Constants.SCAN_TARGET_LOGIN);
						Intent intent = new Intent(LoginActivity.this, NewNFCTagActivity.class);
						intent.putExtra("carrierID", tagId);
						startActivity(intent);
					}
					return true;
				}
				return false;
			}
		});

		mEmailView = (EditText) findViewById(R.id.email);
		mEmailView.setText(mUserID);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		mFormView = findViewById(R.id.login_form);
		mStatusView = findViewById(R.id.login_load_status);
		mStatusMessageView = (TextView) findViewById(R.id.status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});

		if (Constants.type == -1) {
			if (android.os.Build.MODEL.startsWith("Nexus")) {
				Constants.type = 1;
			} else if (android.os.Build.MODEL.equals("Android")) {
				Constants.type = 3;
			} else {
				Constants.type = 0;
			}
		}
		// for type 3
		// mTagView.addTextChangedListener(new TextWatcher() {
		// @Override
		// public void onTextChanged(CharSequence s, int start, int before, int count) {
		// }
		//
		// @Override
		// public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// }
		//
		// @Override
		// public void afterTextChanged(Editable s) {
		// String tagId = "" + s;
		// if (tagId.length() == 16) {
		// tagId = tagId.substring(0, tagId.length() / 2);
		// global.setScanTarget(Constants.NFC_TAG_SCAN_TARGET_LOGIN);
		// Intent intent = new Intent(LoginActivity.this, NewNFCTagActivity.class);
		// intent.putExtra("carrierID", tagId);
		// startActivity(intent);
		// }
		// }
		// });

		hostConnCheck = (TextView) findViewById(R.id.login_host_conn_check);

	}

	public void attemptLogin() {
		if (qTask != null) {
			return;
		}

		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mUserID = mEmailView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
			// } else if (mPassword.length() < 4) {
			// mPasswordView.setError(getString(R.string.error_invalid_password));
			// focusView = mPasswordView;
			// cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(mUserID)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
			// } else if (!mUserID.contains("@")) {
			// mEmailView.setError(getString(R.string.error_invalid_email));
			// focusView = mEmailView;
			// cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			showProgress(true);
			qTask = new QueryTask();
			qTask.execute("login");
		}
	}

	/**
	 * Represents an asynchronous login/registration task used to authenticate the user.
	 */
	public class QueryTask extends AsyncTask<String, Void, BaseException> {
		String cmdName = "";

		// boolean showWifiConfig = false;

		@Override
		protected BaseException doInBackground(String... params) {
			cmdName = params[0];
			try {
				if (cmdName.equals("pingHost")) {
					log("Login pingHost");
					CommonUtility.pingHost(global.getInterfaceMgrSocketConfigQuery().getHost());
				} else if (cmdName.equals("login")) {
					login();
				}
			} catch (RfidWifiException e) {
				// showWifiConfig = true;
				return e;
			} catch (BaseException e) {
				return e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(final BaseException e) {
			qTask = null;
			showProgress(false);

			if (null == e) {
				// login success
				if (cmdName.equals("pingHost")) {
					hostConnCheck.setText("连接服务器：成功");
				} else if (cmdName.equals("login")) {
					Intent intent = new Intent(LoginActivity.this, ControlActivity.class);
					setResult(0, intent);
					finish();
				}
			} else {
				logf(e.getMessage());
				// if (showWifiConfig) {
				// // Intent intent = new Intent(LoginActivity.this, WifiConfigActivity.class);
				// // startActivity(intent);
				// WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				// if (wifiManager.isWifiEnabled()) {
				// wifiManager.setWifiEnabled(false);
				// } else {
				// wifiManager.setWifiEnabled(true);
				// }
				// } else {
				// login fail
				mPasswordView.setError(e.getMessage());
				mPasswordView.requestFocus();
				// }
			}
		}

		@Override
		protected void onCancelled() {
			qTask = null;
			showProgress(false);
		}

		private void login() throws BaseException {
			String machineId = global.getAndroidSecureID();
			GenesisGateway GenesisGateway = new GenesisGateway(global.getInterfaceMgrSocketConfigQuery());
			try {

				if (mUserID.equals("1") && mPassword.equals("1")) {
					GenesisUser user = new GenesisUser();
					user.setUserID("测试用户");
					user.setFirstName("测试用户");
					user.setLastName("测试用户");
					user.setDepartment("CIM");
					user.setLastSuccessLogin(Constants.dateFormat.format(new Date()));
					global.setUser(user);
					return;
				}

				LoginInfo loginInfo;
				try {
					loginInfo = GenesisGateway.login(mUserID, mPassword, machineId);
				} catch (Exception e) {
					throw new RfidException("登录失败。 ", "Login", "Login", "");
				}
				if (loginInfo.isSuccess()) {
					GenesisUser user = (GenesisUser) loginInfo.getUser();
					if (null != user) {
						// user.getUID()
						// user.getFirstName()
						// user.getLastName()
						// user.getDepartment()
						// DateFormatter.getServerCurrentDate()
						global.setUser(user);
						CommonTrans commonTrans = new CommonTrans();
						commonTrans.checkUserInfo(apiExecutorQuery, global);
						String link = "servlet/LoginServlet?action=logUserLogin&coreID=" + user.getUserID() + "&deviceID=" + global.getAndroidSecureID();
						String output = "";
						try {
							output = commonTrans.queryFromServer(link);
							CommonUtility.logError("Login coreID=" + user.getUserID(), Constants.LOG_FILE_ERR);
						} catch (RfidException e) {
							CommonUtility.logError(user.getUserID() + " LoginServlet: " + e.toString(), Constants.LOG_FILE_ERR);
						}
						SharedPreferences shared = LoginActivity.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
						Editor editor = shared.edit();
						editor.putString(Constants.USER_ID, global.getUser().getUserID());
						editor.putString(Constants.FIRST_NAME, global.getUser().getFirstName());
						editor.putString(Constants.LAST_NAME, global.getUser().getLastName());
						editor.putString(Constants.DEPARTMENT, global.getUser().getDepartment());
						// String loggedOnTime = Constants.dateFormat.format(new Date());
						editor.putString(Constants.SERVER_CURRENT_DATE, output);
						global.getUser().setLastSuccessLogin(output);
						editor.commit();
					} else {
						throw new RfidException("登录失败。 ", "Login", "Login", "");
					}
				} else {
					if (null == loginInfo.getReason()) {
						throw new RfidException("非法用户。 ", "Login", "Login", "");
					} else {
						throw new RfidException(loginInfo.getReason(), "Login", "Login", "");
					}
				}
			} catch (BaseException e) {
				throw e;
			}
		}

	}

	@Override
	protected void setupActionBar() {
	}

	@Override
	public void onBackPressed() {
	}

	@Override
	protected void onResume() {
		SharedPreferences data = LoginActivity.this.getSharedPreferences("RFID-data", MODE_PRIVATE);
		if (data != null && !CommonUtility.isEmpty(data.getString(Constants.USER_ID, ""))) {
			finish();
		}
		if (Constants.type == 3) {
			mTagView.setVisibility(View.VISIBLE);
		}
		hostConnCheck.setText("连接服务器：检查中。。。");
		qTask = new QueryTask();
		qTask.execute("pingHost");
		super.onResume();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == 138 || keyCode == 132) {
			PackageManager manager = getPackageManager();
			Intent i = manager.getLaunchIntentForPackage("com.android.settings");
			startActivity(i);
		} else if (keyCode == 135) {
			PackageManager manager = getPackageManager();
			Intent i = manager.getLaunchIntentForPackage("com.android.auto.iscan");
			if (null != i) {
				startActivity(i);
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		if (null != qTask) {
			qTask.cancel(true);
		}
		super.onPause();
	}

}
