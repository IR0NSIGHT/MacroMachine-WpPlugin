package org.demo.wpplugin.Gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author afsal villan
 * @version 1.0
 * <p>
 * http://www.genuinecoder.com
 */
public class Heightmap3dApp extends Application {
    public static Heightmap3dApp instance;
    public static float[][] heightMap = DefaultHeightMap.loadFloatArrayFromFile("default_heightmap.txt");
    public static float[][] waterMap = DefaultHeightMap.loadFloatArrayFromFile("default_watermap.txt");
    public static Texture[][] blockmap = DefaultHeightMap.loadTextureArrayFromFile("default_blockmap.txt");
    public static Point2D globalOffset = new Point2D(0,0);
    public static Consumer<Point3D> setWaterHeight = point -> {
        System.out.println("callback: change waterlevel to" + point.toString());

    };
    /**
     * set heightmap to height Z at position x/y
     */
    public static Consumer<Point3D> setHeightMap = point -> {
        System.out.println("callback: change height to" + point.toString());
    };

    public static int SIZEFACTOR = 100;
    private static boolean isJavaFXRunning = false;
    private final float[] quadUp = new float[]{0, 0, 0,//left
            100, 0, 0,   //right
            100, 0, 100,  //deep right
            0, 0, 100 //deep left
    };
    private final float[] quadXPos = new float[]{100, 0, 100,//left
            100, 0, 0,   //right
            100, 100, 0,  //deep right
            100, 100, 100 //deep left
    };
    private final float[] quadXNeg = new float[]{0, 0, 0,//left
            0, 0, 100,   //right
            0, 100, 100,  //deep right
            0, 100, 0 //deep left
    };
    private final float[] quadZNeg = new float[]{0, 0, 0, 0, 100, 0, 100, 100, 0, 100, 0, 0};
    private final float[] quadZPos = new float[]{0, 0, 100, 100, 0, 100, 100, 100, 100, 0, 100, 100};
    Rotate lightRotY = new Rotate(0, Rotate.Y_AXIS);
    Rotate worldRotX = new Rotate(0, Rotate.X_AXIS);
    Rotate worldRotY = new Rotate(0, Rotate.Y_AXIS);
    //
// The handleMouse() method is used in the MoleculeSampleApp application to
// handle the different 3D camera views.
// This method is used in the Getting Started with JavaFX 3D Graphics tutorial.
//
    double mousePosX;
    double mousePosY;
    double mouseOldX;
    double mouseOldY;
    double mouseDeltaX;
    double mouseDeltaY;
    double SHIFT_MULTIPLIER = 5f;
    double CONTROL_MULTIPLIER = 10f;
    double ROTATION_SPEED = 0.1f;
    double CAMERA_MOVE_SPEED = 100f;
    Rotate camYRot = new Rotate();
    Rotate camXRot = new Rotate();


    private Group root;
    private boolean moveCameraOnMouseMove = false;
    private Point2D selectedCoord = new Point2D(128, 128);
    private Group selectedPointer;

    public static void main(String... args) {
        if (instance == null) startJavaFX();
        else {
            Platform.runLater(() -> {instance.reloadScene();});

        }
    }

    public static void startJavaFX() {
        if (!isJavaFXRunning) {
            // Start JavaFX runtime in a background thread
            new Thread(() -> Application.launch(Heightmap3dApp.class)).start();
            isJavaFXRunning = true;
        }
    }

