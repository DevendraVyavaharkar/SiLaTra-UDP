package team_silatra.camera;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by ABC on 06-Mar-18.
 */

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences sharedpreferences;
    Button btn;
    EditText edt;

    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String IP1 = "ip1Key";
    public static final String IP2 = "ip2Key";
    public static final String IP3 = "ip3Key";
    public static final String IP4 = "ip4Key";
    public static final String Port = "portKey";
    public static final String DirectConnection = "directConnectionKey";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);   //Shared Preferences

        updateTextFromFetchedPreferences();
        handleCheckBoxEffect();

        btn=(Button)findViewById(R.id.applyChangesBtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            String ipAddr1 = appendZero(((EditText) findViewById(R.id.serverEditText1)).getText().toString());
            String ipAddr2 = appendZero(((EditText) findViewById(R.id.serverEditText2)).getText().toString());
            String ipAddr3 = appendZero(((EditText) findViewById(R.id.serverEditText3)).getText().toString());
            String ipAddr4 = appendZero(((EditText) findViewById(R.id.serverEditText4)).getText().toString());
            int portNumber = Integer.parseInt("0"+(((EditText)findViewById(R.id.portNumEditText)).getText().toString()));
            boolean directConnectionEnable = ((CheckBox) findViewById(R.id.directConnCheckBox)).isChecked();

            SharedPreferences.Editor editor = sharedpreferences.edit();

            editor.putString(IP1,ipAddr1);
            editor.putString(IP2,ipAddr2);
            editor.putString(IP3,ipAddr3);
            editor.putString(IP4,ipAddr4);
            editor.putInt(Port,portNumber);
            editor.putBoolean(DirectConnection,directConnectionEnable);
            editor.apply();

            finish();
            }
        });

        findViewById(R.id.resetBtn).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                sharedpreferences.edit().clear().commit(); //Reference: https://stackoverflow.com/a/3687333/5370202
                Toast.makeText(getApplicationContext(), "Reset preferences to defaults", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

    }

    public void updateTextFromFetchedPreferences(){
        if(sharedpreferences.contains(IP1)){           // IP Address block 1
            edt=(EditText)findViewById(R.id.serverEditText1);
            edt.setText(sharedpreferences.getString(IP1,null));
        }

        if(sharedpreferences.contains(IP2)){           // IP Address block 2
            edt=(EditText)findViewById(R.id.serverEditText2);
            edt.setText(sharedpreferences.getString(IP2,null));
        }

        if(sharedpreferences.contains(IP3)){           // IP Address block 3
            edt=(EditText)findViewById(R.id.serverEditText3);
            edt.setText(sharedpreferences.getString(IP3,null));
        }

        if(sharedpreferences.contains(IP4)){           // IP Address block 4
            edt=(EditText)findViewById(R.id.serverEditText4);
            edt.setText(sharedpreferences.getString(IP4,null));
        }

        if(sharedpreferences.contains(Port)){         // Port Number
            edt=(EditText)findViewById(R.id.portNumEditText);
            edt.setText(Integer.toString(sharedpreferences.getInt(Port,0)));
        }

        if(sharedpreferences.contains(DirectConnection)){           // Direct Connection Boolean
            CheckBox checkBox = (CheckBox)findViewById(R.id.directConnCheckBox);
            checkBox.setChecked(new Boolean(sharedpreferences.getBoolean(DirectConnection,false)));
        }
    }

    public void handleCheckBoxEffect(){
        boolean checked = ((CheckBox) findViewById(R.id.directConnCheckBox)).isChecked();

        if(checked){
            findViewById(R.id.portNumEditText).setEnabled(true);
        }
        else{
            findViewById(R.id.portNumEditText).setEnabled(false);
        }
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        handleCheckBoxEffect();
    }

    public String appendZero(String s)
    {
        if(s.length()==0)
            s="000";
        else if(s.length()==1)
            s="00"+s;
        else if(s.length()==2)
            s="0"+s;
        return s;
    }
}