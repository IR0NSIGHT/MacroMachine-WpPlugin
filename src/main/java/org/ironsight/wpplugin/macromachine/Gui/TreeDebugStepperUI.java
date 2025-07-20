package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.BreakpointListener;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * visualize the debug stepper, breakpoints etc in a tree of macro/actions
 */
public class TreeDebugStepperUI implements BreakpointListener {
    HashMap<MappingAction, MacroTreeNode> actionToNode = new HashMap<>();
    ArrayList<MappingAction> executionSteps;
    Consumer<TreePath> setStepperToNode;
    public TreeDebugStepperUI(MacroTreeNode rootMacro, MacroContainer macroContainer,
                              MappingActionContainer actionContainer, Consumer<TreePath> setStepperToNode) {
        this.setStepperToNode = setStepperToNode;
        addToMapRecursive(rootMacro,actionToNode);
    }

    private void addToMapRecursive(MacroTreeNode node, HashMap<MappingAction, MacroTreeNode> actionToNode) {
        if (node.getPayloadType() == GlobalActionPanel.SELECTION_TPYE.ACTION) {
            actionToNode.put(node.getAction(),node);
        }
        for (MacroTreeNode child: node.getChildren())
            addToMapRecursive(child,actionToNode);
    }

    @Override
    public void OnReachedBreakpoint(int idx) {
        assert idx < executionSteps.size();
        MappingAction action = executionSteps.get(idx);
        MacroTreeNode node = actionToNode.get(action);
        assert node != null;
        TreePath path = node.getPath();
        setStepperToNode.accept(path);
    }

    @Override
    public void PostReachedBreakpoint(int idx) {

    }

    @Override
    public void SetBreakpoints(ArrayList<MappingAction> breakpoints) {
        this.executionSteps = breakpoints;
    }

    @Override
    public void afterEverything() {

    }
}
