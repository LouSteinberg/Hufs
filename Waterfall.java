package hufs;


import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class Waterfall {

	/*
	 * returns a vector of counts that produce optimal average waterfall utility from repeats random
	 * rootlevel specs.  ASSUMES 3 LEVELS.
	 * 
	 */
	public static int [] optimalWaterfallCounts(Level rootLevel, double tauAtStart, int repeats){
		System.out.println("*  get optWaterfallCounts.   repeats = "+repeats+ " tauAtStart = "+tauAtStart);
		double time1 = rootLevel.levelDown.timeToGenFrom;
		double time2 = rootLevel.timeToGenFrom;
		double maxUtil = 0.0;
		int bestCt1 = 0;
		int bestCt2 = 0;
		Design specs [ ] = new Design [repeats];
		for(int rpt = 0; rpt<repeats; rpt++){
			specs[rpt]= rootLevel.randomSpecs(true);
		}
		System.out.println("time1= "+time1);
		System.out.println("time2= "+time2);
		System.out.println("V ct2    ct1>");
		for (int ct2=1; ct2*time2<= tauAtStart - time1; ct2++){
			for (int ct1 = 1; ct1*time1<= tauAtStart-ct2*time2; ct1++){	
				int [] counts = {0, ct1, ct2};
				double sumUtils = 0.0;
				double tauAtEnd = tauAtStart-(ct1*time1+ct2*time2);
				for (int rpt = 0; rpt < repeats; rpt++){
					Design rootSpecs = specs[rpt];
					rootSpecs.restartPregenerated();
					Design wfResult = waterfall(rootSpecs, counts);
					double util = Hufs.U0.apply(wfResult.score, tauAtEnd);
					sumUtils += util;
				}
				double avgUtil = sumUtils / repeats;
				System.out.format("%8.2f   ", avgUtil);
				if (avgUtil > maxUtil){
					bestCt1 = ct1;
					bestCt2 = ct2;
					maxUtil = avgUtil;
				}
			}
			System.out.println();
		}
//		specs[0].printDescendants();
		System.out.println("-  optimal waterfall counts:  1: "+bestCt1+", 2: "+bestCt2);
		int [] result = {0, bestCt1, bestCt2};
		return result;
	}
	/**
	 * does a 2-level waterfall in all possible ways for several problems but saves designs so we 
	 * can later run hufs on the same sets of designs
	 * @param specsArray - an array of rootDesigns, with repeats elements: the specs for the designs
	 * @param maxGens - total number of children to be generated, including
	 * 					both levels but not including the specs, since that 
	 * 					is not a child of anything.
	 * 	 !!!!  Does not repect level.timeToGenFrom !!!!
	 * 
	 * ASSUMES TIME PER GENERATE CHILD IS ALWAYS .5
	 */

	public static void testWaterfall(Design [] specsArray, int maxGens, SummaryStatistics[][] stats, int repeats){
		System.out.println("** test waterfall.   repeats = "+repeats);
		double tauAtStart = Hufs.BEGINTAU;
		Design [] [] resultArray = new Design[maxGens][maxGens]; 
		Design result = null;
		for (int rpt = 0; rpt < repeats; rpt++){
//			System.out.println("TWF  "+rpt+" "+specsArray[rpt]);
			for (int sm2=1; sm2<= maxGens-1; sm2++){
				for (int sm1 = 1; sm1 <= maxGens-sm2; sm1++){	
					int [] counts = {0, sm1, sm2};
					result = waterfall(specsArray[rpt], counts);
					resultArray[sm1][sm2] = result;
//					System.out.print(result.id+": "+result.score+"    ");
					double tauAtEnd = tauAtStart-(sm1+sm2)*0.5; // <---- FIX HERE
					double util = Hufs.U0.apply(result.score, tauAtEnd);
//					System.out.format("sm1,2: %d %d, score: %8.2f, tau %8.2f,  util: %8.2f\n", sm1, sm2, result.score, tauAtEnd, util);
					stats[sm1][sm2].addValue(util);
//					System.out.print(result.id+"   ");
				}
//				System.out.println();
			}
		}
		System.out.println();
		System.out.print("sm2 \\ sm1");
		for (int sm1 = 1; sm1 <= maxGens-1; sm1++){
			System.out.format("%7d         ", sm1);
			}
		System.out.println( );
		for  (int sm2=1; sm2<= maxGens-1; sm2++){
			System.out.print(" "+sm2+"      ");
			for (int sm1 = 1; sm1 <= maxGens-sm2; sm1++){	
				if (repeats == 1){
					System.out.format(" %8.1f %d", resultArray[sm1][sm2].score,resultArray[sm1][sm2].id);
				} else {
					System.out.format(" %8.1f %6.0f", stats[sm1][sm2].getMean(), stats[sm1][sm2].getStandardDeviation());
				}
			}
			System.out.println( );
		}
		System.out.println("-- end Waterfall. ");

	}
	/**
	 * 
	 */		
	public static Design waterfall(Design rootDesign, int [ ] counts){
		rootDesign.restartPregenerated();
		rootDesign.clearLevels();
//		System.out.println("WF "+rootDesign.id+" "+rootDesign.score); 
		Design bestChild=null;
		Design focus;
		for (focus = rootDesign; ! focus.level.isGround( ); focus = bestChild){
			bestChild = focus.generateChild(true);
//			System.out.println(" wf gen p: "+focus+" 1stc: "+bestChild);
			for (int i = 1; i < counts[focus.level.number]; i++){
				Design newDesign = focus.generateChild(true);
				if (newDesign.score > bestChild.score){
					bestChild = newDesign;
				}
//			 	System.out.println(" wf 2 gen p: "+focus+" nc: "+newDesign);
			}
		}
//		System.out.println(" result: "+bestChild.ancestry+" "+bestChild.id);
		return bestChild;
	}

	/*
	 * ASSUMES TIME PER GENERATE CHILD IS ALWAYS .5
	 */
	public static void testPerfectWaterfall(Design [] specsArray, double tauAtStart){
		int repeats = specsArray.length;
		System.out.println("** test perfect wf. repeats =  "+repeats);
		int maxGens = (int) (tauAtStart / 0.5); // <---- FIX HERE
		double utilSum = 0.0;
		Design result = null;
		for (int rpt = 0; rpt < repeats; rpt++){
			result = perfectWaterfall(specsArray[rpt], maxGens);
			double util = result.cachedUtil;
//			System.out.format("%d: %5.2f ",result.id, util);
//			System.out.format("score: %8.2f, tau %8.2f,  util: %8.2f\n", result.score, result.cachedTau, util);
			utilSum += util;
		}
//		System.out.println( );
		if (repeats == 1){
			System.out.println("result is "+result);
		}
		System.out.println("-- end test perfect wf.  avg util: "+utilSum/repeats);
	}
	/**
	 * does waterfall for each possible choice of how many children to generate at each level, using up to maxGens.
	 * 
	 * @param specs the Design object representing specs of design problem
	 * @param maxGens the max number of total children that can be generated.  
	 *  !!!!  Does not repect level.timeToGenFrom !!!!
	 *    This code assumes generation at each
	 *        level takes time of 0.5 units, so tau at the start is 0.5 * maxGens
	 * @return best resulting, level 0 design
	 */
	public static Design perfectWaterfall(Design specs, int maxGens){
		specs.restartPregenerated();
		specs.clearLevels();
		double tauAtStart = maxGens * 0.5; //  <---- *** FIX HERE
		double tauAtEnd;
		double bestUtil = -1;
//		double bestTau = -1;
		Design bestDesign = null;
		for (int sm2=1; sm2<= maxGens-1; sm2++){
			for (int sm1 = 1; sm1 <= maxGens-sm2; sm1++){	
				int [] counts = {0, sm1, sm2};
				// old cached Tau and Util are cleared by call to waterfall
				Design result = waterfall(specs, counts);
//				tauAtEnd = tauAtStart-(sm1+sm2)*0.5;  //  <---- *** FIX HERE
				tauAtEnd = tauAtStart 
						- sm2*specs.level.timeToGenFrom 
						- sm1*specs.level.levelDown.timeToGenFrom;
				result.hufsDoneTau = tauAtEnd;
				double util = Hufs.U0.apply(result.score, tauAtEnd);
				result.cachedUtil = util;
				System.out.println("pw R: " +result);
				if (bestUtil < util){
					bestDesign = result;
					bestUtil = util;
//					bestTau = tauAtEnd;
				}
//				System.out.println("pw B:    "+ bestDesign.ancestry+" sc="+bestDesign.score
//						+" util@ "+bestTau+" "+bestUtil);
			}
		}
//		bestDesign.cachedTau = bestTau;
		bestDesign.cachedUtil = bestUtil;
		return bestDesign;
	}
}