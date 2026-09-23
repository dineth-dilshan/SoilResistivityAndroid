package com.pasan.soilresistivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Physical blocky 1-D Wenner-alpha inversion. */
public final class LayerInverter {
    private static final int SAMPLES = 900;
    private static final double ROBUST_SCALE = .025;
    private LayerInverter() { }

    public static LayerModel fit(List<SurveyPoint> source, int layers, ArrayType ignored) {
        if (source.size() < Math.max(6, layers + 2))
            throw new IllegalArgumentException("More Wenner readings are required for this layer count.");
        List<SurveyPoint> data = new ArrayList<>(source);
        data.sort(Comparator.comparingDouble(SurveyPoint::wennerSpacing));
        layers = Math.max(2, Math.min(5, layers));
        double[] x = new double[data.size()], y = new double[data.size()];
        for (int i=0;i<data.size();i++) {
            x[i]=data.get(i).wennerSpacing(); y[i]=data.get(i).apparentResistivity();
            if (!(x[i]>0) || !(y[i]>0)) throw new IllegalArgumentException("Spacing and apparent resistivity must be positive.");
        }
        double[] best=null; double bestScore=Double.POSITIVE_INFINITY;
        for (int start=0;start<4;start++) {
            double[] candidate=optimize(initial(x,y,layers,start),x,y,layers);
            double score=objective(candidate,x,y,layers);
            if(score<bestScore){bestScore=score;best=candidate;}
        }
        double[] rho=new double[layers],h=new double[layers-1]; unpack(best,rho,h);
        double[] calculated=response(x,rho,h);
        return new LayerModel(rho,h,calculated,relativeRms(y,calculated));
    }

    private static double[] initial(double[] x,double[] y,int layers,int variant){
        double[] p=new double[2*layers-1];
        for(int i=0;i<layers;i++){
            int at=(int)Math.round(i*(y.length-1.0)/(layers-1.0));
            double factor=Math.exp((variant-1.5)*.10*(i%2==0?1:-1));
            p[i]=Math.log(y[at]*factor);
        }
        double previous=0;
        for(int i=0;i<layers-1;i++){
            double depth=x[0]*Math.pow(x[x.length-1]/x[0],(i+.55)/layers);
            p[layers+i]=Math.log(Math.max(depth-previous,x[0]*.08)*Math.exp((variant-1.5)*.08));
            previous=depth;
        }
        return p;
    }

    private static double[] optimize(double[] p,double[] x,double[] y,int layers){
        double lambda=.03,current=objective(p,x,y,layers);
        for(int iteration=0;iteration<50;iteration++){
            double[] residual=residuals(p,x,y,layers); int m=p.length,n=y.length;
            double[][] j=new double[n][m]; double delta=.004;
            for(int column=0;column<m;column++){
                double old=p[column];p[column]=old+delta;double[] plus=residuals(p,x,y,layers);
                p[column]=old-delta;double[] minus=residuals(p,x,y,layers);p[column]=old;
                for(int row=0;row<n;row++)j[row][column]=(plus[row]-minus[row])/(2*delta);
            }
            double[][] normal=new double[m][m];double[] right=new double[m];
            for(int row=0;row<n;row++){
                double ratio=residual[row]/ROBUST_SCALE,weight=1.0/(1.0+ratio*ratio);
                for(int a=0;a<m;a++){right[a]-=weight*j[row][a]*residual[row];for(int b=0;b<m;b++)normal[a][b]+=weight*j[row][a]*j[row][b];}
            }
            for(int i=0;i<m;i++)normal[i][i]+=lambda*(normal[i][i]+1e-8);
            double[] step=solve(normal,right);if(step==null)break;
            double[] trial=p.clone();double length=0;
            for(int i=0;i<m;i++){step[i]=Math.max(-.55,Math.min(.55,step[i]));trial[i]=clamp(p[i]+step[i],i<layers,x);length+=step[i]*step[i];}
            double next=objective(trial,x,y,layers);
            if(next<current){p=trial;current=next;lambda=Math.max(1e-6,lambda*.42);if(Math.sqrt(length)<1e-4)break;}
            else lambda=Math.min(1e7,lambda*7);
        }
        return p;
    }

