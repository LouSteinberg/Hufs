package hufs;

import java.util.ArrayList;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.util.FastMath;

public class ArrayFns {
	public static double [ ] arraySample(RealDistribution dist, int size){
		double [ ] x = new double [size];
		for (int i=0; i<size; i++){
			x[i] = dist.sample();
		}
		return x;
	}
	public static double arrayMean(double[ ] x){
		return arrayMean(x, 0.0, x.length);
		}
	public static double arrayMean(double[ ] x, double init){
		return arrayMean(x, init, x.length+1);
	}
	public static double arrayMean(double[ ] x, double init, int n){
		double sum = init;
		for (int i = 0; i<x.length; i++){
			sum += x[i];
		}
		return sum/n;
	}
	public static double aLMean(ArrayList<Double> x){
		double sum = 0.0;
		for (double elt: x){
			sum += elt;
		}
		return sum/x.size();
	}
	public static double aLStDev(ArrayList<Double> x){
		double sum = 0.0;
		double sumOfSqs = 0.0;
		double ct = x.size();
		for (double elt: x){
			sum += elt;
			sumOfSqs += elt * elt;
		}
		return FastMath.sqrt((ct*sumOfSqs - sum*sum) / (ct*(ct-1)));
	}
	public static double sampleMean( RealDistribution dist){
		return sampleMean(dist, 1000);
	}
	public static double sampleMean(RealDistribution dist, int nSamples){
		return arrayMean(arraySample(dist, nSamples), 0.0);
	}
	public static double sampleStDev(RealDistribution dist ){
		return sampleStDev(dist, 1000);
	}
	public static double sampleStDev(RealDistribution dist, int nSamples){
		return arrayStDev(arraySample(dist, nSamples));
	}
	public static double arrayStDev(double [ ] x){
		double sum = 0.0;
		double sumOfSqs = 0.0;
		double ct = x.length;
		for (int i = 0; i<x.length; i++){
			sum += x[i];
			sumOfSqs += x[i]*x[i];
		}
		return FastMath.sqrt((ct*sumOfSqs - sum*sum) / (ct*(ct-1)));
	}

	public static void main(String [] args){
		ArrayList<Double> al = new ArrayList<Double>();
		for (double d = 0.0; d<4.0; d++){
			al.add(d);
		}
		System.out.println(aLMean(al)+" "+aLStDev(al));
	}
}
