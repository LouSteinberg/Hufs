package hufs;

import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.distribution.*;


public class Design {
	public static int nextId = 0;
	public int id = nextId++;
	public ArrayList<Integer> ancestry;
	public Level level;
	public Design parent; // parent design or null if this is root
	public ArrayList<Design> kids; // children of this design generated in
									// current XFall call
	// initialized by restartPregenerated()
	public ArrayList<Design> preGeneratedKids = new ArrayList<Design>(); // children
																			// of
																			// this
																			// design
	// including those generated by previous runs.
	// kids should be a prefix of preGeneratedKids
	// public Structure structure; // description of this design or specs if
	// this is root
	public double score; // score according to level's heuristic eval fn
	public double cachedUtil; // cached utility of object
	public double wfDoneTau; // tau at which design was created by latest waterfall
	public double hufsDoneTau; // tau at which design was created by latest hufs
	// public double utility; // calculated utility of generating from this
	// design,
	// based on this design's child score
	// distribution and on child-level utility fn
	public RealDistribution childScoreDistribution;
	public int nextChildNumber;

	public double quality; // true quality of this design
	public RealDistribution childQualityDistribution; // distribution of child
														// qualities

	// **** constructors

	// generate a child of parent

	/*
	 * Constructor for Design object, given a parent Design
	 * 
	 * @param reuse is false => new Design is hypothetical, don't save on kids
	 * of parent
	 */
	public Design(Design parent, boolean reuse) {
		fillIn(parent, reuse);
	}

	public void fillIn(Design parent, boolean reuse) {
		if (parent.level.isGround()) {
			// cannot generate child of a design at lowest level
			throw new IllegalArgumentException();
		}
		Design child = this; // for easier reading
		child.parent = parent;
		child.level = parent.level.levelDown;
		child.preGeneratedKids = new ArrayList<Design>();
		child.restartPregenerated();
		child.ancestry = new ArrayList<Integer>(parent.ancestry);
		child.ancestry.add(child.id);
		
		child.quality = parent.childQualityDistribution.sample();
		if (child.level.number > 0) {
			// set CQD of child, i.e., distribution of qualities of children of *child*
			child.childQualityDistribution = child.level.genChildQDistribution(child.quality);
		}
		child.score = child.level.scoreFromQuality(child.quality);
		child.childScoreDistribution = child.genCSD();

		if (reuse) {
			parent.kids.add(child);
		}
	}

	// create a design at the root level, representing a problem
	public Design(Level rootLevel, boolean reuse) {
		this.parent = null;
		this.level = rootLevel;
		this.preGeneratedKids = new ArrayList<Design>(); // needed since restart
									// only resets kids, not preGeneratedKids
		this.restartPregenerated();
		this.ancestry = new ArrayList<Integer>();
		this.ancestry.add(this.id);
		this.quality = 1.0 + Hufs.rng.nextDouble();
		this.childQualityDistribution = level.genChildQDistribution(this.quality);
		this.score = level.scoreFromQuality(quality);
		this.childScoreDistribution = level.genCSD(score);
		if (reuse) {
			this.level.designs.add(this);
		}
	}

	/**
	 * generates a child of this
	 * 
	 * @param reuse
	 *            true if should reuse pregenerated and save newly generated
	 *            ones
	 * @return the next child, a pregenerated one if it exists and reuse is
	 *         true, or new
	 *
	 *         initializes/updates nextChildNumber and kids of this and child,
	 *         either directly or by calling a method which does
	 */
	public Design generateChild(boolean reuse) {
		Design child;
		Design focus = this;
		// System.out.println("gc reuse "+reuse+" "+nextId) ;
		if (reuse) {
			// System.out.println("gc: pa nchldn pgk "+this+"
			// "+nextChildNumber+" "+preGeneratedKids.size());
			if (nextChildNumber > preGeneratedKids.size()) { // haven't yet
																// generated
																// child
																// childNumber-1
				// System.out.println("gc iae: "+nextChildNumber+"
				// "+preGeneratedKids.size());
				throw new IllegalArgumentException();

			}
			if (nextChildNumber < preGeneratedKids.size()) { // if we have child
																// already
				// generated, return it
				// System.out.print("returning ");
				child = this.preGeneratedKids.get(nextChildNumber);
				child.restartPregenerated(); // restart kid sequence of *child*
				focus.kids.add(child); // this kid is now visible in *parent*
			} else { // no child yet with given number
				// System.out.print("generating ");
				child = new Design(this, true); // this calls
												// restartPregenerated on the
												// *child* & clears its
												// preGeneartedKids on new
												// design
				focus.preGeneratedKids.add(child); // add kid to *parent*
													// pregenerated list
			}
			focus.nextChildNumber++;
			child.level.designs.add(child);

		} else { // reuse is false
			child = new Design(this, false);
			child.restartPregenerated();
			child.preGeneratedKids = new ArrayList<Design>();
		}
		// System.out.format("> %d (%d %7.2f) -> %d (%d %7.2f)%n",
		// focus.id, focus.level.number, focus.score, child.id,
		// child.level.number, child.score);
		return child;
	}

