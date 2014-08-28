package de.rwthaachen.hyperhallsolver.solver;

import java.util.Collection;
import java.util.List;
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
public class ConnectedComponentsTest {

   public ConnectedComponentsTest() {
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

   @Test
   public void testGetConnectedComponent() {
      ConnectedComponents cc = new ConnectedComponents();
      cc.addVertex("a");
      cc.addVertex("b");
      cc.addVertex("c");
      cc.addVertex("d");
      cc.addVertex("e");
      cc.addEdge("a", "c");
      cc.addEdge("a", "e");
      cc.addEdge("c", "b");
      cc.addEdge("b", "d");
      cc.addEdge("d", "b");
      cc.addEdge("e", "d");
      List<String> component = (List<String>)(List<?>) cc.getConnectedComponent("c");

      assertThat(component, hasItem("c"));
      assertThat(component, hasItem("b"));
      assertThat(component, hasItem("d"));
   }
}
