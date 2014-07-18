package kn.uni.voronoitreemap.treemap;

import java.awt.Rectangle;

import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.interfaces.StatusObject;

public class StatusObjectImpl implements StatusObject {
	public void finishedNode(String node, int layer, String[] children,
	        PolygonSimple[] polygons) {
		// System.out.println("finished node Name. " + node + " in layer: " +
		// layer + " with children: " + Arrays.toString(children));
		// if(polygons!=null){
		// for(PolygonSimple poly : polygons){
		// Rectangle bound = poly.getBounds();
		// System.out.println(bound.getWidth()+" "+bound.getHeight());
		// }
		// }
	}

	public void finished() {
		// System.out.println("Computation is finished!");
	}
}
