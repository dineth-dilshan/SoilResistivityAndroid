package com.pasan.soilresistivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int CREATE_PDF=40, CREATE_CSV=41, OPEN_HISTORY=42;
    private static final String PREFS="soil_resistivity_data";
    private final List<SurveyPoint> points=new ArrayList<>();
    private final DecimalFormat number=new DecimalFormat("0.00##");
    private EditText projectName,mnInput,abInput,rInput;
    private LinearLayout rows,layerRows;
    private ResistivityGraphView graph;
    private Spinner layerCount,arrayTypeSpinner;
    private TextView errorText;
    private Button addButton;
    private LayerModel model;
    private int editingIndex=-1;
    private String historyId=null;

    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(buildScreen());loadDraft();focusMn();}
    @Override protected void onPause(){super.onPause();saveDraft();}

    private View buildScreen(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=column();root.setPadding(dp(16),dp(16),dp(16),dp(30));scroll.addView(root);
        TextView title=text("Soil Resistivity Survey",26,Color.rgb(11,74,45));title.setTypeface(null,1);root.addView(title);
        arrayTypeSpinner=new Spinner(this);arrayTypeSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Wenner (alpha) — offline 1-D inversion"}));root.addView(arrayTypeSpinner,matchWrap());
        projectName=input("Survey name",false);root.addView(projectName,matchWrap());
        root.addView(text("Enter a measurement",20,Color.rgb(11,74,45)));
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.HORIZONTAL);mnInput=input("MN (m)",true);abInput=input("a (m)",true);rInput=input("ρa (Ωm)",true);form.addView(mnInput,weighted());form.addView(abInput,weighted());form.addView(rInput,weighted());root.addView(form);
        arrayTypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){updateMethodInputs();refresh();}});
        addButton=button("Add reading");addButton.setOnClickListener(v->addOrUpdateReading());root.addView(addButton,matchWrap());
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button sample=button("Sample");sample.setOnClickListener(v->loadSample());Button save=button("Save");save.setOnClickListener(v->saveToHistory());Button history=button("History");history.setOnClickListener(v->startActivityForResult(new Intent(this,HistoryActivity.class),OPEN_HISTORY));Button clear=button("Clear");clear.setOnClickListener(v->confirmClear());
        actions.addView(sample,weighted());actions.addView(save,weighted());actions.addView(history,weighted());actions.addView(clear,weighted());root.addView(actions);
        root.addView(text("Readings — tap a row to edit",20,Color.rgb(11,74,45)));
        HorizontalScrollView tableScroll=new HorizontalScrollView(this);rows=column();tableScroll.addView(rows,new ViewGroup.LayoutParams(dp(650),ViewGroup.LayoutParams.WRAP_CONTENT));root.addView(tableScroll,matchWrap());
        root.addView(text("Resistivity graph (logarithmic)",20,Color.rgb(11,74,45)));graph=new ResistivityGraphView(this);root.addView(graph,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(330)));
        LinearLayout inversion=new LinearLayout(this);inversion.setOrientation(LinearLayout.HORIZONTAL);layerCount=new Spinner(this);layerCount.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"2 layers","3 layers","4 layers","5 layers"}));layerCount.setSelection(1);Button invert=button("Run layer fit");invert.setOnClickListener(v->runInversion());inversion.addView(layerCount,weighted());inversion.addView(invert,weighted());root.addView(inversion);
        errorText=text("Black: measured   Red: fitted   Blue: layer model",14,Color.DKGRAY);root.addView(errorText);layerRows=column();root.addView(layerRows,matchWrap());
        LinearLayout exports=new LinearLayout(this);exports.setOrientation(LinearLayout.HORIZONTAL);Button pdf=button("Export PDF");pdf.setOnClickListener(v->beginExport(true));Button csv=button("Export Excel CSV");csv.setOnClickListener(v->beginExport(false));exports.addView(pdf,weighted());exports.addView(csv,weighted());root.addView(exports);
        root.addView(text("Enter apparent resistivity (ρa) directly, as shown by IPI2Win.",13,Color.DKGRAY));refresh();return scroll;
    }

    private ArrayType arrayType(){return ArrayType.WENNER;}
    private void updateMethodInputs(){if(mnInput==null)return;mnInput.setVisibility(View.GONE);abInput.setHint("a = AB/3 (m)");rInput.setHint("ρa (Ωm)");}

    private void addOrUpdateReading(){
        try{
            double spacing=Double.parseDouble(abInput.getText().toString().trim()),rhoA=Double.parseDouble(rInput.getText().toString().trim());
            SurveyPoint point=arrayType()==ArrayType.WENNER?new SurveyPoint(spacing/2.0,spacing*1.5,rhoA):new SurveyPoint(Double.parseDouble(mnInput.getText().toString().trim())/2.0,spacing,rhoA);
            if(editingIndex>=0){points.set(editingIndex,point);editingIndex=-1;addButton.setText("Add reading");toast("Reading updated");}else points.add(point);
            model=null;clearInputs();refresh();saveDraft();focusMn();
        }catch(Exception e){toast(arrayType()==ArrayType.WENNER?"Enter valid positive a and ρa values.":"Enter valid positive MN, AB/2 and ρa values. AB/2 must exceed MN/2.");}
    }

    private void editReading(int index){SurveyPoint p=points.get(index);editingIndex=index;if(arrayType()==ArrayType.SCHLUMBERGER)mnInput.setText(number.format(p.mnHalf*2));abInput.setText(number.format(arrayType()==ArrayType.WENNER?p.wennerSpacing():p.abHalf));rInput.setText(number.format(p.apparentResistivity()));addButton.setText("Update reading");focusFirst();}
    private void clearInputs(){mnInput.setText("");abInput.setText("");rInput.setText("");}
    private void focusMn(){focusFirst();}
    private void focusFirst(){EditText first=arrayType()==ArrayType.WENNER?abInput:mnInput;if(first==null)return;first.post(()->{first.requestFocus();first.setSelection(0);((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(first,InputMethodManager.SHOW_IMPLICIT);});}

    private void refresh(){
        if(rows==null)return;rows.removeAllViews();
        if(arrayType()==ArrayType.WENNER)rows.addView(tableRow("No.","a (m)","ρa (Ωm)","","", "",-1));else rows.addView(tableRow("No.","AB/2","MN","ρa (Ωm)","", "",-1));
        for(int i=0;i<points.size();i++){SurveyPoint p=points.get(i);if(arrayType()==ArrayType.WENNER)rows.addView(tableRow(String.valueOf(i+1),number.format(p.wennerSpacing()),number.format(p.apparentResistivity()),"","","",i));else rows.addView(tableRow(String.valueOf(i+1),number.format(p.abHalf),number.format(p.mnHalf*2),number.format(p.apparentResistivity()),"","",i));}
        if(graph!=null)graph.setData(points,model,arrayType());refreshLayers();
    }

    private View tableRow(String n,String mn,String ab,String r,String k,String rho,int index){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);for(String value:new String[]{n,mn,ab,r,k,rho}){TextView cell=text(value,14,Color.BLACK);cell.setGravity(Gravity.CENTER);cell.setPadding(4,13,4,13);row.addView(cell,new LinearLayout.LayoutParams(dp(92),ViewGroup.LayoutParams.WRAP_CONTENT));}
        if(index>=0){row.setOnClickListener(v->editReading(index));Button delete=button("Delete");delete.setOnClickListener(v->{points.remove(index);editingIndex=-1;addButton.setText("Add reading");model=null;refresh();saveDraft();focusMn();});row.addView(delete,new LinearLayout.LayoutParams(dp(98),dp(48)));}
        row.setBackgroundColor(index%2==0?Color.rgb(235,247,240):Color.WHITE);return row;
    }

    private void loadSample(){points.clear();model=null;editingIndex=-1;historyId=null;double[] rho={29.4,16.7,12.7,10.6,8.58,7.72,6.74,5.63,4.92,4.30,3.82,3.01,3.33};for(int i=0;i<rho.length;i++){double a=.5*(i+1);points.add(arrayType()==ArrayType.WENNER?new SurveyPoint(a/2,a*1.5,rho[i]):new SurveyPoint(a/2,a*1.5,rho[i]));}projectName.setText("Sample "+arrayType().label+" Survey");refresh();saveDraft();focusFirst();}

    private void confirmClear(){new AlertDialog.Builder(this).setTitle("Clear current survey?").setMessage("Saved surveys in History will not be removed.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{points.clear();model=null;editingIndex=-1;historyId=null;projectName.setText("");addButton.setText("Add reading");clearInputs();refresh();saveDraft();focusMn();}).show();}

    private void runInversion(){
        if(points.size()<6){toast("Add at least 6 Wenner readings first");return;}
        final List<SurveyPoint> copy=new ArrayList<>(points);final int layers=layerCount.getSelectedItemPosition()+2;
        errorText.setText("Calculating physical Wenner layer model…");
        new Thread(()->{try{LayerModel result=LayerInverter.fit(copy,layers,ArrayType.WENNER);runOnUiThread(()->{model=result;refresh();saveDraft();toast("Wenner layer fit completed");});}catch(Exception e){runOnUiThread(()->{errorText.setText("Layer fit could not be completed");toast(e.getMessage()==null?"Layer fit failed":e.getMessage());});}},"wenner-inversion").start();
    }
    private void refreshLayers(){
        if(layerRows==null||errorText==null)return;
        layerRows.removeAllViews();
        if(model==null){errorText.setText("Black: measured   Red: fitted   Blue: layer model");return;}
        errorText.setText("Relative RMS error: "+number.format(model.errorPercent)+"%   •   Wenner 1-D layered-earth model");
        layerRows.addView(layerResultRow("Layer","ρ (Ωm)","h (m)","d (m)","Alt (m)",true,0));
        double depth=0;
        for(int i=0;i<model.resistivity.length;i++){
            String h=i<model.thickness.length?number.format(model.thickness[i]):"∞";
            String d=i<model.thickness.length?number.format(depth+=model.thickness[i]):"—";
            String alt=i<model.thickness.length?number.format(-depth):"—";
            layerRows.addView(layerResultRow(String.valueOf(i+1),number.format(model.resistivity[i]),h,d,alt,false,i));
        }
    }

    private LinearLayout layerResultRow(String layer,String rho,String h,String depth,String alt,boolean header,int index){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4),dp(3),dp(4),dp(3));
        if(!header&&index%2==1)row.setBackgroundColor(Color.rgb(235,247,240));
        row.addView(layerCell(layer,header,Gravity.START),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,0.65f));
        row.addView(layerCell(rho,header,Gravity.END),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1.35f));
        row.addView(layerCell(h,header,Gravity.END),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1.0f));
        row.addView(layerCell(depth,header,Gravity.END),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1.15f));
        row.addView(layerCell(alt,header,Gravity.END),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1.15f));
        return row;
    }

    private TextView layerCell(String value,boolean header,int gravity){
        TextView cell=text(value,14,header?Color.BLACK:Color.DKGRAY);
        cell.setGravity(gravity);
        cell.setSingleLine(true);
        cell.setPadding(dp(4),dp(7),dp(4),dp(7));
        if(header)cell.setTypeface(null,1);
        return cell;
    }

    private SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
    private JSONArray pointsJson() throws Exception{JSONArray a=new JSONArray();for(SurveyPoint p:points)a.put(p.toJson());return a;}
    private JSONObject modelJson() throws Exception{return new JSONObject().put("rho",doubleArray(model.resistivity)).put("h",doubleArray(model.thickness)).put("calculated",doubleArray(model.calculated)).put("error",model.errorPercent);}
    private JSONArray doubleArray(double[] values) throws Exception{JSONArray a=new JSONArray();for(double v:values)a.put(v);return a;}
    private double[] readDoubleArray(JSONArray a)throws Exception{double[] values=new double[a.length()];for(int i=0;i<a.length();i++)values[i]=a.getDouble(i);return values;}

    private void saveDraft(){try{prefs().edit().putString("draft_name",projectName==null?"":projectName.getText().toString()).putString("draft_points",pointsJson().toString()).putString("draft_model",model==null?"":modelJson().toString()).putInt("draft_layers",layerCount==null?3:layerCount.getSelectedItemPosition()+2).putInt("draft_array",0).putString("draft_history_id",historyId==null?"":historyId).apply();}catch(Exception ignored){}}
    private void loadDraft(){try{arrayTypeSpinner.setSelection(0);updateMethodInputs();projectName.setText(prefs().getString("draft_name",""));points.clear();JSONArray a=new JSONArray(prefs().getString("draft_points","[]"));for(int i=0;i<a.length();i++)points.add(SurveyPoint.fromJson(a.getJSONObject(i)));String m=prefs().getString("draft_model","");model=m.isEmpty()?null:readModel(new JSONObject(m));int layers=prefs().getInt("draft_layers",3);layerCount.setSelection(Math.max(0,Math.min(3,layers-2)));String id=prefs().getString("draft_history_id","");historyId=id.isEmpty()?null:id;refresh();}catch(Exception e){points.clear();model=null;refresh();}}
    private LayerModel readModel(JSONObject o)throws Exception{return new LayerModel(readDoubleArray(o.getJSONArray("rho")),readDoubleArray(o.getJSONArray("h")),readDoubleArray(o.getJSONArray("calculated")),o.getDouble("error"));}

    private void saveToHistory(){
        if(points.isEmpty()){toast("Add at least one reading first");return;}
        try{JSONArray history=new JSONArray(prefs().getString("history","[]"));if(historyId==null)historyId=String.valueOf(System.currentTimeMillis());JSONObject record=new JSONObject().put("id",historyId).put("name",projectName.getText().toString().trim().isEmpty()?"Unnamed survey":projectName.getText().toString().trim()).put("arrayType",arrayType().name()).put("savedAt",System.currentTimeMillis()).put("points",pointsJson()).put("layers",layerCount.getSelectedItemPosition()+2).put("model",model==null?JSONObject.NULL:modelJson());boolean replaced=false;for(int i=0;i<history.length();i++)if(historyId.equals(history.getJSONObject(i).optString("id"))){history.put(i,record);replaced=true;break;}if(!replaced)history.put(record);prefs().edit().putString("history",history.toString()).apply();saveDraft();toast("Survey saved to History");}catch(Exception e){toast("Could not save the survey");}
    }

    private void loadRecord(JSONObject record)throws Exception{historyId=record.optString("id",null);arrayTypeSpinner.setSelection(0);updateMethodInputs();projectName.setText(record.optString("name",""));points.clear();JSONArray a=record.getJSONArray("points");for(int i=0;i<a.length();i++)points.add(SurveyPoint.fromJson(a.getJSONObject(i)));int layers=record.optInt("layers",3);layerCount.setSelection(Math.max(0,Math.min(3,layers-2)));model=record.isNull("model")?null:readModel(record.getJSONObject("model"));editingIndex=-1;addButton.setText("Add reading");clearInputs();refresh();saveDraft();focusFirst();}

    private void beginExport(boolean pdf){if(points.isEmpty()){toast("Add at least one reading first");return;}Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType(pdf?"application/pdf":"text/csv");intent.putExtra(Intent.EXTRA_TITLE,safeName()+(pdf?".pdf":".csv"));startActivityForResult(intent,pdf?CREATE_PDF:CREATE_CSV);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==OPEN_HISTORY){if(resultCode==RESULT_OK&&data!=null)try{loadRecord(new JSONObject(data.getStringExtra("survey_json")));}catch(Exception e){toast("Could not open that survey");}return;}if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;try{if(requestCode==CREATE_PDF)writePdf(data.getData());else if(requestCode==CREATE_CSV)writeCsv(data.getData());toast("Report exported successfully");}catch(Exception e){toast("Export failed: "+e.getMessage());}}

    private void writeCsv(Uri uri)throws Exception{
        StringBuilder csv=new StringBuilder(arrayType()==ArrayType.WENNER?"No,a (m),Apparent resistivity (ohm-m)\n":"No,AB/2 (m),MN (m),Apparent resistivity (ohm-m)\n");
        for(int i=0;i<points.size();i++){SurveyPoint p=points.get(i);csv.append(i+1).append(',');if(arrayType()==ArrayType.WENNER)csv.append(p.wennerSpacing()).append(',');else csv.append(p.abHalf).append(',').append(p.mnHalf*2).append(',');csv.append(p.apparentResistivity()).append('\n');}
        if(model!=null){csv.append("\nLayer,Resistivity (ohm-m),Thickness h (m),Depth d (m),Altitude (m)\n");double depth=0;for(int i=0;i<model.resistivity.length;i++){csv.append(i+1).append(',').append(model.resistivity[i]).append(',');if(i<model.thickness.length){depth+=model.thickness[i];csv.append(model.thickness[i]).append(',').append(depth).append(',').append(-depth);}else csv.append("infinite,,");csv.append('\n');}csv.append("Fit error (%),").append(model.errorPercent).append('\n');}
        try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(csv.toString().getBytes(StandardCharsets.UTF_8));}
    }

    private void writePdf(Uri uri)throws Exception{
        PdfDocument pdf=new PdfDocument();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);int pageNo=1,rowIndex=0;
        while(rowIndex<points.size()){PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo++).create());Canvas c=page.getCanvas();p.setColor(Color.rgb(11,74,45));p.setTextSize(22);p.setFakeBoldText(true);c.drawText(projectName.getText().toString().isEmpty()?"Soil Resistivity Survey":projectName.getText().toString(),36,48,p);p.setFakeBoldText(false);p.setColor(Color.BLACK);p.setTextSize(11);c.drawText(arrayType().label+" — apparent resistivity report",36,70,p);c.drawText(arrayType()==ArrayType.WENNER?"No.     a (m)       ρa (Ωm)":"No.     AB/2 (m)    MN (m)      ρa (Ωm)",36,102,p);int y=124;while(rowIndex<points.size()&&y<500){SurveyPoint v=points.get(rowIndex);String line=arrayType()==ArrayType.WENNER?String.format("%-8d %-12s %s",rowIndex+1,number.format(v.wennerSpacing()),number.format(v.apparentResistivity())):String.format("%-8d %-12s %-12s %s",rowIndex+1,number.format(v.abHalf),number.format(v.mnHalf*2),number.format(v.apparentResistivity()));c.drawText(line,36,y,p);y+=22;rowIndex++;}if(rowIndex>=points.size()){if(model!=null){p.setFakeBoldText(true);c.drawText("Layer model — error "+number.format(model.errorPercent)+"%",36,500,p);p.setFakeBoldText(false);int ly=516;double depth=0;for(int i=0;i<model.resistivity.length&&ly<580;i++){String h="infinite",d="—",alt="—";if(i<model.thickness.length){depth+=model.thickness[i];h=number.format(model.thickness[i]);d=number.format(depth);alt=number.format(-depth);}c.drawText("Layer "+(i+1)+": ρ="+number.format(model.resistivity[i])+", h="+h+", d="+d+", Alt="+alt,36,ly,p);ly+=15;}}Bitmap chart=Bitmap.createBitmap(Math.max(graph.getWidth(),800),Math.max(graph.getHeight(),400),Bitmap.Config.ARGB_8888);graph.draw(new Canvas(chart));c.drawBitmap(chart,null,new android.graphics.Rect(36,590,559,815),p);}pdf.finishPage(page);}
        try(OutputStream out=getContentResolver().openOutputStream(uri)){pdf.writeTo(out);}pdf.close();
    }

    private String safeName(){String n=projectName.getText().toString().trim();if(n.isEmpty())n="soil-resistivity-report";return n.replaceAll("[^A-Za-z0-9._-]+","-");}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private TextView text(String value,int sp,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(sp);v.setTextColor(color);v.setPadding(4,10,4,10);return v;}
    private EditText input(String hint,boolean decimal){EditText e=new EditText(this);e.setHint(hint);if(decimal)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);return b;}
    private LinearLayout.LayoutParams weighted(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1);p.setMargins(3,4,3,4);return p;}
    private LinearLayout.LayoutParams matchWrap(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.setMargins(0,5,0,5);return p;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
}