    private static double clamp(double value,boolean rho,double[] x){
        double low=rho?.01:x[0]*.015,high=rho?1e6:x[x.length-1]*20;
        return Math.max(Math.log(low),Math.min(Math.log(high),value));
    }
    private static double objective(double[] p,double[] x,double[] y,int layers){
        double sum=0;for(double v:residuals(p,x,y,layers)){double ratio=v/ROBUST_SCALE;sum+=Math.log1p(ratio*ratio);}return sum;
    }
    private static double[] residuals(double[] p,double[] x,double[] y,int layers){
        double[] rho=new double[layers],h=new double[layers-1];unpack(p,rho,h);double[] predicted=response(x,rho,h),r=new double[y.length];
        for(int i=0;i<y.length;i++)r[i]=Math.log(predicted[i]/y[i]);return r;
    }
    private static void unpack(double[] p,double[] rho,double[] h){for(int i=0;i<rho.length;i++)rho[i]=Math.exp(p[i]);for(int i=0;i<h.length;i++)h[i]=Math.exp(p[rho.length+i]);}

    /** Koefoed transform followed by the Wenner-alpha potential integral. */
    public static double[] response(double[] spacing,double[] rho,double[] h){
        double[] out=new double[spacing.length];double minH=Double.POSITIVE_INFINITY,maxA=0;
        for(double v:h)minH=Math.min(minH,v);for(double v:spacing)maxA=Math.max(maxA,v);minH=Math.max(minH,maxA*.001);
        double low=Math.log(1e-7/maxA),high=Math.log(80/minH),dz=(high-low)/(SAMPLES-1);
        double[] wave=new double[SAMPLES],transform=new double[SAMPLES];
        for(int k=0;k<SAMPLES;k++){wave[k]=Math.exp(low+k*dz);transform[k]=transform(wave[k],rho,h)-rho[0];}
        for(int i=0;i<spacing.length;i++){double a=spacing[i],g1=potential(a,rho[0],wave,transform,dz),g2=potential(2*a,rho[0],wave,transform,dz);out[i]=Math.max(1e-6,2*a*(g1-g2));}
        return out;
    }
    public static double response(double spacing,double[] rho,double[] h){return response(new double[]{spacing},rho,h)[0];}
    private static double transform(double wave,double[] rho,double[] h){
        double t=rho[rho.length-1];for(int i=h.length-1;i>=0;i--){double q=Math.tanh(wave*h[i]);t=rho[i]*(t+rho[i]*q)/(rho[i]+t*q);}return t;
    }
    private static double potential(double r,double top,double[] wave,double[] transform,double dz){
        double integral=0;for(int k=0;k<wave.length;k++){double v=transform[k]*j0(wave[k]*r)*wave[k];integral+=(k==0||k==wave.length-1)?.5*v:v;}return top/r+integral*dz;
    }

    // Cephes J0 approximation.
    private static double j0(double x){
        double ax=Math.abs(x);
        if(ax<8){double y=x*x;double p=57568490574.0+y*(-13362590354.0+y*(651619640.7+y*(-11214424.18+y*(77392.33017+y*-184.9052456))));double q=57568490411.0+y*(1029532985.0+y*(9494680.718+y*(59272.64853+y*(267.8532712+y))));return p/q;}
        double z=8/ax,y=z*z,phase=ax-.785398164;double p=1+y*(-.1098628627e-2+y*(.2734510407e-4+y*(-.2073370639e-5+y*.2093887211e-6)));double q=-.1562499995e-1+y*(.1430488765e-3+y*(-.6911147651e-5+y*(.7621095161e-6-y*.934945152e-7)));return Math.sqrt(.636619772/ax)*(Math.cos(phase)*p-z*Math.sin(phase)*q);
    }
    private static double relativeRms(double[] observed,double[] calculated){double sum=0;for(int i=0;i<observed.length;i++){double e=(calculated[i]-observed[i])/observed[i];sum+=e*e;}return Math.sqrt(sum/observed.length)*100;}
    private static double[] solve(double[][] a,double[] b){
        int n=b.length;double[][] m=new double[n][n+1];for(int i=0;i<n;i++){System.arraycopy(a[i],0,m[i],0,n);m[i][n]=b[i];}
        for(int col=0;col<n;col++){int pivot=col;for(int row=col+1;row<n;row++)if(Math.abs(m[row][col])>Math.abs(m[pivot][col]))pivot=row;if(Math.abs(m[pivot][col])<1e-14)return null;double[] tmp=m[col];m[col]=m[pivot];m[pivot]=tmp;double div=m[col][col];for(int k=col;k<=n;k++)m[col][k]/=div;for(int row=0;row<n;row++)if(row!=col){double f=m[row][col];for(int k=col;k<=n;k++)m[row][k]-=f*m[col][k];}}
        double[] result=new double[n];for(int i=0;i<n;i++)result[i]=m[i][n];return result;
    }
}
