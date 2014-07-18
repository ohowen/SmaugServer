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
package kn.uni.voronoitreemap.debug;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import javax.swing.Timer;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.JToolTip;

import kn.uni.voronoitreemap.debug.ToolTip;
import kn.uni.voronoitreemap.interfaces.data.Tuple2ID;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.polyShape.RectanglePoly;
import kn.uni.voronoitreemap.treemap.VoroNode;
import kn.uni.voronoitreemap.treemap.VoronoiTreemap;
import kn.uni.voronoitreemap.core.VoronoiCore;

/**
 * JFrame with a buffered image.
 * @author Arlind Nocaj
 */

public class ImageFrame extends JComponent { // JComponent JFrame
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private BufferedImage image;
	private JPanel panel = new JPanel();
	// private JTextArea area;
	private VoroNode root = null;
	private VoroNode displayedRoot = null;
	private VoroNode tempNode = new VoroNode();
	private String tempStr = new String();

	public ImageFrame(){
		super();
		this.root = getRootCell();
		initListeners();
		this.addMouseMotionListener(new HandleMouseMotion());
	}
	
	public ImageFrame(BufferedImage image) {
		super();
		this.image = image;
		this.root = getRootCell();
		initListeners();
		this.addMouseMotionListener(new HandleMouseMotion());
	}

//	public void SetToolTip(String msg, int x, int y) {
//		panel = new JPanel();
//		panel.setLocation(x, y-50);
//		panel.setSize(40, 20);
//		panel.setOpaque(false);
//		text = new JTextField();
//		text.setText(msg);
//		panel.add(text);
//		this.getLayeredPane().add(panel, JLayeredPane.POPUP_LAYER);   //将panel置于顶层
//		this.setVisible(true);
//	}
	
	private void initListeners() {
		this.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				System.out.println(e.getX() + " " + e.getY());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				//OperateToolTip.close();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
			}
		});
	}

	private class HandleMouseMotion extends MouseMotionAdapter {
		private int tx, ty;

		public void mouseMoved(MouseEvent e) {
			//panel.setVisible(false);
			if (VoroNode.isInPolygon(VoronoiTreemap.rootPolygon, e.getX(),
					e.getY())) {   //判定鼠标区域是否在总区域里面
				VoroNode t = getRootCell().getActiveLeaf(e.getX(), e.getY());   //获取鼠标点对应的节点
				if (getActiveLeaf() != t) {
					setActiveLeaf(t);
					repaint();
				}
				if (t != null) {    //节点存在
					System.err.println(t.getNodeName());
					if (tx != e.getX() || ty != e.getY()) {	    //鼠标移动					
						if (tempNode == t) {          //保留上次获取的节点信息，先做判定，如果鼠标仍在上次子区域中，直接输出信息
							//ToolTip.setToolTip(tempStr);						
							//VoronoiCore.iframe.SetToolTip(tempStr, e.getX(),e.getY());
							setToolTipText(tempStr);
							repaint();
						} else {                      //如果鼠标移动到其他子区域，获取该区域信息
							for (int i = 0; i < VoronoiTreemap.areaGoals.size(); i++) {
								Tuple2ID area = VoronoiTreemap.areaGoals.get(i);
								if (t.getNodeName() == area.name) {
									tempStr = t.getNodeName() + " " + area.weight;
									//VoronoiCore.iframe.SetToolTip(t.getNodeName()+ " " + area.weight, e.getX(),e.getY());
									//ToolTip.setToolTip(t.getNodeName()+" "+area.weight);
									setToolTipText(t.getNodeName()+" "+area.weight);
									break;
								}
							}
						}
						tx = e.getX();
						ty = e.getY();
					}
				} else {
				    setToolTipText(null);
					//ToolTip.setToolTip(null);
					//VoronoiCore.iframe.SetToolTip(null, e.getX(), e.getY());
				}
				tempNode = t;
			} else {
				//panel.setVisible(false);
				repaint();
				//OperateToolTip.close();
			}
		}
	}

	public VoroNode getRoot() {
	    return this.root;
	  }
	protected VoroNode getRootCell() {
		return VoronoiTreemap.root;
	}
	
	public VoroNode getDisplayedRoot() {
		return displayedRoot;
	}

	public void setDisplayedRoot(VoroNode displayedRoot) {
		this.displayedRoot = displayedRoot;
	}

	public VoroNode getActiveLeaf() {
		return VoronoiTreemap.activeLeaf;
	}

	public void setActiveLeaf(VoroNode newActiveLeaf) {
		if (newActiveLeaf == null || newActiveLeaf.isLeaf()) {
			VoronoiTreemap.activeLeaf = newActiveLeaf;
		}
	}
	protected void setNewPolygon(VoroNode dest){
    	PolygonSimple poly = dest.getPolygon();
    	Rectangle bounds = poly.getBounds();
    	
    	for(int i=0;i<poly.length;i++){
    		poly.getXPoints()[i] = poly.getXPoints()[i]*(VoronoiTreemap.iframe.getWidth()/bounds.getWidth());
    		poly.getYPoints()[i] = poly.getYPoints()[i]*(VoronoiTreemap.iframe.getHeight()/bounds.getHeight());
    	}
    }

	@Override
	public void paint(Graphics g) {
		g.drawImage(image, 10, 0, this);
		g.setColor(Color.white);
	}
	
	
	
	public void setRoot(VoroNode root) {
		this.root = root;
	}

}
