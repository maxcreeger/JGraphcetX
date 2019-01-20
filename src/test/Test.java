package test;

import java.awt.Dimension;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxCodecRegistry;
import com.mxgraph.io.mxModelCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;

import jgrafcet.destructured.GrafcetReader;
import parsing.Actions;
import parsing.Convergence;
import parsing.Divergence;
import parsing.Execute;
import parsing.Grafcet;
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
        displayInJGraph(new File("C:\\Users\\Marmotte\\Git\\JGrafcet\\resources\\test\\testGrafcet.xml"));
    }

    public static void displayInJGraph(File file) throws JAXBException {
        Grafcet grafcetDAO = read(file);

        mxGraph graph = new mxGraph() {
            @Override
            public String convertValueToString(Object cell) {
                if (cell instanceof mxCell) {
                    Object value = ((mxCell) cell).getValue();
                    if (value instanceof Step) {
                        Step step = (Step) value;
                        return "Step N°" + step.getNum();
                    } else if (value instanceof Transition) {
                        Transition transition = (Transition) value;
                        return "Transition N°" + transition.getNum();
                    }
                }
                return super.convertValueToString(cell);
            }
        };
        graph.getModel().beginUpdate();
        Object parent = graph.getDefaultParent();

        // Build all steps
        Map<Integer, Object> graphSteps = new HashMap<>();
        Map<Integer, Step> daoSteps = new HashMap<>();
        for (Step stepDAO : grafcetDAO.getSteps().getStep()) {
            Object step = graph.insertVertex(parent, null, stepDAO, stepDAO.getX() * 100, stepDAO.getY() * 100 - 20, 80, 60, "ROUNDED");
            graphSteps.put(stepDAO.getNum(), step);
            daoSteps.put(stepDAO.getNum(), stepDAO);
            // TODO: do something wit actions!
            Actions actions = stepDAO.getActions();
            if (actions != null) {
                for (String action : actions.getAction()) {
                    // Do something?
                }
            }
        }

        // Build transitions
        Map<Integer, Object> graphTransitions = new HashMap<>();
        Map<Integer, Transition> daoTransitions = new HashMap<>();
        for (Transition transitionDAO : grafcetDAO.getTransitions().getTransition()) {
            Object tran = graph.insertVertex(parent, null, transitionDAO, transitionDAO.getX() * 100, transitionDAO.getY() * 100, 80, 12, "fillColor=black");
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
                graph.insertEdge(parent, null, step2transitionDAO, step, transition);
                System.out.println("Adding link: S" + step.getClass());
            } else if (someLink instanceof Transtion2Step) {
                Transtion2Step transition2stepDAO = (Transtion2Step) someLink;
                Object transition = graphTransitions.get(transition2stepDAO.getTransition());
                Object step = graphSteps.get(transition2stepDAO.getStep());
                graph.insertEdge(parent, null, transition2stepDAO, transition, step);
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
                Object concentrator = graph.insertVertex(parent, null, divergenceDAO, xMin * 100 + 10, daoTransition.getY() * 100 + 50,
                    (xMax - xMin) * 100 + 60, 1);
                graph.insertEdge(parent, null, "", graphTransition, concentrator);
                // Link to each step
                for (Execute executeDAO : divergenceDAO.getExecute()) {
                    Object step = graphSteps.get(executeDAO.getStep());
                    graph.insertEdge(parent, null, executeDAO, concentrator, step);
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
                Object concentrator = graph.insertVertex(parent, null, convergenceDAO, xMin * 100, daoTransition.getY() * 100 - 30, (xMax - xMin) * 100, 1);
                graph.insertEdge(parent, null, "", concentrator, graphTransition);
                // Link to each Tran
                for (Execute executeDAO : convergenceDAO.getExecute()) {
                    Object graphStep = graphSteps.get(executeDAO.getStep());
                    graph.insertEdge(parent, null, executeDAO, graphStep, concentrator);
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
        frame.setPreferredSize(new Dimension(400, 320));
        frame.getContentPane().add(graphComponent);
        frame.setVisible(true);
        frame.pack();

        // write to xml
        mxCodec encoder = new mxCodec();
        Node xmlRoot = encoder.encode(graph.getModel());
        String pretty = mxUtils.getPrettyXml(xmlRoot);
        System.out.println(pretty);

        // save using mxgraph
        saveGraph(graphComponent, "testFile.xml");

    }

    public static void loadGraph(mxGraphComponent graphComponent, String fileName) {

        // necessary for this to work:
        mxCodecRegistry.addPackage("parsing");
        mxCodecRegistry.register(new mxModelCodec(new parsing.Step()));

        try {

            mxGraph graph = graphComponent.getGraph();

            // taken from EditorActions class
            Document document = mxXmlUtils.parseXml(mxUtils.readFile(fileName));
            mxCodec codec = new mxCodec(document);
            codec.decode(document.getDocumentElement(), graph.getModel());

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    public static void saveGraph(mxGraphComponent graphComponent, String fileName) {

        // info: necessary for this to work:

        try {

            mxGraph graph = graphComponent.getGraph();

            // taken from EditorActions class
            mxCodec codec = new mxCodec();
            String xml = mxXmlUtils.getXml(codec.encode(graph.getModel()));
            mxUtils.writeFile(xml, fileName);

            JOptionPane.showMessageDialog(graphComponent, "File saved to: " + fileName);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

}
