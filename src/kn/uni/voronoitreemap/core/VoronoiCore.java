/*******************************************************************************
 * Copyright (c) 2013 Arlind Nocaj, University of Konstanz.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * For distributors of proprietary software, other licensing is possible on request: arlind.nocaj@gmail.com
 * 
 * This work is based on the publication below, please cite on usage, e.g.,  when publishing an article.
 * Arlind Nocaj, Ulrik Brandes, "Computing Voronoi Treemaps: Faster, Simpler, and Resolution-independent", Computer Graphics Forum, vol. 31, no. 3, June 2012, pp. 855-864
 ******************************************************************************/
package kn.uni.voronoitreemap.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JFrame;
import com.sun.awt.AWTUtilities;

import kn.uni.voronoitreemap.datastructure.OpenList;
import kn.uni.voronoitreemap.debug.ImageFrame;
import kn.uni.voronoitreemap.debuge.Colors;
import kn.uni.voronoitreemap.diagram.PowerDiagram;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;
import kn.uni.voronoitreemap.treemap.VoronoiTreemap;

/**
 * Core class for generating Voronoi Treemaps. position and weight of sites is
 * changed on each iteration to get the wanted area for a cell.
 * 
 * @author Arlind Nocaj
 */
public class VoronoiCore {
	// TODO remove when finished
	public static boolean debugMode = false;
	public static JFrame frame = new JFrame();
	public static ImageFrame iframe;
	public static Graphics2D graphics;
	// private SVGGraphics2D svgGraphics;
	private static Random rand = new Random(99);
	/** If this mode is true, svg files are created in each iteration **/
	private boolean outPutMode;
	/** Counter for creating the output files. **/
	private int outCounter = 1;

	/**
	 * core variables
	 */

	private final boolean firstIteration = true;
	private static final double nearlyOne = 0.99;

	/**
	 * Settings for the Core
	 */

	/**
	 * A site which wants to reach more then preflowPercentage is considered for
	 * preflow extrapolation. default value: 0.08
	 */
	private double preflowPercentage = 0.08;
	/**
	 * If a region wants to increase its area by the factor preflowIncrease it
	 * is considered for preflow extrapolation default value is 1.5 or 1.6
	 */
	private double preflowIncrease = 1.5;

	private boolean useNegativeWeights = true;

	private boolean useExtrapolation = false;
	protected boolean cancelOnAreaErrorThreshold = false;
	protected boolean cancelOnMaxIterat = true;
	protected double errorAreaThreshold = 0.05;
	protected PolygonSimple clipPolygon;
	private boolean guaranteeInvariant = false;

	protected OpenList sites;
	private int numberMaxIterations;
	protected double completeArea = 1;
	protected boolean preflowFinished = false;

	double maxDelta = 0;

	protected PowerDiagram diagram;
	private double currentMaxError;
	protected double currentAreaError = 0;

	/**
	 * Temporary, only for debugging
	 * 
	 * @return
	 */
	protected double currentEuclidChange = 0;
	protected double lastMaxWeight;
	public double lastAreaError = 1;
	public double lastAVGError = 1;
	public double lastMaxError = 1;
	public double lastSumErrorChange = 1;
	protected double lastEuclidChange = 0;
	private double currentMaxNegativeWeight;
	private boolean aggressiveMode = false;

	public OpenList getSiteList() {
		return sites;
	}

	public static void setDebugMode() {
		debugMode = true;
		BufferedImage image = new BufferedImage(2000, 2000,
				BufferedImage.TYPE_INT_RGB);

		iframe = new ImageFrame(image);
		iframe.setVisible(true);
		//iframe.setShape(VoronoiTreemap.rootPolygon);   //不成功
		 //com.sun.awt.AWTUtilities.setWindowOpacity(iframe, 0.6f);
		iframe.setBounds(20, 20, 600, 600);
		iframe.setBackground(Color.white);
		graphics = image.createGraphics();
	}

