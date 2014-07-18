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
package kn.uni.voronoitreemap.treemap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeModel;

import kn.uni.voronoitreemap.IO.Value;
import kn.uni.voronoitreemap.IO.ValuePercent;
import kn.uni.voronoitreemap.debug.ImageFrame;
import kn.uni.voronoitreemap.gui.JPolygon;
import kn.uni.voronoitreemap.helper.InterpolColor;
import kn.uni.voronoitreemap.interfaces.MainClass;
import kn.uni.voronoitreemap.interfaces.StatusObject;
import kn.uni.voronoitreemap.interfaces.VoronoiTreemapInterface;
import kn.uni.voronoitreemap.interfaces.data.Tuple2ID;
import kn.uni.voronoitreemap.interfaces.data.Tuple3ID;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;
import kn.uni.voronoitreemap.polyShape.*;

/**
 * Main Voronoi Treemap class implementing the Voronoi Treemap interface and
 * maintaining the settings.
 * 
 * @author nocaj
 * 
 */
public class VoronoiTreemap extends JFrame implements Iterable<VoroNode>,
        StatusObject, VoronoiTreemapInterface, ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * DEBUGGING
	 */
	// TODO remove debug Mode
	public static boolean debugMode = false;
	public JFrame frame = new JFrame();
	public static ImageFrame iframe = new ImageFrame(null);
	public static Graphics2D graphics;
	// geVoroRenderer renderer;
	private static final String OPEN_XML_FILE = "Open Xml File";
	private static final String EXIT = "Exit";
	private DefaultTreeModel treeModel;
	protected JTree treeView = new JTree();
	private JComboBox cmbPolyShape;
	private LinkedHashMap polyShapes = new LinkedHashMap();
	private ZoomPopupMenu zoomPopup;
	public static VoroNode displayedRoot;
	public static VoroNode originalNode;
	public String finish = null;
	/**
	 * DEBUGGING
	 */
	private boolean initialized = false;
	private boolean useBorder = false;
	private double shrinkPercentage = 1;
	private boolean showLeafs = false;
	private int numberThreads = 1;
	public static VoroNode root;
	public static VoroNode activeLeaf = null;
	public static ArrayList<Tuple2ID> areaGoals = new ArrayList<Tuple2ID>();
	public static PolygonSimple rootPolygon;
	public static InterpolColor interpolColor = new InterpolColor(0, 1, 0.0,
	        0.73, 0.58, 224.0 / 225.0, 0.73, 0.58);
	int amountAllNodes = 0;
	int alreadyDoneNodes = 0;

	long timeStart;
	long timeEnd;
	private Semaphore lock = new Semaphore(1);

	/**
	 * Settings for the Core
	 */
	private int numberMaxIterations = 200;
	/**
	 * A site which wants to reach more then preflowPercentage is considered for
	 * preflow extrapolation. default value: 0.08
	 */
	private double preflowPercentage = 0.08;
	/**
	 * If a region wants to increase its area by the factor preflowIncrease it
	 * is considered for preflow extrapolation default value is 1.5 or 1.6
	 */
	private double preflowIncrease = 1.3;
	private boolean useNegativeWeights = true;
	private boolean useExtrapolation = false;
	private boolean cancelOnThreshold = false;
	private boolean cancelOnMaxIterat = true;
	protected double errorAreaThreshold = 0.001;
	protected boolean preflowFinished = false;
	private boolean guaranteeValidCells = false;

	private boolean aggressiveMode = false;

	/**
	 * This queue handles the voronoi cells which have to be calculated
	 */
	BlockingQueue<VoroNode> cellQueue = new LinkedBlockingQueue<VoroNode>();
	private int[] levelsMaxIteration;
	private StatusObject statusObject;

	/**
	 * used for, e.g., random positioning of points
	 */
	static long randomSeed = 1985;
	static Random rand = new Random(randomSeed);
	static HashMap<String, VoroNode> labelToNode;

	/** when a node is finished the status object is notified. **/

	public VoronoiTreemap(StatusObject statusObject) {
		this();
		this.statusObject = statusObject;
	}

	public VoronoiTreemap(StatusObject statusObject, boolean multiThreaded) {
		this();
		this.statusObject = statusObject;
		if (multiThreaded) {
			setNumberThreads(Runtime.getRuntime().availableProcessors());
		}
		/**
		 * 2014.3.13
		 */
		this.root = getDefaultRoot();
		setDebugMode();
	}

	public VoronoiTreemap() {
		init();
	}

	protected void recalculatePercentage() {
		amountAllNodes = 0;
		alreadyDoneNodes = 0;
		root.calculateWeights();
	}

	protected void setRootCell(VoroNode cell) {
		this.root = cell;
		root.setHeight(1);
		root.setWantedPercentage(0);
	}

	protected VoroNode getRootCell() {
		return root;
	}

	protected void init() {
		initialized = false;
		useBorder = false;
		shrinkPercentage = 1;
		showLeafs = false;
		amountAllNodes = 0;
		numberThreads = 1;
		root = null;
		displayedRoot = null;
		areaGoals = null;
		rootPolygon = null;
		numberMaxIterations = 200;
		preflowPercentage = 0.08;
		preflowIncrease = 1.3;
		useNegativeWeights = true;
		useExtrapolation = false;
		cancelOnThreshold = false;
		cancelOnMaxIterat = true;
		errorAreaThreshold = 0.001;
		preflowFinished = false;
		guaranteeValidCells = false;
		if (cellQueue != null)
			cellQueue.clear();
		statusObject = null;
		rand = new Random(1985);
		if (labelToNode != null)
			labelToNode.clear();
		lock = new Semaphore(1);
	}

	protected void initVoroNodes() {
		if (!initialized && root != null) {
			initialized = true;
			cellQueue.clear();
			root.calculateWeights();
		}
	}

	public void setDebugMode() {
		frame.setBounds(0, 0, 840, 720); // 20, 20, 1040, 920
		BufferedImage image = new BufferedImage(620, 620,// 820,820
		        BufferedImage.TYPE_INT_ARGB);
		iframe = new ImageFrame(image);
		iframe.setVisible(true);
		iframe.setBackground(Color.white);
		iframe.setFont(new Font(null, Font.BOLD, 16));
		iframe.setPreferredSize(new Dimension(image.getWidth(), image
		        .getHeight()));
		iframe.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		graphics = image.createGraphics();
		// graphics.setColor(Color.WHITE);
		// graphics.fillRect(0, 0, 600, 600);
		this.zoomPopup = new ZoomPopupMenu(this.iframe);

		try {
			initGUI();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void initGUI() throws Exception {
		this.setTitle("Voronoi Example");
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				this_windowClosing(e);
			}
		});
		addMenu(frame);
		addPanelCenter(frame.getContentPane());
		addPanelNorth(frame.getContentPane());
	}

	protected void this_windowClosing(WindowEvent e) {
		System.exit(0);
	}

	private void addMenu(JFrame frame) {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");

		JMenuItem item = new JMenuItem(OPEN_XML_FILE);
		item.addActionListener(this);
		item.setAccelerator(KeyStroke.getKeyStroke('O',
		        java.awt.event.InputEvent.ALT_MASK));
		menu.add(item);

		item = new JMenuItem(EXIT);
		item.setAccelerator(KeyStroke.getKeyStroke('X',
		        java.awt.event.InputEvent.ALT_MASK));
		item.addActionListener(this);
		menu.add(item);

		menuBar.add(menu);
		this.setJMenuBar(menuBar);
		frame.setJMenuBar(menuBar);
	}

	private void addPanelCenter(Container parent) {
		JSplitPane splitPaneCenter = new JSplitPane();
		splitPaneCenter.setBorder(BorderFactory.createEmptyBorder());
		parent.add(splitPaneCenter, BorderLayout.CENTER);

		JScrollPane jScrollPane1 = new JScrollPane();
		this.treeModel = new DefaultTreeModel(this.root);
		this.treeView = new JTree(this.treeModel);
		jScrollPane1.getViewport().add(this.treeView);
		jScrollPane1.setPreferredSize(new Dimension(160, this.iframe
		        .getHeight()));
		splitPaneCenter.setTopComponent(jScrollPane1);
		splitPaneCenter.setBottomComponent(this.iframe);
		this.treeView.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				// for each selected elements ont the treeView, we zoom the
				// ImageFrame
				VoroNode dest = (VoroNode) VoronoiTreemap.this.treeView
				        .getLastSelectedPathComponent();

				// if the element is a leaf, we select the parent
				if (dest != null && dest.isLeaf())
					dest = (VoroNode) dest.getParent();
				if (dest == null)
					return;

				// setHashNode(root);
				// dest = matchNode(root, dest);

				displayedRoot = dest;
				// if(displayedRoot.getNodeName()!=root.getNodeName()){
				// setNewPolygon(displayedRoot);
				// }else{
				// displayedRoot = root;
				// }
				recompute(displayedRoot);
				VoronoiTreemap.this.iframe.repaint();

				/*
				 * try { saveImage(iframe.getGraphics()); } catch (IOException
				 * e1) { // TODO Auto-generated catch block
				 * e1.printStackTrace(); }
				 */
			}
		});
	}

	/**
	 * 获取图形显示区域的图形
	 * 
	 * @return
	 * @throws IOException
	 */
	public BufferedImage image = new BufferedImage(620, 620,
	        BufferedImage.TYPE_INT_RGB);

	public void saveImage(Graphics g) throws IOException {
		g = image.getGraphics();
		// g = this.graphics;
		Graphics2D graphics = (Graphics2D) g;
		VoronoiTreemap.this.iframe.paint(graphics);
		//ImageIO.write(image,"jpg",new File("component.jpg"));
		System.out.println("已经保存");
	}

	/**
	 * onTouch function
	 */
	public void onTouch(int x, int y) {
		VoroNode node = this.displayedRoot.getChild(x, y);
		//System.out.println(node.getNodeName());
		// if the element is a leaf, we select the parent
		if (node != null && node.isLeaf())
			node = (VoroNode) node.getParent();
		if (node == null)
			return;
		
		//this.displayedRoot = node;
		displayedRoot = node;
		//rootPolygon = setDefaultPolygon();
		//this.displayedRoot.setPolygon(rootPolygon);
		recompute(node);
		VoronoiTreemap.this.iframe.repaint();
	}
	
	public String getInfo(int x, int y){
		VoroNode node = this.displayedRoot.getChild(x, y);
		String a = node.getNodeName();
		return a;
	}

	public VoroNode matchNode(VoroNode root, VoroNode tar) {
		VoroNode node1 = null;
		if (root.getNodeName() == tar.getNodeName())
			return root;
		if (root.children != null) {
			for (VoroNode node2 : root.children) {
				if (node2.getNodeName() == tar.getNodeName()) {
					return node2;
				}
				node1 = matchNode(node2, tar);
				break;
			}
		}
		return node1;
	}

	private void addPanelNorth(Container parent) {
		JPanel panelNorth = new JPanel();
		panelNorth.setBorder(BorderFactory.createEmptyBorder());
		JLabel lblStrategy = new JLabel();
		lblStrategy.setText("PolyShape :");
		parent.add(panelNorth, BorderLayout.NORTH);
		panelNorth.add(lblStrategy);
		this.cmbPolyShape = new JComboBox();
		panelNorth.add(this.cmbPolyShape);

		createShapes();

		this.cmbPolyShape.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateShape();
			}
		});
	}

	private void setNewPolygon(VoroNode dest) {
		Site site = dest.getSite();
		PolygonSimple poly = site.getPolygon();
		Rectangle bounds = poly.getBounds();

		Rectangle rootBound = rootPolygon.getBounds();
		double x0 = rootBound.getX();
		double y0 = rootBound.getY();
		double tw = rootBound.getWidth();
		double th = rootBound.getHeight();
		double[] xPoints = new double[poly.length];
		double[] yPoints = new double[poly.length];
		for (int i = 0; i < poly.length; i++) {
			xPoints[i] = (poly.getXPoints()[i] - bounds.x)
			        * (tw / bounds.getWidth()) + x0;
			yPoints[i] = (poly.getYPoints()[i] - bounds.y)
			        * (th / bounds.getHeight()) + y0;
		}
		PolygonSimple newPoly = new PolygonSimple(xPoints, yPoints);
		bounds = newPoly.getBounds();
		site.setPolygon(newPoly);
		dest.setPolygon(newPoly);
		dest.setSite(site);
	}

	public void recompute(VoroNode dest) {
		// initVoroNodes();
		cellQueue.clear();
		cellQueue.add(dest);
		ArrayList<VoroNode> children = dest.getChildren();
		if (children != null) {
			for (VoroNode node : children) {
				cellQueue.add(node);
			}
		}
		startComputeThreads();
	}

	private void startComputeThreads() {
		// start as much VoroCPUs as there are CPUS available
		for (int i = 0; i < getNumberThreads(); i++) {
			new VoroCPU(cellQueue, this).start();
		}
	}

	public void compute() {
		if (rootPolygon == null)
			throw new RuntimeException("Root Polygon not set.");
		timeStart = System.currentTimeMillis();
		initVoroNodes();
		cellQueue.add(displayedRoot);

		ArrayList<VoroNode> children = root.getChildren();
		if (children != null) {
			for (VoroNode node : children) {
				cellQueue.add(node);
			}
		}
		startComputeThreads();
	}

	public void computeLocked() {
		try {
			lock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		compute();
		try {
			lock.acquire();
			lock.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected void setSettingsToVoroNode(VoroNode node) {
		node.setTreemap(this);
	}

	public static PolygonSimple setDefaultPolygon() {
		PolygonSimple rootPoly = new PolygonSimple(4);
		rootPoly.add(150, 40);
		rootPoly.add(450, 40);
		rootPoly.add(580, 150);
		rootPoly.add(580, 450);
		rootPoly.add(450, 580);
		rootPoly.add(150, 580);
		rootPoly.add(40, 450);
		rootPoly.add(40, 150);
		return rootPoly;
	}

	public static void main(String[] args) {
		PolygonSimple rootPoly = new PolygonSimple(4);
		rootPoly.add(150, 40);
		rootPoly.add(450, 40);
		rootPoly.add(580, 150);
		rootPoly.add(580, 450);
		rootPoly.add(450, 580);
		rootPoly.add(150, 580);
		rootPoly.add(40, 450);
		rootPoly.add(40, 150);
		// VoroNode defaultRoot = getDefaultRoot();
		// root = defaultRoot;
		// VoronoiTreemap.displayedRoot = root;
		// Create a StatusObject of your implementation
		StatusObjectImpl statusOjbect1 = new StatusObjectImpl();
		// Get the voronoi treemap with the your status object as parameter and
		// whether you want to use multithreaded computation
		VoronoiTreemapInterface voronoiTreemap = MainClass.getInstance(
		        statusOjbect1, false); // 开启线程 true
		// voronoiTreemap.setUseBorder(true);
		voronoiTreemap.setShrinkPercentage(0.95);
		// Set root polygon
		voronoiTreemap.setRootPolygon(rootPoly);

		VoroNode defaultRoot = getDefaultRoot();
		root = defaultRoot;
		voronoiTreemap.setHashNode(defaultRoot);
		root.setPolygon(rootPoly);
		voronoiTreemap.setDisplayedRoot(root);

		// Now compute the voronoi treemap
		voronoiTreemap.compute();
		// voronoiTreemap.computeLocked();
	}

	@Override
	public void setHashNode(VoroNode rootNode) {
		labelToNode = new HashMap<String, VoroNode>();
		setRootToTree(rootNode);
		for (VoroNode voroNode : labelToNode.values()) {
			double x = rand.nextDouble();
			double y = rand.nextDouble();
			voroNode.setRelativeVector(new Point2D(x, y));
		}
	}

	protected final void setRootToTree(VoroNode parent) {
		for (int i = 0; i < parent.getChildren().size(); i++) {
			int numberChildren = parent.getChildren().size();
			VoroNode child = parent.getChildren().get(i);
			if (numberChildren >= 1) {
				labelToNode.put(child.getNodeName(), child);
				// child.setParent(parent);
				// parent.setTreemap(this);
				setSettingsToVoroNode(parent);
			}
			setRootToTree(child);
		}
	}

	private static VoroNode getDefaultRoot() {
		VoroNodeBuilder builder = new VoroNodeBuilder();

		VoroNode root = builder.buildBranch("Root", null);
		VoroNode tmn1 = builder.buildBranch("branch1", root);
		VoroNode tmn11 = builder.buildBranch("branch11", tmn1);
		Value value = new ValuePercent(0.45);
		builder.buildLeaf("leaf111", 1.0, value, tmn11);
		value = new ValuePercent(-5.0);
		builder.buildLeaf("leaf112", 2.0, value, tmn11);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf113", 0.5, value, tmn11);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf114", 3.0, value, tmn11);
		value = new ValuePercent(-5.0);
		builder.buildLeaf("leaf115", 0.25, value, tmn11);
		VoroNode tmn12 = builder.buildBranch("branch12", tmn1);
		value = new ValuePercent(1.0);
		builder.buildLeaf("leaf121", 1.0, value, tmn12);
		value = new ValuePercent(5.0);
		builder.buildLeaf("leaf122", 2.0, value, tmn12);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf123", 0.5, value, tmn12);
		value = new ValuePercent(-2.0);
		builder.buildLeaf("leaf124", 3.0, value, tmn12);
		value = new ValuePercent(0.0);
		builder.buildLeaf("leaf125", 0.25, value, tmn12);
		VoroNode tmn13 = builder.buildBranch("branch13", tmn1);
		value = new ValuePercent(1.0);
		builder.buildLeaf("leaf131", 1.0, value, tmn13);
		value = new ValuePercent(5.0);
		builder.buildLeaf("leaf132", 2.0, value, tmn13);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf133", 0.5, value, tmn13);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf134", 3.0, value, tmn13);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf14", 3.0, value, tmn1);
		value = new ValuePercent(-5.0);
		builder.buildLeaf("leaf15", 2.0, value, tmn1);
		VoroNode tmn2 = builder.buildBranch("branch2", root);
		VoroNode tmn21 = builder.buildBranch("branch21", tmn2);
		value = new ValuePercent(-1.0);
		builder.buildLeaf("leaf211", 1.0, value, tmn21);
		value = new ValuePercent(-5.0);
		builder.buildLeaf("leaf212", 2.0, value, tmn21);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf213", 0.5, value, tmn21);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf214", 3.0, value, tmn21);
		value = new ValuePercent(5.0);
		builder.buildLeaf("leaf215", 0.25, value, tmn21);
		VoroNode tmn22 = builder.buildBranch("branch22", tmn2);
		value = new ValuePercent(1.0);
		builder.buildLeaf("leaf221", 1.0, value, tmn22);
		value = new ValuePercent(5.0);
		builder.buildLeaf("leaf222", 2.0, value, tmn22);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf223", 0.5, value, tmn22);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf224", 3.0, value, tmn22);
		VoroNode tmn3 = builder.buildBranch("branch3", root);
		VoroNode tmn31 = builder.buildBranch("branch31", tmn3);
		value = new ValuePercent(-1.0);
		builder.buildLeaf("leaf311", 1.0, value, tmn31);
		value = new ValuePercent(-5.0);
		builder.buildLeaf("leaf312", 2.0, value, tmn31);
		value = new ValuePercent(-2.0);
		builder.buildLeaf("leaf313", 0.5, value, tmn31);
		value = new ValuePercent(-2.0);
		builder.buildLeaf("leaf314", 3.0, value, tmn31);
		value = new ValuePercent(-5.0);
		builder.buildLeaf("leaf315", 0.25, value, tmn31);
		VoroNode tmn32 = builder.buildBranch("branch32", tmn3);
		value = new ValuePercent(-1.0);
		builder.buildLeaf("leaf321", 1.0, value, tmn32);
		value = new ValuePercent(-5.0);
		builder.buildLeaf("leaf322", 2.0, value, tmn32);
		value = new ValuePercent(0.0);
		builder.buildLeaf("leaf323", 0.5, value, tmn32);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf324", 3.0, value, tmn32);
		value = new ValuePercent(-5.0);
		builder.buildLeaf("leaf325", 0.25, value, tmn32);
		VoroNode tmn33 = builder.buildBranch("branch33", tmn3);
		value = new ValuePercent(-1.0);
		builder.buildLeaf("leaf331", 1.0, value, tmn33);
		value = new ValuePercent(5.0);
		builder.buildLeaf("leaf332", 2.0, value, tmn33);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf333", 0.5, value, tmn33);
		value = new ValuePercent(-2.0);
		builder.buildLeaf("leaf334", 3.0, value, tmn33);
		VoroNode tmn34 = builder.buildBranch("branch34", tmn3);
		value = new ValuePercent(-1.0);
		builder.buildLeaf("leaf341", 1.0, value, tmn34);
		value = new ValuePercent(5.0);
		builder.buildLeaf("leaf342", 2.0, value, tmn34);
		value = new ValuePercent(-2.0);
		builder.buildLeaf("leaf343", 0.5, value, tmn34);
		VoroNode tmn4 = builder.buildBranch("branch4", root);
		VoroNode tmn41 = builder.buildBranch("branch41", tmn4);
		value = new ValuePercent(1.0);
		builder.buildLeaf("leaf411", 1.0, value, tmn41);
		value = new ValuePercent(5.0);
		builder.buildLeaf("leaf412", 2.0, value, tmn41);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf413", 0.5, value, tmn41);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf414", 3.0, value, tmn41);
		value = new ValuePercent(-5.0);
		builder.buildLeaf("leaf415", 0.25, value, tmn41);
		VoroNode tmn42 = builder.buildBranch("branch42", tmn4);
		value = new ValuePercent(1.0);
		builder.buildLeaf("leaf421", 1.0, value, tmn42);
		value = new ValuePercent(5.0);
		builder.buildLeaf("leaf422", 2.0, value, tmn42);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf423", 0.5, value, tmn42);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf424", 3.0, value, tmn42);
		value = new ValuePercent(-5.0);
		builder.buildLeaf("leaf425", 0.25, value, tmn42);
		VoroNode tmn43 = builder.buildBranch("branch43", tmn4);
		value = new ValuePercent(1.0);
		builder.buildLeaf("leaf431", 1.0, value, tmn43);
		value = new ValuePercent(-5.0);
		builder.buildLeaf("leaf432", 2.0, value, tmn43);
		value = new ValuePercent(2.0);
		builder.buildLeaf("leaf433", 0.5, value, tmn43);
		value = new ValuePercent(0.0);
		builder.buildLeaf("leaf434", 3.0, value, tmn43);
		value = new ValuePercent(0.0);
		builder.buildLeaf("leaf5", 5.0, value, root);

		return builder.getRoot();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setTreeAndWeights(j2d.PolygonSimple,
	 * java.util.ArrayList, java.util.ArrayList, java.util.ArrayList)
	 */
	public void setTreeAndWeights(PolygonSimple rootPolygon,
	        final ArrayList<ArrayList<String>> treeStructure,
	        final ArrayList<Tuple2ID> areaGoals,
	        ArrayList<Tuple3ID> relativePositions) {
		this.rootPolygon = rootPolygon;
		setTree(treeStructure);
		setAreaGoals(areaGoals);
		if (relativePositions == null) {
			for (VoroNode voroNode : labelToNode.values()) {
				double x = rand.nextDouble();
				double y = rand.nextDouble();
				voroNode.setRelativeVector(new Point2D(x, y));
			}
		} else {
			setReferenceMap(relativePositions);
		}
		root.setVoroPolygon(rootPolygon);
	}

	protected final VoroNode createVoroNode(HashMap<String, VoroNode> idToNode,
	        final ArrayList<String> adjacencyLine) {
		ArrayList<String> childList = adjacencyLine;
		if (adjacencyLine == null) {
			return null;
		}
		String parentId = childList.get(0);
		int numberChildren = 0;
		numberChildren = childList.size() - 1;
		VoroNode voroNode = null;
		voroNode = idToNode.get(parentId);
		if (voroNode == null) {
			voroNode = new VoroNode(parentId, numberChildren);
		}
		voroNode.setTreemap(this);
		setSettingsToVoroNode(voroNode);
		// create child nodes
		if (numberChildren >= 1) {
			for (int i = 1; i < (numberChildren + 1); i++) {
				String childId = childList.get(i);
				VoroNode voroChild = new VoroNode(childId);
				idToNode.put(childId, voroChild);
				voroNode.addChild(voroChild);
				voroChild.setParent(voroNode);
				voroNode.setTreemap(this);
				setSettingsToVoroNode(voroNode);
			}
		}
		return voroNode;
	}

	void setShowLeafs(boolean showLeafs) {
		this.showLeafs = showLeafs;
	}

	public boolean getShowLeafs() {
		return showLeafs;
	}

	public VoroNode getDisplayedRoot() {
		return displayedRoot;
	}

	public void setDisplayedRoot(VoroNode displayedRoot) {
		this.displayedRoot = displayedRoot;
	}

	/**
	 * Iterator for going over the VoroNodes of this Treemap
	 * 
	 * @author nocaj
	 * 
	 */
	private class NodeIterator implements Iterator<VoroNode> {
		Stack<VoroNode> stack;

		public NodeIterator(VoroNode root) {
			stack = new Stack<VoroNode>();
			stack.addAll(root.getChildren());
		}

		@Override
		public boolean hasNext() {
			if (stack.size() > 0) {
				return true;
			} else
				return false;
		}

		@Override
		public VoroNode next() {
			VoroNode t = stack.pop();
			if (t != null && t.getChildren() != null
			        && t.getChildren().size() > 0) {
				stack.addAll(t.getChildren());
			}
			return t;
		}

		@Override
		public void remove() {

		}
	}

	@Override
	public Iterator<VoroNode> iterator() {
		return new NodeIterator(root);
	}

	public Iterator<VoroNode> iterator(VoroNode dest) {
		return new NodeIterator(dest);
	}

	@Override
	public synchronized void finished() {
		timeEnd = System.currentTimeMillis();
		// System.out.println("AmountSites:" + VoronoiCore.amountSites);
		double diff = timeEnd - timeStart;
		System.out.println("ComputationTime seconds:" + diff / 1000.0);

		setDebugMode();
		drawTreemapWithComponents(graphics);
		//drawTreemap(graphics);
		this.finish = "finished";
		frame.setVisible(true);
		frame.repaint();

		for (VoroNode node : this) {
			int height = node.getHeight();
			Site site = node.getSite();
			if (site != null) {
				PolygonSimple poly = site.getPolygon();
				if (poly != null) {
					if (site.cellObject != null) {
						site.cellObject.setVoroPolygon(poly);
						site.cellObject.doFinalWork();
					}
				}
			}
			JPolygon component = node.getPolygonComponent();
			if (component != null) {
				if (areaGoals != null) {
					this.getLayeredPane().add(component, new Integer(height));
				}
			}
			if (statusObject != null)
				statusObject.finished();
			lock.release();
		}
		// drawTreemapWithComponents(graphics);
		repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#drawTreemap(java.awt.Graphics2D)
	 */
	public void drawTreemap(Graphics2D g) {
		g.clearRect(0, 0, 1000, 1000);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		        RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.blue);
		int lastNodes = 0;
		int amountPolygons = 0;

		// for (VoroNode node : this) {
		// int height = node.getHeight();
		// if (node.getChildren() == null) {
		// lastNodes++;
		// }
		// Site site = node.getSite();
		// if (site != null) {
		//
		// PolygonSimple poly = site.getPolygon();
		// if (poly != null) {
		// // poly.shrinkForBorder(0.95);
		// amountPolygons++;
		// g.draw(poly);
		// }
		// }
		// }

		for (int i = 0; i < displayedRoot.children.size(); i++) {
			VoroNode node = displayedRoot.children.get(i);
			Site site = node.getSite();
			if (site != null) {
				PolygonSimple poly = site.getPolygon();
				if (poly != null) {
					g.draw(poly);
				}
			}
		}
		// VoroRenderer renderer = new VoroRenderer();
		// renderer.setTreemap(this);
		// renderer.renderTreemap("VE");
	}

	protected void drawTreemapWithComponents(Graphics2D g) {
		g.clearRect(-100, -100, 2000, 2000);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		        RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.blue);
		
