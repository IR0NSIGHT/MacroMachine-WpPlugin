package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import static java.lang.String.format;
import static org.pepsoft.util.swing.MessageUtils.*;
import static org.pepsoft.worldpainter.ExceptionHandler.doWithoutExceptionReporting;
import static org.pepsoft.worldpainter.Platform.Capability.NAME_BASED;
import static org.pepsoft.worldpainter.objects.WPObject.ATTRIBUTE_FILE;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Configuration;
import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.layers.AbstractLayerEditor;
import org.pepsoft.worldpainter.layers.EditLayerDialog;
import org.pepsoft.worldpainter.layers.bo2.EditObjectAttributes;
import org.pepsoft.worldpainter.layers.bo2.WPObjectListCellRenderer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;
import org.pepsoft.worldpainter.objects.WPObject;
import org.pepsoft.worldpainter.plugins.CustomObjectManager;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

public class CityLayerEditor extends AbstractLayerEditor<CityLayer> implements ListSelectionListener, DocumentListener
{
    private static final Preferences prefs = Preferences.userNodeForPackage(CityLayerEditor.class);
    private static final String LAST_DIR_KEY = "lastDirectory";

    // LayerEditor
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CityLayerEditor.class);
    private static final long serialVersionUID = 1L;
    private final DefaultListModel<WPObject> listModel;
    private final Set<Integer> replacedObjectIndices = new HashSet<>();
    private javax.swing.JButton buttonAddFile;
    private javax.swing.JButton buttonEdit;
    private javax.swing.JButton buttonReloadAll;
    private javax.swing.JButton buttonRemoveFile;

    private javax.swing.JTextField fieldName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelObejcts;
    private javax.swing.JLabel jLabelName;
    private javax.swing.JLabel jLabelPaint;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JList<WPObject> listObjects;
    private org.pepsoft.worldpainter.layers.renderers.PaintPicker paintPicker1;
    private ColourScheme colourScheme;

    public CityLayerEditor() {
        initComponents();

        listModel = new DefaultListModel<>();
        listObjects.setModel(listModel);
        listObjects.setCellRenderer(new WPObjectListCellRenderer());

        listObjects.getSelectionModel().addListSelectionListener(this);
        fieldName.getDocument().addDocumentListener(this);
    }

    public static void main(String[] args) {
        // set up singletons
        WPPluginManager.initialise(UUID.randomUUID(), null);
        Configuration.setInstance(new Configuration());

        Window parent = new JWindow();
        EditLayerDialog dlg = new EditLayerDialog(parent, DefaultPlugin.JAVA_ANVIL_1_19, new CityLayer("test", "test"));
        dlg.setVisible(() -> {
        });
    }

    @Override
    public CityLayer createLayer() {
        CityLayer cityLayer = new CityLayer("My City Layer",
                "Custom (e.g. bo2, bo3, nbt, schem and/or schematic) objects");
        return cityLayer;
    }

    @Override
    public void setLayer(CityLayer layer) {
        super.setLayer(layer);
        reset();
    }

    @Override
    public void commit() {
        if (!isCommitAvailable()) {
            throw new IllegalStateException("Settings invalid or incomplete");
        }
        saveSettings(layer);

        try {
            Dimension dim = context.getDimension();
            layer.repaintAll(dim);
        } catch (Exception ignored) {

        }
        CityEditToolOperation.updateInstance();
    }

    @Override
    public void reset() {
        fieldName.setText(layer.getName());
        paintPicker1.setPaint(layer.getPaint());
        paintPicker1.setOpacity(layer.getOpacity());
        replacedObjectIndices.clear();
        listModel.clear();
        for (WPObject object : layer.getObjectList()) {
            listModel.addElement(object.clone());
        }

        settingsChanged();
    }

    @Override
    public ExporterSettings getSettings() {
        if (!isCommitAvailable()) {
            throw new IllegalStateException("Settings invalid or incomplete");
        }

        final CityLayer previewLayer = saveSettings(null);
        return new ExporterSettings() {
            @Override
            public boolean isApplyEverywhere() {
                return false;
            }

            @Override
            public CityLayer getLayer() {
                return previewLayer;
            }

            @Override
            public ExporterSettings clone() {
                throw new UnsupportedOperationException("Not supported");
            }
        };
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean isCommitAvailable() {
        boolean filesSelected = listModel.getSize() > 0;
        boolean nameSpecified = fieldName.getText().trim().length() > 0;
        return filesSelected && nameSpecified;
    }

    @Override
    public void setContext(LayerEditorContext context) {
        super.setContext(context);
        colourScheme = context.getColourScheme();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        settingsChanged();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        settingsChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        settingsChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        settingsChanged();
    }

    private CityLayer saveSettings(CityLayer layer) {
        if (layer == null) {
            layer = createLayer();
        }

        var newObjects = IntStream.range(0, listModel.getSize())
                .mapToObj(listModel::getElementAt)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        layer.setObjectList(newObjects, replacedObjectIndices);
        layer.setName(fieldName.getText());
        layer.setPaint(paintPicker1.getPaint());
        layer.setOpacity(paintPicker1.getOpacity());

        return layer;
    }

    private void settingsChanged() {
        setControlStates();
        context.settingsChanged();
    }

    private void setControlStates() {
        boolean filesSelected = listModel.getSize() > 0;
        boolean objectsSelected = listObjects.getSelectedIndex() != -1;
        buttonRemoveFile.setEnabled(objectsSelected);
        buttonReloadAll.setEnabled(filesSelected);
        buttonEdit.setEnabled(objectsSelected);
    }

    private Platform getPlatform() {
        try {
            return context.getDimension().getWorld().getPlatform();
        } catch (NullPointerException ex) {
            return DefaultPlugin.JAVA_ANVIL_1_20_5;
        }
    }

    private void addFilesOrDirectory() {
        JFileChooser fileChooser = createFileChooser();
        fileChooser.setDialogTitle("Select File(s) or Directory");
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        CustomObjectManager.UniversalFileFilter fileFilter = CustomObjectManager.getInstance().getFileFilter();
        fileChooser.setFileFilter(fileFilter);
        int result = doWithoutExceptionReporting(() -> fileChooser.showOpenDialog(this));
        rememberChooserDirectory(fileChooser);
        if (result == JFileChooser.APPROVE_OPTION) {

            File[] selectedFiles = fileChooser.getSelectedFiles();
            if (selectedFiles.length > 0) {
                Configuration config = Configuration.getInstance();
                Platform platform = getPlatform();
                boolean checkForNameOnlyMaterials = !platform.capabilities.contains(NAME_BASED);
                Set<String> nameOnlyMaterialsNames = checkForNameOnlyMaterials ? new HashSet<>() : null;
                config.setCustomObjectsDirectory(selectedFiles[0].getParentFile());
                for (File selectedFile : selectedFiles) {
                    if (selectedFile.isDirectory()) {
                        if (fieldName.getText().isEmpty()) {
                            String name = selectedFiles[0].getName();
                            if (name.length() > 12) {
                                name = "..." + name.substring(name.length() - 10);
                            }
                            fieldName.setText(name);
                        }
                        File[] files = selectedFile.listFiles((FilenameFilter) fileFilter);
                        if (files == null) {
                            beepAndShowError(this, selectedFile.getName() + " is not a directory or it cannot be read.",
                                    "Not A Valid Directory");
                        } else if (files.length == 0) {
                            beepAndShowError(this,
                                    "Directory " + selectedFile.getName()
                                            + " does not contain any supported custom object files.",
                                    "No Custom Object Files");
                        } else {
                            for (File file : files) {
                                addFile(checkForNameOnlyMaterials, nameOnlyMaterialsNames, file);
                            }
                        }
                    } else {
                        if (fieldName.getText().isEmpty()) {
                            String name = selectedFile.getName();
                            int p = name.lastIndexOf('.');
                            if (p != -1) {
                                name = name.substring(0, p);
                            }
                            if (name.length() > 12) {
                                name = "..." + name.substring(name.length() - 10);
                            }
                            fieldName.setText(name);
                        }
                        addFile(checkForNameOnlyMaterials, nameOnlyMaterialsNames, selectedFile);
                    }
                }
                settingsChanged();
                if (checkForNameOnlyMaterials && (!nameOnlyMaterialsNames.isEmpty())) {
                    String message;
                    if (nameOnlyMaterialsNames.size() > 4) {
                        message = format("One or more added objects contain block types that are\n"
                                + "incompatible with the current map format (%s):\n" + "%s and %d more\n"
                                + "You will not be able to export this world in this format if you use this layer.",
                                platform.displayName,
                                String.join(", ", new ArrayList<>(nameOnlyMaterialsNames).subList(0, 3)),
                                nameOnlyMaterialsNames.size() - 3);
                    } else {
                        message = format("One or more added objects contain block types that are\n"
                                + "incompatible with the current map format (%s):\n" + "%s\n"
                                + "You will not be able to export this world in this format if you use this layer.",
                                platform.displayName, String.join(", ", nameOnlyMaterialsNames));
                    }
                    beepAndShowWarning(this, message, "Map Format Not Compatible");
                }
            }
        }
    }

    private JFileChooser createFileChooser() {
        JFileChooser fileChooser = new JFileChooser();

        String lastDir = prefs.get(LAST_DIR_KEY, null);
        File rememberedDir = lastDir == null ? null : new File(lastDir);
        if (rememberedDir != null && rememberedDir.isDirectory()) {
            fileChooser.setCurrentDirectory(rememberedDir);
        } else {
            Configuration config = Configuration.getInstance();
            File customObjectsDirectory = config.getCustomObjectsDirectory();
            if (customObjectsDirectory != null && customObjectsDirectory.isDirectory())
                fileChooser.setCurrentDirectory(customObjectsDirectory);
        }

        return fileChooser;
    }

    private void rememberChooserDirectory(JFileChooser fileChooser) {
        File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null && currentDirectory.isDirectory())
            prefs.put(LAST_DIR_KEY, currentDirectory.getAbsolutePath());
    }

    private FileDialog createFileDialog(String title) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        FileDialog fileDialog;
        if (owner instanceof Frame frame) {
            fileDialog = new FileDialog(frame, title, FileDialog.LOAD);
        } else if (owner instanceof Dialog dialog) {
            fileDialog = new FileDialog(dialog, title, FileDialog.LOAD);
        } else {
            fileDialog = new FileDialog((Frame) null, title, FileDialog.LOAD);
        }
        String lastDir = prefs.get(LAST_DIR_KEY, null);
        File rememberedDir = lastDir == null ? null : new File(lastDir);
        if (rememberedDir != null && rememberedDir.isDirectory()) {
            fileDialog.setDirectory(rememberedDir.getAbsolutePath());
        } else {
            Configuration config = Configuration.getInstance();
            File customObjectsDirectory = config.getCustomObjectsDirectory();
            if (customObjectsDirectory != null && customObjectsDirectory.isDirectory())
                fileDialog.setDirectory(customObjectsDirectory.getAbsolutePath());
        }
        var fileFilter = CustomObjectManager.getInstance().getFileFilter();
        fileDialog.setFilenameFilter((directory, name) -> fileFilter.accept(directory, name));
        return fileDialog;
    }

    private File showFileDialog(FileDialog fileDialog) {
        fileDialog.setVisible(true);
        if (fileDialog.getDirectory() != null) {
            prefs.put(LAST_DIR_KEY, fileDialog.getDirectory());
        }
        if (fileDialog.getFile() == null)
            return null;
        return new File(fileDialog.getDirectory(), fileDialog.getFile());
    }

    private void replaceSelectedObjects(int[] rows) {
        if (rows.length == 1) {
            replaceObjectFile(rows[0]);
        } else if (rows.length > 1) {
            replaceObjectsFromFolder(rows);
        }
    }

    private void replaceObjectFile(int row) {
        File selectedFile = showFileDialog(createFileDialog("Replace with other schematic file"));
        if (selectedFile == null)
            return;

        try {
            WPObject replacement = CustomObjectManager.getInstance().loadObject(selectedFile);
            CityLayer.copyObjectSettings(listModel.getElementAt(row), replacement);
            listModel.setElementAt(replacement, row);
            replacedObjectIndices.add(row);
            listObjects.setSelectedIndex(row);
            settingsChanged();
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException while trying to load custom object " + selectedFile, e);
            JOptionPane.showMessageDialog(this,
                    e.getMessage() + " while loading " + selectedFile.getName() + "; it was not added",
                    "Illegal Argument", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            logger.error("I/O error while trying to load custom object " + selectedFile, e);
            JOptionPane.showMessageDialog(this,
                    "I/O error while loading " + selectedFile.getName() + "; it was not added", "I/O Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addFile(boolean checkForNameOnlyMaterials, Set<String> nameOnlyMaterialsNames, File file) {
        try {
            WPObject object = CustomObjectManager.getInstance().loadObject(file);
            if (checkForNameOnlyMaterials) {
                Set<String> materialNamesEncountered = new HashSet<>();
                object.visitBlocks((o, x, y, z, material) -> {
                    if (!materialNamesEncountered.contains(material.name)) {
                        materialNamesEncountered.add(material.name);
                        if (material.blockType == -1) {
                            nameOnlyMaterialsNames.add(material.name);
                        }
                    }
                    return true;
                });
            }
            listModel.addElement(object);
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException while trying to load custom object " + file, e);
            JOptionPane.showMessageDialog(this,
                    e.getMessage() + " while loading " + file.getName() + "; it was not added", "Illegal Argument",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            logger.error("I/O error while trying to load custom object " + file, e);
            JOptionPane.showMessageDialog(this, "I/O error while loading " + file.getName() + "; it was not added",
                    "I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void replaceObjectsFromFolder(int[] rows) {
        File selectedFile = showFileDialog(createFileDialog("Select a file in the folder to scan"));
        if (selectedFile == null)
            return;

        File folder = selectedFile.getParentFile();
        CustomObjectManager.UniversalFileFilter fileFilter = CustomObjectManager.getInstance().getFileFilter();
        File[] files = folder == null ? null : folder.listFiles((FilenameFilter) fileFilter);
        if (files == null) {
            JOptionPane.showMessageDialog(this, "The selected folder could not be read.", "Folder Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, File> filesByName = new HashMap<>();
        for (File file : files) {
            if (file.isFile())
                filesByName.put(file.getName(), file);
        }

        StringBuilder missing = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        int replaced = 0;
        for (int row : rows) {
            WPObject oldObject = listModel.getElementAt(row);
            File oldFile = oldObject.getAttribute(ATTRIBUTE_FILE);
            if (oldFile == null) {
                missing.append(oldObject.getName()).append(" (no source filename)\n");
                continue;
            }

            File replacementFile = filesByName.get(oldFile.getName());
            if (replacementFile == null) {
                missing.append(oldFile.getName()).append('\n');
                continue;
            }

            try {
                WPObject replacement = CustomObjectManager.getInstance().loadObject(replacementFile);
                CityLayer.copyObjectSettings(oldObject, replacement);
                listModel.setElementAt(replacement, row);
                replacedObjectIndices.add(row);
                replaced++;
            } catch (IllegalArgumentException e) {
                logger.error("IllegalArgumentException while trying to load custom object " + replacementFile, e);
                errors.append(replacementFile.getName()).append(" (illegal argument)\n");
            } catch (IOException e) {
                logger.error("I/O error while trying to load custom object " + replacementFile, e);
                errors.append(replacementFile.getName()).append(" (I/O error)\n");
            }
        }

        if (replaced > 0)
            settingsChanged();
        if (missing.length() > 0 || errors.length() > 0) {
            StringBuilder message = new StringBuilder();
            message.append(replaced).append(" of ").append(rows.length).append(" schematics replaced.\n");
            if (missing.length() > 0)
                message.append("\nNo matching file found for:\n").append(missing);
            if (errors.length() > 0)
                message.append("\nFiles that could not be loaded:\n").append(errors);
            JOptionPane.showMessageDialog(this, message, "Batch Replacement Results",
                    replaced == 0 ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE);
        } else {
            showInfo(this, replaced + " schematics successfully replaced", "Success");
        }
    }

    private void removeFiles() {
        JOptionPane.showMessageDialog(this, // parent component (can be null)
                "Removing objects or changing the order of objects will mess up your already painted schematics!", // message
                "Warning", // title of popup
                JOptionPane.WARNING_MESSAGE // icon type
        );

        int[] selectedIndices = listObjects.getSelectedIndices();
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            listModel.removeElementAt(selectedIndices[i]);
        }
        Set<Integer> selectedIndexSet = new HashSet<>();
        for (int index : selectedIndices)
            selectedIndexSet.add(index);
        Set<Integer> adjustedReplacements = new HashSet<>();
        for (int index : replacedObjectIndices) {
            if (selectedIndexSet.contains(index))
                continue;
            int newIndex = index;
            for (int selectedIndex : selectedIndices) {
                if (selectedIndex < index)
                    newIndex--;
            }
            adjustedReplacements.add(newIndex);
        }
        replacedObjectIndices.clear();
        replacedObjectIndices.addAll(adjustedReplacements);
        settingsChanged();
    }

    private void reloadObjects() {
        StringBuilder noFiles = new StringBuilder();
        StringBuilder notFound = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        int[] indices;
        if (listObjects.getSelectedIndex() != -1) {
            indices = listObjects.getSelectedIndices();
        } else {
            indices = new int[listModel.getSize()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }
        }
        CustomObjectManager customObjectManager = CustomObjectManager.getInstance();
        for (int index : indices) {
            WPObject object = listModel.getElementAt(index);
            File file = object.getAttribute(ATTRIBUTE_FILE);
            if (file != null) {
                if (file.isFile() && file.canRead()) {
                    try {
                        Map<String, Serializable> existingAttributes = object.getAttributes();
                        object = customObjectManager.loadObject(file);
                        if (existingAttributes != null) {
                            Map<String, Serializable> attributes = object.getAttributes();
                            if (attributes == null) {
                                attributes = new HashMap<>();
                            }
                            attributes.putAll(existingAttributes);
                            object.setAttributes(attributes);
                        }
                        listModel.setElementAt(object, index);
                    } catch (IOException e) {
                        logger.error("I/O error while reloading " + file, e);
                        errors.append(file.getPath()).append('\n');
                    }
                } else {
                    notFound.append(file.getPath()).append('\n');
                }
            } else {
                noFiles.append(object.getName()).append('\n');
            }
        }
        if ((noFiles.length() > 0) || (notFound.length() > 0) || (errors.length() > 0)) {
            StringBuilder message = new StringBuilder();
            message.append("Not all files could be reloaded!\n");
            if (noFiles.length() > 0) {
                message.append("\nThe following objects came from an old layer and have no filename stored:\n");
                message.append(noFiles);
            }
            if (notFound.length() > 0) {
                message.append("\nThe following files were missing or not accessible:\n");
                message.append(notFound);
            }
            if (errors.length() > 0) {
                message.append("\nThe following files experienced I/O errors while loading:\n");
                message.append(errors);
            }
            JOptionPane.showMessageDialog(this, message, "Not All Files Reloaded", JOptionPane.ERROR_MESSAGE);
        } else {
            showInfo(this, indices.length + " objects successfully reloaded", "Success");
        }
    }

    private void editObjects() {
        JOptionPane.showMessageDialog(this, // parent component (can be null)
                "Editing schematics will also apply to your already painted schematics!", // message
                "Warning", // title of popup
                JOptionPane.WARNING_MESSAGE // icon type
        );
        List<WPObject> selectedObjects = new ArrayList<>(listObjects.getSelectedIndices().length);
        int[] selectedIndices = listObjects.getSelectedIndices();
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            selectedObjects.add(listModel.getElementAt(selectedIndices[i]));
        }
        EditObjectAttributes dialog = new EditObjectAttributes(SwingUtilities.getWindowAncestor(this), selectedObjects,
                colourScheme);
        dialog.setVisible(true);
        if (!dialog.isCancelled()) {
            settingsChanged();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonReloadAll = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        buttonEdit = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        listObjects = new javax.swing.JList<>();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabelName = new javax.swing.JLabel();
        fieldName = new javax.swing.JTextField();
        jLabelPaint = new javax.swing.JLabel();
        paintPicker1 = new org.pepsoft.worldpainter.layers.renderers.PaintPicker();
        jLabelObejcts = new javax.swing.JLabel();
        buttonAddFile = new javax.swing.JButton();
        buttonRemoveFile = new javax.swing.JButton();

        buttonReloadAll.setIcon(new javax.swing.ImageIcon(
                getClass().getResource("/org/pepsoft/worldpainter/icons/arrow_rotate_clockwise.png"))); // NOI18N
        buttonReloadAll.setToolTipText("Reload all or selected objects from disk");
        buttonReloadAll.setEnabled(false);
        buttonReloadAll.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonReloadAll.addActionListener(this::buttonReloadAllActionPerformed);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        buttonEdit.setIcon(
                new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/brick_edit.png"))); // NOI18N
        buttonEdit.setToolTipText("Edit selected object(s) options");
        buttonEdit.setEnabled(false);
        buttonEdit.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonEdit.addActionListener(this::buttonEditActionPerformed);

        listObjects.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                showListPopup(evt);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                showListPopup(evt);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt))
                    listObjectsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(listObjects);

        jLabel1.setText("Define your custom object layer on this screen. Make sure all objects have a proper offset.");

        jLabelName.setText("Name:");

        fieldName.setColumns(15);

        jLabelPaint.setText("Paint:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabelName)
                                .addComponent(jLabelPaint))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(paintPicker1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap()));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabelName)
                                .addComponent(fieldName, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabelPaint)
                                .addComponent(paintPicker1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))));

        jLabelObejcts.setText("Object(s):");

        buttonAddFile.setIcon(
                new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/brick_add.png"))); // NOI18N
        buttonAddFile.setToolTipText("Add one or more objects");
        buttonAddFile.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonAddFile.addActionListener(this::buttonAddFileActionPerformed);

        buttonRemoveFile.setIcon(
                new javax.swing.ImageIcon(getClass().getResource("/org/pepsoft/worldpainter/icons/brick_delete.png"))); // NOI18N
        buttonRemoveFile.setToolTipText("Remove selected object(s)");
        buttonRemoveFile.setEnabled(false);
        buttonRemoveFile.setMargin(new java.awt.Insets(2, 2, 2, 2));
        buttonRemoveFile.addActionListener(this::buttonRemoveFileActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(buttonAddFile, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(buttonRemoveFile, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(buttonEdit, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(buttonReloadAll, javax.swing.GroupLayout.Alignment.TRAILING)))
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabelObejcts)
                                .addComponent(jLabel1)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 2,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE))));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGap(18, 18, 18)
                        .addComponent(jLabelObejcts)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(buttonAddFile)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(buttonRemoveFile)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(buttonEdit)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(buttonReloadAll)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jSeparator2)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))));
    } // </editor-fold>//GEN-END:initComponents

    private void buttonReloadAllActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_buttonReloadAllActionPerformed
        reloadObjects();
    } // GEN-LAST:event_buttonReloadAllActionPerformed

    private void buttonEditActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_buttonEditActionPerformed
        editObjects();
    } // GEN-LAST:event_buttonEditActionPerformed

    private void listObjectsMouseClicked(java.awt.event.MouseEvent evt) { // GEN-FIRST:event_listObjectsMouseClicked
        if (evt.getClickCount() == 2) {
            int row = listObjects.getSelectedIndex();
            if (row != -1) {
                WPObject object = listModel.getElementAt(row);
                EditObjectAttributes dialog = new EditObjectAttributes(SwingUtilities.getWindowAncestor(this), object,
                        colourScheme);
                dialog.setVisible(true);
            }
        }
    } // GEN-LAST:event_listObjectsMouseClicked

    private void showListPopup(java.awt.event.MouseEvent evt) {
        if (!evt.isPopupTrigger())
            return;

        int row = listObjects.locationToIndex(evt.getPoint());
        if (row < 0 || !listObjects.getCellBounds(row, row).contains(evt.getPoint()))
            return;

        if (!listObjects.isSelectedIndex(row))
            listObjects.setSelectedIndex(row);
        int[] selectedRows = listObjects.getSelectedIndices();
        JPopupMenu popupMenu = new JPopupMenu();
        String label = selectedRows.length == 1
                ? "Replace with other schematic file"
                : "Replace selected schematics from folder";
        JMenuItem replaceItem = new JMenuItem(label);
        replaceItem.addActionListener(event -> replaceSelectedObjects(selectedRows));
        popupMenu.add(replaceItem);
        popupMenu.show(listObjects, evt.getX(), evt.getY());
    }

    private void buttonAddFileActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_buttonAddFileActionPerformed
        addFilesOrDirectory();
    } // GEN-LAST:event_buttonAddFileActionPerformed

    private void buttonRemoveFileActionPerformed(java.awt.event.ActionEvent evt) { // GEN-FIRST:event_buttonRemoveFileActionPerformed
        removeFiles();
    } // GEN-LAST:event_buttonRemoveFileActionPerformed
}
