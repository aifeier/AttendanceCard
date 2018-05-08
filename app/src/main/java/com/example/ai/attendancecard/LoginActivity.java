package com.example.ai.attendancecard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor>, View.OnClickListener {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "test:123456", "test1:123456", "test2:123456"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;


    //设置
    private Button startTimeBtn, endTimeBtn;
    private ListView listView;
    private BaseAdapter adapter;
    private List<ScanResult> currentScanResultList;
    private int currentSelectedWifiPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        initView();
        mEmailView.setText("test");
        mPasswordView.setText("123456");
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (WifiManager.WIFI_STATE_DISABLED == wifiManager.getWifiState()) {
            Toast.makeText(this, "开启WIFI将提高定位精度", Toast.LENGTH_SHORT).show();
            try {
                wifiManager.setWifiEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initView() {
        currentScanResultList = new ArrayList<>();
        Variable.companyWIFIs = new ArrayList<>();
        Variable.companyWIFIs.add(new MWifiInfo("upsoft_5g", "30:fc:68:18:ac:20"));
        Variable.companyWIFIs.add(new MWifiInfo("upsoft_yt", "30:fc:68:18:ac:1e"));
        startTimeBtn = findViewById(R.id.start_time);
        endTimeBtn = findViewById(R.id.end_time);
        listView = findViewById(R.id.listview);
        startTimeBtn.setOnClickListener(this);
        endTimeBtn.setOnClickListener(this);
        findViewById(R.id.setCompany).setOnClickListener(this);
        findViewById(R.id.add_wifi).setOnClickListener(this);

        startTimeBtn.setText(Variable.startTime);
        endTimeBtn.setText(Variable.endTime);

        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return Variable.companyWIFIs.size();
            }

            @Override
            public MWifiInfo getItem(int position) {
                return Variable.companyWIFIs.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder viewHolder;
                if (null == convertView) {
                    convertView = getLayoutInflater().inflate(R.layout.layout_wifi_item, parent, false);
                    viewHolder = new ViewHolder();
                    viewHolder.ssid = convertView.findViewById(R.id.item_ssid);
                    viewHolder.del = convertView.findViewById(R.id.item_del);
                    viewHolder.del.setOnClickListener(LoginActivity.this);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                viewHolder.del.setTag(position);
                viewHolder.ssid.setText(getItem(position).SSID);
                return convertView;
            }

            class ViewHolder {
                TextView ssid;
                Button del;
            }
        };
        listView.setAdapter(adapter);
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }
// else if (!isEmailValid(email)) {
//            mEmailView.setError(getString(R.string.error_invalid_email));
//            focusView = mEmailView;
//            cancel = true;
//        }

        if (!Utils.isNetWorkConnected(getApplicationContext())) {
            Toast.makeText(LoginActivity.this, "当前网络不可用，请联网后重试", Toast.LENGTH_LONG).show();
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    private int checkHourAndMinute(int srcHourOfDay, int srcMinute, int dstHourOfDay, int dstMinute) {
        Calendar srcCalendar = Calendar.getInstance();
        Calendar dstCalendar = (Calendar) srcCalendar.clone();
        srcCalendar.set(Calendar.HOUR_OF_DAY, srcHourOfDay);
        srcCalendar.set(Calendar.MINUTE, srcMinute);

        dstCalendar.set(Calendar.HOUR_OF_DAY, dstHourOfDay);
        dstCalendar.set(Calendar.MINUTE, dstMinute);
        return srcCalendar.compareTo(dstCalendar);

    }

    @Override
    public void onClick(View v) {
        Calendar calendar = Calendar.getInstance();
        switch (v.getId()) {
            case R.id.start_time:
                new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String[] time = Variable.endTime.split(":");
                        if (checkHourAndMinute(hourOfDay, minute, Integer.valueOf(time[0]), Integer.valueOf(time[1])) >= 0) {
                            Toast.makeText(LoginActivity.this, "上班时间只能小于下班时间", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Variable.startTime = hourOfDay + ":" + minute;
                        startTimeBtn.setText(hourOfDay + ":" + minute);
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
                break;
            case R.id.end_time:
                new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String[] time = Variable.startTime.split(":");
                        if (checkHourAndMinute(hourOfDay, minute, Integer.valueOf(time[0]), Integer.valueOf(time[1])) <= 0) {
                            Toast.makeText(LoginActivity.this, "下班班时间只能大于上班时间", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Variable.endTime = hourOfDay + ":" + minute;
                        endTimeBtn.setText(hourOfDay + ":" + minute);
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
                break;
            case R.id.item_del:
                int p = (int) v.getTag();
                if (Variable.companyWIFIs.size() > p) {
                    Variable.companyWIFIs.remove(p);
                    adapter.notifyDataSetChanged();
                }
                break;
            case R.id.add_wifi:
                List<ScanResult> temp = Utils.getWifiList(getApplicationContext());
                currentScanResultList.clear();
                for (ScanResult result : temp) {
                    if (Variable.companyWIFIs.contains(new MWifiInfo(result.SSID, result.BSSID)))
                        continue;
                    currentScanResultList.add(result);
                }
                if (currentScanResultList.size() <= 0) {
                    Toast.makeText(LoginActivity.this, "没有可供选择的WIFI", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] strings = new String[currentScanResultList.size()];
                for (int i = 0; i < currentScanResultList.size(); i++) {
                    ScanResult result = currentScanResultList.get(i);
                    strings[i] = result.SSID;
                }
                new AlertDialog.Builder(this)
                        .setTitle("选择要添加的WIFI")
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (currentSelectedWifiPosition < currentScanResultList.size()) {
                                    ScanResult result = currentScanResultList.get(currentSelectedWifiPosition);
                                    MWifiInfo wifiInfo = new MWifiInfo(result.SSID, result.BSSID);
                                    if (!Variable.companyWIFIs.contains(wifiInfo)) {
                                        Variable.companyWIFIs.add(wifiInfo);
                                        adapter.notifyDataSetChanged();
                                        Toast.makeText(LoginActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                                        dialog.cancel();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "该WIFI已添加，请重新选择", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        })
                        .setSingleChoiceItems(strings, 0, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                currentSelectedWifiPosition = which;
                            }
                        })
                        .show();
                break;
            case R.id.setCompany:
                startActivity(new Intent(this, SetCompanyActivity.class));
                break;
        }
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, AttendanceMapActivity.class));
//                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

