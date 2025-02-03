import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class MinimalJTreeExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MinimalJTreeExample::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        // Create the root node
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root");

        // Create child nodes
        DefaultMutableTreeNode childNode1 = new DefaultMutableTreeNode("Child 1");
        DefaultMutableTreeNode childNode2 = new DefaultMutableTreeNode("Child 2");

        // Add child nodes to root
        rootNode.add(childNode1);
        rootNode.add(childNode2);

        // Create the JTree with the root node
        JTree tree = new JTree(rootNode);

        // Optional: Wrap the JTree inside a JScrollPane to enable scrolling
        JScrollPane scrollPane = new JScrollPane(tree);

        // Create the frame to display the JTree
        JFrame frame = new JFrame("Minimal JTree Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.add(scrollPane);
        frame.setVisible(true);
    }
}
