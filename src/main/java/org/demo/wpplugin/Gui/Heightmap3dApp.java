package org.demo.wpplugin.Gui;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

/**
 * @author afsal villan
 * @version 1.0
 * <p>
 * http://www.genuinecoder.com
 */
public class Heightmap3dApp extends Application {

    public static float[][] heightMap = new float[][]{
            {62,65,68,80},
            {62,65,68,80},
            {62,65,68,80},
    };

    public static int SIZEFACTOR = 100;
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

    public static void main(String... args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Group world = createEnvironment();


        Scene scene = new Scene(world, SIZEFACTOR, SIZEFACTOR, true);
        scene.setFill(Color.LIGHTBLUE);
        primaryStage.setScene(scene);
        primaryStage.setWidth(16 * SIZEFACTOR);
        primaryStage.setHeight(9 * SIZEFACTOR);

        Camera camera = new PerspectiveCamera();
        camera.setFarClip(2000);
        camera.setNearClip(1);

        int camDist = 10000;
        camera.setTranslateX(-8 * SIZEFACTOR);
        camera.setTranslateY(-2000);
        camera.setTranslateZ(-camDist);

        scene.setCamera(camera);

        handleMouse(scene, scene.getRoot(), camera);

        world.getTransforms().addAll(worldRotY, worldRotX);



        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            double mod = 1f;
            if (event.isShiftDown())
                mod += SHIFT_MULTIPLIER;
            if (event.isControlDown())
                mod += CONTROL_MULTIPLIER;
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
                    moveNodeRelative(camera, mod, LocalDir.FORWARD);
                    break;
                case S:
                    moveNodeRelative(camera, -mod, LocalDir.FORWARD);
                    break;
                case A:// a/d is x axis
                    moveNodeRelative(camera, -mod, LocalDir.RIGHT);
                    break;
                case D:
                    moveNodeRelative(camera, mod, LocalDir.RIGHT);
                    break;
            }
        });

        primaryStage.show();
    }

    enum LocalDir {
        FORWARD,
        UP,
        RIGHT
    }
    private Point3D getNodeDir(Node n, LocalDir dir) {
        Point3D local = null;
        switch (dir) {
            case UP:
                local = new Point3D(0,1,0);
                break;
            case FORWARD:
                local = new Point3D(0,0,1);
                break;

            case RIGHT:
                local = new Point3D(1,0,0);
                break;
            default:
                throw new IllegalArgumentException();
        }
        Point3D forward = n.getLocalToSceneTransform().transform(local);
        forward = forward.subtract(n.getTranslateX(), n.getTranslateY(), n.getTranslateZ());
        return forward;
    }

    private void moveNodeRelative(Node node, double distance, LocalDir dir) {
        Point3D forward = getNodeDir(node, dir);
        // Update the camera's position based on the forward vector
        node.setTranslateX(node.getTranslateX() + forward.getX() * distance);
        node.setTranslateY(node.getTranslateY() + forward.getY() * distance);
        node.setTranslateZ(node.getTranslateZ() + forward.getZ() * distance);
    }


    private Group createEnvironment() {
        Group group = new Group();

        Box ground = new Box();
        ground.setHeight(1);
        ground.setWidth(5000);
        ground.setDepth(5000);
        ground.setTranslateY(0.5);
        PhongMaterial m = new PhongMaterial();
        m.setDiffuseColor(Color.DARKGREEN);
        ground.setMaterial(m);

        Box edge = new Box();
        edge.setHeight(25 * 100);
        edge.setWidth(100);
        edge.setDepth(100);
        edge.setTranslateY(-edge.getHeight() / 2f);

        group.getChildren().addAll(ground, edge);

        m.setDiffuseColor(Color.DARKGREEN);
        ground.setMaterial(m);

        Color dimmedColor = Color.WHITE.deriveColor(0, 1, 0.5, 1);
        AmbientLight ambientLight = new AmbientLight(dimmedColor);
        group.getChildren().add(ambientLight);

        PointLight l = new PointLight();
        l.setColor(dimmedColor);
        l.setTranslateX(10000);
        l.setTranslateY(-10000);
        l.setTranslateZ(10000);
        group.getChildren().add(l);


        group.getChildren().add(createHeightmapMesh());
        return group;
    }

    private void handleMouse(Scene scene, final Node root, final Camera camera) {

        scene.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent me) {
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseOldX = me.getSceneX();
                mouseOldY = me.getSceneY();
            }
        });
        scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
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
                if (me.isPrimaryButtonDown()) {

                    rotateCameraAroundYAxis(camera,mouseDeltaX * modifier * ROTATION_SPEED);
                    rotateCameraAroundXAxis(camera, mouseDeltaY * modifier * ROTATION_SPEED);
                } else if (me.isSecondaryButtonDown()) {
                    Point3D f = getNodeDir(camera, LocalDir.FORWARD);
                    Point3D fPlane = new Point3D(f.getX(), 0, f.getZ());
                    fPlane = fPlane.multiply(mouseDeltaY * 0.1f * modifier * CAMERA_MOVE_SPEED);

                    f = getNodeDir(camera, LocalDir.RIGHT);
                    f = new Point3D(f.getX(), 0, f.getZ());
                    f = f.multiply(mouseDeltaX * 0.1f * modifier * CAMERA_MOVE_SPEED);

                    fPlane = fPlane.add(f);
                    fPlane = fPlane.add(camera.getTranslateX(), camera.getTranslateY(), camera.getTranslateZ());
                  
                    camera.setTranslateX(fPlane.getX() );
                    camera.setTranslateY(fPlane.getY());
                    camera.setTranslateZ(fPlane.getZ());

                } else if (me.isMiddleButtonDown()) {
                    camera.setTranslateY(camera.getTranslateY() + mouseDeltaY * 0.1f * modifier * CAMERA_MOVE_SPEED);
                }

            }
        }); // setOnMouseDragged
    } //handleMouse

    Rotate camYRot = new Rotate();
    Rotate camXRot = new Rotate();

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

    private static class FaceDef {
        float[] verts;
        float[] texCoords;
        int[] faces;
    }

    private float[] quadUp = new float[]{
            0,0,0,//left
            100,0,0,   //right
            100,0,100,  //deep right
            0,0,100 //deep left
    };

    private float[] quadXPos = new float[]{
            0,0,0,//left
            0,0,100,   //right
            0,100,100,  //deep right
            0,100,0 //deep left
    };

    private float[] quadXNeg = new float[]{
           100,0,100,//left
           100,0,0,   //right
           100,100,0,  //deep right
           100,100,100 //deep left
    };

    private float[] quadZNeg = new float[]{
            0,0,100,
            100,0,100,
            100,100,100,
            0,100,100
    };

    private float[] quadZPos = new float[]{
            0,0,0,
            0,100,0,
            100,100,0,
            100,0,0
    };



    enum Dir {
        UP,
        XPOS,
        XNEG,
        ZPOS,
        ZNEG
    }
    private void addFace(Point3D position, TriangleMesh view, Dir dir) {
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
                face.verts = new float[3*4];
                break;
        }
        for (int i = 0; i < face.verts.length; i+=3)
            face.verts[i] += (float) position.getX();
        for (int i = 1; i < face.verts.length; i+=3)
            face.verts[i] += face.verts[i] == 0 ? (float) position.getY() : 0;
        for (int i = 2; i < face.verts.length; i+=3)
            face.verts[i] += (float) position.getZ();



        face.texCoords = new float[]{0,0,
                1,0,
                1,1,
                0,1};
        face.faces = new int[]{
                0,0,
                1,1,
                2,2,

                2,2,
                3,3,
                0,0};

        //vertices are absolute and stay untouched
        //tex coords are absolute and stay untouched
        //face indices must be shifted
        for (int i = 0; i < face.faces.length; i++) {
            face.faces[i] += view.getPoints().size()/3;
        }

        view.getPoints().addAll(face.verts);
        view.getTexCoords().addAll(face.texCoords);
        view.getFaces().addAll(face.faces);

    }

    private MeshView createHeightmapMesh() {
        TriangleMesh mesh = new TriangleMesh();
        for (int z= 0; z < heightMap.length; z++)
            for (int x = 0; x < heightMap[0].length; x++) {
                float y = heightMap[z][x];
                Point3D center = new Point3D(x * 100, -y * 100, z * 100);
                addFace(center, mesh, Dir.UP);
                addFace(center, mesh, Dir.XNEG);
                addFace(center, mesh, Dir.XPOS);
                addFace(center, mesh, Dir.ZNEG);
                addFace(center, mesh, Dir.ZPOS);
            }


        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.RED);

        MeshView meshView = new MeshView(mesh);
        meshView.setDrawMode(javafx.scene.shape.DrawMode.FILL); // Render filled triangles
        meshView.setMaterial(material);

        return meshView;
    }
}