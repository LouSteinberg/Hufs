package hufs;

import java.util.ArrayList;
import java.util.function.BiFunction;

import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.fitting.*;

public class Level{

	BiFunction<Double, Double, Double> utilityFn;

	ArrayList<Design> designs = new ArrayList<Design>();

	double maxThisQuality;     // max, min, & range of quality of designs at this level
	double minThisQuality;
	double rangeThisQuality;
	double maxChildQuality;    // max and min quality of designs at child level
	double minChildQuality;
	double rangeChildQuality;  	// range of child qualities
	double maxDevCQD;          	// max, min & range of mean & dev of child quality distribution
	double minDevCQD;		   	// for this level
	double rangeDevCQD;			//    level ----> tq/cq-dev/mean/range -> mean/dev-s/b -> distrib of child quality
	double maxMeanCQD;			//    design -> quality  ------------------------------/
	double minMeanCQD; 			// ie every object on a level with a given quality has same distrib of child qualities
	double rangeMeanCQD;		//	
	double childQMeanS;    // parameters that determine generate child quality fns at this level
	double childQMeanB;
	double childQDevS;
	double childQDevB;
	double timeToGenFrom; // time to generate a child from a parent at this level
	
	
	public static void setCQDParams(Level [] levels){
		// choose & set parameters to generate child quality distribution
		// of designs as fn of the design's quality
		// Also sets level's timeToGenFrom
		System.out.println("**********    setCQDParams level 2");
		Level lev = levels[2];
		lev.timeToGenFrom = 1;
		lev.deriveCQDParams(0.5, 1);     
		System.out.println("**********    setCQDParams level 1");
		lev = levels[1];
		lev.timeToGenFrom = 2;
		lev.deriveCQDParams(0.1, 0.5);

		System.out.println("**********    setCQDParams level 0");
		lev = levels[0];
		lev.deriveCQDParams(0.0, 0.0);
	}

	// maxDevCQD must be < 0.125*rangeThisQuality = 1.25
	public void deriveCQDParams(double minDevCQDArg, double maxDevCQDArg){
		//		Without loss of generality, assume min, max, and rangeThisQuality = 10, 20,  10
		minThisQuality = 10;
		maxThisQuality = 20;
		rangeThisQuality = 10;
		if(! isGround()){
			minChildQuality = 10;
			maxChildQuality = 20;
			rangeChildQuality = 10;

			maxDevCQD = maxDevCQDArg; //  must be < rangeThisQuality / 8 = 1.25
			minDevCQD = minDevCQDArg;
			rangeDevCQD = maxDevCQD - minDevCQD;

			minMeanCQD = minChildQuality + 4*maxDevCQD;
			maxMeanCQD = maxChildQuality - 4*maxDevCQD; //    minMeanCQD must be < maxMeanCQD
			rangeMeanCQD = maxMeanCQD - minMeanCQD;
			
			ensurePos(rangeThisQuality);
			ensurePos(rangeMeanCQD);
			ensurePos(rangeDevCQD);
			
			childQMeanB = minMeanCQD;
			childQMeanS = rangeMeanCQD / rangeThisQuality;
			childQDevB = minDevCQD;
			childQDevS = rangeDevCQD / rangeThisQuality;

			if (minChildQuality > maxChildQuality){
				throw new IllegalArgumentException();
			}
			
			ensurePos(childQMeanB);
			ensurePos(childQMeanS);
			ensurePos(childQDevB);
		}
	}

	public double scoreFromQuality(double quality){
//		if (quality > maxQuality){
//			System.out.println("sfq q: "+quality+" "+maxQuality+ " " + number);
//			throw new IllegalArgumentException( );
//		}
		return (quality);  // change this to model inaccuracy in score fn
		//*(.95+(Hufs.rng.nextDouble()*0.1));
	}

	public static double ensurePos(double x){
		if (x<=0){
			throw new NotPositiveException(x);
		}
		return x;
	}
	
//	public double childMaxQuality( ){
//		return childQMeanM*maxQuality+childQMeanB + 
//				4*(childQStDevM*maxQuality+childQStDevB);
//	}
	
