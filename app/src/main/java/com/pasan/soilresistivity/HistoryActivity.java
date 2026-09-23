package com.pasan.soilresistivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

public class HistoryActivity extends Activity {
    private static final String PREFS="soil_resistivity_data";
    private LinearLayout list;
    private JSONArray history;

    @Override protected void onCreate(Bundle state){super.onCreate(state);buildScreen();loadHistory();}

    private void buildScreen(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(16),dp(16),dp(24));scroll.addView(root);
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);Button back=button("Back");back.setOnClickListener(v->finish());TextView title=text("Survey History",25,Color.rgb(11,74,45));title.setTypeface(null,1);header.addView(back,new LinearLayout.LayoutParams(dp(90),ViewGroup.LayoutParams.WRAP_CONTENT));header.addView(title,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));root.addView(header);
        root.addView(text("Open an earlier survey to view its data and graph.",14,Color.DKGRAY));list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);setContentView(scroll);
    }

    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private void loadHistory(){
        list.removeAllViews();try{history=new JSONArray(prefs().getString("history","[]"));}catch(Exception e){history=new JSONArray();}
        if(history.length()==0){list.addView(text("No saved surveys yet. Return and press Save after entering data.",16,Color.GRAY));return;}
        for(int position=history.length()-1;position>=0;position--){
            try{JSONObject record=history.getJSONObject(position);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setBackgroundColor(Color.rgb(235,247,240));TextView name=text(record.optString("name","Unnamed survey"),19,Color.rgb(11,74,45));name.setTypeface(null,1);card.addView(name);int readings=record.optJSONArray("points")==null?0:record.optJSONArray("points").length();String date=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(record.optLong("savedAt",0)));String method="SCHLUMBERGER".equals(record.optString("arrayType"))?"Schlumberger":"Wenner (alpha)";card.addView(text(method+" • "+date+" • "+readings+" readings",14,Color.DKGRAY));LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);Button open=button("Open");open.setOnClickListener(v->openRecord(record));Button delete=button("Delete");final int index=position;delete.setOnClickListener(v->confirmDelete(index));buttons.addView(open,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));buttons.addView(delete,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));card.addView(buttons);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);cp.setMargins(0,6,0,6);list.addView(card,cp);}catch(Exception ignored){}
        }
    }

    private void openRecord(JSONObject record){Intent data=new Intent();data.putExtra("survey_json",record.toString());setResult(RESULT_OK,data);finish();}
    private void confirmDelete(int index){new AlertDialog.Builder(this).setTitle("Delete saved survey?").setMessage("This removes this survey from History.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{history.remove(index);prefs().edit().putString("history",history.toString()).apply();loadHistory();}).show();}
    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setPadding(4,8,4,8);return v;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);return b;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