	/**
	 * The resulting Voronoi Cells are clipped with this polygon
	 * 
	 * @param polygon
	 */
	public void setClipPolygon(PolygonSimple polygon) {
		clipPolygon = polygon;
		maxDelta = Math.max(clipPolygon.getBounds2D().getWidth(), clipPolygon
				.getBounds2D().getHeight());

		if (diagram != null) {
			diagram.setClipPoly(polygon);
		}
	}

	/**
	 * 
	 * @param rectangle
	 */
	public void setClipPolygon(Rectangle2D rectangle) {
		PolygonSimple poly = new PolygonSimple();
		poly.add(rectangle.getMinX(), rectangle.getMinY());
		poly.add(rectangle.getMaxX(), rectangle.getMinY());
		poly.add(rectangle.getMaxX(), rectangle.getMaxY());
		poly.add(rectangle.getMinX(), rectangle.getMaxY());
		setClipPolygon(poly);
	}

	private void init() {
		diagram = new PowerDiagram();
		if (clipPolygon != null) {
			maxDelta = Math.max(clipPolygon.getBounds2D().getWidth(),
					clipPolygon.getBounds2D().getHeight());
		}
	}

	public VoronoiCore() {
		sites = new OpenList();
		init();
	}

	public VoronoiCore(Rectangle2D rectangle) {
		PolygonSimple poly = new PolygonSimple(4);
		poly.add(rectangle.getMinX(), rectangle.getMinY());
		poly.add(rectangle.getMinX() + rectangle.getWidth(),
				rectangle.getMinY());
		poly.add(rectangle.getMinX() + rectangle.getWidth(),
				rectangle.getMinY() + rectangle.getHeight());
		poly.add(rectangle.getMinX(),
				rectangle.getMinY() + rectangle.getHeight());

		this.clipPolygon = poly;
		init();
		diagram.setClipPoly(poly);

	}

	public VoronoiCore(PolygonSimple clipPolygon) {
		sites = new OpenList();
		this.clipPolygon = clipPolygon;
		init();
		diagram.setClipPoly(clipPolygon);
		//iframe.setShape(VoronoiTreemap.rootPolygon);
	}

	public VoronoiCore(OpenList sites, PolygonSimple clipPolygon) {
		this.sites = sites;
		this.clipPolygon = clipPolygon;
		init();
	}

	/**
	 * Adds a site, which is a voronoi cell to the list
	 * 
	 * @param site
	 */
	public void addSite(Site site) {
		sites.add(site);
	}

