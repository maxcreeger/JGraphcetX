package test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.xml.bind.JAXBException;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

import parsing.Convergence;
import parsing.Divergence;
import parsing.Execute;
import parsing.Grafcet;
import parsing.GrafcetReader;
import parsing.Links;
import parsing.Step;
import parsing.Step2Transition;
import parsing.Transition;
import parsing.Transtion2Step;

public class Test extends GrafcetReader {

	public static void main(String[] arg) throws InterruptedException, JAXBException {
		testLoadXML();
		//testOrDivergence();
		//testDestructured();
	}

	public static void testLoadXML() throws JAXBException, InterruptedException {
		displayInJGraph(new File("C:\\Users\\Gwen\\eclipse-workspace\\JGrafcet\\resources\\test\\testGrafcet.xml"));
	}

	public static void displayInJGraph(File file) throws JAXBException {
		Grafcet grafcetDAO = read(file);

		mxGraph graph = new mxGraph();
		graph.getModel().beginUpdate();
		Object parent = graph.getDefaultParent();

		// Build all steps
		Map<Integer, Object> graphSteps = new HashMap<>();
		Map<Integer, Step> daoSteps = new HashMap<>();
		for (Step stepDAO : grafcetDAO.getSteps().getStep()) {
			Object step = graph.insertVertex(parent, null, "Step " + stepDAO.getNum(), stepDAO.getX() * 100, stepDAO.getY() * 100 - 20, 80, 60, "ROUNDED");
			graphSteps.put(stepDAO.getNum(), step);
			daoSteps.put(stepDAO.getNum(), stepDAO);
			// Add actions
			List<Object> actions = stepDAO.getContent();
		}

		// Build transitions
		Map<Integer, Object> graphTransitions = new HashMap<>();
		Map<Integer, Transition> daoTransitions = new HashMap<>();
		for (Transition transitionDAO : grafcetDAO.getTransitions().getTransition()) {
			Object tran = graph.insertVertex(parent, null, "Trans " + transitionDAO.getNum(), transitionDAO.getX() * 100, transitionDAO.getY() * 100, 80, 12,
				"fillColor=black");
			graphTransitions.put(transitionDAO.getNum(), tran);
			daoTransitions.put(transitionDAO.getNum(), transitionDAO);
		}

		// Link
		Links linksDAO = grafcetDAO.getLinks();
		for (Object someLink : linksDAO.getStep2TransitionOrDivergenceOrConvergence()) {
			if (someLink instanceof Step2Transition) {
				Step2Transition step2transitionDAO = (Step2Transition) someLink;
				Object step = graphSteps.get(step2transitionDAO.getStep());
				Object transition = graphTransitions.get(step2transitionDAO.getTransition());
				graph.insertEdge(parent, null, "", step, transition);
				System.out.println("Adding link: S" + step.getClass());
			} else if (someLink instanceof Transtion2Step) {
				Transtion2Step transition2stepDAO = (Transtion2Step) someLink;
				Object transition = graphTransitions.get(transition2stepDAO.getTransition());
				Object step = graphSteps.get(transition2stepDAO.getStep());
				graph.insertEdge(parent, null, "", transition, step);
			} else if (someLink instanceof Divergence) {
				Divergence divergenceDAO = (Divergence) someLink;
				Object graphTransition = graphTransitions.get(divergenceDAO.getTransition());
				Transition daoTransition = daoTransitions.get(divergenceDAO.getTransition());
				// Compute concentrator span
				int xMin = daoTransition.getX();
				int xMax = daoTransition.getX();
				for (Execute executeDAO : divergenceDAO.getExecute()) {
					xMin = Math.min(xMin, daoSteps.get(executeDAO.getStep()).getX());
					xMax = Math.max(xMax, daoSteps.get(executeDAO.getStep()).getX());
				}
				// Create concentrator
				Object concentrator = graph.insertVertex(parent, null, "", xMin * 100 + 10, daoTransition.getY() * 100 + 50, (xMax - xMin) * 100 + 60, 1);
				graph.insertEdge(parent, null, "", graphTransition, concentrator);
				// Link to each step
				for (Execute executeDAO : divergenceDAO.getExecute()) {
					Object step = graphSteps.get(executeDAO.getStep());
					graph.insertEdge(parent, null, "", concentrator, step);
				}
			} else if (someLink instanceof Convergence) {
				Convergence convergenceDAO = (Convergence) someLink;
				Object graphTransition = graphTransitions.get(convergenceDAO.getTransition());
				Transition daoTransition = daoTransitions.get(convergenceDAO.getTransition());
				// Compute concentrator span
				int xMin = daoTransition.getX();
				int xMax = daoTransition.getX();
				for (Execute executeDAO : convergenceDAO.getExecute()) {
					xMin = Math.min(xMin, daoSteps.get(executeDAO.getStep()).getX());
					xMax = Math.max(xMax, daoSteps.get(executeDAO.getStep()).getX());
				}
				// Create concentrator
				Object concentrator = graph.insertVertex(parent, null, "", xMin * 100, daoTransition.getY() * 100 - 30, (xMax - xMin) * 100, 1);
				graph.insertEdge(parent, null, "", concentrator, graphTransition);
				// Link to each Tran
				for (Execute executeDAO : convergenceDAO.getExecute()) {
					Object graphStep = graphSteps.get(executeDAO.getStep());
					graph.insertEdge(parent, null, "", graphStep, concentrator);
				}
			}
		}
		graph.getModel().endUpdate();
		graph.getStylesheet().getDefaultEdgeStyle().put(mxConstants.STYLE_EDGE,
			mxConstants.EDGESTYLE_ELBOW /*your desired style e.g. mxConstants.EDGESTYLE_ELBOW*/);
		System.out.println(graph.toString());
		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		graphComponent.setConnectable(false);
		//graph.setAllowDanglingEdges(false);
		graph.setDisconnectOnMove(false);
		//graphComponent.setEnabled(false); disables all edition
		graph.setCellsEditable(false);
		JFrame frame = new JFrame("jgraph: grafcet");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 320);
		frame.setVisible(true);
		frame.getContentPane().add(graphComponent);
	}

}
