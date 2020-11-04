package com.tokbox.android.annotations;

import android.graphics.Paint;

/**
 * Defines an object to be added to the annotations view.
 */
public class Annotatable {

    private AnnotationsView.Mode mode;
    private String data;

    private String cid;
    private AnnotatableType type;
    private AnnotationsPath path;

    private AnnotationsText text;
    private Paint paint;

    /**
     * Defines the type of the objects to be added
     */
    public static enum AnnotatableType {
        PATH,
        TEXT
    }

    /**
     * Constructor
     * @param mode The AnnotationsView.Mode
     * @param path The path to be added
     * @param paint the style of the annotation
     * @param cid the connection id
     */
    public Annotatable(AnnotationsView.Mode mode, AnnotationsPath path, Paint paint, String cid) throws Exception{
        if ( cid == null || paint == null || path == null || mode == null || cid.isEmpty() ) {
            throw  new Exception ("Parameters (cid, paint, path or mode) cannot be null.");
        }

        this.cid = cid;
        this.mode = mode;
        this.path = path;
        this.paint = paint;
    }

    /**
     * Constructor
     * @param mode The AnnotationsView.Mode
     * @param text The text to be added
     * @param paint the style of the annotation
     * @param cid the connection id
     */
    public Annotatable(AnnotationsView.Mode mode, AnnotationsText text, Paint paint, String cid) throws Exception {
        if ( cid == null || paint == null || text == null || mode == null || cid.isEmpty() ) {
            throw  new Exception ("Parameters (cid, paint, text or mode) cannot be null.");
        }

        this.cid = cid;
        this.mode = mode;
        this.text = text;
        this.paint = paint;
    }

    /**
     * Sets the AnnotationsView.Mode of the annotatable object
     * @param mode
     * @throws Exception
     */
    public void setMode(AnnotationsView.Mode mode) throws Exception{

        if (mode == null ) {
            throw  new Exception ("Mode cannot be null.");
        }
        this.mode = mode;
    }

    /**
     * Sets the data of the annotatable object
     * @param data
     * @throws Exception
     */
    public void setData(String data) throws Exception{
        if (data == null ) {
            throw  new Exception ("Data cannot be null.");
        }
        this.data = data;
    }

    /**
     * Sets the AnnotatableType of the annotatable object
     * @param type
     */
    public void setType(AnnotatableType type) {
        this.type = type;
    }

    /**
     * Returns the AnnotationsView.Mode
     */
    public AnnotationsView.Mode getMode() {
        return mode;
    }

    /**
     * Returns the data
     */
    public String getData() {
        return data;
    }

    /**
     * Returns the AnnotationsPach
     */
    public AnnotationsPath getPath() {
        return path;
    }

    /**
     * Returns the AnnotatableType
     */
    public AnnotatableType getType() {
        return type;
    }

    /**
     * Returns the connectionId
     */
    public String getCId() {
        return cid;
    }

    /**
     * Returns the AnnotationsText
     */
    public AnnotationsText getText() {
        return text;
    }

    /**
     * Returns the Paint
     */
    public Paint getPaint() {
        return paint;
    }

}

