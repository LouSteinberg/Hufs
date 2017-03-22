package hufs;

import java.util.ArrayList;
import java.util.function.BiFunction;
import org.apache.commons.math3.random.Well1024a;

public class Hufs {	
	final public static double BEGINTAU = 12.0;    // tau at start of design process
	final public static double TAUSTARTCOST = 5.0; // tau at which util starts to decline
	final public static int NUMLEVELS = 3;         // number of levels to the current design problem
	final public static int NUMPARENTS = 40;       // number of parents per level for level statistics
	final public static int NUMKIDS = 40;          // number of kids to generate per parent for level statistics 

	public static boolean empiricalUtility = false;
	
	//final public static int MAXGENS = (int) (BEGINTAU);  // ASSUME each level's time to generate is 0.5
	final public static int TOPLEVEL = NUMLEVELS-1;            // level number of top level (bottom is 0) 
	
//	public static long rngSeed = 29292929290L; // 0 means no fixed seed, i.e. different numbers each run
	public static long rngSeed = 0L; // 0 means no fixed seed, i.e. different numbers each run
	public static Well1024a rng = rng();

//	   the U0 on the next line is equivalent to slopeUtil(score, tau, 2.0)
//	final public static BiFunction<Double, Double, Double> U0 = (Double score, Double tau)->Math.max(0, Math.min(1, tau/2.0))*(score);

//	final public static BiFunction<Double, Double, Double> U0 = (Double score, Double tau)-> flatUtil(score, tau);//cliff at tau==0
//	final public static BiFunction<Double, Double, Double> U0 = (Double score, Double tau)-> gradualUtil(score, tau);
	final public static BiFunction<Double, Double, Double> U0 = (Double score, Double tau)-> slopeUtil(score, tau, TAUSTARTCOST);
//	final public static BiFunction<Double, Double, Double> U0 = (Double score, Double tau)-> goodEnoughSoonEnough(score, tau, 10.0);
	
	public static double  goodEnoughSoonEnough(double score, double tau, double goodEnough){
		if (score >= goodEnough && tau >= 0){
			return 1.0;
		} else {
			return 0;
		}
	}
	
	public static double  flatUtil(double score, double tau){
		return tau < 0?0.0:(score);
	}
	public static double gradualUtil(double score, double tau){
		double multiple;
		if (tau < 0){
			multiple = 0.0;
		} else if (tau <= 0.5){
			multiple = 0.1;
		} else if (tau <= 1.0){
			multiple = 0.2;
		} else if (tau <= 1.5){
			multiple = 0.5;
		} else if (tau <= 2.0){
			multiple = 0.8;
		} else if (tau <= 2.5){
			multiple = 0.9;
		} else {
			multiple = 1.0;
		}
		return multiple * score; 
	}
	public static double slopeUtil(double score, double tau, double slopeStart){
		double multiple;
		if (tau < 0){
			multiple = 0.0;
		} else if (tau <= slopeStart){
			multiple = tau/slopeStart;
		} else {
			multiple = 1.0;
		}
		return multiple * score; 
	}
	public static Level [ ] levels;

