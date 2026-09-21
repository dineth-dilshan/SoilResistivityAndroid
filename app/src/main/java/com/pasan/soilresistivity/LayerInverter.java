package com.pasan.soilresistivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Fast smooth layered-curve fitter intended for preliminary field screening. */
public final class LayerInverter {
    private LayerInverter() { }
    public static LayerModel fit(List<SurveyPoint> source, int layers) {
        if(source.size()<Math.max(5,layers+1)) throw new IllegalArgumentException("More readings are required for this layer count.");
        List<SurveyPoint> data=new ArrayList<>(source); data.sort(Comparator.comparingDouble(SurveyPoint::wennerSpacing));
        layers=Math.max(2,Math.min(5,layers)); int n=data.size(); double[] p=new double[layers*2-1];
        for(int i=0;i<layers;i++){int at=Math.min(n-1,Math.round(i*(n-1f)/(layers-1f)));p[i]=Math.log(data.get(at).apparentResistivity());}
        double minX=data.get(0).wennerSpacing(),maxX=data.get(n-1).wennerSpacing();
        for(int i=0;i<layers-1;i++){double z=minX*Math.pow(maxX/minX,(i+1.0)/layers);double prev=i==0?0:Math.exp(p[layers+i-1]);p[layers+i]=Math.log(Math.max(z-prev,minX*.15));}
        double best=objective(data,p,layers),step=.75;
        for(int round=0;round<28;round++){boolean improved=false;for(int j=0;j<p.length;j++){double original=p[j],local=best,chosen=original;for(int direction:new int[]{-1,1}){p[j]=original+direction*step;double score=objective(data,p,layers);if(score<local){local=score;chosen=p[j];}}p[j]=chosen;if(local<best){best=local;improved=true;}}if(!improved)step*=.58;if(step<.01)break;}
        double[] rho=new double[layers],h=new double[layers-1];for(int i=0;i<layers;i++)rho[i]=Math.exp(p[i]);for(int i=0;i<h.length;i++)h[i]=Math.exp(p[layers+i]);
        double[] calc=new double[n];for(int i=0;i<n;i++)calc[i]=response(data.get(i).wennerSpacing(),rho,h);
        return new LayerModel(rho,h,calc,Math.sqrt(best/n)*100);
    }
    private static double objective(List<SurveyPoint> data,double[] p,int layers){double[] rho=new double[layers],h=new double[layers-1];for(int i=0;i<layers;i++)rho[i]=Math.exp(p[i]);for(int i=0;i<h.length;i++)h[i]=Math.exp(p[layers+i]);double sum=0;for(SurveyPoint point:data){double e=Math.log(response(point.wennerSpacing(),rho,h)/point.apparentResistivity());sum+=e*e;}return sum;}
    public static double response(double spacing,double[] rho,double[] h){double value=Math.log(rho[0]),depth=0;for(int i=0;i<h.length;i++){depth+=h[i];double z=Math.max(-40,Math.min(40,4*Math.log(spacing/Math.max(depth,1e-6))));double w=1/(1+Math.exp(-z));value+=(Math.log(rho[i+1])-Math.log(rho[i]))*w;}return Math.exp(value);}
}
