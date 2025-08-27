package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import org.junit.jupiter.api.Test;

import static org.ironsight.wpplugin.macromachine.Layers.CityBuilder.CityInfoDatabase.NO_DATA;
import static org.junit.jupiter.api.Assertions.*;

class CityInfoDatabaseTest {

    @Test
    void deleteAllWithValue() {
        CityInfoDatabase db = new CityInfoDatabase();
        db.setDataAt(10,10,57);
        db.setDataAt(10,13,50);
        db.setDataAt(120,13,50);
        db.setDataAt(1400,13,50);

        assertEquals(57,db.getDataAt(10,10));
        assertEquals(50,db.getDataAt(10,13));
        assertEquals(50,db.getDataAt(120,13));
        assertEquals(50,db.getDataAt(1400,13));

        boolean deletedSome= db.deleteAllWithValue(50,0xFFFFFFFF);
        assertTrue(deletedSome);
        assertEquals(57,db.getDataAt(10,10));
        assertEquals(NO_DATA,db.getDataAt(10,13));
        assertEquals(NO_DATA,db.getDataAt(120,13));
        assertEquals(NO_DATA,db.getDataAt(1400,13));
    }

    @Test
    void isEmpty() {
        CityInfoDatabase db = new CityInfoDatabase();
        assertTrue(db.isEmpty());

        db.setDataAt(100,200,300);
        assertFalse(db.isEmpty());

        db.setDataAt(100,200,NO_DATA);
        assertTrue(db.isEmpty());
    }

    @Test
    void getTileData() {
    }

    @Test
    void setGetDataAt() {
        CityInfoDatabase db = new CityInfoDatabase();
        assertTrue(db.isEmpty());
        assertEquals(CityInfoDatabase.NO_DATA, db.getDataAt(15,16));

        // add data
        db.setDataAt(15,16, 37);
        assertEquals(37, db.getDataAt(15,16));
        assertFalse(db.isEmpty());

        //mutate data
        db.setDataAt(15,16, 12345);
        assertEquals(12345, db.getDataAt(15,16));
        assertFalse(db.isEmpty());

        //remove data
        db.setDataAt(15,16, NO_DATA);
        assertEquals(NO_DATA, db.getDataAt(15,16));
        assertTrue(db.isEmpty());
    }
}