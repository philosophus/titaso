package de.rwthaachen.hyperhallsolver.solver;

import de.rwthaachen.hyperhallsolver.model.Event;
import de.rwthaachen.hyperhallsolver.model.Instance;
import de.rwthaachen.hyperhallsolver.model.Timeslot;
import de.rwthaachen.hyperhallsolver.model.TimeslotGroup;
import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class ColoringSolver {

   private Instance instance;

   GRBEnv env;
   GRBModel model;
   Map<TimeslotGroup, GRBVar> variables;

   public ColoringSolver(Instance instance) {
      this.instance = instance;
   }

   public void setUp(String logfilename) throws GRBException {
      assert(logfilename != null);
      assert(logfilename.length() >= 1);

      env = new GRBEnv(logfilename);
      model = new GRBModel(env);
   }

   public void createVaribles() throws GRBException {
      assert(env != null);
      assert(model != null);

      variables = new HashMap();
      for (Event event: instance.getEvents()) {
         for (TimeslotGroup possibleTimeslot : event.getPossibleTimeslots()) {
            String varName = "Event " + event.getId() + " at time";
            for (Timeslot timeslot : possibleTimeslot.getTimeslots()) {
               varName += " " +timeslot.getId();
            }
            GRBVar var = model.addVar(0.0, 1.0, possibleTimeslot.getWeight(), GRB.BINARY, varName);

            variables.put(possibleTimeslot, var);
         }
      }
   }

   public GRBModel getModel() {
      return model;
   }

   public Map<TimeslotGroup, GRBVar> getVariables() {
      return variables;
   }
}