	/**
	 * sets max quality of this level from max quality and CQD params of level
	 * above.  Should not  be called on top level since that has no level above.
	 * 
	 */


	public double genChildQMean(double parentQuality){
		return parentQuality*childQMeanS+childQMeanB;
	}
	public double genChildQStDev(double parentQuality){
		return parentQuality*childQDevS+childQDevB;
	}
	public RealDistribution genChildQDistribution(double parentQuality){
		double mean = genChildQMean(parentQuality);
		double dev = genChildQStDev(parentQuality);
		if (! (mean > 4*dev)){
			System.out.println("**** q dev too big "+dev+" "+mean);
			throw new IllegalArgumentException();
//			dev = (mean/5.0)*.98;  // prevent quality <= 0
		}
		RealDistribution distr = new BoundedNormalDistribution(mean, dev);
		return distr;
	}
	public void printQSParams( ){
		if (number > 0){
			System.out.format("%3d   mean  sfit %14.8f   sreal %10.4f   bfit %10.4f   breal %10.4f   minTQ %10.4f%n", 
					number, childSMeanS, childQMeanS, childSMeanB, childQMeanB, minThisQuality);
			System.out.format("     stdev  sfit %14.8f   sreal %10.4f   bfit %10.4f   breal %10.4f   maxTQ %10.4f%n",
					childSDevS, childQDevS, childSDevB, childQDevB, maxThisQuality);
			System.out.format("     mean  %10.4f to %10.4f, r= %10.4f    ", minMeanCQD, maxMeanCQD, rangeMeanCQD);
			System.out.format("dev  %10.4f to %10.4f, r= %10.4f%n", minDevCQD, maxDevCQD, rangeDevCQD);
			
			
//			System.out.format("%3d   mean  slope %10.4f   base %10.4f  minQ %6.2f\n", 
//					number, childQMeanS, childQMeanB, minThisQuality);
//			System.out.format("     stdev  slope %10.4f   base %10.4f  maxQ % 6.2f\n",
//					childQDevS, childQDevB, maxThisQuality);
		} else {
			System.out.format("%3d  minQ %8.2f  maxQ %8.2f\n", 
					number, minThisQuality, maxThisQuality);
			
		}
	}

	public Level(int number){
		this.number = number;
	}
	public String toString(){
		return "Level "+number+" "+designs;
	}

	// *** levels up and down
	public Level levelUp;
	public Level levelDown;
	public int number; // level 0 is ground, larger number -> higher level
	
	public boolean isTop( ){
		return levelUp == null;
	}	
	public boolean isGround( ){
		return levelDown == null;
	}
	
	public Level groundOf(){
		Level lev;
		for(lev = this; ! lev.isGround(); lev=lev.levelDown){
		}
		return lev;
	}
 
	public Level TopOf(){
		Level lev;
		for(lev = this; ! lev.isTop(); lev=lev.levelUp){
		}
		return lev;
	}
	
	public double timeFromCounts(int[ ] counts){
		double time = 0;
		for (Level l = this; ! l.isGround(); l = l.levelDown){
			time += l.timeToGenFrom*counts[l.number];
		}
		return time;
	}
	
	public static void initializeLevels(Level [] levels){
		connect(levels);
		setCQDParams(levels);
	}
	public static void connect(Level [ ] levels){
		int numLevels = levels.length;
		for (int i = 0; i<numLevels-1; i++){
			levels[i].levelUp = levels[i+1];
		}
		for (int i = 1; i<numLevels; i++){
			levels[i].levelDown = levels[i-1];
		}
		


		levels[0].utilityFn = Hufs.U0;
	}	
	// *** estimating CSDs of designs at this level
	public double childSMeanS;  // slope of fn parentScore -> mean childScore
	public double childSMeanB;  // intercept of fn parentScore -> mean childScore
	public double childSDevS; // slope of fn parentScore -> stdev of childScores
	public double childSDevB; // intercept of fn parentScore -> stdev of childScores
	public RealDistribution csd;
	
