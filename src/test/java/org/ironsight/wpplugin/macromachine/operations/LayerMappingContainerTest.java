package org.ironsight.wpplugin.macromachine.operations;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class LayerMappingContainerTest {

    @Test
    void updateMapping() {
        MappingActionContainer container = new MappingActionContainer(System.getProperty("user.dir") + "/TestActions" +
                ".json");
        MappingAction mapping = container.addMapping();

        {
            MappingPoint[] newPoints =
                    IntStream.range(0, 17).mapToObj(i -> new MappingPoint(i, 3)).toArray(MappingPoint[]::new);
            MappingAction newMapping = mapping.withNewPoints(newPoints);
            assertNotEquals(newMapping, mapping);
            assertEquals(17, newMapping.getMappingPoints().length);
            assertEquals(newMapping.getUid(), mapping.getUid());

            container.updateMapping(newMapping, f -> {});
            assertSame(newMapping, container.queryById(mapping.getUid()));
        }


        // update mapping but use mulitple points with same input.
        {
            MappingPoint[] newPoints = new MappingPoint[17];
            Arrays.fill(newPoints, new MappingPoint(0, 3));
            MappingAction newMapping = mapping.withNewPoints(newPoints);
            assertNotEquals(newMapping, mapping);
            assertEquals(1, newMapping.getMappingPoints().length);
            assertEquals(newMapping.getUid(), mapping.getUid());

            container.updateMapping(newMapping, f -> {});


            assertSame(newMapping, container.queryById(mapping.getUid()));
        }
    }

    @Test
    void deleteMapping() {
        MappingActionContainer container = new MappingActionContainer(System.getProperty("user.dir") + "/TestActions" +
                ".json");

        UUID uid = container.addMapping().getUid();
        assertEquals(uid, container.queryById(uid).getUid());

        container.deleteMapping(uid);
        assertNull(container.queryById(uid));
    }

    @Test
    void addMapping() {
        MappingActionContainer container = new MappingActionContainer(System.getProperty("user.dir") + "/TestActions" +
                ".json");
        UUID uid = container.addMapping().getUid();
        assertEquals(uid, container.queryById(uid).getUid());

        {
            UUID uid2 = container.addMapping().getUid();
            assertNotEquals(uid, uid2);
        }
    }

    @Test
    void saveLoad() {
        MappingActionContainer container = new MappingActionContainer(System.getProperty("user.dir") + "/TestActions" +
                ".json");
        MappingActionContainer.SetInstance(container);
                MappingAction saved = container.addMapping().withName("hello i am a test mapping");
        container.updateMapping(saved, f -> {});

        assertEquals(saved, container.queryById(saved.getUid()));

        container.setFilePath(System.getProperty("user.dir") + "/test_saves.txt");
        container.writeToFile();
        container.readFromFile();

        MappingActionContainer newContainer = new MappingActionContainer(System.getProperty("user.dir") + "/TestActions" +
                ".json");
        newContainer.setFilePath(container.getFilePath());
        newContainer.readFromFile();
        MappingAction loaded = newContainer.queryById(saved.getUid());
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
        MappingActionContainer container = new MappingActionContainer(System.getProperty("user.dir") + "/TestActions" +
                ".json");
        container.subscribe(runnable);

        MappingAction mapping = container.addMapping();

        assertEquals(1, ran[0]);
        container.updateMapping(mapping.withNewPoints(new MappingPoint[]{new MappingPoint(10, 7)}), f-> {});
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
        MappingActionContainer container = new MappingActionContainer(System.getProperty("user.dir") + "/TestActions" +
                ".json");

        MappingAction mapping = container.addMapping();
        container.subscribeToMapping(mapping.getUid(), runnable);

        assertEquals(0, ran[0]);
        container.updateMapping(mapping.withNewPoints(new MappingPoint[]{new MappingPoint(1, 2)}), f-> {});
        assertEquals(1, ran[0]);
        container.deleteMapping(mapping.getUid());
        assertEquals(2, ran[0]);

        container.addMapping();
        assertEquals(2, ran[0], "update by UID ran for unrelated new mapping.");

        container.unsubscribe(runnable);
        MappingAction newMapping = mapping.withNewPoints(new MappingPoint[0]);
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