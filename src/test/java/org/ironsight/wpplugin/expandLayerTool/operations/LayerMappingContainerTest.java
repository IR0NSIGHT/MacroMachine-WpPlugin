package org.ironsight.wpplugin.expandLayerTool.operations;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LayerMappingContainerTest {

    @Test
    void updateMapping() {
        LayerMappingContainer container = new LayerMappingContainer();
        LayerMapping mapping = new LayerMapping(null, null, new LayerMapping.MappingPoint[0],
                LayerMapping.ActionType.SET, "hello", "world");

        int uid = container.addMapping(mapping);
        assertEquals(uid, mapping.getUid());

        LayerMapping.MappingPoint[] newPoints = new LayerMapping.MappingPoint[17];
        Arrays.fill(newPoints, new LayerMapping.MappingPoint(10, 20));
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
        LayerMapping mapping = new LayerMapping(null, null, new LayerMapping.MappingPoint[0],
                LayerMapping.ActionType.SET, "hello", "world");

        int uid = container.addMapping(mapping);
        assertEquals(uid, mapping.getUid());

        container.deleteMapping(uid);
        assertNull(container.queryMappingById(uid));
    }

    @Test
    void addMapping() {
        LayerMappingContainer container = new LayerMappingContainer();
        LayerMapping mapping = new LayerMapping(null, null, new LayerMapping.MappingPoint[0],
                LayerMapping.ActionType.SET, "hello", "world");

        int uid = container.addMapping(mapping);
        assertEquals(uid, mapping.getUid());

        {
            int uid2 = container.addMapping(mapping);
            assertEquals(uid, mapping.getUid());
            assertEquals(uid2, -1);
        }
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

        LayerMapping mapping = new LayerMapping(null, null, new LayerMapping.MappingPoint[0],
                LayerMapping.ActionType.SET, "hello", "world");

        int uid = container.addMapping(mapping);
        assertEquals(1,ran[0]);
        container.updateMapping(mapping.withNewPoints(new LayerMapping.MappingPoint[0]));
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

        LayerMapping mapping = new LayerMapping(null, null, new LayerMapping.MappingPoint[0],
                LayerMapping.ActionType.SET, "hello", "world");

        int uid = container.addMapping(mapping);
        container.subscribeToMapping(uid, runnable);

        assertEquals(0,ran[0]);
        container.updateMapping(mapping.withNewPoints(new LayerMapping.MappingPoint[0]));
        assertEquals(1,ran[0]);
        container.deleteMapping(uid);
        assertEquals(2,ran[0]);

        container.addMapping(new LayerMapping(null, null, new LayerMapping.MappingPoint[0],
                LayerMapping.ActionType.SET, "hello", "world"));
        assertEquals(2,ran[0]);

        container.unsubscribe(runnable);
        LayerMapping newMapping = mapping.withNewPoints(new LayerMapping.MappingPoint[0]);
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