    // Static method to open a new stage
    public void reloadScene() {
            try {
                DefaultHeightMap.saveFloatArrayToFile(heightMap, "default_heightmap.txt");
                DefaultHeightMap.saveFloatArrayToFile(waterMap, "default_watermap.txt");
                DefaultHeightMap.saveTextureArrayToFile(blockmap, "default_blockmap.txt");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            root.getChildren().clear(); // Clear existing nodes
            Group world = createEnvironment();

            updateSelectedPointer();
            root.getChildren().add(world);
    }

    private Group createEnvironment() {
        Group group = new Group();
        {
            Sphere center = new Sphere();
            center.setRadius(25 * 100);
            PhongMaterial material = new PhongMaterial(Color.YELLOW);
            center.setMaterial(material);
        //    group.getChildren().add(center);
        }

        float ambientStrenght = 0.3f;
        Color ambientColor = Color.WHITE.deriveColor(0, 1, ambientStrenght, 1);
        AmbientLight ambientLight = new AmbientLight(ambientColor);
        group.getChildren().add(ambientLight);

        Group lightAnchor = new Group();
        Sphere anchorHelper = new Sphere();
        anchorHelper.setRadius(10*100);
        lightAnchor.getChildren().add(anchorHelper);
        PointLight l = new PointLight();
        Color directionalColor = Color.WHITE.deriveColor(0, 1, 1 - ambientStrenght, 1);
        l.setColor(directionalColor);
        lightAnchor.getTransforms().add(lightRotY);
        l.setTranslateX(100000);
        l.setTranslateY(-100000);
        l.setTranslateZ(100000);
        lightAnchor.getChildren().add(l);

        group.getChildren().add(lightAnchor);


        group.getChildren().add(createHeightmapMesh(false));

        group.getChildren().add(createHeightmapMesh(true));

        {
            Group selected = new Group();
            Box selectedOut = new Box();
            selectedOut.setWidth(110);
            selectedOut.setHeight(110);
            selectedOut.setDepth(110);
            PhongMaterial red = new PhongMaterial();
            Color r = new Color(1, 0, 0, 0.4f);
            red.setDiffuseColor(r);
            selectedOut.setMaterial(red);
            selectedPointer = selected;
            selected.getChildren().add(selectedOut);
            group.getChildren().add(selected);
        }
        return group;
    }

    private MeshView createHeightmapMesh(boolean isWaterMap) {
        float[][] heightMap = isWaterMap ? waterMap : Heightmap3dApp.heightMap;

        TriangleMesh mesh = new TriangleMesh();
        for (int z = 0; z < heightMap.length; z++)
            for (int x = 0; x < heightMap[0].length; x++) {
                float y =  Math.round(isWaterMap ? getWaterHeightAt(x,z) : getHeightAt(x,z));
                if (isWaterMap && getHeightAt(x,z) >= y)
                    continue;   //dont draw water if its below/equal of terrain
                Point3D center = new Point3D(x * 100, -y * 100, z * 100);
                Texture blockType;
                if (!isWaterMap) {
                    blockType = Heightmap3dApp.blockmap[z][x];
                } else {
                    blockType = Texture.WATER;
                }

                addFace(center, mesh, Dir.UP, blockType, 1);

                if (blockType == Texture.GRASS) blockType = Texture.DIRT;  //sideways
                for (Dir dir : new Dir[]{Dir.ZPOS, Dir.ZNEG, Dir.XPOS, Dir.XNEG}) {
                    if (isWaterMap) addFace(center, mesh, dir, blockType, (float) center.getY());
                    else {
                        float xNegY = getYLowerOrDefault(center, heightMap, -1, dir);
                        if (xNegY != -1) {
                            for (double yy = center.getY(); yy < xNegY; yy += 100) {
                                Point3D offsetCenter = new Point3D(center.getX(), yy, center.getZ());
                                addFace(offsetCenter, mesh, dir, blockType, (float) yy);
                            }

                        }
                    }
                }
            }

        // Load the PNG image as a texture
        Image textureImage = new Image("file:main_color_texture_worldpainter.png");

        // Create a PhongMaterial and set the texture map
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(textureImage);  // Apply the PNG texture as the diffuse map
        if (isWaterMap) {
            Color waterColor = new Color(1, 1, 1, 0.2);
            material.setDiffuseColor(waterColor);
        }
        //  material.setDiffuseColor(Color.W);
        //  material.setSpecularColor(Color.LIGHTGREEN);
        //  material.setSpecularPower(30);  // Moderate shininess

        MeshView meshView = new MeshView(mesh);
        meshView.setDrawMode(javafx.scene.shape.DrawMode.FILL); // Render filled triangles
        meshView.setMaterial(material);
        meshView.setTranslateX(-heightMap.length / 2 * 100);
        meshView.setTranslateZ(-heightMap.length / 2 * 100);

        return meshView;
    }

    private void addFace(Point3D position, TriangleMesh view, Dir dir, Texture texture, float y) {
        FaceDef face = new FaceDef();
        switch (dir) {
            case UP:
                face.verts = quadUp.clone();
                break;
            case XPOS:
                face.verts = quadXPos.clone();
                break;
            case XNEG:
                face.verts = quadXNeg.clone();
                break;
            case ZNEG:
                face.verts = quadZNeg.clone();
                break;
            case ZPOS:
                face.verts = quadZPos.clone();
                break;
            default:
                face.verts = new float[3 * 4];
                break;
        }
        for (int i = 0; i < face.verts.length; i += 3)
            face.verts[i] += (float) position.getX();
        for (int i = 1; i < face.verts.length; i += 3)
            face.verts[i] += face.verts[i] == 0 ? (float) position.getY() : y;
        for (int i = 2; i < face.verts.length; i += 3)
            face.verts[i] += (float) position.getZ();

        float texPos;

        float texWidth = Texture.values().length;
        switch (texture) {
            case ROCK:
                texPos = 1;
                break;
            case GRASS:
                texPos = 0;
                break;
            case WATER:
                texPos = 2f;
                break;
            case DIRT:
                texPos = 3f;
                break;
            case SNOW:
                texPos = 4;
                break;
            case GRAVEL:
                texPos = 5f;
                break;
            case SAND:
                texPos = 6f;
                break;
            case SELECTED:
                texPos = 7f;
                break;
            default:
                texPos = 0f;
                break;
        }
        float startPosTex = texPos / texWidth + 0.01f;
        float endPosTex = texPos / texWidth + 1 / texWidth - 0.01f;
        face.texCoords = new float[]{startPosTex, startPosTex, endPosTex, startPosTex, endPosTex, endPosTex, startPosTex, endPosTex,};
        face.faces = new int[]{0, 0, 1, 1, 2, 2,

                2, 2, 3, 3, 0, 0};

        //vertices are absolute and stay untouched
        //tex coords are absolute and stay untouched
        //face indices must be shifted
        for (int i = 0; i < face.faces.length; i++) {
            face.faces[i] += view.getPoints().size() / 3;
        }

        view.getPoints().addAll(face.verts);
        view.getTexCoords().addAll(face.texCoords);
        view.getFaces().addAll(face.faces);

    }

    private float getYLowerOrDefault(Point3D pos, float[][] heightMap, float defaultV, Dir dir) {
        int x = (int) pos.getX() / 100;
        int z = (int) pos.getZ() / 100;

        switch (dir) {
            case XNEG:
                x -= 1;
                break;
            case XPOS:
                x += 1;
                break;
            case ZNEG:
                z -= 1;
                break;
            case ZPOS:
                z += 1;

        }
        if (z < 0 || z >= heightMap.length || x < 0 || x >= heightMap[0].length) return 0;
        float height = Math.round(heightMap[z][x]) * 100;
        if (height >= -pos.getY()) return defaultV;
        return -height;
    }

    public static void printFloatArray(float[][] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("float[][] array = {\n");

        for (int i = 0; i < array.length; i++) {
            sb.append("    {");
            for (int j = 0; j < array[i].length; j++) {
                sb.append(array[i][j]);
                if (j < array[i].length - 1) {
                    sb.append("f, ");
                }
            }
            sb.append("}");
            if (i < array.length - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("};");

        // Print the constructed syntax
        System.out.println(sb);
    }

    @Override
    public void init() {
        // Set the flag to true when the application is initialized
        instance = this;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        root = new Group();
        this.reloadScene();
        Scene scene = new Scene(root, SIZEFACTOR, SIZEFACTOR, true);
        scene.setFill(Color.LIGHTBLUE);
        primaryStage.setScene(scene);
        primaryStage.setWidth(16 * SIZEFACTOR);
        primaryStage.setHeight(9 * SIZEFACTOR);

        Camera camera = new PerspectiveCamera();
        camera.setFarClip(2000);
        camera.setNearClip(1);

        //i dont know why does numbers are what they are, i just positioned the camera manually and saved the coords.
        camera.setTranslateX(870 * 100);
        camera.setTranslateY(-307 * 100);
        camera.setTranslateZ(450 * 100);

        rotateCameraAroundYAxis(camera,180);
        rotateCameraAroundXAxis(camera,-30);
        scene.setCamera(camera);

        handleMouse(scene, scene.getRoot(), camera);

        root.getTransforms().addAll(worldRotY, worldRotX);
        root.setScaleX(-1);

        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            double mod = 1f;
            if (event.isShiftDown()) mod += SHIFT_MULTIPLIER;
            if (event.isControlDown()) mod += CONTROL_MULTIPLIER;
            mod *= CAMERA_MOVE_SPEED;
            switch (event.getCode()) {
                case LEFT:
                    worldRotY.setAngle(worldRotY.getAngle() + 10);
                    break;
                case RIGHT:
                    worldRotY.setAngle(worldRotY.getAngle() - 10);
                    break;
                case UP:
                    worldRotX.setAngle(worldRotX.getAngle() + 10);
                    break;
                case DOWN:
                    worldRotX.setAngle(worldRotX.getAngle() - 10);
                case W: //w/s is for z
                    selectedCoord = selectedCoord.add(0, -1);
                    break;
                case S:
                    selectedCoord = selectedCoord.add(0, 1);
                    break;
                case A:// a/d is x axis
                    selectedCoord = selectedCoord.add(-1, 0);
                    break;
                case D:
                    selectedCoord = selectedCoord.add(1, 0);
                    break;
                case SPACE:
                    moveCameraOnMouseMove = !moveCameraOnMouseMove;
                case R: //raise heightmap
                {
                    changeSelectedTerrain(1,false);
                    reloadScene();
                    break;
                }
                case F: //lower heightmap
                {
                    changeSelectedTerrain(-1,false);
                    reloadScene();
                    break;
                }
                case T: //raise water
                {
                    changeSelectedTerrain(1,true);
                    reloadScene();
                    break;
                }
                case G: //lower water
                {
                    changeSelectedTerrain(-1,true);
                    reloadScene();
                    break;
                }

            }
            updateSelectedPointer();
        });

        primaryStage.show();
    }

    private void handleMouse(Scene scene, final Node root, final Camera camera) {
        scene.setOnMouseDragged(me -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseDeltaX = (mousePosX - mouseOldX);
            mouseDeltaY = (mousePosY - mouseOldY);

            double modifier = 1.0;

            if (me.isControlDown()) {
                modifier = CONTROL_MULTIPLIER;
            }
            if (me.isShiftDown()) {
                modifier = SHIFT_MULTIPLIER;
            }
            if (moveCameraOnMouseMove) return;
            if (me.isPrimaryButtonDown()) {
                if (me.isShiftDown()) {
                    //rotate world
                    rotateRootAroundYAxis(mouseDeltaX * modifier * ROTATION_SPEED);
                } else if (me.isControlDown())
                    //rotate light
                    lightRotY.setAngle(lightRotY.getAngle() + mouseDeltaX * modifier * ROTATION_SPEED);
                else {
                    //pan camera

                }
            } else if (me.isSecondaryButtonDown()) {
                Point3D f = getNodeDir(camera, LocalDir.FORWARD);
                Point3D fPlane = new Point3D(f.getX(), 0, f.getZ());
                fPlane = fPlane.multiply(mouseDeltaY * 0.1f * modifier * CAMERA_MOVE_SPEED);

                f = getNodeDir(camera, LocalDir.RIGHT);
                f = new Point3D(f.getX(), 0, f.getZ());
                f = f.multiply(mouseDeltaX * -0.1f * modifier * CAMERA_MOVE_SPEED);

                fPlane = fPlane.add(f);
                fPlane = fPlane.add(camera.getTranslateX(), camera.getTranslateY(), camera.getTranslateZ());

                camera.setTranslateX(fPlane.getX());
                camera.setTranslateY(fPlane.getY());
                camera.setTranslateZ(fPlane.getZ());
                System.out.println("camera pos =" + camera.getTranslateX() + "," + camera.getTranslateY()+ "," + camera.getTranslateZ());
            } else if (me.isMiddleButtonDown()) {
                camera.setTranslateY(camera.getTranslateY() + mouseDeltaY * -0.1f * modifier * CAMERA_MOVE_SPEED);
            }
        });
        scene.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                mouseOldX = mousePosX;
                mouseOldY = mousePosY;
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseDeltaX = (mousePosX - mouseOldX);
                mouseDeltaY = (mousePosY - mouseOldY);

                double modifier = 1.0;

                if (me.isControlDown()) {
                    modifier = CONTROL_MULTIPLIER;
                }
                if (me.isShiftDown()) {
                    modifier = SHIFT_MULTIPLIER;
                }

                if (moveCameraOnMouseMove && !me.isPrimaryButtonDown() && !me.isSecondaryButtonDown() && !me.isMiddleButtonDown()) {
                    rotateCameraAroundYAxis(camera, mouseDeltaX * modifier * ROTATION_SPEED);
                    rotateCameraAroundXAxis(camera, -mouseDeltaY * modifier * ROTATION_SPEED);

                }

            }
        }); // setOnMouseDragged
    } //handleMouse

    private void updateSelectedPointer() {
        selectedCoord = new Point2D(
                Math.max(0,Math.min(255,selectedCoord.getX())),
                Math.max(0,Math.min(255,selectedCoord.getY())));

        selectedPointer.setTranslateX((-heightMap.length / 2 + 0.5f + selectedCoord.getX()) * 100);
        selectedPointer.setTranslateZ((-heightMap.length / 2 + 0.5f + selectedCoord.getY()) * 100);
        float height = Math.round(getHeightAt(selectedCoord.getX(),selectedCoord.getY()));
        selectedPointer.setTranslateY((-height + 0.5f) * 100);
        System.out.println("selected pointer at " + globalOffset.add(selectedCoord).toString());
    }

    private void rotateRootAroundYAxis(double angleInDegrees) {
        //double total = root.getRotate() + angleInDegrees
        //root.setRotate(total);
        worldRotY.setAxis(Rotate.Y_AXIS);
        double total = (worldRotY.getAngle() + angleInDegrees) % 360;
        worldRotY.setAngle(total);
        // No need to clear and re-add the transform every time
        if (!root.getTransforms().contains(worldRotY)) {
            root.getTransforms().add(worldRotY);  // Add the transform if not already added
        }
    }

    private Point3D getNodeDir(Node n, LocalDir dir) {
        Point3D local = null;
        switch (dir) {
            case UP:
                local = new Point3D(0, 1, 0);
                break;
            case FORWARD:
                local = new Point3D(0, 0, 1);
                break;

            case RIGHT:
                local = new Point3D(1, 0, 0);
                break;
            default:
                throw new IllegalArgumentException();
        }
        Point3D forward = n.getLocalToSceneTransform().transform(local);
        forward = forward.subtract(n.getTranslateX(), n.getTranslateY(), n.getTranslateZ());
        return forward;
    }

    private void rotateCameraAroundYAxis(Camera camera, double angleInDegrees) {
        // Create a Rotate transform
        camYRot.setAxis(Rotate.Y_AXIS);
        camYRot.setAngle(camYRot.getAngle() + angleInDegrees);
        // Apply the rotation to the camera
        camera.getTransforms().clear();
        camera.getTransforms().addAll(camYRot, camXRot);
    }

    private void rotateCameraAroundXAxis(Camera camera, double angleInDegrees) {
        // Create a Rotate transform
        Rotate rot = camXRot;
        rot.setAxis(Rotate.X_AXIS);
        rot.setAngle(rot.getAngle() + angleInDegrees);
        // Apply the rotation to the camera
        camera.getTransforms().clear();
        camera.getTransforms().addAll(camYRot, camXRot);
    }

    private float getWaterHeightAt(double x, double z) {
        return waterMap[(int)z][(int)x];
    }
    private void setWaterHeightAt(double x, double z, double height) {
        waterMap[(int)z][(int)x] = (float)height;
        setWaterHeight.accept(new Point3D(globalOffset.getX() + x,globalOffset.getY() + z,height));
    }

    private float getHeightAt(double x, double z) {
        return heightMap[(int)z][(int)x];
    }
    private void setHeightAt(double x, double z, double height) {
        heightMap[(int)z][(int)x] = (float)height;
        setHeightMap.accept(new Point3D(globalOffset.getX() + x,globalOffset.getY() + z,height));
    }

    private Point2D fromWorldpainterCoord(int x, int y) {
        return new Point2D(x,y);
    }

    private void changeSelectedTerrain(int change, boolean isWater) {
        Point2D pos = selectedCoord;
        if (!isWater) {
            float height = getHeightAt(pos.getX(), pos.getY());
            height+=change;
            setHeightAt(pos.getX(), pos.getY(),height);
        } else {
            float height = getWaterHeightAt(pos.getX(), pos.getY());
            float terrain = getHeightAt(pos.getX(), pos.getY());
            height+=change;
            if (change > 0 && height <= terrain) {
                height = terrain + change;  //place directly above terrain, regardless of old waterheight
            } else if (change < 0 && height <= terrain){
                height = 0; //place at zero regardless of old waterheight
            }
            setWaterHeightAt(pos.getX(), pos.getY(),height);
        }

    }

    public enum Texture {
        GRASS, ROCK, WATER, DIRT, SNOW, GRAVEL, SAND, SELECTED
    }


    enum LocalDir {
        FORWARD, UP, RIGHT
    }

    enum Dir {
        UP, XPOS, XNEG, ZPOS, ZNEG
    }

    private static class FaceDef {
        float[] verts;
        float[] texCoords;
        int[] faces;
    }
}