	/**
	 * restart generating the sequence of saved kids of this
	 */
	public void restartPregenerated() {
		nextChildNumber = 0;
		kids = new ArrayList<Design>();
//		this.cachedTau = -1.0;
//		this.cachedUtil = -1.0;
	}
	/**
	 * clear level.designs for all levels from this.level on down, adds this as
	 * to designs field of top level
	 */
	public void clearLevels() {
		for (Level l = level; l != null; l = l.levelDown) {
			l.designs = new ArrayList<Design>();
		}
		level.designs.add(this);
	}
	
	/**
	 * 
	 */

	public String toString() {
		return "Design " + ancestry + " " + id
				+ String.format(": l=%d sc=%-4.4f  doneTau wf, hufs %3.1f, %3.1f", level.number, score, wfDoneTau, hufsDoneTau, cachedUtil);
	}

	public String altToString() {
		String res = String.format("Design: %d, l=%d,sc=%7.2f ", id, level.number, score) + ancestry;
		return res;
	}
	
	public void printDescendants( ){
		System.out.println(this);
		for (Design d: this.preGeneratedKids){
			d.printDescendants();
		}
	}

	public RealDistribution genCSD() {
		double mean = level.childSMean(score);
		double stDev = level.childSStDev(score);
//		if (kids.size() > 0) {
//			double[] scores = new double[kids.size()];
//			for (int j = 0; j < kids.size(); j++) {
//				scores[j] = kids.get(j).score;
//			}
//			mean = ArrayFns.arrayMean(scores, level.childSMean(score));
//			if (kids.size() > 3) {
//				stDev = ArrayFns.arrayStDev(scores);
//			}
//
//		}
		if (mean == 0 && stDev == 0) { // if the CSD parameters have not yet
										// been set
			// throw new IllegalArgumentException();
			return null;
		}
		return new BoundedNormalDistribution(mean, stDev);
	}

	// **** estimate utility

	public double timeDone(double tau) { // if we start generating a child from
		// this design at time tau, when will it be done?
		return tau - level.timeToGenFrom;
	}
	

	public double utility(double tau) {
		if (Hufs.empiricalUtility) {
			return empiricalUtil(tau, 0);
		} else {
			return utilOfGen(tau, 0);
		}
	}

	/**
	 * 
	 * @param tau
	 * @param ulev
	 *            is for indenting
	 * @return utility of having this design at time tau
	 */
	public double utilOfGen(double tau, int ulev) {
		double util;
		if (tau < 0.0) { // too late, utility is 0
			util = 0.0;
		} else if (level.isGround()) { // this design is at ground level.
			util = Hufs.U0.apply(score, tau); // utility is given by
												// user-supplied U0.
		} else { // all you can do with this design is generate a child from it
			// System.out.println(indent(ulev)+"stt des uOG level, tau:
			// "+level.number+", "+tau+" "+score);
			double putil = utilOfGen(timeDone(tau), ulev + 1); // if ignore
																// child, util
																// is util of
			// parent but after gen is done
			// System.out.println(indent(ulev)+"putil "+ putil);
			TrapezoidIntegrator ti = new TrapezoidIntegrator(1e-5, 1e-5, 1, 63);
			// System.out.println(indent(ulev)+"D ti start");
			if (childScoreDistribution == null) {
				throw new IllegalArgumentException();
			}
			// System.out.println(indent(ulev)+"integrate; tau, putil, plevel
			// "+tau+" "+putil+" "+level.number);
//			System.out.println("begin integration");
			util = ti.integrate(10000, sc -> utilIntegrand(putil, sc, tau, ulev + 1),
					childScoreDistribution.getSupportLowerBound(), childScoreDistribution.getSupportUpperBound());

//			System.out.println("end integration");
			// System.out.println(indent(ulev)+"integral: "+util);
		}
		cachedUtil = util; // cachedUtil and cachedTau are set at the end here
							// so that the
		hufsDoneTau = tau; // values set by a top-level call to utilOfGen are the
							// ones that remain
		return util;
	}

