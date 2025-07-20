package org.ironsight.wpplugin.macromachine.Gui;

import org.ironsight.wpplugin.macromachine.operations.Macro;
import org.ironsight.wpplugin.macromachine.operations.MacroContainer;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.ironsight.wpplugin.macromachine.operations.MappingActionContainer;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IDisplayUnit;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueGetter;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.IPositionValueSetter;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

class MacroTreeNode implements TreeNode {
    final Object payload;
    GlobalActionPanel.SELECTION_TPYE payloadType;

    public MacroTreeNode[] getChildren() {
        return children;
    }

    private MacroTreeNode[] children;
    private MacroTreeNode parent;

    public boolean isActive() {
        return isActive;
    }

    private boolean isActive;

    /**
     * constructor from root
     *
     * @param actions
     * @param macros
     */
    public MacroTreeNode(MappingActionContainer actions, MacroContainer macros) {
        //ROOT NODE
        children = new MacroTreeNode[macros.queryAll().size()];
        int i = 0;
        for (Macro macro : macros.queryAll()
                .stream()
                .sorted(Comparator.comparing(Macro::getName))
                .toArray(Macro[]::new)) {
            children[i++] = new MacroTreeNode(macro, true, actions, macros);
        }
        for (MacroTreeNode child : children)
            child.setParent(this);
        payloadType = GlobalActionPanel.SELECTION_TPYE.INVALID;
        payload = new IDisplayUnit() {
            @Override
            public String getName() {
                return "All macros";
            }

            @Override
            public String getDescription() {
                return "root";
            }

            @Override
            public String getToolTipText() {
                return "";
            }
        };
        assert parent == null;
    }

    /**
     * constructor for a macro (and recursively all nested macros and actions)
     *
     * @param macro
     * @param actions
     * @param macros
     */
    public MacroTreeNode(Macro macro, boolean isActive, MappingActionContainer actions, MacroContainer macros) {
        payload = macro;
        LinkedList<MacroTreeNode> nodes = new LinkedList<>();
        int idx = 0;
        for (UUID id : macro.getExecutionUUIDs()) {
            if (macros.queryContains(id))
                nodes.add(new MacroTreeNode(macros.queryById(id), macro.getActiveActions()[idx], actions, macros));
            else if (actions.queryContains(id))
                nodes.add(new MacroTreeNode(actions.queryById(id), macro.getActiveActions()[idx]));
            idx++;
        }
        children = nodes.toArray(new MacroTreeNode[0]);

        for (MacroTreeNode child : children)
            child.setParent(this);
        payloadType = GlobalActionPanel.SELECTION_TPYE.MACRO;
        this.isActive = isActive;
    }

    /**
     * constructor for action
     *
     * @param action
     */
    public MacroTreeNode(MappingAction action, boolean isActive) {
        payload = action;
        children = new MacroTreeNode[2];
        children[0] = new MacroTreeNode(action.input, action);
        children[1] = new MacroTreeNode(action.output, action);
        for (MacroTreeNode child : children)
            child.setParent(this);
        payloadType = GlobalActionPanel.SELECTION_TPYE.ACTION;
        this.isActive = isActive;
    }

    public MacroTreeNode(IPositionValueSetter output, MappingAction action) {
        payload = action;
        children = new MacroTreeNode[0];
        payloadType = GlobalActionPanel.SELECTION_TPYE.OUTPUT;
        this.isActive = true;
    }

    public MacroTreeNode(IPositionValueGetter input, MappingAction action) {
        payload = action;
        children = new MacroTreeNode[0];
        payloadType = GlobalActionPanel.SELECTION_TPYE.INPUT;
        this.isActive = true;
    }

    public TreePath getPath() {
        LinkedList<Object> path = new LinkedList<>();
        path.add(this);
        MacroTreeNode it = this;
        while (it.getParent() != null) {
            it = (MacroTreeNode) it.getParent();
            path.add(0, it);
        }

        return new TreePath(path.toArray(new Object[0]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroTreeNode that = (MacroTreeNode) o;
        if (this.getPayloadType() != that.getPayloadType())
            return false;
        switch (getPayloadType()) {
            case INVALID: //root
                return true;
            case MACRO:
                return this.getMacro().getUid().equals(that.getMacro().getUid());
            case ACTION:
            case INPUT:
            case OUTPUT:
                return this.getAction().getUid().equals(that.getAction().getUid());
            default:
                throw new RuntimeException("incomplete enum");
        }
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return children[childIndex];
    }

    @Override
    public int getChildCount() {
        return children.length;
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    private void setParent(MacroTreeNode node) {
        this.parent = node;
    }

    @Override
    public int getIndex(TreeNode node) {
        if (node == null) {
            return -1;
        }
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) == node) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return children.length == 0;
    }

    @Override
    public Enumeration<? extends TreeNode> children() {
        return Collections.enumeration(Arrays.asList(children));
    }

    public GlobalActionPanel.SELECTION_TPYE getPayloadType() {
        return payloadType;
    }

    public MappingAction getAction() {
        return (MappingAction) payload;
    }

    public Macro getMacro() {
        return (Macro) payload;
    }

    public IPositionValueGetter getInput() {
        return ((MappingAction) payload).input;
    }

    public IPositionValueSetter getOutput() {
        return ((MappingAction) payload).output;
    }

    public IDisplayUnit getPayload() {
        switch (payloadType) {
            case INVALID:
            case MACRO:
            case ACTION:
                return (IDisplayUnit) payload;
            case INPUT:
                return getInput();
            case OUTPUT:
                return getOutput();
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return "MacroTreeNode{" +
                "payload=" + ((IDisplayUnit) payload).getName() +
                ", payloadType=" + payloadType +
                '}';
    }
}