	public void iterate() {
		currentMaxNegativeWeight = 0;
		currentEuclidChange = 0;
		currentAreaError = 0;
		currentMaxError = 0;

		// move the sites to their center
		completeArea = clipPolygon.getArea();
		//drawCurrentState(false);
		double errorArea = 0;

		/**
		 * Extrapolation：推理
		 */
		int amount = 0;
		if (isUseExtrapolation() && !preflowFinished) {
			Site[] array = sites.array;
			int size = sites.size;
			for (int z = 0; z < size; z++) {
				Site site = array[z];
				PolygonSimple poly = site.getPolygon();
				double percentage = site.getPercentage();
				double wantedArea = completeArea * percentage;
				double currentArea = poly.getArea();
				double increase = wantedArea / currentArea;
				/**
				 * Extrapolation
				 */
				if (percentage >= getPreflowPercentage()
						&& increase >= getPreflowIncrease()) {
					// System.out.println("Percentage:" + percentage
					// + "\t Increase:" + preflowIncrease);
					amount++;
					double radiusIncrease = Math.sqrt(increase);
					double radiusCurrent = Math.sqrt(site.getWeight());
					double deltaRadius = radiusCurrent * (radiusIncrease)
							- radiusCurrent;

					for (int y = 0; y < size; y++) {
						Site other = array[y];
						if (other != site
								&& other.getPercentage() < getPreflowPercentage()) {
							Point2D vector = new Point2D();
							// Vector2d vector=new Vec
							vector.x = other.getX() - site.getX();
							vector.y = other.getY() - site.getY();

							double length = vector.length();
							double linearDistanceScaledDeltaRadius = deltaRadius
									* (1 - ((length - radiusCurrent) / maxDelta));  //??
							double scale = linearDistanceScaledDeltaRadius
									/ length;
							// double scale = deltaRadius / length;
							vector.scale(scale);

							other.preflowVector.x += vector.x;
							other.preflowVector.y += vector.y;
						}
					}

				}

			}
		}
		if (amount == 0) {
			preflowFinished = true;
		}

		/**
		 * move to centers
		 */
		Site[] array = sites.array;
		int size = sites.size;
		for (int z = 0; z < size; z++) {
			Site point = array[z];
			// point.preflowVector.scale(1.0/(double)amount);

			double error = 0;

			double percentage = point.getPercentage();
			PolygonSimple poly = point.getPolygon();
			if (poly != null) {
				Point2D centroid = poly.getCentroid();
				double centroidX = centroid.getX();
				double centroidY = centroid.getY();
				double dx = centroidX - point.getX();
				double dy = centroidY - point.getY();
				currentEuclidChange += dx * dx + dy * dy;
				double currentArea = poly.getArea();
				double wantedArea = completeArea * point.getPercentage();
				double increase = (wantedArea / currentArea);
				error = Math.abs(wantedArea - currentArea);
				double minDistanceClipped = poly.getMinDistanceToBorder(
						centroidX, centroidY);
				minDistanceClipped = minDistanceClipped * nearlyOne;
				// scale preflow vector to have minDistanceClipped length
				double rootBorder = clipPolygon.getMinDistanceToBorder(
						point.getX(), point.getY());
				// if (isUseExtrapolation() && !preflowFinished &&
				// rootBorder>maxDelta/Math.sqrt(sites.size()))) {
				if (isUseExtrapolation() && !preflowFinished) {
					if (point.preflowVector.length() != 0) {
						point.preflowVector.scale(minDistanceClipped
								/ point.preflowVector.length());
						if (point.preflowVector.length() > 5) {
							centroidX += point.preflowVector.x;
							centroidY += point.preflowVector.y;
						}
						point.preflowVector.x = 0;
						point.preflowVector.y = 0;

					}

					/**
					 * This could also be used as further speedup, but is not
					 * 100% stable
					 */
					// if (point.preflowVector.length()!=0){
					// Point2D centroidNew = poly.getRelativePosition(new
					// Point2D.Double(point.preflowVector.x,
					// point.preflowVector.y));
					// centroidX=centroidNew.getX();
					// centroidY=centroidNew.getY();
					// }

				}
				double minDistance = point.nonClippedPolyon
						.getMinDistanceToBorder(centroidX, centroidY);

				/**
				 * radius has to be at most the minimal distance to the border
				 * segments of the polygon
				 */
				double weight = Math.min(point.getWeight(), minDistance
						* minDistance);
				if (weight < 0.00000001) {
					weight = 0.00000001;
				}

				// set new position to the centroid
				point.setXYW(centroidX, centroidY, weight);

			}

			error = error / (completeArea * 2);

			errorArea += error;
			// System.out.println("ErrorArea:"+error);
		}
		// if (site 0) {
		// errorArea = 1;
		// } else {
		// errorArea = errorArea / (completeArea * 2);
		currentAreaError += errorArea;
		// }
		// drawCurrentState(false);
		// System.out.println("Area Error: "+errorArea);
		voroDiagram();
		// drawCurrentState(false);
		// try to improve the radius of the circle so that the wanted area gets
		// improved
		// int a = 0;
		// if (a == 0)
		// ;

		/**
		 * adapt weights
		 */
		/**
		 * if activated also allow negative weights
		 */
		/**
		 * compute the ordinary Voronoi diagram
		 */
		OpenList sitesCopy = null;
		if (guaranteeInvariant) {
			sitesCopy = sites.cloneWithZeroWeights();
			diagram.setSites(sitesCopy);
			diagram.setClipPoly(clipPolygon);
			diagram.computeDiagram();
		}
		for (int z = 0; z < size; z++) {
			Site point = array[z];

			PolygonSimple poly = point.getPolygon();
			double completeArea = clipPolygon.getArea();
			double currentArea = poly.getArea();
			double wantedArea = completeArea * point.getPercentage();

			double currentRadius = Math.sqrt(currentArea / Math.PI);
			double wantedRadius = Math.sqrt(wantedArea / Math.PI);
			double deltaCircle = currentRadius - wantedRadius;

			double increase = wantedArea / currentArea;
			if (getAggressiveMode() == false) {
				increase = Math.sqrt(increase);
			}

			// get the minimal distance to the neighbours. otherwise the
			// neighbours could be dominated

			double minDistance = 0;
			if (guaranteeInvariant) {
				Site pointOrdinary = sitesCopy.array[z];
				minDistance = getMinNeighbourDistance(pointOrdinary);
			} else {
				minDistance = getMinNeighbourDistance(point);
			}
			minDistance = minDistance * nearlyOne;
			// minDistance = Math.max(1, minDistance);
			// change the radius of the circles
			/**
			 * we have to take care that the radius doesn't get to big to
			 * dominate other sites.
			 */
			double radiusOld = Math.sqrt(point.getWeight());

			double radiusNew = radiusOld * increase;

			double deltaRadius = radiusNew - radiusOld;
			if (radiusNew > minDistance) {
				radiusNew = minDistance;
			}
			double finalWeight = radiusNew * radiusNew;
			if (useNegativeWeights) {
				Point2D center = poly.getCentroid();
				double distanceBorder = poly.getMinDistanceToBorder(center.x,
						center.y);
				double maxDelta = Math.min(distanceBorder, deltaCircle);
				if (finalWeight < 0.0001) {
					double radiusNew2 = radiusNew - maxDelta;
					if (radiusNew2 < 0) {
						finalWeight = -(radiusNew2 * radiusNew2);
						if (finalWeight < currentMaxNegativeWeight) {
							currentMaxNegativeWeight = finalWeight;
						}
					}
				}
			}

			point.setWeight(finalWeight);

		}

		/**
		 * make weights non negative again by adding the smallest negative value
		 */
		if (useNegativeWeights) {

			if (currentMaxNegativeWeight < 0) {
				currentMaxNegativeWeight += (1 - nearlyOne);
				currentMaxNegativeWeight = -currentMaxNegativeWeight;
				for (int z = 0; z < size; z++) {
					Site s = array[z];
					double w = s.getWeight();
					w += currentMaxNegativeWeight;
					s.setWeight(w);
				}
			}

		}
		// drawCurrentState(false);
		voroDiagram();
	    //drawCurrentState(false);
		/**
		 * compute the maximal error of the area.
		 * 
		 */
		currentMaxError = 0;
		array = sites.array;
		size = sites.size;
		for (int z = 0; z < size; z++) {
			Site site = array[z];
			PolygonSimple poly = site.getPolygon();
			double percentage = site.getPercentage();

			double wantedArea = completeArea * percentage;
			double currentArea = poly.getArea();
			double singleError = Math.abs(1 - (currentArea / wantedArea));
			if (singleError > currentMaxError) {
				currentMaxError = singleError;
			}

		}
		lastEuclidChange = currentEuclidChange / size;
		lastSumErrorChange = Math.abs(lastAreaError - currentAreaError);
		lastAreaError = currentAreaError;
		lastMaxError = currentMaxError;
		lastAVGError = currentAreaError / size;
		// long ende=System.currentTimeMillis();
		// System.out.println((ende-start));
	}

