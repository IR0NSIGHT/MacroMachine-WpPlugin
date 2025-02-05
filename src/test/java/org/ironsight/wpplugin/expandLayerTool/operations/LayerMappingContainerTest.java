package org.ironsight.wpplugin.expandLayerTool.operations;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LayerMappingContainerTest {

    @Test
    void updateMapping() {
        LayerMappingContainer container = new LayerMappingContainer();
        LayerMapping mapping = container.addMapping();

        MappingPoint[] newPoints = new MappingPoint[17];
        Arrays.fill(newPoints, new MappingPoint(10, 20));
        LayerMapping newMapping = mapping.withNewPoints(newPoints);
        assertNotEquals(newMapping, mapping);
        assertEquals(17, newMapping.getMappingPoints().length);
        assertEquals(newMapping.getUid(), mapping.getUid());

        container.updateMapping(newMapping);

        assertSame(newMapping, container.queryById(mapping.getUid()));

    }

    @Test
    void deleteMapping() {
        LayerMappingContainer container = new LayerMappingContainer();

        UUID uid = container.addMapping().getUid();
        assertEquals(uid, container.queryById(uid).getUid());

        container.deleteMapping(uid);
        assertNull(container.queryById(uid));
    }

    @Test
    void addMapping() {
        LayerMappingContainer container = new LayerMappingContainer();
        UUID uid = container.addMapping().getUid();
        assertEquals(uid, container.queryById(uid).getUid());

        {
            UUID uid2 = container.addMapping().getUid();
            assertNotEquals(uid, uid2);
        }
    }

    @Test
    void saveLoad() {
        LayerMappingContainer container = new LayerMappingContainer();
        LayerMapping saved = container.addMapping().withName("hello i am a test mapping");
        container.updateMapping(saved);
        assertEquals(saved, container.queryById(saved.getUid()));

        container.filePath = System.getProperty("user.dir") + "/test_saves.txt";
        container.writeToFile();
        container.readFromFile();

        LayerMapping loaded = container.queryById(saved.getUid());
        assertEquals(saved, loaded);
    }

    @Test
    void subscribe() {
        final int[] ran = {0};
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ran[0]++;
            }
        };
        assertEquals(0, ran[0]);
        LayerMappingContainer container = new LayerMappingContainer();
        container.subscribe(runnable);

        LayerMapping mapping = container.addMapping();

        assertEquals(1, ran[0]);
        container.updateMapping(mapping.withNewPoints(new MappingPoint[]{new MappingPoint(10, 20)}));
        assertEquals(2, ran[0]);
        container.deleteMapping(mapping.getUid());
        assertEquals(3, ran[0]);

        container.unsubscribe(runnable);
        container.addMapping();
        assertEquals(3, ran[0]);
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
                ran[0]++;
            }
        };
        assertEquals(0, ran[0]);
        LayerMappingContainer container = new LayerMappingContainer();

        LayerMapping mapping = container.addMapping();
        container.subscribeToMapping(mapping.getUid(), runnable);

        assertEquals(0, ran[0]);
        container.updateMapping(mapping.withNewPoints(new MappingPoint[]{new MappingPoint(1, 2)}));
        assertEquals(1, ran[0]);
        container.deleteMapping(mapping.getUid());
        assertEquals(2, ran[0]);

        container.addMapping();
        assertEquals(2, ran[0], "update by UID ran for unrelated new mapping.");

        container.unsubscribe(runnable);
        LayerMapping newMapping = mapping.withNewPoints(new MappingPoint[0]);
        assertEquals(newMapping.getUid(), mapping.getUid(), "update ran after unsubscribing from UID");
        container.addMapping();
        assertEquals(2, ran[0], "update by UID ran for unrelated new mapping.");
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