package de.rwthaachen.hyperhallsolver;

import de.rwthaachen.hyperhallsolver.model.Instance;
import de.rwthaachen.hyperhallsolver.solver.ColoringSolver;
import gurobi.GRBException;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class Main {

   /**
    * @param args the command line arguments
    */
   public static void main(String[] args) throws IOException, GRBException {

      String filename = args[0];
      Instance instance = new Instance(new File(filename));

      ColoringSolver solver = new ColoringSolver(instance);
      solver.setUp("log");

      solver.createVaribles();
      solver.createBoundEventOccurenceConstraints();
      solver.createStrictTimeConflictConstraints();
      solver.createSoftTimeConflictConstraintsAndVariables();
      solver.addHyperHallSeperator();
      solver.setObjective();

      solver.solve();
   }
}
