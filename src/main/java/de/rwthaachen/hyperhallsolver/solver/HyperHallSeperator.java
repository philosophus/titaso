package de.rwthaachen.hyperhallsolver.solver;

import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBModel;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class HyperHallSeperator extends GRBCallback {

   GRBModel model;

   public HyperHallSeperator(GRBModel model) {
      this.model = model;
   }

   

   @Override
   protected void callback() {
      if (this.where == GRB.CB_MIPNODE)
         System.out.println("** Mipnode");
      if (this.where == GRB.CB_MIPSOL)
         System.out.println("** Mipsol");
   }

}