	/**
	 * generate stats about child score distributions
	 * 
	 * 
	 */
	public static void setChildScoreParams(Level [ ] levels, int numParents, int numKids){
		double [ ] parentQuality = new double[numParents];
		double [ ] kidScore = new double[numKids];
		double [ ] kidQuality = new double[numKids];
		Design [ ] parents = new Design[numParents];
		Design [ ] nextParents = new Design[numParents];
		Design [ ] kids = new Design[numKids];
		Level topLevel = levels[levels.length-1];
		for (int j = 0; j<numParents; j++){
			parents[j] = topLevel.randomSpecs(false);
		}
		PolynomialCurveFitter fitter;
		for (Level level = topLevel; level.levelDown != null; level = level.levelDown){
			System.out.println("--VVVV--"+level.number);
			WeightedObservedPoints meanObs = new WeightedObservedPoints();
			WeightedObservedPoints stdevObs = new WeightedObservedPoints();
			for (int p = 0; p < numParents; p++){
				//						System.out.println("p: "+p);
				parentQuality[p] = parents[p].quality;
				for (int k = 0; k < numKids; k++){
					//					System.out.println("k: "+k);
					kids[k] = new Design(parents[p], false);
					kidScore[k] = kids[k].score; 
					kidQuality[k] = kids[k].quality; 
					if (kidScore[k]!=kidQuality[k]){
						System.out.println("*** score !+ quality " + k);
					}
				}
				meanObs.add(parents[p].score, ArrayFns.arrayMean(kidScore));
				stdevObs.add(parents[p].score, ArrayFns.arrayStDev(kidScore));

				//						System.out.println(parentQuality[p]+" "+ArrayFns.arrayStDev(kidScore));
				//						System.out.println(parents[p].score+", "+ ArrayFns.arrayMean(kidScore));
				//						System.out.println(parents[p].score+", "+ ArrayFns.arrayStDev(kidScore));
//										System.out.println("%%"+parentQuality[p]+", "+ArrayFns.arrayStDev(kidQuality));
				nextParents[p] = kids[0];
			}
			fitter = PolynomialCurveFitter.create(1);
			double[] meanCoeff = fitter.fit(meanObs.toList());
			level.childSMeanS = meanCoeff[1];
			level.childSMeanB = meanCoeff[0];
			fitter = PolynomialCurveFitter.create(1);
			double[] stdevCoeff = fitter.fit(stdevObs.toList());
			level.childSDevS = stdevCoeff[1];
			level.childSDevB = stdevCoeff[0];
			if (level.childSDevS < 0){
				System.out.println("======================Negative Score Deviation Slope");
				level.childSDevS = 0;	
			}
			if (level.childSDevB <= 0){
				System.out.println("======================Negative Score Deviation Base");
			}
			if (level.childSDevB+level.minThisQuality*level.childSDevS <= 0){
				System.out.println("======================Negative Score min Deviation");
			//	level.childSDevB = 0.001;
			}
			for (int p = 0; p<numParents; p++){
				parents[p] = nextParents[p];
			}
		}
	}	
	public double childSMean(double parentScore){
		return parentScore*childSMeanS+childSMeanB;
	}
	public double childSStDev(double parentScore){
		return parentScore*childSDevS+childSDevB;
	}
	/**
	 * 
	 * @param parentScore
	 * @return estimated child score distribution based on level and score of parent
	 */
	public RealDistribution genCSD(double parentQuality){
		double mean = childSMean(parentQuality);
		double stDev = childSStDev(parentQuality);
		if (mean == 0 && stDev == 0){  // if the CSD parameters have not yet been set
			return null;
		}
		if (mean < 0 || stDev < 0){
			throw new IllegalArgumentException(mean+" "+stDev);
		}
		return new BoundedNormalDistribution(mean, stDev);
	}
	// *** domain independent generate & score 
	
