package com.desudesu.simpleftp;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.widget.*;
import java.net.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(15,23,42), PANEL=Color.rgb(30,41,59), TEXT=Color.rgb(241,245,249);
    private static final int MUTED=Color.rgb(148,163,184), BLUE=Color.rgb(59,130,246);
    private EditText user, pass, port;
    private TextView state, address;
    private Button toggle;
    private ImageButton peek;
    private CheckBox anonymous;
    private LinearLayout credentials;
    private SharedPreferences prefs;
    private boolean passwordVisible;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){render();handler.postDelayed(this,1000);}};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);prefs=getSharedPreferences("ftp",MODE_PRIVATE);setContentView(buildUi());
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},10);
        anonymous.setOnCheckedChangeListener((button,checked)->{prefs.edit().putBoolean("anonymous",checked).apply();updateCredentialVisibility();});
        peek.setOnClickListener(v->togglePasswordVisibility());toggle.setOnClickListener(v->toggleServer());render();
    }

    private View buildUi(){
        int pad=dp(24);ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(BG);scroll.setFillViewport(true);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(pad,dp(40),pad,pad);box.setBackgroundColor(BG);scroll.addView(box);
        TextView title=text("Simple FTP Server",28,TEXT);title.setTypeface(null,1);box.addView(title);
        TextView sub=text("Private, ad-free file transfer on your local network.",15,MUTED);sub.setPadding(0,dp(8),0,dp(28));box.addView(sub);
        state=text("Stopped",18,Color.rgb(248,113,113));state.setTypeface(null,1);box.addView(state);
        address=text("",17,Color.rgb(96,165,250));address.setPadding(0,dp(8),0,dp(20));address.setTextIsSelectable(true);box.addView(address);

        anonymous=new CheckBox(this);anonymous.setText("Anonymous access");anonymous.setTextColor(TEXT);anonymous.setTextSize(16);
        anonymous.setButtonTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{BLUE,MUTED}));
        anonymous.setChecked(prefs.getBoolean("anonymous",false));box.addView(anonymous,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView anonHint=text("Anyone on this network can connect without credentials.",13,MUTED);anonHint.setPadding(dp(4),0,0,dp(4));box.addView(anonHint);

        credentials=new LinearLayout(this);credentials.setOrientation(LinearLayout.VERTICAL);box.addView(credentials,new LinearLayout.LayoutParams(-1,-2));
        credentials.addView(label("Username"));user=field(prefs.getString("user","ftp"),false);credentials.addView(user);
        credentials.addView(label("Password"));LinearLayout passRow=new LinearLayout(this);passRow.setOrientation(LinearLayout.HORIZONTAL);
        pass=field(prefs.getString("pass","ftp"),true);pass.setLayoutParams(new LinearLayout.LayoutParams(0,dp(52),1));passRow.addView(pass);
        peek=new ImageButton(this);peek.setImageResource(android.R.drawable.ic_menu_view);peek.setColorFilter(TEXT);peek.setContentDescription("Show password");peek.setBackground(boxBackground(PANEL,8));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(52),dp(52));pp.setMargins(dp(8),0,0,0);passRow.addView(peek,pp);credentials.addView(passRow);

        box.addView(label("Port"));port=field(String.valueOf(prefs.getInt("port",2121)),false);port.setInputType(InputType.TYPE_CLASS_NUMBER);box.addView(port);
        toggle=new Button(this);toggle.setText("START SERVER");toggle.setTextColor(Color.WHITE);toggle.setTextSize(15);toggle.setTypeface(null,1);toggle.setBackgroundTintList(ColorStateList.valueOf(BLUE));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(56));bp.setMargins(0,dp(24),0,dp(16));box.addView(toggle,bp);
        TextView note=text("Files: /storage/emulated/0\nUse only on a trusted network. Standard FTP does not encrypt passwords or files.",13,MUTED);note.setLineSpacing(0,1.25f);box.addView(note);
        updateCredentialVisibility();return scroll;
    }

    private TextView label(String s){TextView v=text(s,13,Color.rgb(203,213,225));v.setPadding(0,dp(14),0,dp(4));return v;}
    private EditText field(String s,boolean secret){EditText e=new EditText(this);e.setText(s);e.setTextSize(17);e.setTextColor(TEXT);e.setSingleLine(true);e.setPadding(dp(12),0,dp(12),0);e.setBackground(boxBackground(PANEL,8));e.setInputType(secret?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT);e.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(52)));return e;}
    private GradientDrawable boxBackground(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),Color.rgb(51,65,85));return d;}
    private TextView text(String s,float n,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(n);v.setTextColor(c);return v;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}

    private void togglePasswordVisibility(){passwordVisible=!passwordVisible;int cursor=pass.getSelectionStart();pass.setTransformationMethod(passwordVisible?null:PasswordTransformationMethod.getInstance());if(cursor>=0)pass.setSelection(cursor);peek.setContentDescription(passwordVisible?"Hide password":"Show password");peek.setAlpha(passwordVisible?1f:.7f);}
    private void updateCredentialVisibility(){if(credentials!=null)credentials.setVisibility(anonymous.isChecked()?View.GONE:View.VISIBLE);}

    private void toggleServer(){
        if(FtpService.running){stopService(new Intent(this,FtpService.class));render();return;}
        if(Build.VERSION.SDK_INT>=30&&!Environment.isExternalStorageManager()){Toast.makeText(this,"Allow file access, then tap Start again",Toast.LENGTH_LONG).show();try{startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));}return;}
        int p;try{p=Integer.parseInt(port.getText().toString());if(p<1024||p>65535)throw new Exception();}catch(Exception e){Toast.makeText(this,"Choose a port from 1024 to 65535",Toast.LENGTH_LONG).show();return;}
        boolean anon=anonymous.isChecked();String u=user.getText().toString().trim(),pw=pass.getText().toString();if(!anon&&(u.isEmpty()||pw.isEmpty())){Toast.makeText(this,"Username and password cannot be empty",Toast.LENGTH_LONG).show();return;}
        prefs.edit().putString("user",u).putString("pass",pw).putInt("port",p).putBoolean("anonymous",anon).apply();
        Intent i=new Intent(this,FtpService.class);i.putExtra("user",u);i.putExtra("pass",pw);i.putExtra("port",p);i.putExtra("anonymous",anon);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);handler.postDelayed(this::render,250);
    }

    private void render(){boolean on=FtpService.running;state.setText(on?"Server running":"Server stopped");state.setTextColor(on?Color.rgb(74,222,128):Color.rgb(248,113,113));address.setText(on?"ftp://"+localIp()+":"+prefs.getInt("port",2121):"Tap Start when connected to Wi-Fi");toggle.setText(on?"STOP SERVER":"START SERVER");anonymous.setEnabled(!on);user.setEnabled(!on);pass.setEnabled(!on);peek.setEnabled(!on);port.setEnabled(!on);updateCredentialVisibility();}
    static String localIp(){try{for(NetworkInterface n:Collections.list(NetworkInterface.getNetworkInterfaces()))for(InetAddress a:Collections.list(n.getInetAddresses()))if(!a.isLoopbackAddress()&&a instanceof Inet4Address)return a.getHostAddress();}catch(Exception ignored){}return"device-ip";}
    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
