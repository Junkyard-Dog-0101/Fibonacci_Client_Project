package team_1887.fibonacci_client_project;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import team_1887.fibonacci_common_library_project.FibonacciRequest;
import team_1887.fibonacci_common_library_project.FibonacciResponse;
import team_1887.fibonacci_common_library_project.IFibonacciService;

public class MainActivity extends Activity implements View.OnClickListener, ServiceConnection {

    private static final String TAG = "MainActivity";
    private EditText input;
    private Button button;
    private RadioGroup type;
    private TextView output;
    private IFibonacciService service;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        input = (EditText) findViewById(R.id.input);
        button = (Button) findViewById(R.id.button);
        type = (RadioGroup) findViewById(R.id.type);
        output = (TextView) findViewById(R.id.output);
        button.setOnClickListener(this);
        button.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!super.bindService(new Intent(IFibonacciService.class.getName()), this, BIND_AUTO_CREATE)) {
            Log.e("MainActivity", "Failed to bind to service");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        super.unbindService(this);
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        this.service = IFibonacciService.Stub.asInterface(service);
        this.button.setEnabled(true);
    }

    public void onServiceDisconnected(ComponentName name) {
        this.service = null;
        this.button.setEnabled(false);
    }

    public void onClick(View view) {
        final long n;
        String s = this.input.getText().toString();
        if (TextUtils.isEmpty(s)) {
            return;
        }
        n = Long.parseLong(s);

        final FibonacciRequest.Type type;
        switch (MainActivity.this.type.getCheckedRadioButtonId()) {
            case R.id.type_fib_jr:
                type = FibonacciRequest.Type.RECURSIVE_JAVA;
                break;
            case R.id.type_fib_ji:
                type = FibonacciRequest.Type.ITERATIVE_JAVA;
                break;
            case R.id.type_fib_nr:
                type = FibonacciRequest.Type.RECURSIVE_NATIVE;
                break;
            case R.id.type_fib_ni:
                type = FibonacciRequest.Type.ITERATIVE_NATIVE;
                break;
            default:
                return;
        }
        final FibonacciRequest request = new FibonacciRequest(n, type);

        final ProgressDialog dialog = ProgressDialog.show(this, "", super.getText(R.string.progress_text), true);
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    long totalTime = SystemClock.uptimeMillis();
                    FibonacciResponse response = MainActivity.this.service.fib(request);
                    totalTime = SystemClock.uptimeMillis() - totalTime;
                    return String.format(
                            "fibonacci(%d)=%d\nin %d ms\n(+ %d ms)", n,
                            response.getResult(), response.getTimeInMillis(),
                            totalTime - response.getTimeInMillis());
                } catch (RemoteException e) {
                    Log.e("MainActivity", "Failed to communicate with the service", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                dialog.dismiss();
                if (result == null) {
                    Toast.makeText(MainActivity.this, R.string.fib_error,
                            Toast.LENGTH_SHORT).show();
                } else {
                    MainActivity.this.output.setText(result);
                }
            }
        }.execute();
    }
}