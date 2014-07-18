package kn.uni.voronoitreemap.treemap;

import java.util.ArrayList;

import kn.uni.voronoitreemap.IO.Value;

/**
 * Tree builder for a JTreeMap.
 * 
 * @author Laurent Dutheil
 */

public class VoroNodeBuilder {
  private VoroNode root;

  /**
   * Add a branch to the tree. <BR>
   * If the parent is null, the build node become the root if and only if the
   * tree have no root yet. If the parent is null and if the root is already
   * build, the node will NOT be added to the tree.
   * 
   * @param label label of the node
   * @param parent father of the node
   * @return the created node
   */
  public VoroNode buildBranch(String label, VoroNode parents) {
	  VoroNode node = new VoroNode(label);
    if (parents != null){
    	parents.add(node);
    	parents.addChild(node);
    }
    else if (this.root == null)
      this.root = node;
    return node;
  }

  /**
   * add a leaf to the tree. <BR>
   * If the parent is null, the build node become the root if and only if the
   * tree have no root yet. If the parent is null and if the root is already
   * build, the node will NOT be added to the tree.
   * @param label label of the leaf
   * @param weight weight of the leaf
   * @param value Value of the leaf
   * @param parent father of the leaf
   * @return the created node
   */
  public VoroNode buildLeaf(String label, double weight, Value value,
		  VoroNode parents) {
	  VoroNode node = new VoroNode(label, weight/1000, value);
    if (parents != null){   	
    	parents.add(node);
    	parents.addChild(node);
    }
    else if (this.root == null)
      this.root = node;
    return node;
  }

  /**
   * get the build tree.
   * @return the root of the tree
   */
  public VoroNode getRoot() {
    return this.root;
  }
}
