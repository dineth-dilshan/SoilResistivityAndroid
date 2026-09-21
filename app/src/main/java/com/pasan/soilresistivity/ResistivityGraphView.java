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
    private List<SurveyPoint> points=new ArrayList<>(); private LayerModel model;
    public ResistivityGraphView(Context c){super(c);} public ResistivityGraphView(Context c,AttributeSet a){super(c,a);}
    public void setData(List<SurveyPoint> values,LayerModel value){points=new ArrayList<>(values);points.sort(Comparator.comparingDouble(SurveyPoint::wennerSpacing));model=value;invalidate();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.drawColor(Color.WHITE);float left=74,top=32,right=getWidth()-24,bottom=getHeight()-66;if(right<=left||bottom<=top)return;RectF plot=new RectF(left,top,right,bottom);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(Color.DKGRAY);canvas.drawRect(plot,paint);paint.setStyle(Paint.Style.FILL);paint.setTextSize(24);canvas.drawText("AB/3 spacing (m)",(left+right)/2-90,getHeight()-18,paint);canvas.save();canvas.rotate(-90,22,(top+bottom)/2);canvas.drawText("Apparent resistivity (Ωm)",22,(top+bottom)/2,paint);canvas.restore();
        if(points.isEmpty()){paint.setColor(Color.GRAY);canvas.drawText("Add readings to create the graph",left+35,(top+bottom)/2,paint);return;}
        double minX=points.stream().mapToDouble(SurveyPoint::wennerSpacing).min().orElse(.1),maxX=points.stream().mapToDouble(SurveyPoint::wennerSpacing).max().orElse(10),minY=points.stream().mapToDouble(SurveyPoint::apparentResistivity).min().orElse(1),maxY=points.stream().mapToDouble(SurveyPoint::apparentResistivity).max().orElse(100);
        if(model!=null)for(double v:model.resistivity){minY=Math.min(minY,v);maxY=Math.max(maxY,v);}minX=Math.max(minX*.8,.0001);maxX*=1.25;minY=Math.max(minY*.7,.0001);maxY*=1.4;double lx0=Math.log10(minX),lx1=Math.log10(maxX),ly0=Math.log10(minY),ly1=Math.log10(maxY);
        paint.setColor(Color.LTGRAY);paint.setStrokeWidth(1);for(int i=1;i<5;i++){float gx=left+plot.width()*i/5f,gy=top+plot.height()*i/5f;canvas.drawLine(gx,top,gx,bottom,paint);canvas.drawLine(left,gy,right,gy,paint);}
        if(model!=null){Path red=new Path();paint.setColor(Color.RED);paint.setStrokeWidth(4);paint.setStyle(Paint.Style.STROKE);for(int i=0;i<points.size();i++){float px=x(points.get(i).wennerSpacing(),lx0,lx1,plot),py=y(model.calculated[i],ly0,ly1,plot);if(i==0)red.moveTo(px,py);else red.lineTo(px,py);}canvas.drawPath(red,paint);Path blue=new Path();paint.setColor(Color.BLUE);double depth=0;float px=x(minX,lx0,lx1,plot),py=y(model.resistivity[0],ly0,ly1,plot);blue.moveTo(px,py);for(int i=0;i<model.thickness.length;i++){depth+=model.thickness[i];float dx=x(Math.max(depth,minX),lx0,lx1,plot);blue.lineTo(dx,py);py=y(model.resistivity[i+1],ly0,ly1,plot);blue.lineTo(dx,py);}blue.lineTo(x(maxX,lx0,lx1,plot),py);canvas.drawPath(blue,paint);}
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.BLACK);for(SurveyPoint p:points)canvas.drawCircle(x(p.wennerSpacing(),lx0,lx1,plot),y(p.apparentResistivity(),ly0,ly1,plot),7,paint);
    }
    private float x(double v,double a,double b,RectF r){return(float)(r.left+(Math.log10(v)-a)/(b-a)*r.width());}
    private float y(double v,double a,double b,RectF r){return(float)(r.bottom-(Math.log10(v)-a)/(b-a)*r.height());}
}
