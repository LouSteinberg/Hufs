package hufs;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.analysis.integration.*;
import org.apache.commons.math3.random.RandomAdaptor;

public class BoundedNormalDistribution extends NormalDistribution {
	static final long serialVersionUID = 1L;  // to make eclipse happy
	double supportLB;
	double supportUB;
	int numSigmas;
	
	
	
	public BoundedNormalDistribution(double mean, double sd, int nSig){
		super(mean, sd);
		fillIn(mean, sd, nSig);
	}
	public BoundedNormalDistribution(double mean, double sd){
		super(new RandomAdaptor(Hufs.rng),mean, sd);
		fillIn(mean, sd, 4);
	}
	public void fillIn (double mean, double sd, int nSig){
		numSigmas = nSig;
		supportLB = mean - numSigmas*sd;
		supportUB = mean + numSigmas*sd;
		// supportLB<0 is not a problem, since scores <0 are not a problem ???, just
		// a very bad design
//		if (supportLB < 0){
//			System.out.println("*** "+mean+" "+sd+" "+nSig);
//			throw new IllegalArgumentException( );
//		}
	}
	
	
	public double density(double x){
		double result = super.density(x);
		if (x > supportUB || x < supportLB){
			return 0.0;
		} else {
			return result;
		}
	}
	public double sample( ){
		double s = super.sample();
		return Math.max(supportLB, Math.min(supportUB,  s));
	}
	public double getSupportLowerBound( ){
		return this.supportLB;
	}
	public double getSupportUpperBound( ){
		return this.supportUB;
	}
	
	public double printDensity(double x){
		double d = density(x);
		System.out.println("dx: "+d+" "+x);
		return d;
	}
	
	public static void main(String args[]){
		BoundedNormalDistribution dist = new BoundedNormalDistribution(20.0, 2.0);
		System.out.println("LB "+dist.getSupportLowerBound( )+" UB "+dist.getSupportUpperBound( ));
		TrapezoidIntegrator ti =  new TrapezoidIntegrator(1.0e-2, 1.0e-2, 1, 63);
		double result = ti.integrate(2000, dist::printDensity, dist.getSupportLowerBound(), dist.getSupportUpperBound());

		
		System.out.println("integral "+ result);
		System.out.println("prob "+dist.probability(dist.getSupportLowerBound(), dist.getSupportUpperBound()));
	}
}
