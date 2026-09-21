package com.pasan.soilresistivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int CREATE_PDF = 40, CREATE_CSV = 41;
    private final List<SurveyPoint> points = new ArrayList<>();
    private final DecimalFormat number = new DecimalFormat("0.00##");
    private EditText projectName, mnInput, abInput, rInput;
    private LinearLayout rows;
    private ResistivityGraphView graph;
    private Spinner layerCount;
    private LinearLayout layerRows;
    private TextView errorText;
    private LayerModel model;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildScreen());
        loadSavedSurvey();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column();
        root.setPadding(dp(16), dp(16), dp(16), dp(30));
        scroll.addView(root);

        TextView title = text("Soil Resistivity Survey", 26, Color.rgb(11, 74, 45));
        title.setTypeface(null, 1); root.addView(title);
        root.addView(text("Wenner (alpha) • Offline", 15, Color.DKGRAY));

        projectName = input("Survey name", false); root.addView(projectName, matchWrap());
        root.addView(text("Enter a measurement", 20, Color.rgb(11, 74, 45)));
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.HORIZONTAL);
        mnInput = input("MN/2 (m)", true); abInput = input("AB/2 (m)", true); rInput = input("R (Ω)", true);
        form.addView(mnInput, weighted()); form.addView(abInput, weighted()); form.addView(rInput, weighted()); root.addView(form);
        Button add = button("Add reading"); add.setOnClickListener(v -> addReading()); root.addView(add, matchWrap());

        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button sample = button("Sample"); sample.setOnClickListener(v -> loadSample());
        Button save = button("Save"); save.setOnClickListener(v -> saveSurvey());
        Button clear = button("Clear"); clear.setOnClickListener(v -> confirmClear());
        actions.addView(sample, weighted()); actions.addView(save, weighted()); actions.addView(clear, weighted()); root.addView(actions);

        root.addView(text("Readings", 20, Color.rgb(11, 74, 45)));
        HorizontalScrollView tableScroll = new HorizontalScrollView(this);
        rows = column(); tableScroll.addView(rows, new ViewGroup.LayoutParams(dp(650), ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(tableScroll, matchWrap());

        root.addView(text("Resistivity graph (logarithmic)", 20, Color.rgb(11, 74, 45)));
        graph = new ResistivityGraphView(this); root.addView(graph, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(330)));

        LinearLayout inversion = new LinearLayout(this); inversion.setOrientation(LinearLayout.HORIZONTAL);
        layerCount = new Spinner(this);
        layerCount.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"2 layers","3 layers","4 layers","5 layers"}));
        layerCount.setSelection(1);
        Button invert = button("Run layer fit"); invert.setOnClickListener(v -> runInversion());
        inversion.addView(layerCount, weighted()); inversion.addView(invert, weighted()); root.addView(inversion);
        errorText = text("Black: measured   Red: fitted   Blue: layer model", 14, Color.DKGRAY); root.addView(errorText);
        layerRows = column(); root.addView(layerRows, matchWrap());

        LinearLayout exports = new LinearLayout(this); exports.setOrientation(LinearLayout.HORIZONTAL);
        Button pdf = button("Export PDF"); pdf.setOnClickListener(v -> beginExport(true));
        Button csv = button("Export Excel CSV"); csv.setOnClickListener(v -> beginExport(false));
        exports.addView(pdf, weighted()); exports.addView(csv, weighted()); root.addView(exports);
        root.addView(text("ρa = K × R     K = π[(AB/2)² − (MN/2)²] ÷ [2(MN/2)]", 13, Color.DKGRAY));
        refresh(); return scroll;
    }

    private void addReading() {
        try {
            double mn = Double.parseDouble(mnInput.getText().toString().trim());
            double ab = Double.parseDouble(abInput.getText().toString().trim());
            double r = Double.parseDouble(rInput.getText().toString().trim());
            points.add(new SurveyPoint(mn, ab, r));
            model = null;
            mnInput.setText(""); abInput.setText(""); rInput.setText(""); refresh();
        } catch (Exception e) { toast("Enter valid positive values. AB/2 must be greater than MN/2."); }
    }

    private void refresh() {
        if (rows == null) return;
        rows.removeAllViews();
        rows.addView(tableRow("No.", "MN/2", "AB/2", "R (Ω)", "K", "ρa (Ωm)", -1));
        for (int i = 0; i < points.size(); i++) {
            SurveyPoint p = points.get(i);
            rows.addView(tableRow(String.valueOf(i + 1), number.format(p.mnHalf), number.format(p.abHalf),
                    number.format(p.resistance), number.format(p.geometricFactor()), number.format(p.apparentResistivity()), i));
        }
        
        if (graph != null) graph.setData(points, model);
        refreshLayers();
    }

    private View tableRow(String n, String mn, String ab, String r, String k, String rho, int index) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        String[] values = {n, mn, ab, r, k, rho};
        for (String value : values) {
            TextView cell = text(value, 14, Color.BLACK); cell.setGravity(Gravity.CENTER); cell.setPadding(4, 13, 4, 13);
            row.addView(cell, new LinearLayout.LayoutParams(dp(92), ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (index >= 0) {
            Button delete = button("Delete"); delete.setOnClickListener(v -> { points.remove(index); model=null; refresh(); });
            row.addView(delete, new LinearLayout.LayoutParams(dp(98), dp(48)));
        }
        row.setBackgroundColor(index % 2 == 0 ? Color.rgb(235, 247, 240) : Color.WHITE);
        return row;
    }

    private void loadSample() {
        points.clear();
        model = null;
        double[][] data = {{.25,.75,29.4},{.5,1.5,16.7},{.75,2.25,12.7},{1,3,10.6},{1.25,3.75,8.58},
                {1.5,4.5,7.72},{1.75,5.25,6.74},{2,6,5.63},{2.25,6.75,4.92},{2.5,7.5,4.30},
                {2.75,8.25,3.82},{3,9,3.33},{3.25,9.75,3.01}};
        for (double[] d : data) points.add(new SurveyPoint(d[0], d[1], d[2]));
        projectName.setText("Sample VES Survey"); refresh();
    }

    private void saveSurvey() {
        try {
            JSONArray array = new JSONArray(); for (SurveyPoint p : points) array.put(p.toJson());
            getPreferences(MODE_PRIVATE).edit().putString("name", projectName.getText().toString())
                    .putString("points", array.toString()).apply(); toast("Survey saved on this phone");
        } catch (Exception e) { toast("Could not save the survey"); }
    }

    private void loadSavedSurvey() {
        try {
            projectName.setText(getPreferences(MODE_PRIVATE).getString("name", ""));
            JSONArray array = new JSONArray(getPreferences(MODE_PRIVATE).getString("points", "[]"));
            for (int i = 0; i < array.length(); i++) points.add(SurveyPoint.fromJson(array.getJSONObject(i)));
            refresh();
        } catch (Exception ignored) { }
    }

    private void confirmClear() {
        new AlertDialog.Builder(this).setTitle("Clear readings?").setMessage("This removes all rows from the current screen.")
                .setNegativeButton("Cancel", null).setPositiveButton("Clear", (d, w) -> { points.clear(); model=null; refresh(); }).show();
    }

    private void runInversion() {
        try {
            model = LayerInverter.fit(points, layerCount.getSelectedItemPosition() + 2);
            refresh();
            toast("Layer fit completed");
        } catch (Exception e) { toast(e.getMessage()); }
    }

    private void refreshLayers() {
        if (layerRows == null || errorText == null) return;
        layerRows.removeAllViews();
        if (model == null) { errorText.setText("Black: measured   Red: fitted   Blue: layer model"); return; }
        errorText.setText("Fit error: " + number.format(model.errorPercent) + "%   •   preliminary field model");
        layerRows.addView(text("Layer     ρ (Ωm)       h (m)       depth (m)", 14, Color.BLACK));
        double depth = 0;
        for (int i = 0; i < model.resistivity.length; i++) {
            String h = i < model.thickness.length ? number.format(model.thickness[i]) : "∞";
            String d = i < model.thickness.length ? number.format(depth += model.thickness[i]) : "—";
            layerRows.addView(text((i+1) + "             " + number.format(model.resistivity[i]) + "          " + h + "          " + d, 14, Color.DKGRAY));
        }
    }

    private void beginExport(boolean pdf) {
        if (points.isEmpty()) { toast("Add at least one reading first"); return; }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(pdf ? "application/pdf" : "text/csv");
        String base = safeName(); intent.putExtra(Intent.EXTRA_TITLE, base + (pdf ? ".pdf" : ".csv"));
        startActivityForResult(intent, pdf ? CREATE_PDF : CREATE_CSV);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            if (requestCode == CREATE_PDF) writePdf(data.getData()); else if (requestCode == CREATE_CSV) writeCsv(data.getData());
            toast("Report exported successfully");
        } catch (Exception e) { toast("Export failed: " + e.getMessage()); }
    }

  private void writeCsv(Uri uri) throws Exception {
    StringBuilder csv = new StringBuilder(
        "No,MN/2 (m),AB/2 (m),R (ohm),K (m),Apparent resistivity (ohm-m)\n"
    );

    for (int i = 0; i < points.size(); i++) {
        SurveyPoint p = points.get(i);

        csv.append(i + 1).append(',')
            .append(p.mnHalf).append(',')
            .append(p.abHalf).append(',')
            .append(p.resistance).append(',')
            .append(p.geometricFactor()).append(',')
            .append(p.apparentResistivity()).append('\n');
    }

    if (model != null) {
        csv.append(
            "\nLayer,Resistivity (ohm-m),Thickness (m),Bottom depth (m)\n"
        );

        double depth = 0;

        for (int i = 0; i < model.resistivity.length; i++) {
            csv.append(i + 1).append(',')
                .append(model.resistivity[i]).append(',');

            if (i < model.thickness.length) {
                depth += model.thickness[i];

                csv.append(model.thickness[i])
                    .append(',')
                    .append(depth);
            }

            csv.append('\n');
        }

        csv.append("Fit error (%),")
            .append(model.errorPercent)
            .append('\n');
    }

    try (OutputStream out =
             getContentResolver().openOutputStream(uri)) {

        out.write(
            csv.toString().getBytes(StandardCharsets.UTF_8)
        );
    }
}

    private void writePdf(Uri uri) throws Exception {
        PdfDocument pdf = new PdfDocument(); Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int pageNo = 1, rowIndex = 0;
        while (rowIndex < points.size()) {
            PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNo++).create());
            Canvas c = page.getCanvas(); p.setColor(Color.rgb(11,74,45)); p.setTextSize(22); p.setFakeBoldText(true);
            c.drawText(projectName.getText().toString().isEmpty() ? "Soil Resistivity Survey" : projectName.getText().toString(), 36, 48, p);
            p.setFakeBoldText(false); p.setColor(Color.BLACK); p.setTextSize(11);
            c.drawText("Wenner (alpha) — apparent resistivity report", 36, 70, p);
            c.drawText("No.    MN/2 (m)    AB/2 (m)    R (Ω)       K (m)       ρa (Ωm)", 36, 102, p);
            int y = 124;
            while (rowIndex < points.size() && y < 500) {
                SurveyPoint v = points.get(rowIndex);
                String line = String.format("%-7d %-13s %-13s %-11s %-11s %s", rowIndex+1, number.format(v.mnHalf),
                        number.format(v.abHalf), number.format(v.resistance), number.format(v.geometricFactor()), number.format(v.apparentResistivity()));
                c.drawText(line, 36, y, p); y += 22; rowIndex++;
            }
            if (rowIndex >= points.size()) {
                if (model != null) {
                    p.setFakeBoldText(true); c.drawText("Layer model — error " + number.format(model.errorPercent) + "%", 36, 500, p); p.setFakeBoldText(false);
                    double depth = 0; int ly = 516;
                    for (int i = 0; i < model.resistivity.length && ly < 570; i++) {
                        String h = i < model.thickness.length ? number.format(model.thickness[i]) : "infinite";
                        if (i < model.thickness.length) depth += model.thickness[i];
                        c.drawText("Layer " + (i+1) + ": ρ=" + number.format(model.resistivity[i]) + " Ωm, h=" + h + " m", 36, ly, p); ly += 13;
                    }
                }
                Bitmap chart = Bitmap.createBitmap(Math.max(graph.getWidth(), 800), Math.max(graph.getHeight(), 400), Bitmap.Config.ARGB_8888);
                graph.draw(new Canvas(chart)); c.drawBitmap(chart, null, new android.graphics.Rect(36, 575, 559, 815), p);
            }
            pdf.finishPage(page);
        }
        try (OutputStream out = getContentResolver().openOutputStream(uri)) { pdf.writeTo(out); }
        pdf.close();
    }

    private String safeName() {
        String n = projectName.getText().toString().trim(); if (n.isEmpty()) n = "soil-resistivity-report";
        return n.replaceAll("[^A-Za-z0-9._-]+", "-");
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private TextView text(String value, int sp, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(sp); v.setTextColor(color); v.setPadding(4, 10, 4, 10); return v; }
    private EditText input(String hint, boolean decimal) { EditText e = new EditText(this); e.setHint(hint); if (decimal) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); return e; }
    private Button button(String label) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); return b; }
    private LinearLayout.LayoutParams weighted() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1); p.setMargins(3,4,3,4); return p; }
    private LinearLayout.LayoutParams matchWrap() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.setMargins(0,5,0,5); return p; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
}