	public double timeDone(double tau){
		return tau - timeToGenFrom;
	}
//	/**
//	 * average utility of generating a child from a (hypothetical) (design at level with score) starting at tau
//	 */
//	public double utilOfAvgChild(double score, double  tau){
//		if (tau < 0.0){
//			return 0.0;
//		} else if (isGround()){
//			return Hufs.U0.apply(score, tau);
//		} else {
//			RealDistribution csd = genCSD(score);
//			TrapezoidIntegrator ti = new TrapezoidIntegrator( );
//			return ti.integrate(
//							1000, 
//							sc->(csd.density(sc)*levelDown.utilOfGen(sc, timeDone(tau))),
//							csd.getSupportLowerBound(), csd.getSupportUpperBound());
//		}
//	}
//	static int levIntCt=0;
	/**
	 * 
	 * @param score
	 * @param tau
	 * @param ulev for indenting
	 * @return utility of creating a design at this level from a parent 
	 *         with given score finishing at given tau
	 */
	public double utilOfGen(double score, double tau, int ulev){
		double result;
		if (tau< 0.0){
			result = 0.0;
		} else if (isGround()){
			result = Hufs.U0.apply(score, tau);
		} else {
//			System.out.println("lic "+levIntCt+" "+Design.indent(ulev)+"str lev uOG level num, tau: "+number+", "+tau+" "+score);
			double putil = utilOfGen(score, timeDone(tau),ulev+1);
			RealDistribution csd = genCSD(score);
			TrapezoidIntegrator ti = new TrapezoidIntegrator(1e-3, 1e-3, 1, 63 );
			result =  ti.integrate(
					1000,
//					sc->(Math.max(putil, 
//							csd.density(sc)*levelDown.utilOfGen(sc, timeDone(tau), ulev+1))),
					sc -> utilIntegrand(putil, sc, csd, tau, ulev+1),
					csd.getSupportLowerBound(), csd.getSupportUpperBound());
		}
		return result;
	}
	public double utilIntegrand(double putil, double sc, RealDistribution csd, double tau, int ulev){
		double csdValue = csd.density(sc);
		double levelEval = levelDown.utilOfGen(sc, timeDone(tau), ulev+1);
//		Design.detailPrint("sc, csd(sc), level,  level eval: "+sc+" "+ csdValue+' '+levelEval+" "+ number, ulev);
//		return Math.max(putil, csdValue*levelEval);
		return csdValue*Math.max(putil, levelEval);
	}
	/**
	 * @param tau 
	 * @return design from this.designs that has max utility of design starting at tau
	 */
	public Design bestOnLevel(double tau){
		double bestUtil = 0.0;
		double util;
		Design bestParent = null;
		for(Design d : designs){
			util = d.utility(tau);
			if (d.cachedUtil >= bestUtil){
				bestParent = d;
				bestUtil = util;
			}
			if (d.cachedUtil < 0){
				throw new IllegalArgumentException();
			}
		}
		return bestParent;
	}
	/**
	 * find the existing design at level of this or below with the highest utility
	 * @param tau
	 * @param traceDetails
	 * @return highest utility design
	 */
	public Design highestUtilityDesign(double tau, boolean traceDetails){
		double util;
		double bestUtil = 0.0;
		Design bestParent = null;
		for (Level l = this; l != null && tau>=0; l = l.levelDown){
			for(Design d : l.designs){
				util = d.utility(tau);
//				d.cachedTau = tau;
//				System.out.println("hud "+d.id+" "+tau+" "+d.cachedUtil);
				if (traceDetails){
					System.out.println("hUD " +d);
				}
				if (util >= bestUtil){
					bestParent = d;
					bestUtil = d.cachedUtil;
				}
				if (d.cachedUtil < 0){
					throw new IllegalArgumentException(); 
				}
			}
		}
//		System.out.println("best design: "+ bestParent);
		return bestParent;
	}
	
	// *** domain dependent generate & score 
	public Design randomSpecs(boolean saveObjects){
		return new Design(this, saveObjects);
	}
}
