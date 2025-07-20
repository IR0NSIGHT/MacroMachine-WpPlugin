package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.BreakpointListener;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * visualize the debug stepper, breakpoints etc in a tree of macro/actions
 */
public class TreeDebugStepperUI implements BreakpointListener {
    HashMap<UUID, MacroTreeNode> actionToNode = new HashMap<>();
    ArrayList<MappingAction> executionSteps;
    Consumer<TreePath> setStepperToNode;
    public TreeDebugStepperUI(MacroTreeNode rootMacro, MacroContainer macroContainer,
                              MappingActionContainer actionContainer, Consumer<TreePath> setStepperToNode) {
        this.setStepperToNode = setStepperToNode;
        addToMapRecursive(rootMacro,actionToNode);
    }

    private void addToMapRecursive(MacroTreeNode node, HashMap<UUID, MacroTreeNode> actionToNode) {
        if (node.getPayloadType() == GlobalActionPanel.SELECTION_TPYE.ACTION) {
            actionToNode.put(node.getAction().getUid(),node);
        }
        if (node.getPayloadType() == GlobalActionPanel.SELECTION_TPYE.MACRO) {
            assert node.getChildren().length == node.getMacro().getExecutionUUIDs().length;
            for (MacroTreeNode child: node.getChildren())
                addToMapRecursive(child,actionToNode);
        }

    }

    @Override
    public void OnReachedBreakpoint(int idx) {
        assert idx < executionSteps.size();
        MappingAction action = executionSteps.get(idx);
        MacroTreeNode node = actionToNode.get(action.getUid());
        assert node != null;
        if (node == null)
            return;
        TreePath path = node.getPath();
        SwingUtilities.invokeLater(()->{
            setStepperToNode.accept(path);
        });
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