	public static void main(String[] args) {
		levels = new Level[NUMLEVELS];
		for (int i = 0; i<NUMLEVELS; i++){
			levels[i] = new Level(i);
		}
		Level.initializeLevels(levels);
		Level rootLevel = levels[NUMLEVELS-1];
		System.out.println("Start stats");
		Level.setChildScoreParams(levels, NUMPARENTS, NUMKIDS);
		for (Level l = rootLevel; l != null; l = l.levelDown){
			l.printQSParams( );
		}
		System.out.println("StatsDone");
		
		final int REPEATS = 1000;
		
//		compareCEUtils(rootLevel.randomSpecs(true), 4.5);

//		   compare hufs vs waterfall with and without empirical utility
//		empiricalUtility = false;
//		System.out.println("empiricalUtility = "+empiricalUtility);
//		compareHW(rootLevel, BEGINTAU, REPEATS);
//		
//		empiricalUtility = true;
//		System.out.println("empiricalUtility = "+empiricalUtility);
//		compareHW(rootLevel, BEGINTAU, REPEATS);
		
		compareHW(rootLevel, BEGINTAU, REPEATS);
		
//		Design [] specsArray = new Design [REPEATS];
//		for (int i = 0; i< specsArray.length; i++){
//			specsArray[i] = rootLevel.randomSpecs(true); // true since not hypothetical specs		
//		}
//		
//		testSGU(specsArray[0]);		
		
//		Waterfall.testPerfectWaterfall(specsArray, BEGINTAU);
//		
//		testHufs(specsArray, BEGINTAU);
//				
//		SummaryStatistics [][] utils = new SummaryStatistics [MAXGENS][MAXGENS];
//		for  (int sm2=1; sm2<= MAXGENS-1; sm2++){
//			for (int sm1 = 1; sm1 <= MAXGENS-sm2; sm1++){
//				utils[sm1][sm2] = new SummaryStatistics();
//				}
//		}
//		Waterfall.testWaterfall(specsArray, MAXGENS, utils, REPEATS);
//		testReuse(rootLevel);

		System.out.println("run done");
	}
	
	/**
	 * tests save/reuse of design objects
	 * @param rootLevel level object that is at top level
	 */
	public static void testReuse(Level rootLevel){
		Design []  root = new Design[4];
		for  (int i = 0; i<4; i++){
			System.out.println("i "+i);
			root[i] = rootLevel.randomSpecs(true);
			Design l1d = root[i].generateChild(true);
			System.out.println("root:  "+root[i]+"\nl1d:   "+l1d);
			for (int j = 0; j<3; j++){
				System.out.println("l0 j "+j+" "+l1d.generateChild(j>0));
			}
		}
		System.out.println("reuse");
		for  (int i = 0; i<4; i++){
			System.out.println("i "+i);
			root[i].restartPregenerated();
			Design l1d = root[i].generateChild(true);
			System.out.println("root:  "+root[i]+"\nl1d:   "+l1d);
			for (int j = 0; j<4; j++){
				System.out.println("l0 j "+j+" "+l1d.generateChild(true));
			}
		}
	}

