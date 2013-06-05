package de.rwthaachen.hyperhallsolver.solver;

import de.rwthaachen.hyperhallsolver.model.Instance;
import gurobi.GRBException;
import java.io.File;
import java.io.IOException;
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
public class ColoringSolverTest {

   private Instance instance;

   public ColoringSolverTest() {
   }

   @BeforeClass
   public static void setUpClass() {
   }

   @AfterClass
   public static void tearDownClass() {
   }

   @Before
   public void setUp() throws IOException {
      instance = new Instance(new File("instances/xxs"));
   }

   @After
   public void tearDown() {
   }

   @Test
   public void testColoringSolverSetUp() throws GRBException {
      ColoringSolver solver = new ColoringSolver(instance);
      solver.setUp("testLog.log");
      assertThat(solver.getModel(), notNullValue());

      solver.createVaribles();
      assertThat(solver.getVariables().size(), is(9));

      solver.createBoundEventOccurenceConstraints();
      assertThat(solver.getBoundEventOccurenceConstraints().size(), is(4));

      solver.createStrictTimeConflictConstraints();
      assertThat(solver.getStrictTimeConflictConstraints().size(), is(2));

      solver.setObjective();

      solver.solve();
   }
}
