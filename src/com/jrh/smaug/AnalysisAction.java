package com.jrh.smaug;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kn.uni.voronoitreemap.treemap.VoronoiTreemap;

/**
 * Servlet implementation class AnalysisAction
 */
@WebServlet("/AnalysisAction")
public class AnalysisAction extends HttpServlet {
	public static String contextPath;
	private static final long serialVersionUID = 1L;
	public VoronoiTreemap vt;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AnalysisAction() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
	        HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
	        HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		// response.setContentType("image/jpeg");
		// request.setCharacterEncoding("utf-8");
		// response.setCharacterEncoding("utf-8");

		// 获取工程根目录
		contextPath = request.getContextPath();

		String log = "";
		String requestFileSerial = request.getParameter("analysis_file");
		String x = request.getParameter("x");
		String y = request.getParameter("y");
		String info = request.getParameter("info");

		if (vt == null) {
			vt = new VoronoiTreemap();
		}

		if (requestFileSerial != null && !requestFileSerial.equals("")) {

			response.setContentType("image/jpeg");
			log = requestFileSerial;
			if (log.equals("refresh")) {
				vt.saveImage(vt.iframe.getGraphics());
				OutputStream responseOutputStream = response.getOutputStream();
				ImageIO.write(vt.image, "jpg", responseOutputStream);
			} else {

				// 找到对应的xml文件路径，传给analysis方法
				String dir = locaFile(requestFileSerial);
				vt.analysis(dir);
				log = vt.finish;
				vt.saveImage(vt.iframe.getGraphics());
				OutputStream responseOutputStream = response.getOutputStream();
				ImageIO.write(vt.image, "jpg", responseOutputStream);
			}
		}

		if (x != null && !x.equals("") && y != null && !y.equals("")
		        && info == null) {
			response.setContentType("image/jpeg");
			vt.onTouch(Integer.parseInt(x), Integer.parseInt(y));
			vt.saveImage(vt.iframe.getGraphics());
			OutputStream responseOutputStream = response.getOutputStream();
			ImageIO.write(vt.image, "jpg", responseOutputStream);
		}

		if (info != null && !info.equals("")) {
			response.setContentType("text/html; charset=utf-8");
			log = vt.getInfo(Integer.parseInt(x), Integer.parseInt(y));
			request.setCharacterEncoding("utf-8");
			response.setCharacterEncoding("utf-8");
			PrintWriter out = response.getWriter();
			out.print(log);
		}

		// vt.saveImage(vt.iframe.getGraphics());
		// OutputStream responseOutputStream = response.getOutputStream();
		// ImageIO.write(vt.image, "jpg", responseOutputStream);
		// out.print("ddd");
	}

	/**
	 * 根据客户端传来的参数找到匹配的xml文件路径
	 * 
	 * @param requestFileSerial
	 * @return
	 */
	private String locaFile(String requestFileSerial) {
		String dir = "";
		int serial = Integer.parseInt(requestFileSerial);
		switch (serial) {
		case 1:
			dir = "C:/Users/Rio/files/TreeMap--BCD.xml";
			break;
		case 2:
			dir = "C:/Users/Rio/files/TreeMap4.xml";
			break;
		case 3:
			dir = "C:/Users/Rio/files/TreeMap6--E--BC.xml";
			break;
		case 4:
			dir = "C:/Users/Rio/files/TreeMap6--E--BCD.xml";
			break;
		case 5:
			dir = "C:/Users/Rio/files/TreeMap6--E--C.xml";
			break;
		case 6:
			dir = "C:/Users/Rio/files/TreeMap5.xml";
			break;
		default:
			break;
		}
		return dir;
	}
}
