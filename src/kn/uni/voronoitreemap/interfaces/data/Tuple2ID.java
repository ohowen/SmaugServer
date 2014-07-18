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
package kn.uni.voronoitreemap.interfaces.data;

/**
 * Tuple with two elements. The first is the id and the second is the corresponding value to it.
 * @author Arlind Nocaj
 *
 */
public class Tuple2ID {

//	public int id;
//	public double value;
	public String name;
	public double weight;
	
	public Tuple2ID(){
	}
	
	public Tuple2ID(String name, double weight){
		this.name = name;
		this.weight = weight;
	}
}
