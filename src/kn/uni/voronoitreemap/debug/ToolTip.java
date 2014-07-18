package kn.uni.voronoitreemap.debug;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.border.EtchedBorder;

import kn.uni.voronoitreemap.core.VoronoiCore;
import kn.uni.voronoitreemap.debug.OperateToolTip;

public class ToolTip extends JFrame {

	static boolean isCanTop = true; // 是否要求至顶（jre1.5以上版本方可执行）；

	private JFrame frame;

	private static int xx_Width;// JToolTip提示框的横坐标和纵坐标；

	private static int yy_Height;

	public ToolTip(JFrame frame, int xx, int yy) {
		this.setFrame(frame);
		this.xx_Width = xx + (int) this.frame.getLocation().getX();
		this.yy_Height = yy + (int) this.frame.getLocation().getY();
		isCanTop = true;
		
		try { // 通过调用方法，强制获知是否支持自动窗体置顶；
			JWindow.class.getMethod("setAlwaysOnTop",
					new Class[] { Boolean.class });
		} catch (Exception e) {
			//System.err.println("NotOnTop");
			isCanTop = true;
		}
	}
	
	public ToolTip(int x1,int y1, int xx, int yy) {
		this.xx_Width = xx + x1;
		this.yy_Height = yy + y1;
		isCanTop = true;
		
		try { // 通过调用方法，强制获知是否支持自动窗体置顶；
			JWindow.class.getMethod("setAlwaysOnTop",
					new Class[] { Boolean.class });
		} catch (Exception e) {
			//System.err.println("NotOnTop");
			isCanTop = true;
		}
	}

	public ToolTip(int xx, int yy) {
		this.xx_Width = xx;
		this.yy_Height = yy;
		isCanTop = true;
		
		try {
			JWindow.class.getMethod("setAlwaysOnTop",
					new Class[] { Boolean.class });
		} catch (Exception e) {
			//System.err.println("NotOnTop");
			isCanTop = false;
		}
	}

	public static class ToolTipModel extends JWindow {  //JWindow
		private static final long serialVersionUID = 1L;
		// private JLabel showImage_Label = null;//图片载体；
		private JTextArea showMessage_Texa = null;// 文字载体；

		private JPanel inner_Panel = null;// 内部JPanel；

		private JPanel external_Panel = null;// 外部JPanel；

		public ToolTipModel() {
			initComponents();
		}

		private void initComponents() {
			this.setSize(40, 20);// JToolTip的大小设置（可绝对设置，也可传入参数设置）；
			this.getContentPane().add(getExternal_Panel());
		}

		private JPanel getExternal_Panel() {// 返回外部JPanel；
			if (external_Panel == null) {
				external_Panel = new JPanel(new BorderLayout(1, 1));
				external_Panel.setBackground(new Color(255, 255, 225));
				EtchedBorder etchedBorder = (EtchedBorder) BorderFactory
						.createEtchedBorder();
				external_Panel.setBorder(etchedBorder); // 设定外部面板内容边框为风化效果
				external_Panel.add(getInner_Panel());// 加载内部面板
			}
			return external_Panel;
		}

		private JPanel getInner_Panel() {// 返回内部JPanel；
			if (inner_Panel == null) {
				inner_Panel = new JPanel();
				inner_Panel.setLayout(null);
				inner_Panel.setBackground(new Color(255, 255, 225));
				// inner_Panel.add(get_IconLabel(), null);
				inner_Panel.add(get_Message(), null);
			}
			return inner_Panel;
		}

		private JTextArea get_Message() {
			if (showMessage_Texa == null) {
				showMessage_Texa = new JTextArea();
				showMessage_Texa.setBackground(new Color(255, 255, 225));
				showMessage_Texa.setMargin(new Insets(1, 1, 1, 1));// 设置组件的边框和它的文本之间的空白。
				showMessage_Texa.setLineWrap(true);
				showMessage_Texa.setWrapStyleWord(true);
				showMessage_Texa.setForeground(Color.BLACK);
				showMessage_Texa.setBounds(0, 0, 40, 18);
			}
			return showMessage_Texa;
		}

		// private JLabel get_IconLabel(){
		// if(showImage_Label==null){
		// showImage_Label=new JLabel();
		// showImage_Label.setBounds(10, 10, 140, 225);
		// }
		// return showImage_Label;
		// }

		public void animate() {
			//System.out.println(isCanTop);
			new OperateToolTip(this, isCanTop, xx_Width, yy_Height);
			OperateToolTip.begin();
		}
	}

	private static ToolTipModel single = new ToolTipModel();

	public static void setToolTip(String msg) { // setToolTip(Icon icon, String
												// msg)
		// single = new ToolTipModel();
		// if (icon != null) {
		// single.get_IconLabel().setIcon(icon);
		// }
		single.showMessage_Texa.setText(msg);
		single.animate();
	}

	public JFrame getFrame() {
		return frame;
	}

	public void setFrame(JFrame frame) {
		this.frame = frame;
	}
}