	/**
	 * 
	 * @param tau
	 * @param ulev
	 * @return returns the empirical utility of having this design at time tau.
	 */
	public double empiricalUtil(double tau, int ulev) {
		final int ITERATIONS = 100;
		double util;
		if (tau < 0.0) { // too late, utility is 0
			util = 0.0;
		} else if (level.isGround()) { // this design is at ground level.
			util = Hufs.U0.apply(score, tau); // utility is given by
												// user-supplied U0.
		} else { // all you can do with this design is generate a child from it
			// System.out.println(indent(ulev)+"stt des uOG level, tau:
			// "+level.number+", "+tau+" "+score);
			double putil = empiricalUtil(timeDone(tau), ulev + 1); // if ignore
																	// child,
																	// util is
																	// util of
			// parent but after gen is done
			// System.out.println(indent(ulev)+"putil "+ putil);
			util = 0;
			for (int j = 0; j <= ITERATIONS; j++) {
				// Hufs.euct++;
				Design child = generateChild(false);
				double newUtil = Math.max(putil, child.empiricalUtil(timeDone(tau), ulev + 1));
				util += newUtil;
			}
			util = util / ITERATIONS;
		}
//		cachedUtil = util; // cachedUtil and cachedTau are set at the end here
//							// so that the
//		hufsDoneTau = tau; // values set by a top-level call to utilOfGen are the
							// ones that remain
		return util;
	}

	public static String indent(int n) {
		String res = "";
		for (int i = 0; i < 4 * n; i++) {
			res += " ";
		}
		return res;
	}

	// public static int integrandCt = 0;
	/**
	 * 
	 * @param putil:
	 *            util of parent after generating child
	 * @param sc:
	 *            score of child generated
	 * @param tau:
	 *            time generation starts
	 * @param ulev
	 *            is for indentation
	 * @return expected value of util of generating a child of this design
	 */
	public double utilIntegrand(double putil, double sc, double tau, int ulev) {
		double csdValue = childScoreDistribution.density(sc); // probability
																// that child
																// score will be
																// sc
		double levelEval = level.levelDown.utilOfGen(sc, timeDone(tau), ulev + 1); // util
																					// of
																					// child
																					// with
																					// score
																					// sc
		// System.out.println(indent(ulev)+"D sc, csd(sc), level eval: "+sc+" "+
		// csdValue+' '+levelEval);
//		System.out.println("score "+sc+", value "+levelEval+", csd "+csdValue);
		return csdValue * Math.max(putil, levelEval);
	}
	/**
	 * 
	 * @param focus  the root Design
	 * @param init   initial value for soFar
	 * @param update  fn (next, soFar)-> double, new value of soFar
	 * @param groundValue fn Design -> double, value of next for single ground design
	 * @return
	 */
	public static double summarize(Design focus, double init, BiFunction<Double, Double, Double> update, Function<Design, Double> groundValue){
		if (focus.level.isGround()){
			return groundValue.apply(focus);
		} else {
			double result = init;
			for (Design kid: focus.kids){
				result = update.apply(summarize(kid, init, update, groundValue), result);
			}
			return result;
		}
	}
	public static double sumGroundUtilities(Design rootDesign){
		return summarize(rootDesign, 0.0, (next,  soFar)->soFar+next, d->d.cachedUtil);
	}
	public static double countGroundDesigns(Design rootDesign){
		return summarize(rootDesign, 0.0, (next, soFar)->soFar+next, d->1.0);
	}
}