	public static double gaussianNormalize(double x, double expected,
			double deviation) {
		double temp = ((x - expected) / deviation);
		double result = 1 / (deviation * Math.sqrt(2 * Math.PI))
				* Math.exp((-0.5) * (temp * temp));
		return result;
	}

	private void moveToCenter2(double completeArea, Site point) {
		PolygonSimple poly = point.getPolygon();
		if (poly != null) {
			Point2D centroid = poly.getCentroid();
			double centroidX = centroid.getX();
			double centroidY = centroid.getY();
			double dx = centroidX - point.getX();
			double dy = centroidY - point.getY();
			currentEuclidChange += dx * dx + dy * dy;
			double currentArea = poly.getArea();
			double wantedArea = completeArea * point.getPercentage();

			double error = Math.abs(wantedArea - currentArea);
			double minDistance = point.nonClippedPolyon.getMinDistanceToBorder(
					centroidX, centroidY);

			/**
			 * radius has to be at most the minimal distance to the border
			 * segments of the polygon
			 */
			double weight = Math.min(point.getWeight(), minDistance
					* minDistance);
			if (weight <= 0) {
				weight = 0.01;
			}
			double euclidChange = dx * dx + dy * dy;
			// set new position to the centroid
			point.setXYW(centroidX, centroidY, weight);
			// return error;
		}
		// return Double.MAX_VALUE;
	}


