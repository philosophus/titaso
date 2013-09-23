package de.rwthaachen.hyperhallsolver;

import de.rwthaachen.hyperhallsolver.model.Event;
import de.rwthaachen.hyperhallsolver.model.Instance;
import de.rwthaachen.hyperhallsolver.model.RoomGroup;
import de.rwthaachen.hyperhallsolver.model.TimeslotGroup;
import de.rwthaachen.hyperhallsolver.solver.ColoringSolver;
import de.rwthaachen.hyperhallsolver.solver.SimpleMatcher;
import gurobi.GRBEnv;
import gurobi.GRBException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

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

      Map<Event, TimeslotGroup> assignedTimeslots = solver.getSolution();
      SimpleMatcher matcher = new SimpleMatcher(instance, assignedTimeslots);
      matcher.setUpAndCreateModel(new GRBEnv());
      matcher.solve();
      Map<Event, RoomGroup> assignedRooms = matcher.getSolution();

      instance.assignSolution(assignedTimeslots, assignedRooms);
      instance.save(new File(filename+"_sol"));
   }
}
