package kn.uni.voronoitreemap.IO;

import java.text.NumberFormat;

/**
 * class who can display the values of elements of a JTreeMap with pourcent
 * 
 * @author Laurent Dutheil
 */

public class ValuePercent extends Value {
  private double value;
  private NumberFormat nf;

  /**
   * Constructor of ValuePercent
   */
  public ValuePercent() {
    this.nf = NumberFormat.getInstance();
    this.nf.setMaximumFractionDigits(2);
    this.nf.setMinimumFractionDigits(2);
    this.nf.setMinimumIntegerDigits(1);
  }

  /**
   * Constructor of ValuePercent
   * 
   * @param value double value
   */
  public ValuePercent(double value) {
    this();
    this.value = value;
  }

  public void setValue(double d) {
    this.value = d;
  }

  public void setLabel(String stLibelle) {
  //ignore
  }

  public double getValue() {
    return this.value;
  }

  public String getLabel() {
    if (this.value >= 0) {
      return "+" + this.nf.format(this.value) + " %";
    }
    return this.nf.format(this.value) + " %";
  }

}
