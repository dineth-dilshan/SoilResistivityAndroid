package com.pasan.soilresistivity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ResistivityGraphView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<SurveyPoint> points=new ArrayList<>(); private LayerModel model; private ArrayType arrayType=ArrayType.WENNER;
    private double[] curveX=new double[0],curveY=new double[0];
    public ResistivityGraphView(Context c){super(c);} public ResistivityGraphView(Context c,AttributeSet a){super(c,a);}
    public void setData(List<SurveyPoint> values,LayerModel value,ArrayType type){
        arrayType=type;points=new ArrayList<>(values);points.sort(Comparator.comparingDouble(this::spacing));model=value;
        curveX=new double[0];curveY=new double[0];
        if(model!=null&&!points.isEmpty()){
            double lo=spacing(points.get(0)),hi=spacing(points.get(points.size()-1));curveX=new double[80];
            for(int i=0;i<curveX.length;i++)curveX[i]=lo*Math.pow(hi/lo,i/(curveX.length-1.0));
            curveY=LayerInverter.response(curveX,model.resistivity,model.thickness);
        }
        invalidate();
    }
    private double spacing(SurveyPoint p){return arrayType==ArrayType.WENNER?p.wennerSpacing():p.abHalf;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.drawColor(Color.WHITE);float left=74,top=32,right=getWidth()-24,bottom=getHeight()-66;if(right<=left||bottom<=top)return;RectF plot=new RectF(left,top,right,bottom);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(Color.DKGRAY);canvas.drawRect(plot,paint);paint.setStyle(Paint.Style.FILL);paint.setTextSize(22);canvas.drawText(arrayType.axisLabel,(left+right)/2-95,getHeight()-18,paint);canvas.save();canvas.rotate(-90,22,(top+bottom)/2);canvas.drawText("Apparent resistivity (Ωm)",22,(top+bottom)/2,paint);canvas.restore();
        if(points.isEmpty()){paint.setColor(Color.GRAY);canvas.drawText("Add readings to create the graph",left+35,(top+bottom)/2,paint);return;}
        double minX=points.stream().mapToDouble(this::spacing).min().orElse(.1),maxX=points.stream().mapToDouble(this::spacing).max().orElse(10),minY=points.stream().mapToDouble(SurveyPoint::apparentResistivity).min().orElse(1),maxY=points.stream().mapToDouble(SurveyPoint::apparentResistivity).max().orElse(100);
        if(model!=null)for(double v:model.resistivity){minY=Math.min(minY,v);maxY=Math.max(maxY,v);}minX=Math.max(minX*.8,.0001);maxX*=1.25;minY=Math.max(minY*.7,.0001);maxY*=1.4;double lx0=Math.log10(minX),lx1=Math.log10(maxX),ly0=Math.log10(minY),ly1=Math.log10(maxY);
        paint.setTextSize(15);paint.setStrokeWidth(1);
        for(int decade=(int)Math.ceil(lx0);decade<=Math.floor(lx1);decade++){double value=Math.pow(10,decade);float gx=x(value,lx0,lx1,plot);paint.setColor(Color.LTGRAY);canvas.drawLine(gx,top,gx,bottom,paint);paint.setColor(Color.DKGRAY);canvas.drawText(formatTick(value),gx-10,bottom+20,paint);}
        for(int decade=(int)Math.ceil(ly0);decade<=Math.floor(ly1);decade++){double value=Math.pow(10,decade);float gy=y(value,ly0,ly1,plot);paint.setColor(Color.LTGRAY);canvas.drawLine(left,gy,right,gy,paint);paint.setColor(Color.DKGRAY);canvas.drawText(formatTick(value),left-52,gy+5,paint);}
        if(model!=null){Path red=new Path();paint.setColor(Color.RED);paint.setStrokeWidth(4);paint.setStyle(Paint.Style.STROKE);for(int i=0;i<curveX.length;i++){float px=x(curveX[i],lx0,lx1,plot),py=y(curveY[i],ly0,ly1,plot);if(i==0)red.moveTo(px,py);else red.lineTo(px,py);}canvas.drawPath(red,paint);Path blue=new Path();paint.setColor(Color.BLUE);double depth=0;float px=x(minX,lx0,lx1,plot),py=y(model.resistivity[0],ly0,ly1,plot);blue.moveTo(px,py);for(int i=0;i<model.thickness.length;i++){depth+=model.thickness[i];float dx=x(Math.max(depth,minX),lx0,lx1,plot);blue.lineTo(dx,py);py=y(model.resistivity[i+1],ly0,ly1,plot);blue.lineTo(dx,py);}blue.lineTo(x(maxX,lx0,lx1,plot),py);canvas.drawPath(blue,paint);}
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.BLACK);for(SurveyPoint p:points)canvas.drawCircle(x(spacing(p),lx0,lx1,plot),y(p.apparentResistivity(),ly0,ly1,plot),7,paint);
    }
    private float x(double v,double a,double b,RectF r){return(float)(r.left+(Math.log10(v)-a)/(b-a)*r.width());}
    private float y(double v,double a,double b,RectF r){return(float)(r.bottom-(Math.log10(v)-a)/(b-a)*r.height());}
    private String formatTick(double value){if(value>=1)return String.valueOf((int)Math.round(value));return String.format(java.util.Locale.US,"%.2g",value);}
}
