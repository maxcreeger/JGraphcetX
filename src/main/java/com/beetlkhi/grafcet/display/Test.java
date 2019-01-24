package com.beetlkhi.grafcet.display;

import java.awt.Dimension;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.beetlekhi.grafcet.model.Actions;
import com.beetlekhi.grafcet.model.Executed;
import com.beetlekhi.grafcet.model.Grafcet;
import com.beetlekhi.grafcet.model.GrafcetUtils;
import com.beetlekhi.grafcet.model.Required;
import com.beetlekhi.grafcet.model.Step;
import com.beetlekhi.grafcet.model.Transition;
import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxCodecRegistry;
import com.mxgraph.io.mxModelCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;

public class Test {

    public static void main(String[] arg) throws InterruptedException, JAXBException {
        testLoadXML();
        //testOrDivergence();
        //testDestructured();
    }

    public static void testLoadXML() throws JAXBException, InterruptedException {
        displayInJGraph(new File("C:\\Users\\Marmotte\\Git\\JGraphcetX\\src\\main\\resources\\jgrafcetxTest.xml"));
    }

    public static void displayInJGraph(File file) throws JAXBException {
        Grafcet grafcetDAO = GrafcetUtils.readGrafcetFromXML(file);

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

            // Setup transition upstream step links
            if (transitionDAO.getRequiredSteps() != null) {
                List<Required> requiredStepsDAO = transitionDAO.getRequiredSteps().getRequired();
                if (requiredStepsDAO.size() == 1) {
                    Object step = graphSteps.get(requiredStepsDAO.get(0).getStep());
                    graph.insertEdge(parent, null, null, step, tran);
                } else if (requiredStepsDAO.size() > 1) {
                    // Compute concentrator span
                    int xMin = transitionDAO.getX();
                    int xMax = transitionDAO.getX();
                    for (Required required : requiredStepsDAO) {
                        xMin = Math.min(xMin, daoSteps.get(required.getStep()).getX());
                        xMax = Math.max(xMax, daoSteps.get(required.getStep()).getX());
                    }
                    // Create concentrator
                    Object concentrator = graph.insertVertex(parent, null, null, xMin * 100, transitionDAO.getY() * 100 - 30, (xMax - xMin) * 100, 1);
                    graph.insertEdge(parent, null, "", concentrator, tran);
                    // Link to each Tran
                    for (Required executeDAO : requiredStepsDAO) {
                        Object graphStep = graphSteps.get(executeDAO.getStep());
                        graph.insertEdge(parent, null, executeDAO, graphStep, concentrator);
                    }
                }
            }

            // Setup transition downstream step links
            if (transitionDAO.getExecutedSteps() != null) {
                List<Executed> executedStepsDAO = transitionDAO.getExecutedSteps().getExecuted();
                if (executedStepsDAO.size() == 1) {
                    Object step = graphSteps.get(executedStepsDAO.get(0).getStep());
                    graph.insertEdge(parent, null, null, tran, step);
                } else if (executedStepsDAO.size() > 1) {
                    // Compute concentrator span
                    int xMin = transitionDAO.getX();
                    int xMax = transitionDAO.getX();
                    for (Executed executeDAO : executedStepsDAO) {
                        xMin = Math.min(xMin, daoSteps.get(executeDAO.getStep()).getX());
                        xMax = Math.max(xMax, daoSteps.get(executeDAO.getStep()).getX());
                    }
                    // Create concentrator
                    Object concentrator = graph.insertVertex(parent, null, null, xMin * 100 + 10, transitionDAO.getY() * 100 + 50, (xMax - xMin) * 100 + 60, 1);
                    graph.insertEdge(parent, null, null, tran, concentrator);
                    // Link to each step
                    for (Executed executeDAO : executedStepsDAO) {
                        Object step = graphSteps.get(executeDAO.getStep());
                        graph.insertEdge(parent, null, executeDAO, concentrator, step);
                    }
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
        mxCodecRegistry.register(new mxModelCodec(new com.beetlekhi.grafcet.model.Step()));

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
