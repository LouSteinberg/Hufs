package hufs;

import java.util.function.Function;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
 
public class Utils {
//	public static boolean printTrace = true;
//	public static void tracePrint(String string){
//		if (printTrace){
//			System.out.print(string);
//		}	
//	}
//	public static void tracePrintln(String string){
//		if (printTrace){
//			System.out.println(string);
//		}	
//	}
	public static String dblFmt(double x){
		return String.format("%8.5g",x);
	}
	
	public static double standardize(double value, SummaryStatistics stats){
		return (value-stats.getMean( ))/stats.getStandardDeviation();
	}
	public static void main(String [ ] args){

		System.out.println("x"+dblFmt(12.345678)+"X");
		
		
		
//		SummaryStatistics stats = new SummaryStatistics( );
//		double [ ] data = {1.0,2.0,2.0, 1.0};
//		for (double d: data){
//			stats.addValue(d);
//		}
//		System.out.println(standardize(1.0, stats)+" "+stats.getStandardDeviation());
	}
	public static void printFn(Function<Double, Double> fn, double lb, double ub, double steps){
		double step = (ub-lb)/steps;
		for (double x = lb; x < ub; x += step){
			System.out.println(x + " " + fn.apply(x));
		}
	}
	public static double integrate(Function<Double, Double> fn, double a, double b, int n){
		double total = (fn.apply(a)+fn.apply(b))/2.0;
		for (int k = 1; k<n; k++){
			total += fn.apply(a+k*(b-a)/n);
		}
		return total*(b-a)/n;
	}
}