//		 for (VoroNode child : this) {
//		 Site site = child.getSite();
//		 if(site.getPolygon()!=null){
//		 PolygonSimple poly = site.getPolygon();
//		 JPolygon jp = new JPolygon(poly, child.getNodeName());
//		 jp.paintComponent(g);
//		 }
//		 }
		 
		 
		for (int i = 0; i < displayedRoot.children.size(); i++) {
			VoroNode node = displayedRoot.children.get(i);
			Site site = node.getSite();
			if (site.getPolygon() != null) {
				PolygonSimple poly = site.getPolygon();
				JPolygon jp = new JPolygon(poly, node.getNodeName());
				jp.paintComponent(g);
			}
		}
	}

	void setGraphics(Graphics2D graphics) {
		this.graphics = graphics;
	}

	public Graphics2D getGraphics() {
		return graphics;
	}

	protected void setAmountNodes(int amountNodes) {
		this.amountAllNodes = amountNodes;
	}

	protected int getAmountNodes() {
		return amountAllNodes;
	}

	public void setShrinkPercentage(double shrinkPercentage) {
		this.shrinkPercentage = shrinkPercentage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#getShrinkPercentage()
	 */
	public double getShrinkPercentage() {
		return shrinkPercentage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setUseBorder(boolean)
	 */
	public void setUseBorder(boolean useBorder) {
		this.useBorder = useBorder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#getUseBorder()
	 */
	public boolean getUseBorder() {
		return useBorder;
	}

	/*
	 *
	 */
	public void setNumberIterationsLevel(int[] levelsMaxIteration) {
		this.levelsMaxIteration = levelsMaxIteration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setNumberMaxIterations(int)
	 */
	public void setNumberMaxIterations(int numberMaxIterations) {
		this.numberMaxIterations = numberMaxIterations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#getNumberMaxIterations()
	 */
	public int getNumberMaxIterations() {
		return numberMaxIterations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setPreflowPercentage(double)
	 */
	public void setPreflowPercentage(double preflowPercentage) {
		this.preflowPercentage = preflowPercentage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#getPreflowPercentage()
	 */
	public double getPreflowPercentage() {
		return preflowPercentage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setPreflowIncrease(double)
	 */
	public void setPreflowIncrease(double preflowIncrease) {
		this.preflowIncrease = preflowIncrease;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#getPreflowIncrease()
	 */
	public double getPreflowIncrease() {
		return preflowIncrease;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setUseExtrapolation(boolean)
	 */
	public void setUseExtrapolation(boolean useExtrapolation) {
		this.useExtrapolation = useExtrapolation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#getUseExtrapolation()
	 */
	public boolean getUseExtrapolation() {
		return useExtrapolation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setCancelOnThreshold(boolean)
	 */
	public void setCancelOnThreshold(boolean cancelOnThreshold) {
		this.cancelOnThreshold = cancelOnThreshold;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#getCancelOnThreshold()
	 */
	public boolean getCancelOnThreshold() {
		return cancelOnThreshold;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setCancelOnMaxIteration(boolean)
	 */
	public void setCancelOnMaxIteration(boolean cancelOnMaxIterat) {
		this.cancelOnMaxIterat = cancelOnMaxIterat;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#getCancelOnMaxIteration()
	 */
	public boolean getCancelOnMaxIteration() {
		return cancelOnMaxIterat;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setRootPolygon(j2d.PolygonSimple)
	 */
	public void setRootPolygon(PolygonSimple rootPolygon) {
		this.rootPolygon = rootPolygon;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setRootRectangle(double, double,
	 * double, double)
	 */
	public void setRootRectangle(double x, double y, double width, double height) {
		rootPolygon = new PolygonSimple();
		rootPolygon.add(x, y);
		rootPolygon.add(x + width, y);
		rootPolygon.add(x + width, y + height);
		rootPolygon.add(x, y + height);
	}

	/**
	 * Sets the root rectangle in which the treemap is computed.
	 * 
	 * @param rectangle
	 */
	public void setRootRectangle(Rectangle2D.Double rectangle) {
		setRootRectangle(rectangle.getX(), rectangle.getY(),
		        rectangle.getWidth(), rectangle.getHeight());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#getRootPolygon()
	 */
	public PolygonSimple getRootPolygon() {
		return rootPolygon;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setGuaranteeValidCells(boolean)
	 */
	public void setGuaranteeValidCells(boolean guaranteeInvariant) {
		this.guaranteeValidCells = guaranteeInvariant;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#getGuaranteeValidCells()
	 */
	public boolean getGuaranteeValidCells() {
		return guaranteeValidCells;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#setNumberThreads(int)
	 */
	public void setNumberThreads(int numberThreads) {
		if (numberThreads >= 1) {
			this.numberThreads = numberThreads;
		} else {
			this.numberThreads = 1;
		}

	}

	/**
	 * @return the numberThreads
	 */
	int getNumberThreads() {
		return numberThreads;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * treemap.voronoiTreemapInterface#setStatusObject(libinterfaces.IStatusObject
	 * )
	 */
	public void setStatusObject(StatusObject statusObject) {
		this.statusObject = statusObject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#getStatusObject()
	 */
	public StatusObject getStatusObject() {
		return statusObject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see treemap.voronoiTreemapInterface#finishedNode(int, int, int[],
	 * j2d.PolygonSimple[])
	 */
	@Override
	public void finishedNode(String Node, int layer, String[] children,
	        PolygonSimple[] polygons) {
		if (statusObject != null)
			statusObject.finishedNode(Node, layer, children, polygons);
	}

	@Override
	public void setAreaGoals(ArrayList<Tuple2ID> areaGoals) {
		if (areaGoals != null) {
			for (Tuple2ID tuple : areaGoals) {
				VoroNode voroNode = null;
				voroNode = labelToNode.get(tuple.name);
				if (voroNode != null) {
					voroNode.setWeight(tuple.weight);

				} else if (tuple.name == root.getNodeName()) {
					// we do not care, we don't need the weighting of the root
				} else {
					System.out.println("name: " + tuple.name);
					throw new RuntimeException(
					        "There is no node in the tree structure with this ID.");
				}
			}
		}
	}

	@Override
	public void setReferenceMap(ArrayList<Tuple3ID> relativePositions) {
		for (Tuple3ID tuple : relativePositions) {
			VoroNode voroNode = null;
			voroNode = labelToNode.get(tuple.name);
			if (voroNode != null) {
				voroNode.setRelativeVector(new Point2D(tuple.valueX,
				        tuple.valueY));
			} else if (tuple.name == root.getNodeName()) {
				// we do not care, we can't set a relative position for the root
			} else
				throw new RuntimeException(
				        "ReferencePosition for ID without node in the tree structure.");
		}
	}

	@Override
	public void setTree(ArrayList<ArrayList<String>> treeStructure) {
		labelToNode = new HashMap<String, VoroNode>();
		ArrayList<String> line = treeStructure.get(0);
		int numberLines = treeStructure.size();
		root = createVoroNode(labelToNode, line);
		for (int i = 1; i < numberLines; i++) {
			createVoroNode(labelToNode, treeStructure.get(i));
		}
		for (VoroNode voroNode : labelToNode.values()) {
			double x = rand.nextDouble();
			double y = rand.nextDouble();
			voroNode.setRelativeVector(new Point2D(x, y));
		}
		root.setVoroPolygon(rootPolygon);
	}

	@Override
	public boolean isUseNegativeWeights() {
		return useNegativeWeights;
	}

	@Override
	public void setUseNegativeWeights(boolean use) {
		useNegativeWeights = use;
	}

	@Override
	public void clear() {
		init();
	}

	@Override
	public void setAggressiveMode(boolean mode) {
		aggressiveMode = mode;
	}

	@Override
	public boolean getAggressiveMode() {
		return aggressiveMode;
	}

	@Override
	public void setRandomSeed(long seed) {
		randomSeed = seed;
		rand.setSeed(seed);
	}

	@Override
	public long getRandomSeed() {
		return randomSeed;
	}

	@Override
	public double getCancelErrorThreshold() {
		return errorAreaThreshold;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		VoroNode node = null;
		if (command.equals(OPEN_XML_FILE)) {
			JFileChooser chooser = new JFileChooser(
			        System.getProperty("user.home"));
			FileFilter filter = new XMLFileFilter();
			chooser.setFileFilter(filter);
			int returnVal = chooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				node = setXmlFile(chooser.getSelectedFile().getPath());
			}
			this.root = null;
			this.root = node;
			// this.originalNode = node;
			this.displayedRoot = root;
			setHashNode(node);
			root.setPolygon(rootPolygon);
			compute();
		} else if (command.equals(EXIT)) {
			this_windowClosing(null);
		}
	}

	/**
	 * 外部程序调用该程序定制的方法
	 * 
	 * @param dir
	 *            xml文件路径
	 */
	public void analysis(String dir) {
		VoroNode node = null;
		node = setXmlFile(dir);
		this.root = null;//++
		this.root = node;
		//this.displayedRoot = node;//--
		this.displayedRoot = root;//++
		setHashNode(node);
		rootPolygon = setDefaultPolygon();
		root.setPolygon(rootPolygon);
		compute();
	}

	public VoroNode setXmlFile(String xmlFileName) {
		VoroNode rNode = null;
		try {
			BuilderXML bXml = new BuilderXML(xmlFileName);
			rNode = bXml.getRoot();
			this.iframe.setRoot(rNode);
			//this.treeModel.setRoot(rNode);
		} catch (ParseException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.getMessage(), "File error",
			        JOptionPane.ERROR_MESSAGE);
		}
		return rNode;
	}

	class XMLFileFilter extends FileFilter {
		// return true if should accept a given file
		public boolean accept(File f) {
			if (f.isDirectory())
				return true;
			String path = f.getPath().toLowerCase();
			if (path.endsWith(".xml"))
				return true;
			return false;
		}

		@Override
		public String getDescription() {
			return "XML file (*.xml)";
		}
	}

	private void createShapes() {
		this.polyShapes.put("Rectangle", new RectanglePoly());
		this.polyShapes.put("Hexagon", new HexagonPoly());
		this.polyShapes.put("Octagon", new OctagonPoly());
		this.polyShapes.put("User-defined", new DefinedByUser());
		// this.strategies.put("Voronoi", new SplitByVoronoi());
		this.cmbPolyShape.removeAllItems();
		for (Iterator i = this.polyShapes.keySet().iterator(); i.hasNext();) {
			String key = (String) i.next();
			this.cmbPolyShape.addItem(key);
		}
	}

	void updateShape() {
		String key = (String) this.cmbPolyShape.getSelectedItem();
		// SplitStrategy strat = (SplitStrategy) this.polyShapes.get(key);
		setRootPolygon(rootPolygon);
		this.iframe.repaint();
	}
}