	/**
	 * compare hufs with waterfall
	 * 
	 * @param rootDesign
	 * @param startTau
	 */
	public static void compareHW(Level rootLevel, double startTau, int repeats){
		System.out.println("** compare hufs, waterfall.  repeats = "+repeats);
		int [] counts = Waterfall.optimalWaterfallCounts(rootLevel, startTau, 100);
		double wfEndTau = startTau - rootLevel.timeFromCounts(counts);
		System.out.println("**          startTau= "+startTau+" wfEndTau= "+wfEndTau);
		double sumDiffs = 0.0;
		double sumWfs = 0.0;
		double sumDOverWf = 0.0;
		int ctNeg=0;
		int ctZero = 0;
		int ctPos = 0;
		for (int rpt = 0; rpt < repeats; rpt++){
			Design specs = rootLevel.randomSpecs(true);
			Design wfResult = Waterfall.waterfall(specs, counts);
			wfResult.cachedUtil = Hufs.U0.apply(wfResult.score, wfEndTau);
			if(wfResult.cachedUtil == 0){
				System.out.println("wf util= 0, end tau= "+wfEndTau+" rpt= "+rpt);
			}
			Design hufsResult = hufs(specs, startTau, false);
			double d = (hufsResult.cachedUtil - wfResult.cachedUtil);
			sumDiffs += d;
			sumWfs += wfResult.cachedUtil;
			sumDOverWf += d/wfResult.cachedUtil;
			if (d == 0){
				ctZero++;
			} else if (d < 0) {
				ctNeg++;
			} else {
				ctPos++;
			}
//			System.out.format("cHW tau: %g, w:  %8.1f, h: %8.1f, d: %8g d/w: %g%n",
//					wfResult.cachedTau, wfResult.cachedUtil, hufsResult.cachedUtil, d, d/wfResult.cachedUtil);
		}
		System.out.println("-- compare hufs waterfall  avg diff: "+Utils.dblFmt(sumDiffs/repeats)+
				" avg(diff/wf): "+Utils.dblFmt(sumDOverWf/repeats)+", avgdiff/avgwf: "+
				Utils.dblFmt(sumDiffs/sumWfs)+", <,0,>: "+ctNeg+" "+ctZero+" "+ctPos);
	}
	/**
	 * compare empirical and computed-according-to-theory utilities
	 * @param specs
	 * @param tau
	 */
	public static void compareCEUtils(Design specs, double tau){
		Design child1 = specs;
		for(int i = 0; i<5; i++){
			double eutil = child1.empiricalUtil(tau, 0);
			double cutil = child1.utilOfGen(tau, 0);
			System.out.print("id  "+child1.ancestry+" empirical util: "+Utils.dblFmt(eutil));
			System.out.print(", computed util: "+Utils.dblFmt(cutil));

			System.out.print(", error: "+(cutil-eutil));
			System.out.println(", relative error "+Utils.dblFmt((cutil-eutil)/eutil));
		}
	}
	/*
	 * test of sumGroundUtilities and countGroundDesigns
	 */
	public static void testSGU(Design root){
		final int groundCt = 5;
		final int l1Count = 3;
		int [] counts = {0, groundCt, l1Count};
		Waterfall.waterfall(root, counts);
		double tau = Hufs.BEGINTAU;
		double sum = 0;
		for(Design des: root.level.groundOf().designs){
			des.cachedUtil = Hufs.U0.apply(des.score, tau);
			sum += des.cachedUtil;
			tau -= 0.5;
		}
		System.out.println("actual sum: "+sum+", count: "+groundCt);

		System.out.println(" calc sumGroundUts: "+ Design.sumGroundUtilities(root)+", ct: "+Design.countGroundDesigns(root));
	}
	public static void testHufsFall(Design [] specsArray, double tau){
		Design d;
		int repeat = specsArray.length;
		ArrayList<Double> utils = new ArrayList<Double>();
		
		for(int i = 0; i<repeat; i++){
//			System.out.println("thf i = "+i);
			d = hufsFall(specsArray[i], tau);
//			System.out.println("hf result "+d);
			System.out.flush();
			utils.add(d.cachedUtil);
//			System.out.println("hf result: score, util, tau, id: "+d.score+" "+d.cachedUtil+" "+d.cachedTau+" "+d.ancestry);
		}
		System.out.println("test hf avg util: "+ArrayFns.aLMean(utils)+"   stdev: "+ArrayFns.aLStDev(utils));
	}
	public static void testHufsFallPS(Design [] specsArray, double tau){
		System.out.println("Start test hf ps");
		Design d;
		int repeat = specsArray.length;
		ArrayList<Double> utils = new ArrayList<Double>();
		
		for(int i = 0; i<repeat; i++){
//			System.out.println("thf PS i = "+i);
			d = hufsFallPS(specsArray[i], tau);
//			System.out.println("hf PS result "+d);
			utils.add(Hufs.U0.apply(d.score, d.hufsDoneTau));
			System.out.println("hf PS result: score, util, tau, id: "+d+" "+d.score+" "+d.hufsDoneTau+" "+d.ancestry);
		}
		System.out.println("test hf ps rpt="+repeat+" avg util: "+ArrayFns.aLMean(utils)+"   stdev: "+ArrayFns.aLStDev(utils));
	}
	public static void testHufs(Design [] specsArray, double tau){
		Design d = null;
		int repeat = specsArray.length;
		ArrayList<Double> utils = new ArrayList<Double>();
		
		System.out.println("** hufs.        repeats = "+repeat);
		
		for(int i = 0; i<repeat; i++){
//			System.out.println("thf PS i = "+i);
			d = hufs(specsArray[i], tau, repeat==1);
			utils.add(d.cachedUtil);
//			System.out.println("hufs result: score, util, tau, id: "+d+" "+d.score+" "+d.cachedUtil+" "+d.cachedTau+" "+d.ancestry);
		}
		if (repeat==1){
			System.out.println("result is "+d);
		}
		System.out.println("-- end test hufs avg util: "+ArrayFns.aLMean(utils)+"   stdev: "+ArrayFns.aLStDev(utils));
	}
	public static Well1024a rng(){
		if (rngSeed == 0){
			return new Well1024a();
		} else {
			return new Well1024a (rngSeed);
		}
	}

