package com.opentok.accelerator.annotation;

import java.util.ArrayList;

/**
 * Defines the annotations manager
 */
public class AnnotationsManager {

    private ArrayList<Annotatable> mAnnotatableList;
    protected final String SIGNAL_TYPE = "annotations";

    /**
     * Constructor
     */
    public AnnotationsManager(){
        mAnnotatableList = new ArrayList<Annotatable>();
    }

    /**
     * Adds a new Annotatable object to the list
     */
    public void addAnnotatable(Annotatable annotatable) throws IllegalArgumentException {
        if (annotatable == null) {
            throw new IllegalArgumentException("Annotatable cannot be null.");
        }
        mAnnotatableList.add(annotatable);
        if (annotatable.getPath() != null) {
            annotatable.setType(Annotatable.AnnotatableType.PATH);
        } else {
            if (annotatable.getText() != null) {
                annotatable.setType(Annotatable.AnnotatableType.TEXT);
            }
        }
    }

    /**
     * Returns the list of the current Annotatable objects
     */
    public ArrayList<Annotatable> getAnnotatableList() {
        return mAnnotatableList;
    }

}