	/**
	 * Computes the minimal distance to the voronoi Diagram neighbours
	 */
	private double getMinNeighbourDistance(Site point) {
		double minDistance = Double.MAX_VALUE;
		for (Site neighbour : point.getNeighbours()) {
			double distance = neighbour.distance(point);
			if (distance < minDistance) {
				minDistance = distance;
			}
		}
		return minDistance;
	}

	/**
	 * Computes the diagram and sets the results
	 */
	public synchronized void voroDiagram() {

		diagram.setSites(sites);
		diagram.setClipPoly(clipPolygon);
		try {
			diagram.computeDiagram();
		} catch (Exception e) {
			System.out.println("Error on computing power diagram");
			e.printStackTrace();
		}
	}

	/**
	 * Computes the ordinary diagram and sets the results
	 */
	synchronized protected void voroOrdinaryDiagram(OpenList sites) {

		diagram.setSites(sites);
		diagram.setClipPoly(clipPolygon);
		try {
			diagram.computeDiagram();
		} catch (Exception e) {
			System.out.println("Error on computing power diagram");
			e.printStackTrace();
		}
	}

	public void doIterate() {
		doIterate(numberMaxIterations);
	}

	public void doIterate(final int iterationAmount) {
		// solveDuplicates(this.sites);

		// if there is just one side we don't have to do anything
		if (sites.size == 1) {
			PolygonSimple p = (PolygonSimple) clipPolygon.clone();
			Site site = sites.get(0);
			site.setPolygon(p);
			return;
		}
		if (firstIteration) {
			voroDiagram();
			//drawCurrentState(false);
		}
		// long start = System.nanoTime();

		for (int i = 0; i < iterationAmount; i++) {
			iterate();

			if (isCancelOnAreaErrorThreshold()
					&& lastMaxError < errorAreaThreshold) {
				
				break;
			}
		}
		// now its finish so give the cells a hint
		/**
		 * TODO remove cellObject notification
		 */
		Site[] array = sites.array;
		int size = sites.size;
		for (int z = 0; z < size; z++) {
			Site site = array[z];
			PolygonSimple poly = site.getPolygon();
			if (site.cellObject != null) {
				site.cellObject.setVoroPolygon(poly);
				site.cellObject.doFinalWork();
			}
		}
		//drawCurrentState(true,VoronoiTreemap.rootPolygon);
	}