	public static Design hufsFall(Design rootDesign, double startTau){
		if (! rootDesign.level.isTop()){
			throw new IllegalArgumentException();
		}
		Design focus = rootDesign; // current parent of generate-child operations
		double tau = startTau;
		Design bestChild = null; // not a problem to name variable same as instance variable
								 // because not in class Design and method is static
		rootDesign.restartPregenerated();
		rootDesign.clearLevels();

		while (! focus.level.isGround()){
			Design newChild = focus.generateChild(true);
			tau = focus.timeDone(tau);
			//			System.out.println("levels p,c "+focus.level.number+" " + newChild.level.number);
			if (tau < 0){  // should not happen
				return null;
			}
//			System.out.println("gen at level: "+newChild.level.number+" score: "+newChild.score);
//			System.out.println("tau = "+tau);
			if (bestChild == null || newChild.score > bestChild.score){
//				System.out.println("new best score: "+newChild.score);
				bestChild = newChild;
			}
			double childUtil = bestChild.utility(tau);
			double parentUtil = focus.utility(tau);
//			System.out.println("util of child: "+ childUtil+" parent: "+parentUtil+" "+focus.level.number);
			if (childUtil >= parentUtil){
//				System.out.println("switch to level "+bestChild.level.number+" score: "+bestChild.score);
				focus = bestChild;
				bestChild = null; // change this if parent-switching
			}
		}
//		System.out.println("score, utility, tau of result: "+focus.score+" "+focus.cachedUtil+" "+focus.cachedTau+" "+focus.ancestry);
		return focus;
	}
/**
 * huffs fall with parent switching
 * @param rootDesign
 * @param startTau
 * @return
 *  **** when calling this method, set rootDesign.nextChildNumber to 0 and .kids to empty.
 *  Also  make sure root specs object is inly thing in top-level designs array list
 */
	public static Design hufsFallPS(Design rootDesign, double startTau){
		if (! rootDesign.level.isTop()){
			throw new IllegalArgumentException();
		}
		rootDesign.restartPregenerated();
		rootDesign.clearLevels();

		Design focus = rootDesign; // current parent of generate-child operations
		double tau = startTau;
		Design bestChild; 
		while (! focus.level.isGround()){
			Design newChild = focus.generateChild(true);
			System.out.println("PS gen, parent = "+focus+" new child = "+newChild);
			tau = focus.timeDone(tau);
//			System.out.println("tau = "+tau);
			focus = focus.level.bestOnLevel(tau);
			double focusUtility = focus.utility(tau);
			System.out.println("best parent "+focus+" util "+focusUtility);
			bestChild = focus.level.levelDown.bestOnLevel(tau);
			double childUtility = bestChild.utility(tau);
			System.out.println("PS best child: "+bestChild+" util "+childUtility);
			if (childUtility > focusUtility){
				focus = bestChild;
			}
		}
		System.out.println("**hfps: score, utility, tau of result: "+focus.score+" "+Hufs.U0.apply(focus.score,focus.hufsDoneTau)+" "+focus.hufsDoneTau+" "+focus.ancestry);
		return focus;
	}
	public static Design hufs(Design rootDesign, double startTau, boolean traceDetails){
		if (! rootDesign.level.isTop()){
			throw new IllegalArgumentException();
		}
		rootDesign.restartPregenerated();
		rootDesign.clearLevels( );
		Design focus = rootDesign; // current parent of generate-child operations
		double tau = startTau;
//		Design bestChild; 
		while (! focus.level.isGround()){
			Design newChild = focus.generateChild(true); // newChild redundant since generateChild adds child to child.level.designs
			if(traceDetails){
				System.out.println("xxx hufs gen, parent= "+focus+" child= "+newChild);
			}
			tau = focus.timeDone(tau);
			Design savedFocus = focus;
			focus = rootDesign.level.highestUtilityDesign(tau, traceDetails);
			if (focus != savedFocus && focus.level.number+1 != savedFocus.level.number){ // if change focus to other than
				// 																a design at next level.   apparently never happens
//				throw new IllegalArgumentException();
			}
		}
//		System.out.println("**hufs: utility, tau of result: "+" "+focus.ca chedUtil+" "+focus.cachedTau+" "+focus.ancestry+" "+focus.id);
		return focus;
	}

}
/* testReuse correct results:
**********    setCQDParams level 2
**********    setCQDParams level 1
Start stats
---------2
---------1
  2   mean  slope    10.7167   base     5.8911
     stdev  slope     0.0000   base     1.7225
  1   mean  slope     0.4991   base    16.6869
     stdev  slope     0.0000   base     4.1741
StatsDone
i 0
root:  Design 3240: l=2 sc=1.4809542072734563
l1d:   Design 3241: l=1 sc=22.194883575075696
l0 j 0 Design 3242: l=0 sc=26.318815821557532
l0 j 1 Design 3243: l=0 sc=27.232482692873333
l0 j 2 Design 3244: l=0 sc=27.596123677956022
i 1
root:  Design 3245: l=2 sc=1.6417856946386447
l1d:   Design 3246: l=1 sc=24.295909587103743
l0 j 0 Design 3247: l=0 sc=31.577257809754006
l0 j 1 Design 3248: l=0 sc=21.95074510015622
l0 j 2 Design 3249: l=0 sc=30.46489507429039
i 2
root:  Design 3250: l=2 sc=1.827698378229154
l1d:   Design 3251: l=1 sc=28.20683239523384
l0 j 0 Design 3252: l=0 sc=24.66955678213241
l0 j 1 Design 3253: l=0 sc=31.080432353739365
l0 j 2 Design 3254: l=0 sc=25.08683654603465
i 3
root:  Design 3255: l=2 sc=1.890162758777457
l1d:   Design 3256: l=1 sc=23.50201822616878
l0 j 0 Design 3257: l=0 sc=30.073379179354188
l0 j 1 Design 3258: l=0 sc=17.52584746391482
l0 j 2 Design 3259: l=0 sc=24.109932934460755
reuse
i 0
root:  Design 3240: l=2 sc=1.4809542072734563
l1d:   Design 3241: l=1 sc=22.194883575075696
l0 j 0 Design 3243: l=0 sc=27.232482692873333
l0 j 1 Design 3244: l=0 sc=27.596123677956022
l0 j 2 Design 3260: l=0 sc=28.606748927013168
l0 j 3 Design 3261: l=0 sc=22.691945233570223
i 1
root:  Design 3245: l=2 sc=1.6417856946386447
l1d:   Design 3246: l=1 sc=24.295909587103743
l0 j 0 Design 3248: l=0 sc=21.95074510015622
l0 j 1 Design 3249: l=0 sc=30.46489507429039
l0 j 2 Design 3262: l=0 sc=35.22131898556634
l0 j 3 Design 3263: l=0 sc=24.351728839062986
i 2
root:  Design 3250: l=2 sc=1.827698378229154
l1d:   Design 3251: l=1 sc=28.20683239523384
l0 j 0 Design 3253: l=0 sc=31.080432353739365
l0 j 1 Design 3254: l=0 sc=25.08683654603465
l0 j 2 Design 3264: l=0 sc=27.504703211963008
l0 j 3 Design 3265: l=0 sc=27.29411505546661
i 3
root:  Design 3255: l=2 sc=1.890162758777457
l1d:   Design 3256: l=1 sc=23.50201822616878
l0 j 0 Design 3258: l=0 sc=17.52584746391482
l0 j 1 Design 3259: l=0 sc=24.109932934460755
l0 j 2 Design 3266: l=0 sc=30.868209658912367
l0 j 3 Design 3267: l=0 sc=26.37615954715922

*/