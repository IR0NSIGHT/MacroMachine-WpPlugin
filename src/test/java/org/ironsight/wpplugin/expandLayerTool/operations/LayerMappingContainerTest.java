package org.ironsight.wpplugin.expandLayerTool.operations;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LayerMappingContainerTest {

    @Test
    void updateMapping() {
        LayerMappingContainer container = new LayerMappingContainer();
        LayerMapping mapping = new LayerMapping(null, null, new MappingPoint[0],
                ActionType.SET, "hello", "world",-1);

        int uid = container.addMapping(mapping);
        assertEquals(uid, mapping.getUid());

        MappingPoint[] newPoints = new MappingPoint[17];
        Arrays.fill(newPoints, new MappingPoint(10, 20));
        LayerMapping newMapping = mapping.withNewPoints(newPoints);
        assertNotEquals(newMapping, mapping);
        assertEquals(17, newMapping.getMappingPoints().length);
        assertEquals(newMapping.getUid(), mapping.getUid());

        container.updateMapping(newMapping);

        assertSame(newMapping, container.queryMappingById(mapping.getUid()));

    }

    @Test
    void deleteMapping() {
        LayerMappingContainer container = new LayerMappingContainer();
        LayerMapping mapping = new LayerMapping(null, null, new MappingPoint[0],
                ActionType.SET, "hello", "world",-1);

        int uid = container.addMapping(mapping);
        assertEquals(uid, mapping.getUid());

        container.deleteMapping(uid);
        assertNull(container.queryMappingById(uid));
    }

    @Test
    void addMapping() {
        LayerMappingContainer container = new LayerMappingContainer();
        LayerMapping mapping = new LayerMapping(null, null, new MappingPoint[0],
                ActionType.SET, "hello", "world",-1);

        int uid = container.addMapping(mapping);
        assertEquals(uid, mapping.getUid());

        {
            int uid2 = container.addMapping(mapping);
            assertEquals(uid, mapping.getUid());
            assertEquals(uid2, -1);
        }
    }

    @Test
    void saveLoad(){
        LayerMappingContainer container = new LayerMappingContainer();
        LayerMapping mapping = new LayerMapping(null, null, new MappingPoint[0],
                ActionType.SET, "hello", "world",-1);

        int uid = container.addMapping(mapping);
        assertEquals(uid, mapping.getUid());
        container.filePath = System.getProperty("user.dir")  + "/test_saves.txt";
        container.writeToFile();
        container.readFromFile();
    }

    @Test
    void subscribe() {
        final int[] ran = {0};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ran[0]++;;
            }
        };
        assertEquals(0,ran[0]);
        LayerMappingContainer container = new LayerMappingContainer();
        container.subscribe(runnable);

        LayerMapping mapping = new LayerMapping(null, null, new MappingPoint[0],
                ActionType.SET, "hello", "world",-1);

        int uid = container.addMapping(mapping);
        assertEquals(1,ran[0]);
        container.updateMapping(mapping.withNewPoints(new MappingPoint[]{new MappingPoint(10, 20)}));
        assertEquals(2,ran[0]);
        container.deleteMapping(uid);
        assertEquals(3,ran[0]);

        container.unsubscribe(runnable);
        container.addMapping(mapping);
        assertEquals(3,ran[0]);
    }

    @Test
    void unsubscribe() {
    }

    @Test
    void subscribeToMapping() {
        final int[] ran = {0};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ran[0]++;;
            }
        };
        assertEquals(0,ran[0]);
        LayerMappingContainer container = new LayerMappingContainer();

        LayerMapping mapping = new LayerMapping(null, null, new MappingPoint[0],
                ActionType.SET, "hello", "world",-1);

        int uid = container.addMapping(mapping);
        container.subscribeToMapping(uid, runnable);

        assertEquals(0,ran[0]);
        container.updateMapping(mapping.withNewPoints(new MappingPoint[]{new MappingPoint(1,2)}));
        assertEquals(1,ran[0]);
        container.deleteMapping(uid);
        assertEquals(2,ran[0]);

        container.addMapping(new LayerMapping(null, null, new MappingPoint[0],
                ActionType.SET, "hello", "world",-1));
        assertEquals(2,ran[0]);

        container.unsubscribe(runnable);
        LayerMapping newMapping = mapping.withNewPoints(new MappingPoint[0]);
        assertEquals(newMapping.getUid(), uid);
        container.addMapping(newMapping);
        assertEquals(2,ran[0]);

    }

    @Test
    void unsubscribeToMapping() {
    }

    @Test
    void queryMappingById() {
    }

    @Test
    void queryMappingsAll() {
    }
}