	private void solveDuplicates(OpenList sites) {
		// sort the points, and then check for duplicates
		Site[] array = sites.array;
		int size = sites.size;
		Arrays.sort(array, 0, size);
		int length = size - 1;
		for (int i = 0; i < length; i++) {
			Site s1 = array[i];
			Site s2 = array[i + 1];
			if (s1.compareTo(s2) == 0) {
				int third = i + 2;
				if (third != size) {
					Site s3 = array[third];
					if (s2.compareTo(s3) != 0) {
						double x = s1.getX() + s3.getX();
						double y = s1.getY() + s3.getY();
						x /= 2;
						y /= 2;
						s2.setXY(x + 0.02, y);
						// System.out.println("S1:"+s1+"\nS2:"+s2+"\nS3:"+s3);
					} else {
						throw new RuntimeException(
								"three points have same coordinates");
					}
				} else {
					s2.setX(s2.getX() + 2E-7);
					// System.out.println("Last: S1:"+s1+"\t S2:"+s2);
				}
			}
		}
	}

	public void drawCurrentState(boolean isLast, PolygonSimple shape) {
		if (graphics != null) {
			drawState(graphics, false);	
			frame.setVisible(true);
		    frame.setBounds(20, 20, 600, 600);
			frame.add(iframe);
			frame.repaint();
			//com.sun.awt.AWTUtilities.setWindowOpacity(iframe, 0.63f);
			//iframe.setShape(VoronoiTreemap.rootPolygon);
			//iframe.repaint();
			// frame.resize(frame.getSize());
			// frame.validate();
		}
	}

	void drawState(Graphics2D g, boolean isLast) {
		g.clearRect(-2000, -2000, 5000, 5000);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		// boolean problem=false;
		// for (Site s:sites){
		// if (s.getPolygon()==null){
		// problem=true;
		// break;
		// }
		// }
		if(isLast){
		g.setColor(Color.white);
		g.fillRect(0, 0, 1600, 800);
		}
		
		g.setColor(Colors.circleBorder);
		g.draw(clipPolygon);
		Site[] array = sites.array;
		int size = sites.size;


		for (int z = 0; z < size; z++) {
			Site s = array[z];
			g.setColor(Colors.circleFill);
			s.paint(g);
			PolygonSimple poly = s.getPolygon();
			if (poly != null) {
				// poly.shrinkForBorder(0.1);

				// g.fill(poly);
				// poly.shrinkForBorder(0.9);
				g.setColor(Colors.bisectors);
				g.draw(poly);
				// Double centroid = poly.getCentroid();
				// g.drawRoundRect(centroid.x-, y, width, height, arcWidth,
				// arcHeight)
			} else {
				System.out.println("Poly null:" + s);
			}
			// write the number of the site down
			g.setColor(Color.white);
			g.setFont(g.getFont().deriveFont(7F));
			g.drawString(new Integer(z + 1).toString(), (int) s.getX() + 1,
					(int) s.getY() - 1);

		}

	}

	public void setCancelOnAreaErrorThreshold(boolean cancelOnThreshold) {
		this.cancelOnAreaErrorThreshold = cancelOnThreshold;
	}

	public boolean isCancelOnAreaErrorThreshold() {
		return cancelOnAreaErrorThreshold;
	}

	public void setCancelOnMaxIterat(boolean cancelOnMaxIterat) {
		this.cancelOnMaxIterat = cancelOnMaxIterat;
	}

	public boolean isCancelOnMaxIterat() {
		return cancelOnMaxIterat;
	}

	public void setErrorAreaThreshold(double errorAreaThreshold) {
		this.errorAreaThreshold = errorAreaThreshold;
	}

	public double getErrorAreaThreshold() {
		return errorAreaThreshold;
	}

	public void setSites(OpenList sites) {
		this.sites = sites;
	}

	public OpenList getSites() {
		return sites;
	}
	
