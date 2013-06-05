package de.rwthaachen.hyperhallsolver;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author Florian Dahms <dahms@or.rwth-aachen.de>
 */
public class TestGurobi {

   public TestGurobi() {
   }

   @BeforeClass
   public static void setUpClass() {
   }

   @AfterClass
   public static void tearDownClass() {
   }

   @Before
   public void setUp() {
   }

   @After
   public void tearDown() {
   }
   // TODO add test methods here.
   // The methods must be annotated with annotation @Test. For example:
   //
   // @Test
   // public void hello() {}

   @Test
   public void testGurobiFunctionality() throws GRBException {
      GRBEnv env = new GRBEnv("mip1.log");
      GRBModel model = new GRBModel(env);

      // Create variables

      GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x");
      GRBVar y = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y");
      GRBVar z = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "z");

      // Integrate new variables

      model.update();

      // Set objective: maximize x + y + 2 z

      GRBLinExpr expr = new GRBLinExpr();
      expr.addTerm(1.0, x);
      expr.addTerm(1.0, y);
      expr.addTerm(2.0, z);
      model.setObjective(expr, GRB.MAXIMIZE);

      // Add constraint: x + 2 y + 3 z <= 4

      expr = new GRBLinExpr();
      expr.addTerm(1.0, x);
      expr.addTerm(2.0, y);
      expr.addTerm(3.0, z);
      model.addConstr(expr, GRB.LESS_EQUAL, 4.0, "c0");

      // Add constraint: x + y >= 1

      expr = new GRBLinExpr();
      expr.addTerm(1.0, x);
      expr.addTerm(1.0, y);
      model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c1");

      // Optimize model

      model.optimize();

      // Check solution
      assertThat(x.get(GRB.DoubleAttr.X), is(1.0));
      assertThat(y.get(GRB.DoubleAttr.X), is(0.0));
      assertThat(z.get(GRB.DoubleAttr.X), is(1.0));
      assertThat(model.get(GRB.DoubleAttr.ObjVal), is(3.0));

      // Dispose of model and environment

      model.dispose();
      env.dispose();
   }
}