	/**
	 * Generate Pictures which show iterative algorithm.
	 * 
	 * @param args
	 */
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		VoronoiCore core = new VoronoiCore();
		OpenList sites = new OpenList();
		Random rand = new Random(100);
		int amount = 200;

		PolygonSimple rootPolygon = new PolygonSimple();
		int width = 400;
		int height = 400;
		rootPolygon.add(0, 20);
		rootPolygon.add(width, 20);
		rootPolygon.add(width, height + 20);
		rootPolygon.add(0, height + 20);
		for (int i = 0; i < amount; i++) {
			Site site = new Site(rand.nextInt(width), rand.nextInt(width));
			site.setPercentage(1);
			sites.add(site);
		}
		sites.get(0).setPercentage(3);

		core.setDebugMode();
		// core.setOutputMode();
		core.normalizeSites(sites);
		// ArrayList<Site> sites = TestConvergence.getSites(100, rectangle,
		// true);
		// core.setErrorAreaThreshold(0.00);
		// core.setUseExtrapolation(false);
		// normalizeSites(sites);
		core.setSites(sites);
		core.setClipPolygon(rootPolygon);
		long start = System.currentTimeMillis();

		core.setUseNegativeWeights(true);
		core.setCancelOnAreaErrorThreshold(true);
		core.doIterate(5000);

		long end = System.currentTimeMillis();
		double diff = (end - start) / 1000.0D;
		System.out.println("NeededTime: " + diff);
		// }
	}

	private static void normalizeSites(OpenList sites) {
		double sum = 0;
		Site[] array = sites.array;
		int size = sites.size;
		for (int z = 0; z < size; z++) {
			Site s = array[z];
			sum += s.getPercentage();
		}
		for (int z = 0; z < size; z++) {
			Site s = array[z];
			s.setPercentage(s.getPercentage() / sum);
		}

	}

	public void setUseExtrapolation(boolean useExtrapolation) {
		this.useExtrapolation = useExtrapolation;
	}

	public boolean isUseExtrapolation() {
		return useExtrapolation;
	}

	public double getPreflowPercentage() {
		return preflowPercentage;
	}

	/**
	 * If a region wants to increase its area by the factor preflowIncrease it
	 * is considered for preflow extrapolation. default value is 1.6
	 * (experimental)
	 */
	public void setPreflowIncrease(double preflowIncrease) {
		this.preflowIncrease = preflowIncrease;
	}

	private double getPreflowIncrease() {
		return preflowIncrease;
	}

	public void setIterationAmount(int iterationAmount) {
		this.setNumberMaxIterations(iterationAmount);
	}

	public int getIterationAmount() {
		return getNumberMaxIterations();
	}

	public void setGuaranteeValidCells(boolean guaranteeInvariant) {
		this.guaranteeInvariant = guaranteeInvariant;
	}

	protected boolean isGuaranteeInvariant() {
		return guaranteeInvariant;
	}

	/**
	 * @param numberMaxIterations
	 *            the numberMaxIterations to set
	 */
	public void setNumberMaxIterations(int numberMaxIterations) {
		this.numberMaxIterations = numberMaxIterations;
	}

	/**
	 * @return the numberMaxIterations
	 */
	public int getNumberMaxIterations() {
		return numberMaxIterations;
	}

	/**
	 * @param preflowPercentage
	 *            the preflowPercentage to set
	 */
	public void setPreflowPercentage(double preflowPercentage) {
		this.preflowPercentage = preflowPercentage;
	}

	public void setUseNegativeWeights(boolean useNegativeWeights) {
		this.useNegativeWeights = useNegativeWeights;
	}

	public boolean getUseNegativeWeights() {
		return useNegativeWeights;
	}

	public void setAggressiveMode(boolean aggressiveMode) {
		this.aggressiveMode = aggressiveMode;
	}

	public boolean getAggressiveMode() {
		return aggressiveMode;
	}

}
